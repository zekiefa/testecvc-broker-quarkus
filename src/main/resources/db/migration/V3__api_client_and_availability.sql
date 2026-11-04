-- MB2 — identidade no banco e disponibilidade publicada.

CREATE TABLE api_client
(
    id                 BIGSERIAL PRIMARY KEY,
    name               TEXT      NOT NULL,
    -- SHA-256 da chave em hexadecimal: a chave em claro nunca e gravada nem devolvida
    key_hash           TEXT      NOT NULL CHECK (char_length(key_hash) = 64),
    -- escopos separados por virgula: 'read' ou 'read,admin'
    scopes             TEXT      NOT NULL,
    enabled            BOOLEAN   NOT NULL DEFAULT TRUE,
    rate_limit_per_min INT       NOT NULL DEFAULT 120 CHECK (rate_limit_per_min > 0),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT api_client_hash_unique UNIQUE (key_hash)
);

CREATE TABLE availability
(
    room_type_id BIGINT      NOT NULL REFERENCES room_type (id) ON DELETE CASCADE,
    stay_date    DATE        NOT NULL,
    allotment    INT         NOT NULL CHECK (allotment >= 0),
    stop_sell    BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (room_type_id, stay_date)
);
