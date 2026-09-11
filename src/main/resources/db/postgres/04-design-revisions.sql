-- Ported from docs/sql/2026-09-11-design-revisions.sql — now applied automatically
-- on every boot by SqlMigrationRunner (PostgreSQL only, one transaction per file).
-- Legacy designs remain independent V1s. Idempotent: re-running is a no-op.

ALTER TABLE drawings ADD COLUMN IF NOT EXISTS revision_number integer NOT NULL DEFAULT 1;
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS row_version bigint NOT NULL DEFAULT 0;
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS previous_revision_id bigint REFERENCES drawings(id);
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS superseded_at timestamp with time zone;
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS uploaded_by_id bigint REFERENCES users(id);
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS customer_decided_by_id bigint REFERENCES users(id);
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS customer_decided_at timestamp with time zone;
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS finalized_by_id bigint REFERENCES users(id);
ALTER TABLE drawings ADD COLUMN IF NOT EXISTS finalized_at timestamp with time zone;

-- If Hibernate's ddl-auto got there first it may have left the columns nullable
-- with NULL rows — backfill and pin the constraints either way.
UPDATE drawings SET revision_number = 1 WHERE revision_number IS NULL;
UPDATE drawings SET row_version = 0 WHERE row_version IS NULL;
ALTER TABLE drawings ALTER COLUMN revision_number SET NOT NULL;
ALTER TABLE drawings ALTER COLUMN revision_number SET DEFAULT 1;
ALTER TABLE drawings ALTER COLUMN row_version SET NOT NULL;
ALTER TABLE drawings ALTER COLUMN row_version SET DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS drawings_one_successor ON drawings(previous_revision_id);

-- Hibernate schema-update cannot be relied on to expand existing enum check constraints.
ALTER TABLE drawings DROP CONSTRAINT IF EXISTS drawings_status_check;
ALTER TABLE drawings ADD CONSTRAINT drawings_status_check
    CHECK (status IN ('WIP', 'PENDING_APPROVAL', 'APPROVED', 'FINAL', 'CHANGES_REQUESTED'));
