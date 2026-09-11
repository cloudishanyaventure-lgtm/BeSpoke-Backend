-- Ported from docs/sql/2026-09-03-shop-catalogue.sql — now applied automatically on every boot by
-- SqlMigrationRunner (PostgreSQL only, one transaction per file, after the
-- seeders). Shop catalogue: 63 pieces + 6 design packages under vendor 'bespoke-living'.
-- Idempotent by construction: re-running is a no-op.

-- Shop catalogue seed: one listing per SHOP_SUBCATEGORY, so no rail entry is empty.
-- 63 pieces across the 11 rail categories, plus 6 whole-room packages for the
-- Designs tab (those carry no shop_category — they are not a rail entry).
--
-- Photography is Unsplash (free to use, and the same source frontend/lib/images.ts
-- already uses). Nothing here is scraped from a retailer's site.
--
-- RUN THE NEW BACKEND ONCE FIRST. products.shop_category and shop_sub_category are
-- new columns; Hibernate (ddl-auto: update) adds them on boot. Run this before that
-- and every statement fails with:
--   ERROR: column "shop_category" of relation "products" does not exist
--
-- Everything is attached to the vendor with slug 'bespoke-living'. Change that slug
-- below to file the catalogue under a different vendor. NOTE: the shop only shows
-- products from an active, KYC-VERIFIED vendor company — if nothing appears on the
-- site after running this, check that vendor's kyc_status.
--
-- Safe to run twice: each row is skipped if a product of the same name already exists
-- for that vendor. Wrapped in a transaction — if any statement fails, nothing applies.
--
--   psql "$DB_URL" -f 2026-09-03-shop-catalogue.sql
--
-- Check afterwards (expect 11 rail categories, 63 distinct sub-types):
--   SELECT shop_category, count(*) FROM products
--   WHERE shop_category IS NOT NULL GROUP BY 1 ORDER BY 1;
--
-- To undo — scoped to this catalogue only. Do NOT delete by image_url: the demo
-- products this platform ships with use Unsplash images too, and would go with it.
--   DELETE FROM products p USING companies c
--    WHERE p.company_id = c.id AND c.slug = 'bespoke-living'
--      AND p.shop_sub_category IS NOT NULL;


INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Ashwood 3-seater sofa', 'Solid ashwood frame, high-resilience foam, removable linen covers.', 'FURNITURE', 'LIVING_ROOM', 'Sofas & seating', '3-seater sofa',
       62000, 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Ashwood 3-seater sofa');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Kabir 2-seater loveseat', 'Compact two-seater for apartments — 152cm wide, fits a 2BHK living room.', 'FURNITURE', 'LIVING_ROOM', 'Sofas & seating', '2-seater sofa',
       41500, 'https://images.unsplash.com/photo-1698936061086-2bf99c7b9fc5?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Kabir 2-seater loveseat');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Meridian L-shaped sofa', 'Right-hand chaise, reversible. Seats five comfortably.', 'FURNITURE', 'LIVING_ROOM', 'Sofas & seating', 'L-shaped sofa',
       98000, 'https://images.unsplash.com/photo-1573866926487-a1865558a9cf?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Meridian L-shaped sofa');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Calma single recliner', 'Manual push-back recliner in pebble-grain leatherette.', 'FURNITURE', 'LIVING_ROOM', 'Sofas & seating', 'Recliner',
       34500, 'https://images.unsplash.com/photo-1512212621149-107ffe572d2f?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Calma single recliner');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Tara sofa cum bed', 'Folds flat to a 190cm double. Storage under the seat.', 'FURNITURE', 'LIVING_ROOM', 'Sofas & seating', 'Sofa cum bed',
       38900, 'https://images.unsplash.com/photo-1550581190-9c1c48d21d6c?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Tara sofa cum bed');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Nadia chaise lounge', 'Velvet upholstery on a powder-coated gold frame.', 'FURNITURE', 'LIVING_ROOM', 'Sofas & seating', 'Chaise lounge',
       29500, 'https://images.unsplash.com/photo-1484101403633-562f891dc89a?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Nadia chaise lounge');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Kesar woven pouffe', 'Hand-woven jute pouffe, doubles as a footrest or extra seat.', 'FURNITURE', 'LIVING_ROOM', 'Sofas & seating', 'Ottoman & pouffe',
       7200, 'https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Kesar woven pouffe');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Aravalli king bed', 'Sheesham frame, 183x198cm, slatted base — no box spring needed.', 'FURNITURE', 'MASTER_BEDROOM', 'Beds', 'King bed',
       78000, 'https://images.unsplash.com/photo-1560185128-e173042f79dd?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Aravalli king bed');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Aravalli queen bed', 'The king in a 152x198cm footprint, same sheesham frame.', 'FURNITURE', 'MASTER_BEDROOM', 'Beds', 'Queen bed',
       64000, 'https://images.unsplash.com/photo-1698517486200-e89403ea2738?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Aravalli queen bed');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Nilaya single bed', '91x190cm, engineered wood with a matte walnut finish.', 'FURNITURE', 'MASTER_BEDROOM', 'Beds', 'Single bed',
       26500, 'https://images.unsplash.com/photo-1750420556288-d0e32a6f517b?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Nilaya single bed');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Doon bunk bed', 'Powder-coated steel with an integrated ladder and guard rail.', 'FURNITURE', 'MASTER_BEDROOM', 'Beds', 'Bunk bed',
       44000, 'https://images.unsplash.com/photo-1678978866819-306ed8608e7f?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Doon bunk bed');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Vault hydraulic storage bed', 'Full-width hydraulic lift; holds two seasons of bedding.', 'FURNITURE', 'MASTER_BEDROOM', 'Beds', 'Storage bed',
       89000, 'https://images.unsplash.com/photo-1678786591418-2b107bdda369?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Vault hydraulic storage bed');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Mira upholstered bed', 'Channel-tufted headboard in bouclé, 120cm tall.', 'FURNITURE', 'MASTER_BEDROOM', 'Beds', 'Upholstered bed',
       82000, 'https://images.unsplash.com/photo-1560449752-ac541afdd6b5?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Mira upholstered bed');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Aravalli bedside table', 'Two drawers and an open shelf, matched to the Aravalli beds.', 'FURNITURE', 'MASTER_BEDROOM', 'Beds', 'Bedside table',
       12500, 'https://images.unsplash.com/photo-1731336250970-dc942b5e0746?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Aravalli bedside table');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Linea 3-door sliding wardrobe', 'Soft-close sliders, mirrored centre panel, 270cm wide.', 'FURNITURE', 'MASTER_BEDROOM', 'Wardrobes & storage', 'Sliding wardrobe',
       118000, 'https://images.unsplash.com/photo-1672137233327-37b0c1049e77?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Linea 3-door sliding wardrobe');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Linea 4-door hinged wardrobe', 'Full-height hanging on the left, eight shelves on the right.', 'FURNITURE', 'MASTER_BEDROOM', 'Wardrobes & storage', 'Hinged wardrobe',
       96000, 'https://images.unsplash.com/photo-1649361811423-a55616f7ab11?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Linea 4-door hinged wardrobe');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Atelier walk-in wardrobe', 'Modular open system — hanging, drawers, shoe tiers, per running foot.', 'FURNITURE', 'MASTER_BEDROOM', 'Wardrobes & storage', 'Walk-in wardrobe',
       265000, 'https://images.unsplash.com/photo-1683181181300-44c0c991a2cf?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Atelier walk-in wardrobe');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Linea five-drawer chest', 'Ball-bearing runners, anti-tip wall bracket included.', 'FURNITURE', 'MASTER_BEDROOM', 'Wardrobes & storage', 'Chest of drawers',
       34500, 'https://images.unsplash.com/photo-1640357154220-9775b0f31dec?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Linea five-drawer chest');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Stride 3-tier shoe cabinet', 'Tilt-out doors, holds 15 pairs in a 30cm depth.', 'FURNITURE', 'MASTER_BEDROOM', 'Wardrobes & storage', 'Shoe rack',
       16500, 'https://images.unsplash.com/photo-1736322969168-7105551d1798?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Stride 3-tier shoe cabinet');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Folio open bookshelf', 'Five bays, 180cm tall, solid mango wood.', 'FURNITURE', 'MASTER_BEDROOM', 'Wardrobes & storage', 'Bookshelf',
       28000, 'https://images.unsplash.com/photo-1722650364570-90bf509d7dff?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Folio open bookshelf');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Sundar crockery unit', 'Glazed upper cabinet, closed base, integrated LED strip.', 'FURNITURE', 'MASTER_BEDROOM', 'Wardrobes & storage', 'Crockery unit',
       52000, 'https://images.unsplash.com/photo-1558997519-83ea9252edf8?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Sundar crockery unit');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Banyan 6-seater dining table', 'Solid acacia top on a trestle base, 180x90cm.', 'FURNITURE', 'DINING', 'Tables', 'Dining table',
       68000, 'https://images.unsplash.com/photo-1602872030490-4a484a7b3ba6?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Banyan 6-seater dining table');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Pebble coffee table', 'Rounded travertine top, powder-coated steel legs.', 'FURNITURE', 'DINING', 'Tables', 'Coffee table',
       22500, 'https://images.unsplash.com/photo-1617806118233-18e1de247200?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Pebble coffee table');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Pebble side table', 'The coffee table at 45cm — pairs beside any sofa.', 'FURNITURE', 'DINING', 'Tables', 'Side table',
       11000, 'https://images.unsplash.com/photo-1614597445336-8a67e9314d91?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Pebble side table');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Hallway console table', '120cm long, two drawers, 32cm deep for narrow foyers.', 'FURNITURE', 'DINING', 'Tables', 'Console table',
       24500, 'https://images.unsplash.com/photo-1574966739987-65e38db0f7ce?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Hallway console table');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Focus study desk', '140x60cm with a cable tray and a lockable drawer unit.', 'FURNITURE', 'DINING', 'Tables', 'Study desk',
       27500, 'https://images.unsplash.com/photo-1560448076-ee77deea722b?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Focus study desk');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Banyan dining chair', 'Solid acacia, contoured seat. Sold singly.', 'FURNITURE', 'DINING', 'Chairs & stools', 'Dining chair',
       8900, 'https://images.unsplash.com/photo-1616627547584-bf28cee262db?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Banyan dining chair');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Noor accent chair', 'Curved bouclé shell on a solid oak base.', 'FURNITURE', 'DINING', 'Chairs & stools', 'Accent chair',
       24500, 'https://images.unsplash.com/photo-1567538096621-38d2284b23ff?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Noor accent chair');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Posture ergonomic chair', 'Mesh back, 4D armrests, synchronous tilt with tension control.', 'FURNITURE', 'DINING', 'Chairs & stools', 'Office chair',
       21500, 'https://images.unsplash.com/photo-1621373660651-081fc7f94fa4?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Posture ergonomic chair');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Rise counter stool', 'Gas-lift 60–80cm, footring, fits island counters.', 'FURNITURE', 'DINING', 'Chairs & stools', 'Bar stool',
       12500, 'https://images.unsplash.com/photo-1560448076-ee77deea722b?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Rise counter stool');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Banyan dining bench', '150cm, seats three, tucks fully under the Banyan table.', 'FURNITURE', 'DINING', 'Chairs & stools', 'Bench',
       18500, 'https://images.unsplash.com/photo-1634712282287-14ed57b9cc89?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Banyan dining bench');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Aram rocking chair', 'Cane back on a steam-bent teak frame.', 'FURNITURE', 'DINING', 'Chairs & stools', 'Rocking chair',
       26500, 'https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Aram rocking chair');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Signature modular kitchen', 'L-shaped 10x8ft: base units, wall units, quartz counter and hardware.', 'FURNITURE', 'KITCHEN', 'Kitchen', 'Modular kitchen',
       385000, 'https://images.unsplash.com/photo-1484154218962-a197022b5858?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Signature modular kitchen');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Prep kitchen island', '180x90cm with a quartz top, two-sided storage and a knee recess.', 'FURNITURE', 'KITCHEN', 'Kitchen', 'Kitchen island',
       145000, 'https://images.unsplash.com/photo-1632583824020-937ae9564495?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Prep kitchen island');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Base unit — 600mm', 'Soft-close drawers on full-extension runners. Priced per unit.', 'FURNITURE', 'KITCHEN', 'Kitchen', 'Base unit',
       18500, 'https://images.unsplash.com/photo-1559554704-0f74b35a8718?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Base unit — 600mm');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Wall unit — 600mm', 'Lift-up shutter on gas struts, adjustable shelf.', 'FURNITURE', 'KITCHEN', 'Kitchen', 'Wall unit',
       12500, 'https://images.unsplash.com/photo-1588854337236-6889d631faa8?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Wall unit — 600mm');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Tall unit — 2100mm', 'Full-height pull-out pantry, six wire baskets.', 'FURNITURE', 'KITCHEN', 'Kitchen', 'Tall unit',
       46000, 'https://images.unsplash.com/photo-1602028915047-37269d1a73f7?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Tall unit — 2100mm');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Corner pantry with carousel', 'Two-tier rotating carousel — reaches the dead corner.', 'FURNITURE', 'KITCHEN', 'Kitchen', 'Pantry unit',
       38500, 'https://images.unsplash.com/photo-1556911220-bff31c812dba?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Corner pantry with carousel');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Float wall-mounted TV unit', '180cm floating console, cable management, holds up to 65in.', 'FURNITURE', 'LIVING_ROOM', 'TV & media units', 'Wall-mounted TV unit',
       32500, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Float wall-mounted TV unit');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Anchor floor TV unit', 'Four drawers, open centre bay for a soundbar.', 'FURNITURE', 'LIVING_ROOM', 'TV & media units', 'Floor TV unit',
       28500, 'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Anchor floor TV unit');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Panorama entertainment wall', 'Full wall system — TV bay, shelving and closed storage.', 'FURNITURE', 'LIVING_ROOM', 'TV & media units', 'Entertainment centre',
       96000, 'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Panorama entertainment wall');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Halo flush ceiling light', '40cm opal diffuser, 24W, warm white 3000K.', 'MATERIALS', 'LIVING_ROOM', 'Lighting', 'Ceiling light',
       6500, 'https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Halo flush ceiling light');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Rattan dome pendant', '45cm hand-woven rattan shade, 1.5m adjustable drop.', 'MATERIALS', 'LIVING_ROOM', 'Lighting', 'Pendant light',
       8900, 'https://images.unsplash.com/photo-1559924508-1461423083c5?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Rattan dome pendant');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Cascade 9-light chandelier', 'Nine glass globes on brushed brass stems.', 'MATERIALS', 'LIVING_ROOM', 'Lighting', 'Chandelier',
       42500, 'https://images.unsplash.com/photo-1578678809569-1a8ead9cb802?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Cascade 9-light chandelier');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Arc floor lamp', '2.1m marble-based arc, reaches over a sofa.', 'MATERIALS', 'LIVING_ROOM', 'Lighting', 'Floor lamp',
       18500, 'https://images.unsplash.com/photo-1604572689968-e608a2332849?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Arc floor lamp');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Ceramic table lamp', 'Glazed ceramic base with a linen drum shade.', 'MATERIALS', 'LIVING_ROOM', 'Lighting', 'Table lamp',
       5400, 'https://images.unsplash.com/photo-1632583824020-937ae9564495?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Ceramic table lamp');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Reading wall sconce', 'Adjustable arm, hardwired, mounts beside a bed.', 'MATERIALS', 'LIVING_ROOM', 'Lighting', 'Wall sconce',
       4800, 'https://images.unsplash.com/photo-1550581190-9c1c48d21d6c?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Reading wall sconce');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Cove LED profile — per metre', 'Aluminium profile with a diffuser and 14.4W/m strip.', 'MATERIALS', 'LIVING_ROOM', 'Lighting', 'Cove & profile lighting',
       1250, 'https://images.unsplash.com/photo-1602028915047-37269d1a73f7?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Cove LED profile — per metre');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Blackout curtains — pair', 'Triple-weave blackout, 137x274cm, eyelet heading.', 'MATERIALS', 'LIVING_ROOM', 'Soft furnishings', 'Curtains',
       7800, 'https://images.unsplash.com/photo-1704040686510-b747ff423ebb?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Blackout curtains — pair');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Roller blind — made to measure', 'Priced per sq ft. Chain or motorised operation.', 'MATERIALS', 'LIVING_ROOM', 'Soft furnishings', 'Blinds',
       4200, 'https://images.unsplash.com/photo-1754611362309-71297e9f42fd?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Roller blind — made to measure');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Hand-tufted wool rug', '170x240cm, 100% New Zealand wool, 12mm pile.', 'MATERIALS', 'LIVING_ROOM', 'Soft furnishings', 'Rugs & carpets',
       24500, 'https://images.unsplash.com/photo-1616100587911-1747ac739e4e?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Hand-tufted wool rug');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Cotton cushion cover set', 'Set of four, 45x45cm, hand-block printed.', 'MATERIALS', 'LIVING_ROOM', 'Soft furnishings', 'Cushions',
       2400, 'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Cotton cushion cover set');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Sateen bedding set', '300TC long-staple cotton — duvet cover, fitted sheet, two cases.', 'MATERIALS', 'LIVING_ROOM', 'Soft furnishings', 'Bedding',
       6900, 'https://images.unsplash.com/photo-1560449752-ac541afdd6b5?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Sateen bedding set');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Performance upholstery fabric', 'Per metre. 60,000 rub Martindale, stain resistant.', 'MATERIALS', 'LIVING_ROOM', 'Soft furnishings', 'Upholstery fabric',
       1450, 'https://images.unsplash.com/photo-1600566753086-00f18fb6b3ea?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Performance upholstery fabric');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Framed abstract print', '60x90cm giclée on cotton rag, solid ash frame.', 'MATERIALS', 'LIVING_ROOM', 'Decor & art', 'Wall art',
       8500, 'https://images.unsplash.com/photo-1600566753086-00f18fb6b3ea?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Framed abstract print');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Arched full-length mirror', '180x80cm, 5mm float glass on a powder-coated frame.', 'MATERIALS', 'LIVING_ROOM', 'Decor & art', 'Mirrors',
       16500, 'https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Arched full-length mirror');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Ribbed ceramic planter set', 'Set of three, 30/24/18cm, with drainage and saucers.', 'MATERIALS', 'LIVING_ROOM', 'Decor & art', 'Planters',
       4200, 'https://images.unsplash.com/photo-1608118940326-1f5c3c49d932?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Ribbed ceramic planter set');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Studio pottery vase', 'Wheel-thrown stoneware, reactive glaze — each one differs.', 'MATERIALS', 'LIVING_ROOM', 'Decor & art', 'Vases & showpieces',
       3600, 'https://images.unsplash.com/photo-1486946255434-2466348c2166?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Studio pottery vase');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Minimal wall clock', '40cm brushed aluminium, silent sweep movement.', 'MATERIALS', 'LIVING_ROOM', 'Decor & art', 'Clocks',
       2900, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Minimal wall clock');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Balcony bistro set', 'Two weave chairs and a 60cm table — fits a 4ft balcony.', 'FURNITURE', 'BALCONY', 'Outdoor', 'Balcony seating',
       22500, 'https://images.unsplash.com/photo-1560448205-d82bf18b9bcf?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Balcony bistro set');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Terrace lounge set', 'Corner sofa and coffee table in all-weather PE rattan.', 'FURNITURE', 'BALCONY', 'Outdoor', 'Garden furniture',
       58000, 'https://images.unsplash.com/photo-1719266084633-24981ecdc417?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Terrace lounge set');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Hanging swing chair', 'Powder-coated steel frame with a cushioned seat, 120kg rated.', 'FURNITURE', 'BALCONY', 'Outdoor', 'Swing & hammock',
       26500, 'https://images.unsplash.com/photo-1416331108676-a22ccb276e35?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Hanging swing chair');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Solar bollard light — set of 4', 'IP65, dusk-to-dawn sensor, no wiring.', 'FURNITURE', 'BALCONY', 'Outdoor', 'Outdoor lighting',
       6800, 'https://images.unsplash.com/photo-1560448205-d82bf18b9bcf?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Solar bollard light — set of 4');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Modern minimal living room package', 'Full living room: layout, 3D views, furniture and lighting schedule, material list.', 'DESIGNS', 'LIVING_ROOM', NULL, NULL,
       145000, 'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Modern minimal living room package');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Japandi master bedroom package', 'Wardrobe elevation, bed wall, lighting plan and a complete finish schedule.', 'DESIGNS', 'MASTER_BEDROOM', NULL, NULL,
       118000, 'https://images.unsplash.com/photo-1616594039964-ae9021a400a0?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Japandi master bedroom package');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Modular kitchen design package', 'Working drawings, appliance and service points, counter and shutter selection.', 'DESIGNS', 'KITCHEN', NULL, NULL,
       96000, 'https://images.unsplash.com/photo-1556911220-bff31c812dba?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Modular kitchen design package');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Kids'' room design package', 'Study, storage and play zoning with child-safe finishes throughout.', 'DESIGNS', 'KIDS_ROOM', NULL, NULL,
       74000, 'https://images.unsplash.com/photo-1486946255434-2466348c2166?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Kids'' room design package');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Compact 2BHK full-home package', 'Every room drawn, costed and scheduled — designed for a 650–900 sq ft flat.', 'DESIGNS', 'LIVING_ROOM', NULL, NULL,
       420000, 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Compact 2BHK full-home package');

INSERT INTO products
  (company_id, name, description, category, room_type, shop_category, shop_sub_category,
   price, image_url, active, created_at)
SELECT c.id, 'Home office design package', 'Desk, storage and acoustics for a work-from-home corner or a whole room.', 'DESIGNS', 'STUDY', NULL, NULL,
       62000, 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=1200&auto=format&fit=crop', true, now()
FROM companies c
WHERE c.slug = 'bespoke-living'
  AND NOT EXISTS (
    SELECT 1 FROM products p WHERE p.company_id = c.id AND p.name = 'Home office design package');
