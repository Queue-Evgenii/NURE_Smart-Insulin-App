-- Додавання тестових даних для patient2@example.com
-- Запуститеце цей скрипт в pgAdmin або через psql

WITH user_data AS (
  SELECT id FROM "user" WHERE email = 'patient2@example.com'
)
INSERT INTO glucose_reading (user_id, glucose_value, measurement_type, measured_at, created_at)
SELECT
  u.id,
  glucose_val,
  'MANUAL',
  NOW() - INTERVAL '1 minute' * minutes_ago,
  NOW()
FROM
  (SELECT id FROM user_data) u,
  (VALUES
    -- (glucose_value, minutes_ago)
    (9.2, 40),
    (8.9, 36),
    (8.5, 32),
    (8.2, 28),
    (7.8, 24),
    (7.5, 20),
    (7.2, 16),
    (6.9, 12),
    (6.5, 8),
    (6.2, 4),
    (6.0, 2),
    (5.9, 0)
  ) AS data(glucose_val, minutes_ago)
WHERE EXISTS (SELECT 1 FROM user_data);

-- Перевірка: має бути 12 записів
SELECT COUNT(*) as "Додано записів", MAX(measured_at) as "Останній запис"
FROM glucose_reading
WHERE user_id = (SELECT id FROM "user" WHERE email = 'patient2@example.com');
