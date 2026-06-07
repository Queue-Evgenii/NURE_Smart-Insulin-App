-- Tracking for adaptive ICR/ISF recalculation (diploma section 2.1.1).
-- last_coefficient_update — when coefficients were last recomputed from patient history;
-- using_adaptive_coefficients — true once enough observations replaced the 100/500 rule seeds.
ALTER TABLE user_profiles
    ADD COLUMN last_coefficient_update      TIMESTAMP WITH TIME ZONE,
    ADD COLUMN using_adaptive_coefficients  BOOLEAN NOT NULL DEFAULT FALSE;
