-- The LOCKED brief status (docs/USER_JOURNEY_TESTING.md §13): Hibernate's
-- auto-update won't widen an existing CHECK constraint, so it is rebuilt here.
-- Applied automatically on every boot by SqlMigrationRunner (PostgreSQL only).

ALTER TABLE requirement_forms DROP CONSTRAINT IF EXISTS requirement_forms_status_check;
ALTER TABLE requirement_forms ADD CONSTRAINT requirement_forms_status_check
    CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'LOCKED'));
