CREATE TABLE meal_records (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    meal_name        VARCHAR(255),
    -- Кількість вуглеводів у їжі (грами)
    carbohydrates_g  NUMERIC(7, 2) NOT NULL,
    -- Глікемічний індекс страви (1–100), необов'язкове поле
    glycemic_index   INTEGER,
    meal_time        TIMESTAMP WITH TIME ZONE NOT NULL,
    notes            TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_meal_records_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_meal_records_user_time ON meal_records (user_id, meal_time DESC);
