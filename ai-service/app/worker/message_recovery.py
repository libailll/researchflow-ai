import logging
from typing import Any

import pika

from app.core.config import Settings

RETRY_HEADER = "x-retry-count"
LAST_ERROR_HEADER = "x-last-error"


def will_dead_letter(properties: pika.BasicProperties | None, settings: Settings) -> bool:
    return _retry_count(properties) + 1 > settings.rabbitmq_max_retries


def retry_or_dead_letter(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    method: Any,
    properties: pika.BasicProperties | None,
    body: bytes,
    *,
    queue_name: str,
    settings: Settings,
    error: Exception,
    logger: logging.Logger,
) -> bool:
    """Move a failed delivery to a bounded retry tier or its DLQ, then ack the original."""
    attempt = _retry_count(properties)
    next_attempt = attempt + 1
    headers = dict(properties.headers or {}) if properties else {}
    headers[RETRY_HEADER] = next_attempt
    headers[LAST_ERROR_HEADER] = _safe_error(error)

    if next_attempt <= settings.rabbitmq_max_retries:
        exchange = settings.rabbitmq_retry_exchange
        routing_key = f"{queue_name}.retry.{next_attempt}"
        destination = f"retry tier {next_attempt}/{settings.rabbitmq_max_retries}"
    else:
        exchange = settings.rabbitmq_dead_letter_exchange
        routing_key = f"{queue_name}.dead"
        destination = "dead-letter queue"

    try:
        published = channel.basic_publish(
            exchange=exchange,
            routing_key=routing_key,
            body=body,
            properties=_copy_properties(properties, headers),
            mandatory=True,
        )
        if published is False:
            raise RuntimeError(f"RabbitMQ did not confirm routing to {routing_key}")
        channel.basic_ack(method.delivery_tag)
        logger.warning(
            "Moved failed message from %s to %s: %s",
            queue_name,
            destination,
            error,
        )
        return next_attempt > settings.rabbitmq_max_retries
    except Exception as publish_error:
        logger.error(
            "Could not move failed message from %s; preserving original delivery: %s",
            queue_name,
            publish_error,
        )
        channel.basic_nack(method.delivery_tag, requeue=True)
        return False


def dead_letter_invalid(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    method: Any,
    properties: pika.BasicProperties | None,
    body: bytes,
    *,
    queue_name: str,
    settings: Settings,
    error: Exception,
    logger: logging.Logger,
) -> None:
    """Keep malformed messages for inspection instead of silently discarding them."""
    headers = dict(properties.headers or {}) if properties else {}
    headers[LAST_ERROR_HEADER] = _safe_error(error)
    try:
        published = channel.basic_publish(
            exchange=settings.rabbitmq_dead_letter_exchange,
            routing_key=f"{queue_name}.dead",
            body=body,
            properties=_copy_properties(properties, headers),
            mandatory=True,
        )
        if published is False:
            raise RuntimeError("RabbitMQ did not confirm dead-letter routing")
        channel.basic_ack(method.delivery_tag)
    except Exception as publish_error:
        logger.error("Could not dead-letter invalid message; preserving original: %s", publish_error)
        channel.basic_nack(method.delivery_tag, requeue=True)


def _retry_count(properties: pika.BasicProperties | None) -> int:
    value = (properties.headers or {}).get(RETRY_HEADER, 0) if properties else 0
    try:
        return max(0, int(value))
    except (TypeError, ValueError):
        return 0


def _copy_properties(
    properties: pika.BasicProperties | None,
    headers: dict[str, Any],
) -> pika.BasicProperties:
    return pika.BasicProperties(
        content_type=properties.content_type if properties else "application/json",
        content_encoding=properties.content_encoding if properties else "utf-8",
        headers=headers,
        delivery_mode=2,
        correlation_id=properties.correlation_id if properties else None,
        message_id=properties.message_id if properties else None,
        timestamp=properties.timestamp if properties else None,
        type=properties.type if properties else None,
        app_id=properties.app_id if properties else "researchflow-ai-service",
    )


def _safe_error(error: Exception) -> str:
    text = str(error).strip() or error.__class__.__name__
    return text[:500]
