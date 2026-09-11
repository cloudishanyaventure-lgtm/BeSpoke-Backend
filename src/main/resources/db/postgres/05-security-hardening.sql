-- Ported from docs/sql/2026-09-11-security-hardening.sql — applied automatically
-- on every boot by SqlMigrationRunner (PostgreSQL only). Idempotent.

-- Tokens issued before this instant are rejected by JwtAuthFilter — stamped on
-- password reset and on customer account deletion.
ALTER TABLE users ADD COLUMN IF NOT EXISTS credentials_changed_at timestamp with time zone;
