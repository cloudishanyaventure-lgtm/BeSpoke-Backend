# BeSpoke Backend

Spring Boot 3.3.x (Java 17, Gradle) backend for **BeSpoke**, an interior-design marketplace connecting customers with interior designers.

## Prerequisites

- Java 17 (JDK)
- Gradle 8+ (or use `gradle wrapper` to generate a wrapper)
- PostgreSQL 14+ running on `localhost:5432` with user `postgres` / password `postgres`

## Database setup

Create the database once (JPA `ddl-auto=update` creates the tables automatically):

```bash
createdb -U postgres BeSpoke
# or: psql -U postgres -c "CREATE DATABASE BeSpoke;"
```

Connection settings live in `src/main/resources/application.yml` (`localhost:5432/BeSpoke`, `postgres`/`postgres`). If you want environment-specific overrides, create an `application-dev.yml` next to it and run with `--spring.profiles.active=dev`.

## Run

```bash
gradle bootRun
```

The API starts on `http://localhost:8080`. CORS is open for `http://localhost:3000` (the Next.js frontend).

### Database migrations & seed data (Flyway)

Schema and seed data are managed by **Flyway** — versioned SQL in
`src/main/resources/db/migration`, applied automatically on every startup:

- `V1__baseline_schema.sql` — the full schema (runs once on a fresh database).
- `V2__seed_data.sql` — accounts, designers + profiles + portfolio, the 18-item
  catalogue, and enquiry leads. Idempotent, so it is safe to re-run.

A fresh run is completely hands-off: create the empty DB, start the app, and
Flyway builds the schema and seeds everything. Emails are stored **lowercase**
(the login normalises input with `toLowerCase()`), so sign in with lowercase.

| Role     | Email                | Password    |
|----------|----------------------|-------------|
| ADMIN    | admin@bespoke.in     | admin123    |
| DESIGNER | aarti@bespoke.in     | designer123 |
| DESIGNER | maya@bespoke.in      | designer123 |
| CUSTOMER | riya@example.com     | test1234    |

Seeded accounts: 1 admin, 8 designers (`designer123`), 5 customers (`test1234`),
18 catalogue packages, 3 enquiry leads. To reseed from scratch:
`dropdb BeSpoke && createdb -O postgres BeSpoke` then restart.

## Lead routing (core business logic)

Every purchase or enquiry produces a **Lead** (a.k.a. project). Routing rules:

1. **Paid order WITH a designer selected** → lead is auto-assigned to that designer with status `ASSIGNED`, pending the designer's approval.
2. **Paid order WITHOUT a designer** → lead lands in the admin queue with status `UNASSIGNED_PAID`; an admin assigns a designer (`POST /api/admin/leads/{id}/assign/{designerId}`), moving it to `ASSIGNED`.
3. **Free enquiry** (no payment, `POST /api/enquiries`, anonymous allowed) → admin queue with status `ENQUIRY` for triage/assignment.

When the designer **approves** an `ASSIGNED` lead it becomes `APPROVED` and a **chat thread opens** between customer and designer. If the designer **rejects**, the lead returns to the admin queue as `REJECTED` and can be re-assigned.

Lead status flow: `ENQUIRY | UNASSIGNED_PAID → ASSIGNED → APPROVED → IN_PROGRESS → COMPLETED`, with `REJECTED` as the designer-decline branch.

### Payments

Payments go **directly to the designer**: each paid order creates a `Payment` with `payeeType=DESIGNER` and `payoutStatus=PENDING` (released to the designer out-of-band). Payments are processed through the `PaymentService` interface; the bundled `MockPaymentProvider` always succeeds and issues a `mock_pay_*` reference. **Razorpay** is the intended real provider — implement `PaymentService` against Razorpay Orders/Route and swap the bean.

## Endpoints

