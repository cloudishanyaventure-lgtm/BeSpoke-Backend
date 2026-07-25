# DesignConnect Backend

Spring Boot 3.3.x (Java 17, Gradle) backend for **DesignConnect** — an interior-design CRM
with a Livspace-style lead funnel: public site → signup-as-lead → requirement wizard →
quotes → won projects → invoices & payments.

## Run

```bash
createdb designconnect_crm        # once; JPA ddl-auto=update creates the tables
./gradlew bootRun                 # http://localhost:8080
```

Defaults (override via `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` env vars):
`jdbc:postgresql://localhost:5432/designconnect_crm`, username = current OS user, no password.
Production config in `application-prod.yml` requires `JWT_SECRET`.

> Gradle 8.14 needs a JDK ≤ 21 to launch. If your default `java` is newer:
> `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew bootRun`

## Seed accounts

Seeding is idempotent (only when the users table is empty). No dummy leads/quotes/projects —
empty states are intentional. The room-item catalog (291 items, 6 space types) seeds from
`src/main/resources/room-catalog.json` when its table is empty.

| Role     | Name          | Email                   | Password    |
|----------|---------------|-------------------------|-------------|
| ADMIN    | Prachi Khanna | admin@designconnect.in  | admin123    |
| DESIGNER | Aarti Sharma  | aarti@designconnect.in  | designer123 |
| DESIGNER | Rohan Verma   | rohan@designconnect.in  | designer123 |
| DESIGNER | Priya Nair    | priya@designconnect.in  | designer123 |

## Auth

JWT Bearer (HS256, 24h). `POST /api/auth/register` always creates a CUSTOMER **and** its
lead atomically (signing up IS the lead) and returns `{token, user, leadId}`.

## API

### Public (no auth)

| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/register` | `{name,email,phone,password,city,propertyType?,budgetBand?}` → `{token,user,leadId}` |
| POST | `/api/auth/login` | → `{token,user}` |
| POST | `/api/enquiries` | `{name,phone,email,city,propertyType?,budgetBand?,message?}` → creates ENQUIRY lead |
| GET  | `/api/catalog/room-items` | Room checklist catalog: `[{spaceType, categories:[{category, items}]}]` |
| GET  | `/api/public/designers` | Active designer cards `{id,name,title,city}` |
| GET  | `/actuator/health` | Liveness/readiness |

### Authenticated (any role)

| Method | Path | Notes |
|---|---|---|
| GET/PUT | `/api/me` | Own profile; PUT `{name?,phone?,city?}` |
| POST | `/api/uploads` | Multipart image ≤ 5MB → `{url}` |

### Customer (`/api/my/**`, role CUSTOMER, always scoped to own lead)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/my/journey` | Stage history, designer card, project + milestones, counts |
| GET | `/api/my/requirement-form` | 404 until started |
| PUT | `/api/my/requirement-form` | Upsert draft scalars; **409 once a quote is SENT/APPROVED** |
| PUT | `/api/my/requirement-form/rooms` | Replaces all rooms + selected items |
| POST | `/api/my/requirement-form/submit` | → SUBMITTED, rescores the lead |
| GET | `/api/my/quotes` | SENT/APPROVED/CHANGES_REQUESTED only |
| POST | `/api/my/quotes/{id}/decision` | `{decision: APPROVED\|CHANGES_REQUESTED, comment?}` (quote must be SENT) |
| GET | `/api/my/invoices` | Invoices + payments (drafts hidden) |
| GET/POST | `/api/my/messages` | Thread with the studio; GET marks staff messages read |

### Staff (ADMIN sees all; DESIGNER only their assigned leads/projects — others 404)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/leads?stage=&q=&assigned=` | Funnel list with score + formStatus |
| GET | `/api/leads/{id}` | `{lead, form, activities, quotes}` |
| POST | `/api/leads/{id}/activities` | `{type: NOTE\|CALL\|MEETING, body}` |
| PUT | `/api/leads/{id}/stage` | `{stage, reason?}`. Designers: CONTACTED..NEGOTIATION only. WON needs an assigned designer and auto-creates the project + 6 milestones; LOST logs the reason |
| PUT | `/api/leads/{id}/follow-up` | `{at}` (null clears) |
| GET | `/api/projects` | Flat summaries `{id,leadId,name,stage,health,clientName,designerName,budget(admin),startDate,targetDate,completionPct}` |
| GET | `/api/projects/{id}` | `{project, milestones, leadId, invoices(admin only)}` |
| PUT | `/api/projects/{id}` | `{stage?,health?,budget?(admin),startDate?,targetDate?}` → same detail shape |
| PUT | `/api/projects/{id}/milestones` | Upsert list; missing ids are deleted → same detail shape |
| GET | `/api/clients` / `/api/clients/{id}` | Designers: own clients only, no financials |
| GET | `/api/messages/threads` | Leads with messages or still open (scoped) |
| GET/POST | `/api/messages/{leadId}` | GET marks the other side read |
| GET | `/api/dashboard` | Admin: revenue/outstanding/pipeline/funnel/team load. Designer: own queue |
| GET | `/api/team` | Directory (workload counts admin-only) |

### Admin only

| Method | Path | Notes |
|---|---|---|
| POST | `/api/leads` | Manual capture `{name,phone,email,city,propertyType?,budgetBand?,source}` |
| PUT | `/api/leads/{id}/assign` | `{designerId}` |
| POST | `/api/quotes` | `{leadId,title,validUntil?,items[{category,description,qty,rate,gstPct}]}` → DRAFT |
| PUT | `/api/quotes/{id}` | DRAFT only |
| POST | `/api/quotes/{id}/send` · `/api/quotes/{id}/revise` | Send to customer · copy to DRAFT v+1 |
| GET | `/api/quotes?status=` / `/api/quotes/{id}` | |
| POST | `/api/invoices` | `{projectId,milestoneId?,title,amount,gstPct,dueDate?}` → number `INV-000N` |
| POST | `/api/invoices/{id}/send` | DRAFT → SENT |
| POST | `/api/invoices/{id}/payments` | `{amount,mode: UPI\|NEFT\|RTGS\|CHEQUE\|CASH,reference?,paidAt?}`; full payment → PAID |
| GET | `/api/invoices?status=` | Accepts derived states PARTIALLY_PAID / OVERDUE too |
| POST | `/api/team` | Create DESIGNER/ADMIN `{name,email,phone?,city?,role,title,dept,password}` |
| PUT | `/api/team/{userId}` | `{title?,dept?,active?}`; last active admin cannot be deactivated |

## Domain notes

- **Lead stages:** NEW_INQUIRY → CONTACTED → SITE_VISIT → PROPOSAL_SENT → NEGOTIATION → WON / LOST.
- **Score:** base 20 + budget band (5–30) + property type (6–12) + form draft 10 / submitted 25, capped at 98.
- **Project stages:** DESIGN_BRIEF, CONCEPT_DESIGN, DESIGN_APPROVAL, PROCUREMENT, EXECUTION, SNAG_HANDOVER (also the six default milestones on WON).
- **Invoice status:** DRAFT/SENT/PAID persisted; PARTIALLY_PAID and OVERDUE derived from payments/dueDate.
- **Messages:** one thread per lead; it carries into the project after WON.
