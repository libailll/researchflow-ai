import json
import logging
import threading

import httpx
import pika
from pydantic import ValidationError

from app.core.config import Settings
from app.models.document import DocumentParseMessage
from app.services.document_parser import DocumentParser
from app.services.java_client import JavaDocumentClient
from app.worker.message_recovery import dead_letter_invalid, retry_or_dead_letter, will_dead_letter

logger = logging.getLogger(__name__)


class DocumentConsumer:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None
        self.connected = False

    def start(self) -> None:
        if self._thread and self._thread.is_alive():
            return
        self._thread = threading.Thread(target=self._run, name="document-parse-worker", daemon=True)
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
                    channel.queue_declare(queue=self.settings.document_parse_queue, passive=True)
                    channel.confirm_delivery()
                    channel.basic_qos(prefetch_count=1)
                    self.connected = True
                    logger.info("Listening for document messages on queue %s", self.settings.document_parse_queue)
                    for method, properties, body in channel.consume(
                        self.settings.document_parse_queue,
                        inactivity_timeout=1,
                        auto_ack=False,
                    ):
                        if self._stop.is_set():
                            break
                        if method is None:
                            continue
                        self._handle(channel, method, properties, body)
                    channel.cancel()
            except (pika.exceptions.AMQPError, OSError) as exc:
                self.connected = False
                if not self._stop.is_set():
                    logger.warning("RabbitMQ unavailable, retrying in 5 seconds: %s", exc)
                    self._stop.wait(5)
            finally:
                self.connected = False

    def _handle(self, channel, method, properties, body: bytes) -> None:
        client = JavaDocumentClient(self.settings.java_api_base_url, self.settings.ai_internal_token)
        try:
            message = DocumentParseMessage.model_validate(json.loads(body))
            logger.info("Parsing document %s from %s", message.document_id, message.file_path)
            client.mark_processing(message.document_id)
            parser = DocumentParser(self.settings.chunk_size, self.settings.chunk_overlap)
            chunks = parser.parse_and_chunk(message.file_path)
            client.save_parsed(message.document_id, chunks)
            channel.basic_ack(method.delivery_tag)
            logger.info("Document %s parsed into %s chunks", message.document_id, len(chunks))
        except (json.JSONDecodeError, ValidationError) as exc:
            logger.error("Dead-lettering invalid document message: %s", exc)
            dead_letter_invalid(
                channel, method, properties, body,
                queue_name=self.settings.document_parse_queue,
                settings=self.settings,
                error=exc,
                logger=logger,
            )
        except (httpx.HTTPError, ConnectionError) as exc:
            self._report_final_failure(client, locals().get("message"), properties, exc)
            retry_or_dead_letter(
                channel, method, properties, body,
                queue_name=self.settings.document_parse_queue,
                settings=self.settings,
                error=exc,
                logger=logger,
            )
        except Exception as exc:
            logger.exception("Document parsing failed")
            self._report_final_failure(client, locals().get("message"), properties, exc)
            retry_or_dead_letter(
                channel, method, properties, body,
                queue_name=self.settings.document_parse_queue,
                settings=self.settings,
                error=exc,
                logger=logger,
            )
        finally:
            client.close()

    def _report_final_failure(self, client, message, properties, error: Exception) -> None:
        if message is None or not will_dead_letter(properties, self.settings):
            return
        try:
            client.mark_failed(message.document_id, str(error) or error.__class__.__name__)
        except Exception as callback_error:
            logger.error("Could not update final parsing failure status: %s", callback_error)
