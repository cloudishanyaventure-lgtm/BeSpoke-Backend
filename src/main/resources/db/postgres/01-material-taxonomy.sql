-- Ported from docs/sql/2026-08-29-material-taxonomy.sql — now applied automatically on every boot by
-- SqlMigrationRunner (PostgreSQL only, one transaction per file, after the
-- seeders). Folds the 26 seeded material categories into the 14-category taxonomy.
-- Idempotent by construction: re-running is a no-op.

-- Material library: 26 seeded categories → the 14 top categories.
--
-- Safe to run on production, and safe to run twice: every statement keys off the slug it
-- is changing, so a second run finds nothing left to do. Wrapped in a transaction —
-- if any statement fails, nothing is applied.
--
-- Nothing is deleted. Materials and brand links are moved onto the surviving category,
-- the six categories outside the new taxonomy are hidden (active = false) with their
-- content intact, and the emptied merge sources are removed only after their materials
-- have been re-pointed.
--
--   psql "$DB_URL" -f 2026-08-29-material-taxonomy.sql
--
-- Before/after check:
--   SELECT sort_order, name, slug, active,
--          (SELECT count(*) FROM materials m WHERE m.category_id = c.id) AS materials
--   FROM material_categories c ORDER BY active DESC, sort_order;


-- ---------------------------------------------------------------------------
-- 1. Move materials off the categories that are being folded into another one.
-- ---------------------------------------------------------------------------
UPDATE materials m SET category_id = t.id
FROM material_categories t, material_categories s
WHERE m.category_id = s.id
  AND t.slug = 'plywood-wood-boards'
  AND s.slug IN ('laminates', 'veneers', 'acrylic-high-gloss');

UPDATE materials m SET category_id = t.id
FROM material_categories t, material_categories s
WHERE m.category_id = s.id
  AND t.slug = 'modular-furniture-hardware'
  AND s.slug IN ('handles-knobs');

UPDATE materials m SET category_id = t.id
FROM material_categories t, material_categories s
WHERE m.category_id = s.id
  AND t.slug = 'tiles-stone'
  AND s.slug IN ('quartz-countertops');

UPDATE materials m SET category_id = t.id
FROM material_categories t, material_categories s
WHERE m.category_id = s.id
  AND t.slug = 'electrical-smart-home'
  AND s.slug IN ('lighting');

-- ---------------------------------------------------------------------------
-- 2. Carry the brand associations across, then drop the old pairs.
--    (brand_id, category_id) may already exist, hence the NOT EXISTS guard.
-- ---------------------------------------------------------------------------
INSERT INTO material_brand_categories (brand_id, category_id)
SELECT DISTINCT bc.brand_id, t.id
FROM material_brand_categories bc
JOIN material_categories s ON s.id = bc.category_id
JOIN material_categories t ON t.slug = CASE
        WHEN s.slug IN ('laminates', 'veneers', 'acrylic-high-gloss') THEN 'plywood-wood-boards'
        WHEN s.slug = 'handles-knobs'                                 THEN 'modular-furniture-hardware'
        WHEN s.slug = 'quartz-countertops'                            THEN 'tiles-stone'
        WHEN s.slug = 'lighting'                                      THEN 'electrical-smart-home'
     END
WHERE NOT EXISTS (
        SELECT 1 FROM material_brand_categories x
        WHERE x.brand_id = bc.brand_id AND x.category_id = t.id);

DELETE FROM material_brand_categories bc
USING material_categories s
WHERE bc.category_id = s.id
  AND s.slug IN ('laminates', 'veneers', 'acrylic-high-gloss', 'handles-knobs',
                 'quartz-countertops', 'lighting');

-- ---------------------------------------------------------------------------
-- 3. Rename the survivors and set the running order to the new list.
--    Slugs only change where the category's scope changed — kitchen-accessories,
--    wall-finishes, doors-windows, flooring and ceiling-partition keep their URLs.
-- ---------------------------------------------------------------------------
UPDATE material_categories SET
  slug = 'plyboards-laminates', name = 'Plyboards & Laminates', sort_order = 1, active = true,
  tagline = 'The carcass and the surface stuck to it',
  description = 'Boards are the first specification decision on any interior job: they set the cost, the lifespan and how the joinery behaves in a damp Indian summer. What is pressed onto them decides how the job reads. This section runs from plywood by bonding grade and engineered boards like MDF and HDHMR, through high-pressure laminates organised the way designers actually shop — Brand, Collection, Design — to natural and reconstituted veneers, and the acrylic and PET panels used when a client wants mirror depth.'
WHERE slug = 'plywood-wood-boards';

