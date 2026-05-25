# fix-editar-evento-equipe-backend.md

> Leia `showco.md` e `CLAUDE.md` antes de qualquer ação.
> Apenas backend Spring Boot.
> Commitar ao final: `feat: batch update equipe endpoint`
> Push `main` → Railway faz deploy automático.

---

## Contexto

Atualmente adicionar/remover membros da equipe faz uma chamada de API por operação.
O novo modelo envia **uma única chamada** com o estado final da equipe ao clicar em "Salvar alterações".

---

## Passo 1 — Novo endpoint `PUT /eventos/{id}/equipe`

Este endpoint recebe a lista final de IDs de funcionários da equipe e reconcilia com o estado atual do banco:
- Funcionários na lista nova mas não no banco → adicionar
- Funcionários no banco mas não na lista nova → remover
- Funcionários em ambos → manter (sem operação)

**Arquivo:** `src/main/java/com/ezponto/presentation/EventoController.java`

```java
@PutMapping("/{id}/equipe")
public ResponseEntity<Void> updateEquipe(
    @PathVariable Long id,
    @RequestBody @Valid UpdateEquipeRequest request
) {
    eventoService.updateEquipe(id, request);
    return ResponseEntity.noContent().build(); // 204
}
```

---

## Passo 2 — DTO de request

**Arquivo:** `src/main/java/com/ezponto/presentation/dto/UpdateEquipeRequest.java`

```java
public record UpdateEquipeRequest(
    @NotNull(message = "Lista de membros é obrigatória")
    List<Long> employeeIds
) {}
```

Lista vazia (`[]`) é válida — significa remover todos os membros.

---

## Passo 3 — Método no `EventoService`

**Arquivo:** `src/main/java/com/ezponto/application/EventoService.java`

```java
@Transactional
public void updateEquipe(Long eventoId, UpdateEquipeRequest request) {
    Evento evento = eventoRepository.findById(eventoId)
        .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado: " + eventoId));

    // Bloquear edição de equipe em eventos ENCERRADOS
    if (evento.getStatus() == StatusEvento.ENCERRADO) {
        throw new BusinessException("Não é possível editar a equipe de um evento encerrado.");
    }

    List<Long> idsNovos = request.employeeIds();

    // IDs atualmente no banco para este evento
    List<Long> idsAtuais = equipeRepository.findEmployeeIdsByEventoId(eventoId);

    // Remover os que saíram
    List<Long> idsRemover = idsAtuais.stream()
        .filter(id -> !idsNovos.contains(id))
        .toList();

    // Adicionar os que entraram
    List<Long> idsAdicionar = idsNovos.stream()
        .filter(id -> !idsAtuais.contains(id))
        .toList();

    if (!idsRemover.isEmpty()) {
        // Respeitar regra de remoção por status do evento (UC-10)
        if (evento.getStatus() == StatusEvento.EM_ANDAMENTO) {
            // Só permite remover membros adicionados APÓS o início do evento
            idsRemover.forEach(employeeId -> {
                Equipe membro = equipeRepository
                    .findByEventoIdAndEmployeeId(eventoId, employeeId)
                    .orElseThrow();
                if (!membro.getDataAdicionado().isAfter(evento.getDataInicio())) {
                    throw new BusinessException(
                        "Não é possível remover membros que já estavam na equipe no início do evento."
                    );
                }
            });
        }
        equipeRepository.deleteByEventoIdAndEmployeeIdIn(eventoId, idsRemover);
    }

    if (!idsAdicionar.isEmpty()) {
        // Validar disponibilidade de cada funcionário a adicionar
        idsAdicionar.forEach(employeeId -> {
            Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + employeeId));

            if (employee.getStatus() == EmployeeStatus.INATIVO) {
                throw new BusinessException(
                    "Funcionário inativo não pode ser adicionado à equipe: " + employeeId
                );
            }

            boolean sobreposicao = equipeRepository.existsOverlapForEmployee(
                employeeId, evento.getDataInicio(), evento.getDataFim(), eventoId
            );
            if (sobreposicao) {
                throw new BusinessException(
                    "Funcionário já está alocado em outro evento no mesmo período: " + employeeId
                );
            }
        });

        List<Equipe> novosMembrosMapped = idsAdicionar.stream()
            .map(employeeId -> Equipe.builder()
                .evento(evento)
                .employee(employeeRepository.getReferenceById(employeeId))
                .dataAdicionado(LocalDateTime.now())
                .build())
            .toList();

        equipeRepository.saveAll(novosMembrosMapped);
    }
}
```

---

## Passo 4 — Queries necessárias no `EquipeRepository`

**Arquivo:** `src/main/java/com/ezponto/domain/equipe/EquipeRepository.java`

Adicionar os métodos abaixo se não existirem:

```java
// IDs dos funcionários atualmente na equipe do evento
@Query("SELECT e.employee.id FROM Equipe e WHERE e.evento.id = :eventoId")
List<Long> findEmployeeIdsByEventoId(@Param("eventoId") Long eventoId);

// Buscar membro específico
Optional<Equipe> findByEventoIdAndEmployeeId(Long eventoId, Long employeeId);

// Remover membros específicos
@Modifying
@Query("DELETE FROM Equipe e WHERE e.evento.id = :eventoId AND e.employee.id IN :employeeIds")
void deleteByEventoIdAndEmployeeIdIn(
    @Param("eventoId") Long eventoId,
    @Param("employeeIds") List<Long> employeeIds
);

// Verificar sobreposição de período para o funcionário (excluindo o evento atual)
@Query("""
    SELECT COUNT(e) > 0 FROM Equipe e
    WHERE e.employee.id = :employeeId
      AND e.evento.id != :eventoId
      AND e.evento.dataInicio < :dataFim
      AND e.evento.dataFim > :dataInicio
    """)
boolean existsOverlapForEmployee(
    @Param("employeeId") Long employeeId,
    @Param("dataInicio") LocalDateTime dataInicio,
    @Param("dataFim") LocalDateTime dataFim,
    @Param("eventoId") Long eventoId
);
```

---

## Passo 5 — Testar via Swagger

- `PUT /eventos/{id}/equipe` com `{ "employeeIds": [1, 2, 3] }` → `204`
- Remover todos: `{ "employeeIds": [] }` → `204`
- Funcionário inativo na lista → `409`
- Evento encerrado → `409`
- Funcionário com sobreposição de período → `409`

---

## Finalização

1. `./mvnw test` — todos os testes devem passar
2. Commitar: `feat: batch update equipe endpoint`
3. Push `main` → aguardar deploy Railway
