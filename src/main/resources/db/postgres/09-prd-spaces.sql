-- The spaces the PRD was missing (16 Sep task sheet): lobby, fire area, open to sky,
-- stilt parking and parking. PlatformOptionDefaults only seeds a list that does not
-- exist yet — once the admin owns ROOM_CHOICE, new entries have to arrive here.
-- Idempotent; applied automatically on every boot by SqlMigrationRunner.

-- note = the room-catalog key the space pulls its element checklist from; these five
-- have no catalog of their own, so their elements are added by hand in the PRD.
INSERT INTO platform_options (list_key, value, label, note, sort_order, active) VALUES
  ('ROOM_CHOICE', 'LOBBY',         'Lobby',              NULL, 12, true),
  ('ROOM_CHOICE', 'FIRE_AREA',     'Fire area / refuge', NULL, 13, true),
  ('ROOM_CHOICE', 'OTS',           'Open to sky (OTS)',  NULL, 14, true),
  ('ROOM_CHOICE', 'PARKING_STILT', 'Stilt parking',      NULL, 15, true),
  ('ROOM_CHOICE', 'PARKING',       'Parking',            NULL, 16, true)
ON CONFLICT (list_key, value) DO NOTHING;
