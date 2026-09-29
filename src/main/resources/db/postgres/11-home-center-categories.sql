-- The Home Center keeps four categories: Lights & Switches, Curtains & Fabrics,
-- Decor & Arts, Carpets & Rugs. Three of them are the old Lighting / Soft furnishings /
-- Decor & art renamed, so the listings filed under those move across rather than being
-- orphaned; the rest of the old rail is deactivated, not deleted, so /admin/options can
-- bring any of it back in one click.
--
-- "Modular" stays in SHOP_CATEGORY: vendors file modular pieces under it and /modular is
-- that one category. The /shop rail hides it — it has its own nav entry.
--
-- Runs once: the guard is the presence of 'Lights & Switches'. After that the admin owns
-- the list and this file is a no-op, so re-running never undoes their edits.

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM platform_options
              WHERE list_key = 'SHOP_CATEGORY' AND value = 'Lights & Switches') THEN
    RETURN;
  END IF;

  -- Rename in place where the category is the same shelf under a new name, so every
  -- product and sub-type filed under it comes along.
  UPDATE products SET shop_category = 'Carpets & Rugs'
   WHERE shop_category = 'Soft furnishings' AND shop_sub_category = 'Rugs & carpets';
  UPDATE platform_options SET note = 'Carpets & Rugs'
   WHERE list_key = 'SHOP_SUBCATEGORY' AND value = 'Rugs & carpets';

  UPDATE products SET shop_category = 'Lights & Switches' WHERE shop_category = 'Lighting';
  UPDATE products SET shop_category = 'Curtains & Fabrics' WHERE shop_category = 'Soft furnishings';
  UPDATE products SET shop_category = 'Decor & Arts' WHERE shop_category = 'Decor & art';

  UPDATE platform_options SET value = 'Lights & Switches', label = 'Lights & Switches', sort_order = 0
   WHERE list_key = 'SHOP_CATEGORY' AND value = 'Lighting';
  UPDATE platform_options SET value = 'Curtains & Fabrics', label = 'Curtains & Fabrics', sort_order = 1
   WHERE list_key = 'SHOP_CATEGORY' AND value = 'Soft furnishings';
  UPDATE platform_options SET value = 'Decor & Arts', label = 'Decor & Arts', sort_order = 2
   WHERE list_key = 'SHOP_CATEGORY' AND value = 'Decor & art';

  UPDATE platform_options SET note = 'Lights & Switches'
   WHERE list_key = 'SHOP_SUBCATEGORY' AND note = 'Lighting';
  UPDATE platform_options SET note = 'Curtains & Fabrics'
   WHERE list_key = 'SHOP_SUBCATEGORY' AND note = 'Soft furnishings';
  UPDATE platform_options SET note = 'Decor & Arts'
   WHERE list_key = 'SHOP_SUBCATEGORY' AND note = 'Decor & art';

  -- Carpets & Rugs has no predecessor of its own.
  INSERT INTO platform_options (list_key, value, label, note, sort_order, active) VALUES
    ('SHOP_CATEGORY', 'Lights & Switches',  'Lights & Switches',  NULL, 0, true),
    ('SHOP_CATEGORY', 'Curtains & Fabrics', 'Curtains & Fabrics', NULL, 1, true),
    ('SHOP_CATEGORY', 'Decor & Arts',       'Decor & Arts',       NULL, 2, true),
    ('SHOP_CATEGORY', 'Carpets & Rugs',     'Carpets & Rugs',     NULL, 3, true)
  ON CONFLICT (list_key, value) DO NOTHING;

  INSERT INTO platform_options (list_key, value, label, note, sort_order, active) VALUES
    ('SHOP_SUBCATEGORY', 'Switches & sockets', 'Switches & sockets', 'Lights & Switches', 80, true),
    ('SHOP_SUBCATEGORY', 'Runners',            'Runners',            'Carpets & Rugs',    81, true),
    ('SHOP_SUBCATEGORY', 'Door mats',          'Door mats',          'Carpets & Rugs',    82, true)
  ON CONFLICT (list_key, value) DO NOTHING;

  -- Everything else off the rail. The rows stay so nothing filed under them is lost.
  UPDATE platform_options SET active = false
   WHERE list_key = 'SHOP_CATEGORY'
     AND value NOT IN ('Modular', 'Lights & Switches', 'Curtains & Fabrics',
                       'Decor & Arts', 'Carpets & Rugs');
  UPDATE platform_options SET active = false
   WHERE list_key = 'SHOP_SUBCATEGORY'
     AND (note IS NULL
          OR note NOT IN ('Modular', 'Lights & Switches', 'Curtains & Fabrics',
                          'Decor & Arts', 'Carpets & Rugs'));
END $$;