### Auth (public)
| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/register` | `{name, email, password, role: CUSTOMER\|DESIGNER}` → JWT + role |
| POST | `/api/auth/login` | `{email, password}` → JWT + role |

### Catalog (public)
| Method | Path |
|---|---|
| GET | `/api/services` (optional `?category=KITCHEN`) |
| GET | `/api/services/{id}` |
| GET | `/api/designers` |
| GET | `/api/designers/{id}` |

### Enquiry (public, anonymous allowed)
| Method | Path | Notes |
|---|---|---|
| POST | `/api/enquiries` | `{name, email, phone, message, category?}` → Lead `ENQUIRY` |

### Customer (role CUSTOMER)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/cart` | current cart |
| POST | `/api/cart` | `{serviceId, quantity}` |
| DELETE | `/api/cart/{itemId}` | remove one item |
| DELETE | `/api/cart` | clear cart |
| POST | `/api/checkout` | `{items:[{serviceId, quantity}], designerId?, address}` → Order + mock Payment + routed Lead |
| GET | `/api/my/orders` | my orders |
| GET | `/api/my/projects` | my leads/projects |

### Designer (role DESIGNER)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/designer/leads` | assigned to me, pending approval |
| POST | `/api/designer/leads/{id}/approve` | → `APPROVED`, opens chat thread |
| POST | `/api/designer/leads/{id}/reject` | → `REJECTED`, back to admin queue |
| GET | `/api/designer/projects` | approved / in-progress / completed |

### Admin (role ADMIN)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/leads?status=` | filter by lead status (omit for all) |
| POST | `/api/admin/leads/{id}/assign/{designerId}` | assign designer → `ASSIGNED` |
| GET | `/api/admin/overview` | counts (users, orders, leads by status) |

### Chat (any authenticated participant)
| Method | Path |
|---|---|
| GET | `/api/chat/threads` |
| GET | `/api/chat/threads/{id}/messages` |
| POST | `/api/chat/threads/{id}/messages` (`{content}`) |

**WebSocket:** STOMP endpoint at `/ws` (SockJS supported). Subscribe to `/topic/threads/{id}` to receive new `ChatMessageDto`s; optionally send to `/app/threads/{id}/send`. Messages posted over REST are broadcast to the same topic.

## Sample curl

Register a customer:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Neha Gupta","email":"neha@example.com","password":"secret123","role":"CUSTOMER"}'
```

Login (grab the `token` from the response):

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"neha@example.com","password":"secret123"}'
```

Checkout with a chosen designer (routing rule 1 — `designerId` is the designer's **user id**; omit `designerId` for rule 2):

```bash
TOKEN="<jwt from login>"
curl -X POST http://localhost:8080/api/checkout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"items":[{"serviceId":1,"quantity":1}],"designerId":2,"address":"12 MG Road, Bengaluru 560001"}'
```

Free enquiry (no auth needed, routing rule 3):

```bash
curl -X POST http://localhost:8080/api/enquiries \
  -H "Content-Type: application/json" \
  -d '{"name":"Amit","email":"amit@example.com","phone":"9876543210","message":"Need a kitchen quote","category":"KITCHEN"}'
```

## Package structure

```
com.BeSpoke
├── config        SecurityConfig, WebSocketConfig, SeedDataRunner
├── controller    Auth, Services, Designers, Cart, Checkout, Enquiry, Customer(my), DesignerLead, Admin, Chat (+ STOMP)
├── dto           request/response records (validated; never expose password hashes)
├── entity        User, DesignerProfile, DesignService, CartItem, Order(+Item), Lead, Payment, ChatThread, ChatMessage + enums
├── exception     GlobalExceptionHandler + typed exceptions (JSON errors)
├── repository    Spring Data JPA repositories
├── security      JwtService (jjwt 0.12), JwtAuthFilter, AppUserDetailsService
└── service       Auth, Catalog, Cart, Checkout, Lead, Chat, PaymentService + MockPaymentProvider
```
# BeSpoke-Backend
