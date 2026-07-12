-- BeSpoke — seed data (V2)
-- Idempotent: safe to re-run / safe on an already-populated database.
-- Real bcrypt hashes, so these accounts log in:
--   admin@bespoke.in            / admin123
--   <designer>@bespoke.in       / designer123
--   <customer>@example.com      / test1234

-- ── Accounts ────────────────────────────────────────────────
INSERT INTO users (created_at, email, name, password_hash, role) VALUES
      (now(), 'admin@bespoke.in',   'Admin',           '$2a$10$.JdBddCsHv.R5TJvWWSP/ulvrM/TWQGL2kUBIQsqJz6uZNL//fbU6', 'ADMIN'),
  (now(), 'aarti@bespoke.in',   'Aarti Sharma',    '$2a$10$AFtkovxasofXqjJ86htHi.r3Jq6paUG.ZNXEUx/Mv.ZAQsjtprMiq', 'DESIGNER'),
  (now(), 'rohan@bespoke.in',   'Rohan Mehta',     '$2a$10$AFtkovxasofXqjJ86htHi.r3Jq6paUG.ZNXEUx/Mv.ZAQsjtprMiq', 'DESIGNER'),
  (now(), 'priya@bespoke.in',   'Priya Nair',      '$2a$10$AFtkovxasofXqjJ86htHi.r3Jq6paUG.ZNXEUx/Mv.ZAQsjtprMiq', 'DESIGNER'),
  (now(), 'maya@bespoke.in',    'Maya Nair',       '$2a$10$AFtkovxasofXqjJ86htHi.r3Jq6paUG.ZNXEUx/Mv.ZAQsjtprMiq', 'DESIGNER'),
  (now(), 'vikram@bespoke.in',  'Vikram Desai',    '$2a$10$AFtkovxasofXqjJ86htHi.r3Jq6paUG.ZNXEUx/Mv.ZAQsjtprMiq', 'DESIGNER'),
  (now(), 'sara@bespoke.in',    'Sara Fernandes',  '$2a$10$AFtkovxasofXqjJ86htHi.r3Jq6paUG.ZNXEUx/Mv.ZAQsjtprMiq', 'DESIGNER'),
  (now(), 'dev@bespoke.in',     'Dev Malhotra',    '$2a$10$AFtkovxasofXqjJ86htHi.r3Jq6paUG.ZNXEUx/Mv.ZAQsjtprMiq', 'DESIGNER'),
  (now(), 'nisha@bespoke.in',   'Nisha Reddy',     '$2a$10$AFtkovxasofXqjJ86htHi.r3Jq6paUG.ZNXEUx/Mv.ZAQsjtprMiq', 'DESIGNER'),
  (now(), 'riya@example.com',   'Riya Sharma',     '$2a$10$MceWADovhTPL8D2vMtf/qeDWIPVLajycBPX7Hy3x8g1J/T1J8jv0C', 'CUSTOMER'),
  (now(), 'aarav@example.com',  'Aarav Gupta',     '$2a$10$MceWADovhTPL8D2vMtf/qeDWIPVLajycBPX7Hy3x8g1J/T1J8jv0C', 'CUSTOMER'),
  (now(), 'meera@example.com',  'Meera Iyer',      '$2a$10$MceWADovhTPL8D2vMtf/qeDWIPVLajycBPX7Hy3x8g1J/T1J8jv0C', 'CUSTOMER'),
  (now(), 'kabir@example.com',  'Kabir Khan',      '$2a$10$MceWADovhTPL8D2vMtf/qeDWIPVLajycBPX7Hy3x8g1J/T1J8jv0C', 'CUSTOMER'),
  (now(), 'ananya@example.com', 'Ananya Bose',     '$2a$10$MceWADovhTPL8D2vMtf/qeDWIPVLajycBPX7Hy3x8g1J/T1J8jv0C', 'CUSTOMER')
ON CONFLICT (email) DO NOTHING;

-- ── Designer profiles ───────────────────────────────────────
INSERT INTO designer_profiles (bio, city, rating, specialties, starting_price, user_id)
SELECT v.bio, v.city, v.rating, v.specialties, v.price, u.id
FROM (VALUES
  ('aarti@bespoke.in',  'Gurugram',  4.8, 'Kitchen, Living Room, Full Home', 45000,
   'Award-winning interior designer with 10+ years crafting warm, functional homes across NCR.'),
  ('rohan@bespoke.in',  'Bengaluru', 4.6, 'Wardrobe, Bedroom, Kitchen', 30000,
   'Minimalist modular specialist. I focus on smart storage, clean lines and budget-friendly builds.'),
  ('priya@bespoke.in',  'Mumbai',    4.9, 'Full Home, Living Room, Bathroom', 75000,
   'Luxury and boutique interiors. From concept boards to turnkey execution for full homes.'),
  ('maya@bespoke.in',   'Kochi',     4.9, 'Kitchen, Bathroom', 13999,
   'Kitchens and bathrooms are the hardest rooms in a home — they are all I do, and I do them well.'),
  ('vikram@bespoke.in', 'Ahmedabad', 4.7, 'Living Room, Full Home', 16999,
   'Warm, textured interiors with a strong material story. 12 years, 90+ homes.'),
  ('sara@bespoke.in',   'Goa',       4.8, 'Bedroom, Wardrobe', 10999,
   'Calm, coastal bedrooms and dressing rooms with hotel-suite restraint.'),
  ('dev@bespoke.in',    'Gurugram',  4.6, 'Kitchen, Living Room, Wardrobe', 12499,
   'Practical, budget-honest design for young families. No fluff, no upsell.'),
  ('nisha@bespoke.in',  'Hyderabad', 4.9, 'Full Home, Living Room', 18999,
   'Full-home storyteller — one palette carried through every room of the house.')
) AS v(email, city, rating, specialties, price, bio)
JOIN users u ON u.email = v.email
WHERE NOT EXISTS (SELECT 1 FROM designer_profiles p WHERE p.user_id = u.id);

