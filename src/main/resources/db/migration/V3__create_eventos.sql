CREATE TABLE eventos (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255)     NOT NULL,
    data_inicio     TIMESTAMPTZ      NOT NULL,
    data_fim        TIMESTAMPTZ      NOT NULL,
    endereco        VARCHAR(500),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    raio_metros     INTEGER          NOT NULL DEFAULT 100
                        CHECK (raio_metros >= 50 AND raio_metros <= 500),
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_datas CHECK (data_fim > data_inicio)
);

CREATE INDEX idx_eventos_datas ON eventos(data_inicio, data_fim);
