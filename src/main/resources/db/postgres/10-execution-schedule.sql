-- The execution schedule is the drawing schedule again on its own row: drawing_schedules
-- now holds one row per (lead, kind). Hibernate added the kind column but cannot drop the
-- unique index it once put on lead_id alone, which is what kept a lead to one timeline.
-- Idempotent; applied automatically on every boot by SqlMigrationRunner.

DO $$
DECLARE c text;
BEGIN
  IF to_regclass('public.drawing_schedules') IS NULL THEN
    RETURN;
  END IF;

  ALTER TABLE drawing_schedules ADD COLUMN IF NOT EXISTS kind varchar(20);
  UPDATE drawing_schedules SET kind = 'DRAWING' WHERE kind IS NULL;

  -- Every existing row is a drawing timeline, so a unique constraint on lead_id alone
  -- would now reject the execution one. Hibernate named it, so find it by shape.
  FOR c IN
    SELECT con.conname
    FROM pg_constraint con
    WHERE con.conrelid = 'drawing_schedules'::regclass
      AND con.contype = 'u'
      AND con.conkey = ARRAY[(SELECT attnum FROM pg_attribute
                              WHERE attrelid = 'drawing_schedules'::regclass
                                AND attname = 'lead_id')]
  LOOP
    EXECUTE format('ALTER TABLE drawing_schedules DROP CONSTRAINT %I', c);
  END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_drawing_schedules_lead_kind
  ON drawing_schedules (lead_id, kind);