-- ── Portfolio images (real imagery) ─────────────────────────
INSERT INTO designer_portfolio_images (profile_id, image_url)
SELECT p.id, v.url
FROM (VALUES
  ('aarti@bespoke.in',  'https://images.unsplash.com/photo-1556911220-bff31c812dba?auto=format&fit=crop&w=600&q=70'),
  ('aarti@bespoke.in',  'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=600&q=70'),
  ('rohan@bespoke.in',  'https://images.unsplash.com/photo-1558997519-83ea9252edf8?auto=format&fit=crop&w=600&q=70'),
  ('rohan@bespoke.in',  'https://images.unsplash.com/photo-1616594039964-ae9021a400a0?auto=format&fit=crop&w=600&q=70'),
  ('priya@bespoke.in',  'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=600&q=70'),
  ('priya@bespoke.in',  'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=600&q=70'),
  ('maya@bespoke.in',   'https://images.unsplash.com/photo-1556911220-bff31c812dba?auto=format&fit=crop&w=600&q=70'),
  ('maya@bespoke.in',   'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=600&q=70'),
  ('vikram@bespoke.in', 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=600&q=70'),
  ('sara@bespoke.in',   'https://images.unsplash.com/photo-1595515106864-077d30192c56?auto=format&fit=crop&w=600&q=70'),
  ('dev@bespoke.in',    'https://images.unsplash.com/photo-1565538810643-b5bdb714032a?auto=format&fit=crop&w=600&q=70'),
  ('nisha@bespoke.in',  'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=600&q=70')
) AS v(email, url)
JOIN users u ON u.email = v.email
JOIN designer_profiles p ON p.user_id = u.id
WHERE NOT EXISTS (
  SELECT 1 FROM designer_portfolio_images i WHERE i.profile_id = p.id AND i.image_url = v.url
);

-- ── Catalogue (18 packages, real imagery) ───────────────────
INSERT INTO design_services (category, deliverables, description, image_url, price, title)
SELECT v.category, v.deliverables, v.description, v.image_url, v.price, v.title
FROM (VALUES
  ('KITCHEN','3D renders, 2D layout, material & appliance list, installation plan',
   'L-shaped modular kitchen design with laminate finish, soft-close hardware and quartz countertop options.',
   'https://images.unsplash.com/photo-1556911220-bff31c812dba?auto=format&fit=crop&w=1200&q=70', 95000, 'Modular Kitchen - Essential'),
  ('KITCHEN','3D renders, 2D layout, lighting plan, appliance integration, site supervision',
   'Premium acrylic-finish kitchen with island option, tall units, built-in appliances planning and lighting design.',
   'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=70', 210000, 'Modular Kitchen - Premium'),
  ('WARDROBE','3D renders, internal layout plan, material list',
   'Space-saving 2-door sliding wardrobe with mirror or lacquered glass shutter options and custom internals.',
   'https://images.unsplash.com/photo-1558997519-83ea9252edf8?auto=format&fit=crop&w=1200&q=70', 58000, 'Sliding Wardrobe - 2 Door'),
  ('WARDROBE','3D renders, 2D layout, lighting plan, accessories list',
   'Bespoke walk-in wardrobe with island dresser, accessory drawers and sensor lighting.',
   'https://images.unsplash.com/photo-1595515106864-077d30192c56?auto=format&fit=crop&w=1200&q=70', 145000, 'Walk-in Wardrobe'),
  ('LIVING_ROOM','Mood board, 3D renders, furniture list, decor shopping list',
   'Complete living room design: TV unit, false ceiling, wall treatments, furniture and decor curation.',
   'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=1200&q=70', 120000, 'Living Room Makeover'),
  ('BEDROOM','3D renders, 2D layout, material palette, lighting plan',
   'Serene master bedroom with headboard paneling, wardrobe, study nook and layered lighting.',
   'https://images.unsplash.com/photo-1616594039964-ae9021a400a0?auto=format&fit=crop&w=1200&q=70', 98000, 'Master Bedroom Design'),
  ('BEDROOM','Theme board, 3D renders, furniture list, safety checklist',
   'Playful, safe and study-friendly kids room with themed decor and modular storage that grows with them.',
   'https://images.unsplash.com/photo-1519710164239-da123dc03ef4?auto=format&fit=crop&w=1200&q=70', 72000, 'Kids Bedroom Design'),
  ('BATHROOM','3D renders, tiling layout, fixture list, plumbing notes',
   'Modern bathroom upgrade: vanity design, tiling scheme, sanitaryware selection and waterproofing guidance.',
   'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=1200&q=70', 65000, 'Bathroom Refresh'),
  ('FULL_HOME','Full 3D walkthrough, GFC drawings, BOQ, project schedule, site supervision',
   'End-to-end 2BHK interiors: kitchen, wardrobes, living, bedrooms and bathrooms with turnkey coordination.',
   'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=70', 550000, 'Full Home - 2BHK Package'),
  ('FULL_HOME','Full 3D walkthrough, GFC drawings, BOQ, automation plan, styling & handover',
   'Premium 3BHK turnkey interiors with custom furniture, home automation planning and styling on handover.',
   'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=1200&q=70', 850000, 'Full Home - 3BHK Package'),
  ('KITCHEN','3D renders, island detail, lighting plan, material board',
   'Island kitchen concept with seating, statement lighting and generous storage — designed around how you cook and host.',
   'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=70', 34999, 'Island Kitchen - Signature'),
  ('WARDROBE','3D render, internal layout, shutter finishes, hardware spec',
   'Space-saving sliding wardrobe with internal organisers planned to your exact storage — shelves, drawers, lofts and mirror panels.',
   'https://images.unsplash.com/photo-1558997519-83ea9252edf8?auto=format&fit=crop&w=1200&q=70', 12999, 'Sliding Wardrobe - Essential'),
  ('LIVING_ROOM','3D renders, TV unit drawings, false-ceiling plan, colour palette',
   'TV unit, seating layout, false ceiling and accent-wall concepts composed into one cohesive living space.',
   'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=1200&q=70', 18999, 'Modern Living Room'),
  ('BEDROOM','3D renders, bed-back drawings, wardrobe elevation, lighting plan',
   'Bed-back panelling, wardrobe integration, dresser and layered lighting — a calm, considered master bedroom scheme.',
   'https://images.unsplash.com/photo-1616594039964-ae9021a400a0?auto=format&fit=crop&w=1200&q=70', 16999, 'Master Bedroom - Calm'),
  ('BATHROOM','3D renders, tile layout, vanity detail, sanitaryware list',
   'Spa-inspired bathroom with vanity design, tile scheme, sanitaryware selection and concealed lighting.',
   'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=1200&q=70', 14999, 'Premium Bathroom - Spa'),
  ('FULL_HOME','Full 3D walkthrough, all room drawings, material boards, lighting plan, site support',
   'Complete 2BHK interior design: living, kitchen, both bedrooms and bathrooms — one designer, one cohesive story.',
   'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=70', 79999, 'Full Home Design - 2BHK'),
  ('FULL_HOME','Full 3D walkthrough, all drawings, material boards, lighting plan, unlimited revisions (30d)',
   'Complete 3BHK interior design with premium detailing across every room, plus execution-ready documentation.',
   'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=1200&q=70', 109999, 'Full Home Design - 3BHK'),
  ('KITCHEN','3D renders, 2D layout & elevations, material board, appliance plan',
   'Efficient parallel (galley) kitchen design that maximises counter space and workflow in compact homes.',
   'https://images.unsplash.com/photo-1565538810643-b5bdb714032a?auto=format&fit=crop&w=1200&q=70', 19999, 'Parallel Kitchen - Compact')
) AS v(category, deliverables, description, image_url, price, title)
WHERE NOT EXISTS (SELECT 1 FROM design_services s WHERE s.title = v.title);

-- ── Enquiry leads (populate the admin triage queue) ─────────
INSERT INTO leads (created_at, updated_at, status, category, contact_name, contact_email, contact_phone, message)
SELECT now(), now(), 'ENQUIRY', v.category, v.name, v.email, v.phone, v.message
FROM (VALUES
  ('FULL_HOME','Neha Kapoor','neha.k@example.com','+919812345678',
   '3BHK in Pune, budget around 3L, want to start next month. Prefer a designer who does warm minimal.'),
  ('KITCHEN','Imran Sheikh','imran.s@example.com','+919811122233',
   'Just the kitchen for now — parallel layout, lots of storage. Call after 6pm please.'),
  ('BEDROOM','Tara Menon','tara.m@example.com','+919899988776',
   'Master + kids bedroom, coastal vibe. Flexible on timeline, not on budget (~1.2L).')
) AS v(category, name, email, phone, message)
WHERE NOT EXISTS (SELECT 1 FROM leads l WHERE l.contact_email = v.email);
