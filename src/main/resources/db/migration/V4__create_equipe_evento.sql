CREATE TABLE equipe_evento (
    id                  BIGSERIAL   PRIMARY KEY,
    evento_id           BIGINT      NOT NULL REFERENCES eventos(id),
    funcionario_id      BIGINT      NOT NULL REFERENCES funcionarios(id),
    data_adicionado     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_equipe UNIQUE (evento_id, funcionario_id)
);

CREATE INDEX idx_equipe_evento_id       ON equipe_evento(evento_id);
CREATE INDEX idx_equipe_funcionario_id  ON equipe_evento(funcionario_id);
