-- Clientes de desenvolvimento. As chaves em claro sao read-key-dev e admin-key-dev;
-- aqui so existe o hash. Em producao as chaves devem ter alta entropia (32+ bytes).
INSERT INTO api_client (name, key_hash, scopes)
VALUES ('dev-reader', 'ec0acb2427c69d94b361c674c55e065eb8833fb3fe6582361e733b8f7cf9ce9a', 'read'),
       ('dev-admin', '7e20b5a3f8700ccd15c558570f537097ac69bbc16c7f356e9f256017c74342f1', 'read,admin');

-- Disponibilidade semeada: 3 allotments por noite na janela das tarifas.
-- Algumas noites ficam com allotment 0 (stop-sell) para exercitar o minimo do periodo.
INSERT INTO availability (room_type_id, stay_date, allotment, stop_sell)
SELECT rt.id,
       d::date,
       CASE
           WHEN EXTRACT(DAY FROM d) = 15 THEN 0
           WHEN rt.id % 3 = 0 THEN 1
           ELSE 3
           END,
       EXTRACT(DAY FROM d) = 15
FROM room_type rt
         CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + INTERVAL '400 days', INTERVAL '1 day') AS d;
