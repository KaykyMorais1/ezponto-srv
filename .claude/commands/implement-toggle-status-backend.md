# implement-toggle-status-backend.md

> Leia `showco.md` e `CLAUDE.md` antes de qualquer ação.
> Apenas backend Spring Boot.
> Commitar ao final: `feat: employee status toggle endpoint`
> Push `main` → Railway faz deploy automático.

---

## Contexto

Implementar endpoint `PATCH /employees/{id}/status` para ativar e inativar funcionários.
- Inativar: qualquer status ativo → `INATIVO`
- Reativar: `INATIVO` → `DISPONIVEL` (estado neutro padrão para funcionário ativo sem evento)

---

## Passo 1 — Verificar o campo `status` na entidade `Employee`

**Arquivo:** `src/main/java/com/ezponto/domain/employee/Employee.java`

Garantir que o campo existe com `@Enumerated(EnumType.STRING)`:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private EmployeeStatus status;
```

---

## Passo 2 — Verificar/atualizar o enum `EmployeeStatus`

**Arquivo:** `src/main/java/com/ezponto/domain/employee/EmployeeStatus.java`

Confirmar que `INATIVO` e `DISPONIVEL` existem no enum. Não renomear valores já existentes no banco.

```java
public enum EmployeeStatus {
    PRESENTE,
    AUSENTE,
    DISPONIVEL,
    INATIVO
}
```

---

## Passo 3 — DTO de request

**Arquivo:** `src/main/java/com/ezponto/presentation/dto/UpdateEmployeeStatusRequest.java`

```java
public record UpdateEmployeeStatusRequest(
    @NotNull(message = "Status é obrigatório")
    EmployeeStatus status
) {}
```

---

## Passo 4 — Método no `EmployeeService`

**Arquivo:** `src/main/java/com/ezponto/application/EmployeeService.java`

```java
public EmployeeResponse updateStatus(Long id, UpdateEmployeeStatusRequest request) {
    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));

    EmployeeStatus novoStatus = request.status();

    boolean isInativando = novoStatus == EmployeeStatus.INATIVO;
    boolean isReativando = novoStatus == EmployeeStatus.DISPONIVEL
                           && employee.getStatus() == EmployeeStatus.INATIVO;

    if (!isInativando && !isReativando) {
        throw new BusinessException(
            "Transição inválida. Use INATIVO para desativar ou DISPONIVEL para reativar."
        );
    }

    employee.setStatus(novoStatus);
    return EmployeeMapper.toResponse(employeeRepository.save(employee));
}
```

---

## Passo 5 — Endpoint no `EmployeeController`

**Arquivo:** `src/main/java/com/ezponto/presentation/EmployeeController.java`

```java
@PatchMapping("/{id}/status")
public ResponseEntity<EmployeeResponse> updateStatus(
    @PathVariable Long id,
    @RequestBody @Valid UpdateEmployeeStatusRequest request
) {
    return ResponseEntity.ok(employeeService.updateStatus(id, request));
}
```

---

## Passo 6 — Migration Flyway (se necessário)

Se a coluna `status` não existir na tabela `employees`, criar:

**Arquivo:** `src/main/resources/db/migration/V{N}__add_employee_status.sql`

```sql
ALTER TABLE employees
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL';
```

---

## Passo 7 — Verificar `GlobalExceptionHandler`

Confirmar que `BusinessException` e `ResourceNotFoundException` estão mapeadas. Se não estiverem, adicionar:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(ex.getMessage()));
}

@ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(ex.getMessage()));
}
```

---

## Passo 8 — Testar via Swagger

- `PATCH /employees/{id}/status` com `{ "status": "INATIVO" }` → `200` com funcionário atualizado
- `PATCH /employees/{id}/status` com `{ "status": "DISPONIVEL" }` em funcionário inativo → `200`
- `PATCH /employees/{id}/status` com `{ "status": "DISPONIVEL" }` em funcionário já ativo → `409`
- `PATCH /employees/9999/status` → `404`

---

## Finalização

1. `./mvnw test` — todos os testes devem passar
2. Commitar: `feat: employee status toggle endpoint`
3. Push `main` → aguardar deploy Railway
