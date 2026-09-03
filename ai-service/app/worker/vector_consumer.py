import json
import logging
import threading

import httpx
import pika
from pydantic import ValidationError

from app.core.config import Settings
from app.models.document import DocumentVectorMessage
from app.services.java_client import JavaDocumentClient
from app.services.vector_store import get_vector_store
from app.worker.message_recovery import dead_letter_invalid, retry_or_dead_letter, will_dead_letter

logger = logging.getLogger(__name__)


class VectorConsumer:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None
        self.connected = False

    def start(self) -> None:
        if self._thread and self._thread.is_alive():
            return
        self._thread = threading.Thread(target=self._run, name="document-vector-worker", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._stop.set()
        if self._thread:
            self._thread.join(timeout=8)

    def _run(self) -> None:
        credentials = pika.PlainCredentials(self.settings.rabbitmq_username, self.settings.rabbitmq_password)
        parameters = pika.ConnectionParameters(
            host=self.settings.rabbitmq_host,
            port=self.settings.rabbitmq_port,
            virtual_host=self.settings.rabbitmq_vhost,
            credentials=credentials,
            heartbeat=30,
            blocked_connection_timeout=30,
        )
        while not self._stop.is_set():
            try:
                with pika.BlockingConnection(parameters) as connection:
                    channel = connection.channel()
                    channel.queue_declare(queue=self.settings.document_vectorize_queue, passive=True)
                    channel.queue_declare(queue=self.settings.document_vector_delete_queue, passive=True)
                    channel.confirm_delivery()
                    channel.basic_qos(prefetch_count=1)
                    channel.basic_consume(self.settings.document_vectorize_queue, self._handle_vectorize, auto_ack=False)
                    channel.basic_consume(self.settings.document_vector_delete_queue, self._handle_delete, auto_ack=False)
                    self.connected = True
                    logger.info(
                        "Listening for vector messages on queues %s and %s",
                        self.settings.document_vectorize_queue,
                        self.settings.document_vector_delete_queue,
                    )
                    while not self._stop.is_set():
                        connection.process_data_events(time_limit=1)
            except (pika.exceptions.AMQPError, OSError) as exc:
                self.connected = False
                if not self._stop.is_set():
                    logger.warning("RabbitMQ unavailable for vector worker, retrying in 5 seconds: %s", exc)
                    self._stop.wait(5)
            finally:
                self.connected = False

    def _handle_vectorize(self, channel, method, properties, body: bytes) -> None:
        client = JavaDocumentClient(self.settings.java_api_base_url, self.settings.ai_internal_token)
        try:
            message = DocumentVectorMessage.model_validate(json.loads(body))
            logger.info("Vectorizing document %s", message.document_id)
            client.mark_vector_processing(message.document_id)
            document = client.get_vector_data(message.document_id)
            get_vector_store(self.settings).index_document(document)
            client.mark_vector_success(message.document_id)
            channel.basic_ack(method.delivery_tag)
            logger.info("Document %s vectorized with %s chunks", message.document_id, len(document.chunks))
        except (json.JSONDecodeError, ValidationError) as exc:
            logger.error("Dead-lettering invalid vector message: %s", exc)
            dead_letter_invalid(
                channel, method, properties, body,
                queue_name=self.settings.document_vectorize_queue,
                settings=self.settings,
                error=exc,
                logger=logger,
            )
        except (httpx.HTTPError, ConnectionError) as exc:
            self._report_final_vector_failure(client, locals().get("message"), properties, exc)
            retry_or_dead_letter(
                channel, method, properties, body,
                queue_name=self.settings.document_vectorize_queue,
                settings=self.settings,
                error=exc,
                logger=logger,
            )
        except Exception as exc:
            logger.exception("Document vectorization failed")
            self._report_final_vector_failure(client, locals().get("message"), properties, exc)
            retry_or_dead_letter(
                channel, method, properties, body,
                queue_name=self.settings.document_vectorize_queue,
                settings=self.settings,
                error=exc,
                logger=logger,
            )
        finally:
            client.close()

    def _report_final_vector_failure(self, client, message, properties, error: Exception) -> None:
        if message is None or not will_dead_letter(properties, self.settings):
            return
        try:
            client.mark_vector_failed(message.document_id, str(error) or error.__class__.__name__)
        except Exception as callback_error:
            logger.error("Could not update final vector failure status: %s", callback_error)

    def _handle_delete(self, channel, method, properties, body: bytes) -> None:
        try:
            message = DocumentVectorMessage.model_validate(json.loads(body))
            get_vector_store(self.settings).delete_document(message.document_id)
            channel.basic_ack(method.delivery_tag)
            logger.info("Deleted vectors for document %s", message.document_id)
        except (json.JSONDecodeError, ValidationError) as exc:
            logger.error("Dead-lettering invalid vector delete message: %s", exc)
            dead_letter_invalid(
                channel, method, properties, body,
                queue_name=self.settings.document_vector_delete_queue,
                settings=self.settings,
                error=exc,
                logger=logger,
            )
        except Exception as exc:
            retry_or_dead_letter(
                channel, method, properties, body,
                queue_name=self.settings.document_vector_delete_queue,
                settings=self.settings,
                error=exc,
                logger=logger,
            )