UPDATE material_categories SET
  slug = 'modular-hardware', name = 'Modular Hardware', sort_order = 2, active = true,
  tagline = 'Hinges, drawers and channels — and the handles on top of them',
  description = 'Hardware is where a kitchen quietly succeeds or fails. Boards last; it is the hinge that sags and the channel that grinds after two years. This section follows the way the major brands structure their own catalogues — hinges, drawer systems and channels, each specified by load, opening angle and closing behaviour — and finishes with the handles and knobs the client actually touches every day.'
WHERE slug = 'modular-furniture-hardware';

UPDATE material_categories SET
  name = 'Kitchen Accessories', sort_order = 3, active = true
WHERE slug = 'kitchen-accessories';

UPDATE material_categories SET
  slug = 'tiles-stone-quartz', name = 'Tiles & Stone / Quartz', sort_order = 4, active = true,
  tagline = 'Every floor, wet wall and countertop starts here',
  description = 'Tiles carry more of an Indian project''s area than any other finish, and the difference between a ceramic and a full-body vitrified tile is the difference between a five-year floor and a twenty-year one. Natural stone follows, specified by origin, finish and porosity — and then the countertop, the most-used surface in the house, with engineered quartz, solid surface and sintered stone set side by side on slab size, thickness and price.'
WHERE slug = 'tiles-stone';

UPDATE material_categories SET
  slug = 'electrical-lights', name = 'Electrical & Lights', sort_order = 5, active = true,
  tagline = 'Switches, sockets, automation — and the layers of light above them',
  description = 'Switch plates are the most-touched hardware in a house and the easiest thing to get wrong at the end of a project. This section covers modular wiring devices by grade and the smart-home layer that has to be decided at first fix, then the lighting itself: a good scheme layers ambient, task and accent light on separate circuits, keeps colour temperature consistent, and never relies on a single ceiling fixture.'
WHERE slug = 'electrical-smart-home';

UPDATE material_categories SET
  slug = 'appliances-fans', name = 'Appliances & Fans', sort_order = 6, active = true
WHERE slug = 'kitchen-appliances';

UPDATE material_categories SET
  slug = 'paint-polish', name = 'Paint & Polish', sort_order = 7, active = true
WHERE slug = 'paints-coatings';

UPDATE material_categories SET
  slug = 'sanitary-bathing', name = 'Sanitary & Bathing', sort_order = 8, active = true
WHERE slug = 'bathroom-sanitary';

UPDATE material_categories SET
  slug = 'glass-door-slider', name = 'Glass — Door & Slider', sort_order = 9, active = true,
  tagline = 'Safety grades, sliding systems and the mirrors that stretch a room'
WHERE slug = 'glass-mirrors';

UPDATE material_categories SET name = 'Modern Flooring', sort_order = 10, active = true
WHERE slug = 'flooring';

UPDATE material_categories SET name = 'Wall Finishes', sort_order = 11, active = true
WHERE slug = 'wall-finishes';

UPDATE material_categories SET name = 'Ceiling', sort_order = 12, active = true
WHERE slug = 'ceiling-partition';

UPDATE material_categories SET name = 'Doors & Windows', sort_order = 13, active = true
WHERE slug = 'doors-windows';

UPDATE material_categories SET
  slug = 'locks-latches', name = 'Locks & Latches', sort_order = 14, active = true
WHERE slug = 'door-hardware-locks';

-- ---------------------------------------------------------------------------
-- 4. Remove the now-empty merge sources. The guard is the point: if step 1 left
--    anything behind, the row stays and you find out, instead of losing materials.
-- ---------------------------------------------------------------------------
DELETE FROM material_categories c
WHERE c.slug IN ('laminates', 'veneers', 'acrylic-high-gloss', 'handles-knobs',
                 'quartz-countertops', 'lighting')
  AND NOT EXISTS (SELECT 1 FROM materials m WHERE m.category_id = c.id);

-- ---------------------------------------------------------------------------
-- 5. Hide the six categories outside the new taxonomy. Their materials are kept —
--    flip active back to true in /admin to bring any of them back.
-- ---------------------------------------------------------------------------
-- Fixed positions, not `sort_order + 90`: a relative bump moves them further down the
-- list on every re-run, which is exactly the kind of thing that makes a script unsafe
-- to run twice.
UPDATE material_categories SET active = false, sort_order = v.position
FROM (VALUES ('adhesives-sealants', 101), ('acoustic-materials', 102),
             ('soft-furnishing', 103), ('furniture', 104),
             ('decorative-materials', 105), ('outdoor-balcony', 106)
     ) AS v(slug, position)
WHERE material_categories.slug = v.slug;
