# TicketFlow backend - Stage 6

Adds Transactional Outbox + Kafka OrderConfirmed events and an idempotent Notifications consumer.

## Build

```bash
cd services/orders-service && ./gradlew build
cd ../notifications-service && ./gradlew build
```

Kafka must be running from the existing root docker-compose.yml (`docker compose up -d kafka`).
Restart Orders and Notifications so Flyway applies V3/V2.

## Frontend-facing notification API

`GET /api/notifications/me` with `Authorization: Bearer <JWT>`.

## End-to-end check

Confirm a NEW RESERVED order. Then, after about 1-2 seconds:

```bash
curl -s http://localhost:8080/api/notifications/me \
  -H "Authorization: Bearer $TOKEN" | jq
```

Expected: one `ORDER_CONFIRMED` notification for the new order. Re-confirming an already confirmed order does not create another outbox event or notification.
