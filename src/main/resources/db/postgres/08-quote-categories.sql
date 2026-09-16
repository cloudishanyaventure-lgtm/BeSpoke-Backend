-- The quote categories became the studio's real price list (14–15 Sep task sheets):
-- five per-square-foot design rates plus turnkey and modular, which are priced by hand.
-- Hibernate never widens a CHECK constraint, and the old values are no longer members of
-- the enum, so both the constraint and the rows are moved here.
-- Applied automatically on every boot by SqlMigrationRunner (PostgreSQL only).

ALTER TABLE quotes DROP CONSTRAINT IF EXISTS quotes_category_check;

-- "Design and Drawing" is what the studio calls Interior with MEP.
UPDATE quotes SET category = 'DESIGN_DRAWINGS_INTERIOR_MEP'   WHERE category = 'DESIGN_AND_DRAWINGS';
UPDATE quotes SET category = 'DESIGN_WITH_PROJECT_MANAGEMENT' WHERE category = 'DESIGN_AND_PMC';
UPDATE quotes SET category = 'MODULAR_FURNITURE'              WHERE category = 'MODULAR';

ALTER TABLE quotes ADD CONSTRAINT quotes_category_check
    CHECK (category IS NULL OR category IN (
        'DESIGN_DRAWINGS_STRUCTURE_INTERIOR',
        'DESIGN_DRAWINGS_STRUCTURE',
        'DESIGN_DRAWINGS_INTERIOR_MEP',
        'DESIGN_DRAWINGS_INTERIOR_NO_MEP',
        'DESIGN_WITH_PROJECT_MANAGEMENT',
        'TURNKEY',
        'MODULAR_FURNITURE'));
