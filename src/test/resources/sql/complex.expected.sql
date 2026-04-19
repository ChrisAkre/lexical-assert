WITH user_data AS (
  SELECT
    u.id,
    u.first_name,
    u.last_name,
    p.avatar_url
  FROM
    users u
  JOIN profiles p
    ON u.id = p.user_id
)
SELECT * FROM user_data