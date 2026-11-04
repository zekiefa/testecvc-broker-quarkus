-- Catalogo SEMEADO de Serra Negra. data_source = 'SEED' declara isso na resposta:
-- dado sintetico existe para exercitar contrato, nao para parecer real.

INSERT INTO hotel (name, city_code, city_name, latitude, longitude, data_source)
VALUES ('Hotel Serra Verde', 'serra-negra', 'Serra Negra', -22.611900, -46.701900, 'SEED'),
       ('Pousada Vale das Águas', 'serra-negra', 'Serra Negra', -22.615200, -46.698100, 'SEED'),
       ('Estância Alto da Serra', 'serra-negra', 'Serra Negra', -22.608400, -46.705600, 'SEED'),
       ('Hotel Fazenda Bela Vista', 'serra-negra', 'Serra Negra', -22.629700, -46.712200, 'SEED'),
       ('Pousada Caminho do Sol', 'serra-negra', 'Serra Negra', -22.604100, -46.694300, 'SEED'),
       ('Hotel Central Plaza', 'serra-negra', 'Serra Negra', -22.612800, -46.703100, 'SEED'),
       ('Recanto das Fontes', 'serra-negra', 'Serra Negra', -22.620500, -46.709400, 'SEED'),
       ('Pousada Jardim das Flores', 'serra-negra', 'Serra Negra', -22.617300, -46.699800, 'SEED'),
       ('Hotel Vista do Lago', 'serra-negra', 'Serra Negra', -22.626400, -46.707200, 'SEED'),
       ('Chalés da Montanha', 'serra-negra', 'Serra Negra', -22.632100, -46.714900, 'SEED');

-- dois tipos de quarto por hotel, com capacidade diferente
INSERT INTO room_type (hotel_id, name, max_adults, max_children)
SELECT h.id, t.name, t.max_adults, t.max_children
FROM hotel h
         CROSS JOIN (VALUES ('Standard Casal', 2, 1), ('Família', 3, 2)) AS t(name, max_adults, max_children);

-- dois regimes por quarto: flexível com café, e não-reembolsável sem café
INSERT INTO rate_plan (room_type_id, name, board, cancellation_policy)
SELECT rt.id, p.name, p.board, p.cancellation_policy
FROM room_type rt
         CROSS JOIN (VALUES ('Café da manhã — flexível', 'BB', 'FLEXIBLE'),
                            ('Somente quarto — não reembolsável', 'RO', 'NON_REFUNDABLE'))
    AS p(name, board, cancellation_policy);

-- Tarifas para uma janela rolante: 400 dias a partir de hoje.
-- Preço base varia por hotel (id) e por tipo de quarto, para o catálogo não ser uniforme.
INSERT INTO rate (room_type_id, rate_plan_id, stay_date, adult_amount, child_amount, currency)
SELECT rt.id,
       rp.id,
       d::date,
       ROUND((320 + (rt.hotel_id * 37) % 260 + (CASE rt.name WHEN 'Família' THEN 140 ELSE 0 END)
           + (CASE rp.cancellation_policy WHEN 'NON_REFUNDABLE' THEN -45 ELSE 0 END)
           + (CASE WHEN EXTRACT(ISODOW FROM d) IN (5, 6) THEN 60 ELSE 0 END))::numeric, 2),
       ROUND((160 + (rt.hotel_id * 19) % 130 + (CASE rt.name WHEN 'Família' THEN 70 ELSE 0 END)
           + (CASE rp.cancellation_policy WHEN 'NON_REFUNDABLE' THEN -25 ELSE 0 END))::numeric, 2),
       'BRL'
FROM room_type rt
         JOIN rate_plan rp ON rp.room_type_id = rt.id
         CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + INTERVAL '400 days', INTERVAL '1 day') AS d;
