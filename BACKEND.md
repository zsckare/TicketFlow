# TicketFlow backend

Services: API Gateway 8080, Events 8081, Tickets 8082, Orders 8083, Users/Auth 8084, Payments 8085, Notifications 8086. Each persistent service owns its PostgreSQL database.

## Security
Users Service issues JWTs. In this delivery the gateway is a routing boundary; endpoint-level JWT enforcement across existing services is the next hardening step before production exposure. Never use the development JWT secret in production.

## Payments
Payments Service is provider-agnostic and currently uses a deterministic local success adapter. It implements idempotency keys and persistence but does not charge real cards. A Stripe/Mercado Pago adapter can replace the local adapter without moving payment ownership into Orders.

## Local validation
Run `./gradlew test` in each service. Start infrastructure with `docker compose up -d`. Kubernetes is intentionally not included.
