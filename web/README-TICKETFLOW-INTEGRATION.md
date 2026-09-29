# TicketFlow Web - Gateway integration

## Environment
VITE_API_URL=http://localhost:8080/api

## Run
npm ci
npm run build
npm run dev

## Integrated flows
- Public published events and seat inventory through API Gateway
- Register / login / persistent JWT session
- Authenticated reservation and order confirmation
- Customer order history
- Kafka-backed notifications via GET /api/notifications/me
- ADMIN-only frontend routes for the existing administration area

The frontend only talks to the API Gateway. It no longer needs ports 8081-8086.
