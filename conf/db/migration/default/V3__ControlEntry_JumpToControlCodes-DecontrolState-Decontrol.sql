ALTER TABLE control_entry
  ADD COLUMN jump_to_control_codes TEXT,
  ADD COLUMN decontrolled BOOLEAN;

ALTER TABLE session
  ALTER COLUMN journey_id DROP NOT NULL,
  ADD COLUMN decontrol_codes_found TEXT,
  ADD COLUMN control_codes_to_confirm_decontrolled_status TEXT;
