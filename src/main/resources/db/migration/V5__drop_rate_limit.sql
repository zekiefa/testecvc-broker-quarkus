-- A régua de rate limit nunca foi implementada: a coluna existia sem leitor.
-- Volta junto com o filtro que a aplica, não antes.
ALTER TABLE api_client DROP COLUMN rate_limit_per_min;
