-- MB1 — catalogo, tarifas e planos. O schema e do Flyway; o Hibernate nunca altera.

CREATE TABLE hotel
(
    id          BIGSERIAL PRIMARY KEY,
    name        TEXT        NOT NULL,
    city_code   TEXT        NOT NULL,
    city_name   TEXT        NOT NULL,
    latitude    NUMERIC(9, 6),
    longitude   NUMERIC(9, 6),
    -- ORIGEM DO DADO faz parte do contrato: sintetico nunca se disfarca de real
    data_source TEXT        NOT NULL CHECK (data_source IN ('SEED', 'PUSH')),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX hotel_city_idx ON hotel (city_code);

CREATE TABLE room_type
(
    id           BIGSERIAL PRIMARY KEY,
    hotel_id     BIGINT NOT NULL REFERENCES hotel (id) ON DELETE CASCADE,
    name         TEXT   NOT NULL,
    max_adults   INT    NOT NULL CHECK (max_adults >= 1),
    max_children INT    NOT NULL CHECK (max_children >= 0)
);
CREATE INDEX room_type_hotel_idx ON room_type (hotel_id);

CREATE TABLE rate_plan
(
    id                  BIGSERIAL PRIMARY KEY,
    room_type_id        BIGINT NOT NULL REFERENCES room_type (id) ON DELETE CASCADE,
    name                TEXT   NOT NULL,
    board               TEXT   NOT NULL CHECK (board IN ('RO', 'BB', 'HB', 'FB')),
    cancellation_policy TEXT   NOT NULL CHECK (cancellation_policy IN ('FLEXIBLE', 'NON_REFUNDABLE'))
);
CREATE INDEX rate_plan_room_idx ON rate_plan (room_type_id);

-- Tarifa e por DATA DE ESTADIA: cotar e somar noites, e noite faltando e erro explicito.
CREATE TABLE rate
(
    room_type_id BIGINT        NOT NULL REFERENCES room_type (id) ON DELETE CASCADE,
    rate_plan_id BIGINT        NOT NULL REFERENCES rate_plan (id) ON DELETE CASCADE,
    stay_date    DATE          NOT NULL,
    adult_amount NUMERIC(12, 2) NOT NULL CHECK (adult_amount >= 0),
    child_amount NUMERIC(12, 2) NOT NULL CHECK (child_amount >= 0),
    currency     CHAR(3)       NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    PRIMARY KEY (room_type_id, rate_plan_id, stay_date)
);
CREATE INDEX rate_plan_date_idx ON rate (rate_plan_id, stay_date);
