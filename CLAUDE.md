# EzPonto Backend — Instruções para Claude Code

## Stack
- Java 21 + Spring Boot 3.5 + Maven
- PostgreSQL via Supabase
- Flyway para migrations
- JWT (jjwt 0.12.x)
- MapStruct para mapeamento de DTOs
- Lombok (sem @Autowired em campos — usar @RequiredArgsConstructor)

## Pacote base
com.ezponto

## Regras absolutas
- NUNCA usar @Autowired em campos — sempre constructor injection via @RequiredArgsConstructor
- NUNCA retornar entidades diretamente nos controllers — sempre DTOs de response
- NUNCA persistir status de evento — calculado via getStatus() @Transient na entidade
- NUNCA alterar CPF após criação de funcionário — campo updatable=false
- Soft delete em funcionários — status INATIVO, nunca DELETE SQL
- Timestamp definitivo sempre do servidor — nunca confiar no timestamp do cliente

## Estrutura de pacotes
- domain/         → entidades, repositórios, enums (sem deps de framework exceto JPA)
- application/    → interfaces de serviço + implementações
- config/         → Spring Security, JWT, R2
- presentation/   → controllers, DTOs, exception handler

## Migrations
- Sempre criar novo arquivo V{N}__descricao.sql
- NUNCA editar migrations existentes
- Nomenclatura: snake_case, inglês descritivo

## Endpoints
- Base: /api/v1/
- Admin exclusivo: /api/v1/admin/
- Auth público: /api/v1/auth/

## Antes de qualquer tarefa
Leia showco.md para entender as regras de negócio completas.
