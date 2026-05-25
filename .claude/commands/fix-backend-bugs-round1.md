# fix-backend-bugs-round1.md

> Leia `showco.md` e `CLAUDE.md` antes de qualquer ação.  
> Este arquivo corrige bugs exclusivamente no **backend Spring Boot**.  
> Arquitetura: `domain/` → `application/` → `config/` → `presentation/`  
> Package base: `com.ezponto` | Java 17 | Spring Boot 3 | PostgreSQL Railway interno  
> Aplicar na ordem dos itens. Commitar ao final: `fix: backend bugs round 1`.

---

## BUG-14 · Excluir evento não está funcionando

**Camadas envolvidas:** `presentation/`, `application/`, `domain/evento/`

### Diagnóstico obrigatório antes de alterar código

1. Verificar o controller `EventoController` — o endpoint `DELETE /eventos/{id}` existe e está mapeado corretamente?
2. Verificar se há FK constraints no banco (tabelas `equipe`, `registro`) referenciando `evento_id` sem `ON DELETE CASCADE` na migration Flyway
3. Verificar se o `EventoService.delete()` está implementado ou retorna `UnsupportedOperationException`

### Correções

**`EventoService.delete(Long id)`:**

```java
public void delete(Long id) {
    Evento evento = eventoRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado: " + id));

    if (evento.getStatus() == StatusEvento.EM_ANDAMENTO) {
        throw new BusinessException("Não é possível excluir um evento em andamento.");
    }

    // Remover membros da equipe antes de excluir o evento
    equipeRepository.deleteAllByEventoId(id);

    eventoRepository.delete(evento);
}
```

**`EventoController`:**
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    eventoService.delete(id);
    return ResponseEntity.noContent().build(); // 204 No Content
}
```

**Migration Flyway** (novo arquivo `V{N}__fix_evento_fk_constraints.sql`):

Verificar se as tabelas `equipe` e `registros` têm FK para `eventos`. Se não tiverem `ON DELETE CASCADE`, **não adicionar cascade** — a deleção explícita no service é preferível para manter controle. Garantir apenas que as FKs existem e estão nomeadas.

**`EquipeRepository`:**
```java
@Modifying
@Query("DELETE FROM Equipe e WHERE e.evento.id = :eventoId")
void deleteAllByEventoId(@Param("eventoId") Long eventoId);
```

**Exceções globais** — garantir que `BusinessException` retorna `409 Conflict` no `@ControllerAdvice`:
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(ex.getMessage()));
}
```

---

## BUG-15 · Senha inicial no cadastro + endpoint de alteração de senha

### 15a. Campo senha no `POST /employees`

**Arquivo:** `presentation/dto/CreateEmployeeRequest.java`

Adicionar campo `senha`:
```java
@NotBlank(message = "Senha é obrigatória")
@Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
@Pattern(
    regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
    message = "Senha deve conter ao menos uma maiúscula, um número e um caractere especial"
)
private String senha;
```

**`EmployeeService.create()`:** encodar com BCrypt antes de persistir:
```java
employee.setSenha(passwordEncoder.encode(request.getSenha()));
```

Verificar que `passwordEncoder` é injetado via construtor (nunca `@Autowired` em campo).

### 15b. Novo endpoint `PATCH /employees/{id}/password`

**DTO:** `ChangePasswordRequest.java`
```java
public record ChangePasswordRequest(
    @NotBlank String senhaAtual,
    @NotBlank
    @Size(min = 8)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
             message = "Senha deve conter ao menos uma maiúscula, um número e um caractere especial")
    String novaSenha
) {}
```

**`EmployeeService.changePassword(Long id, ChangePasswordRequest request)`:**
```java
public void changePassword(Long id, ChangePasswordRequest request) {
    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));

    if (!passwordEncoder.matches(request.senhaAtual(), employee.getSenha())) {
        throw new InvalidPasswordException("Senha atual incorreta.");
    }

    employee.setSenha(passwordEncoder.encode(request.novaSenha()));
    employeeRepository.save(employee);
}
```

