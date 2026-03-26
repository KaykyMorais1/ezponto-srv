CREATE TABLE funcionarios (
    id          BIGSERIAL PRIMARY KEY,
    conta_id    BIGINT       NOT NULL REFERENCES contas(id),
    nome        VARCHAR(255) NOT NULL,
    cpf         VARCHAR(14)  NOT NULL UNIQUE,
    cargo       VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ATIVO'
                    CHECK (status IN ('ATIVO', 'INATIVO', 'PRESENTE', 'AUSENTE')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_funcionarios_cpf    ON funcionarios(cpf);
CREATE INDEX idx_funcionarios_status ON funcionarios(status);
