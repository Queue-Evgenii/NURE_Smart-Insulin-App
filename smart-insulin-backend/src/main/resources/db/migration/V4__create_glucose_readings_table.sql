CREATE TABLE glucose_readings (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    -- Рівень глюкози в крові (ммоль/л)
    glucose_value    NUMERIC(5, 2) NOT NULL,
    -- Тип вимірювання: CGM (безперервний моніторинг) або MANUAL (ручне)
    measurement_type VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
    measured_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    notes            TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_glucose_readings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_glucose_readings_user_measured ON glucose_readings (user_id, measured_at DESC);