**`EmployeeController`:**
```java
@PatchMapping("/{id}/password")
public ResponseEntity<Void> changePassword(
    @PathVariable Long id,
    @RequestBody @Valid ChangePasswordRequest request
) {
    employeeService.changePassword(id, request);
    return ResponseEntity.noContent().build(); // 204
}
```

**Exceção `InvalidPasswordException`** → retornar `400 Bad Request` no `@ControllerAdvice`:
```java
@ExceptionHandler(InvalidPasswordException.class)
public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse(ex.getMessage()));
}
```

---

## BUG-16 · Deleção física vs. inativação de funcionário

### 16a. Confirmar `PATCH /employees/{id}/status`

Verificar que o endpoint existe e funciona corretamente:
- Aceita body `{ "status": "INATIVO" }`
- Persiste o status e retorna `200` com o DTO do funcionário atualizado
- Não remove o registro do banco (soft delete)

Se não existir, criar seguindo o mesmo padrão dos outros endpoints PATCH.

### 16b. `DELETE /employees/{id}` — deleção física com guarda de integridade

**`EmployeeService.delete(Long id)`:**
```java
public void delete(Long id) {
    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));

    boolean hasRegistros = registroRepository.existsByEmployeeId(id);
    if (hasRegistros) {
        throw new BusinessException(
            "Este funcionário possui registros de ponto e não pode ser excluído. Use a opção de inativar."
        );
    }

    // Remover da equipe de eventos antes de excluir
    equipeRepository.deleteAllByEmployeeId(id);

    employeeRepository.delete(employee);
}
```

**`EmployeeController`:**
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    employeeService.delete(id);
    return ResponseEntity.noContent().build(); // 204
}
```

**`RegistroRepository`:**
```java
boolean existsByEmployeeId(Long employeeId);
```

**`EquipeRepository`:**
```java
@Modifying
@Query("DELETE FROM Equipe e WHERE e.employee.id = :employeeId")
void deleteAllByEmployeeId(@Param("employeeId") Long employeeId);
```

---

## BUG-17 · Expiração do JWT configurável

**Arquivo:** `config/security/JwtService.java` (ou equivalente) e `application.yml`

### `application.yml`

```yaml
security:
  jwt:
    secret: ${JWT_SECRET}
    expiration-hours: 8
```

### `JwtService.java`

```java
@Value("${security.jwt.expiration-hours:8}")
private long expirationHours;

public String generateToken(UserDetails userDetails) {
    return Jwts.builder()
        .subject(userDetails.getUsername())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expirationHours * 3600 * 1000))
        .signWith(getSigningKey())
        .compact();
}
```

### Resposta padronizada para token expirado

No `JwtAuthenticationFilter` (ou equivalente), quando `ExpiredJwtException` for capturada:

```java
} catch (ExpiredJwtException e) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\": \"TOKEN_EXPIRED\", \"message\": \"Sessão expirada. Faça login novamente.\"}");
    return;
}
```

Garantir que o `@ControllerAdvice` também trata `ExpiredJwtException` com `401` caso ela escape do filtro.

### Variável de ambiente no Railway

Garantir que `JWT_SECRET` está definido nas variáveis de ambiente do projeto no Railway. Se não estiver, a aplicação deve falhar no startup com mensagem clara (não em silêncio).

---

## Finalização

Após aplicar todos os bugs:

1. Rodar as migrations Flyway localmente e verificar que sobem sem erro
2. Testar cada endpoint novo/corrigido via Swagger (`/swagger-ui.html`)
3. Verificar que `@ControllerAdvice` cobre todas as novas exceções criadas
4. Rodar `./mvnw test` — todos os testes existentes devem continuar passando
5. Commitar: `fix: backend bugs round 1`
6. Push na branch `main` — Railway faz deploy automático
7. Atualizar `showco.md` com as decisões novas tomadas nesta sessão
