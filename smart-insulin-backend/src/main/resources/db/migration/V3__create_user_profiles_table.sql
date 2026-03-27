CREATE TABLE user_profiles (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT NOT NULL,
    weight_kg                   NUMERIC(5, 2),
    height_cm                   NUMERIC(5, 2),
    -- Коефіцієнт чутливості до інсуліну: на скільки ммоль/л знижується глюкоза після 1 Од
    insulin_sensitivity_factor  NUMERIC(6, 2),
    -- Інсуліно-вуглеводне співвідношення: скільки г вуглеводів перекриває 1 Од
    insulin_to_carb_ratio       NUMERIC(6, 2),
    target_glucose_min          NUMERIC(5, 2),   -- ммоль/л
    target_glucose_max          NUMERIC(5, 2),   -- ммоль/л
    -- Тривалість дії інсуліну (годин), зазвичай 3–8
    duration_of_insulin_action  NUMERIC(4, 2),
    basal_insulin_type          VARCHAR(100),
    bolus_insulin_type          VARCHAR(100),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_user_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_profiles_user    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
