-- Add DISPONIVEL to the employees status check constraint
-- and migrate existing ATIVO records to DISPONIVEL

ALTER TABLE funcionarios DROP CONSTRAINT IF EXISTS funcionarios_status_check;

ALTER TABLE funcionarios ADD CONSTRAINT funcionarios_status_check
    CHECK (status IN ('ATIVO', 'INATIVO', 'PRESENTE', 'AUSENTE', 'DISPONIVEL'));

ALTER TABLE funcionarios ALTER COLUMN status SET DEFAULT 'DISPONIVEL';

UPDATE funcionarios SET status = 'DISPONIVEL' WHERE status = 'ATIVO';
