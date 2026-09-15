-- Task-sheet 14 Sep 26 items 3, 10, 11, 13, 15. Hibernate's auto-update never
-- widens an existing CHECK constraint or relaxes a NOT NULL, so both are done here.
-- Applied automatically on every boot by SqlMigrationRunner (PostgreSQL only).

-- 3: the work-hub priorities the studio actually uses. LOW/NORMAL/HIGH stay
-- listed so tasks raised before this still read back.
ALTER TABLE staff_tasks DROP CONSTRAINT IF EXISTS staff_tasks_priority_check;
ALTER TABLE staff_tasks ADD CONSTRAINT staff_tasks_priority_check
    CHECK (priority IS NULL OR priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT',
                                            'ESCALATION', 'SITE_VISIT', 'COMPLETE_BY_TODAY'));

-- 10/11: the category is one choice per quote now, not one per line item. The old
-- per-item column is kept (history) but stops being written, so it must go nullable.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'quote_items' AND column_name = 'category') THEN
        EXECUTE 'ALTER TABLE quote_items ALTER COLUMN category DROP NOT NULL';
        EXECUTE 'ALTER TABLE quote_items DROP CONSTRAINT IF EXISTS quote_items_category_check';
    END IF;
END $$;

UPDATE quotes SET category = 'DESIGN_AND_DRAWINGS' WHERE category IS NULL;

-- 13/15: bank transfers are captured with their UTR, so the mode is named for what
-- the payer did rather than which rail the bank used.
ALTER TABLE invoice_payments DROP CONSTRAINT IF EXISTS invoice_payments_mode_check;
ALTER TABLE invoice_payments ADD CONSTRAINT invoice_payments_mode_check
    CHECK (mode IN ('UPI', 'NEFT', 'RTGS', 'CHEQUE', 'CASH', 'BANK_TRANSFER'));
