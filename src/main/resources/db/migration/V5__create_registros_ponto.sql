CREATE TABLE registros_ponto (
    id              BIGSERIAL   PRIMARY KEY,
    funcionario_id  BIGINT      NOT NULL REFERENCES funcionarios(id),
    evento_id       BIGINT      NOT NULL REFERENCES eventos(id),
    tipo            VARCHAR(20) NOT NULL
                        CHECK (tipo IN ('ENTRADA', 'SAIDA', 'INICIO_INTERVALO', 'FIM_INTERVALO')),
    status          VARCHAR(20) NOT NULL DEFAULT 'APROVADO'
                        CHECK (status IN ('APROVADO', 'PENDENTE', 'REJEITADO')),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    foto_url        VARCHAR(500),
    timestamp_servidor TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_registros_funcionario ON registros_ponto(funcionario_id);
CREATE INDEX idx_registros_evento      ON registros_ponto(evento_id);
CREATE INDEX idx_registros_timestamp   ON registros_ponto(timestamp_servidor DESC);
