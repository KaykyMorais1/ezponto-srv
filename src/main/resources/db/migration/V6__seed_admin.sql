-- Senha: Admin@123 (bcrypt hash)
INSERT INTO contas (email, senha_hash, role)
VALUES (
    'admin@showco.com.br',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj2LVoFHFkPG',
    'ADMIN'
);
