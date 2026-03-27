CREATE TABLE insulin_doses (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    -- Кількість одиниць інсуліну
    dose_units       NUMERIC(6, 2) NOT NULL,
    -- Тип дози: BOLUS (їдальній), BASAL (базальний), CORRECTION (корекційний)
    dose_type        VARCHAR(20)   NOT NULL,
    insulin_type     VARCHAR(100),
    injected_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Прив'язка до прийому їжі (необов'язково)
    meal_record_id   BIGINT,
    -- Рівень глюкози до введення інсуліну (ммоль/л)
    glucose_before   NUMERIC(5, 2),
    notes            TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_insulin_doses_user        FOREIGN KEY (user_id)        REFERENCES users (id)        ON DELETE CASCADE,
    CONSTRAINT fk_insulin_doses_meal_record FOREIGN KEY (meal_record_id) REFERENCES meal_records (id) ON DELETE SET NULL
);

CREATE INDEX idx_insulin_doses_user_injected ON insulin_doses (user_id, injected_at DESC);
