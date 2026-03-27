CREATE TABLE recommendations (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT NOT NULL,
    -- Тип рекомендації: DOSE_CALCULATION, FORECAST, ALERT
    recommendation_type         VARCHAR(50)  NOT NULL,
    -- Рекомендована доза інсуліну (Од), якщо застосовно
    recommended_dose            NUMERIC(6, 2),
    -- Прогнозований рівень глюкози (ммоль/л)
    predicted_glucose           NUMERIC(5, 2),
    -- Горизонт прогнозу (хвилини)
    prediction_horizon_minutes  INTEGER,
    -- JSON з контекстними даними, що використовувались під час розрахунку
    context_json                TEXT,
    -- Текст рекомендації для відображення користувачу
    message                     TEXT,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_read                     BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_recommendations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_recommendations_user_created ON recommendations (user_id, created_at DESC);
CREATE INDEX idx_recommendations_user_unread  ON recommendations (user_id, is_read) WHERE is_read = FALSE;
