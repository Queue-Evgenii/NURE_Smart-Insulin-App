ALTER TABLE user_profiles
    ADD COLUMN insulin_resistance_factor NUMERIC(4, 2) NOT NULL DEFAULT 1.0
        CONSTRAINT chk_irf_range CHECK (insulin_resistance_factor BETWEEN 0.5 AND 5.0),
    ADD COLUMN carb_absorption_minutes   INTEGER        NOT NULL DEFAULT 120
        CONSTRAINT chk_cam_range CHECK (carb_absorption_minutes BETWEEN 60 AND 240);
