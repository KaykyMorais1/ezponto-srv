package com.ezponto.domain.funcionario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    boolean existsByCpf(String cpf);

    Optional<Funcionario> findByContaId(Long contaId);

    @Query("""
        SELECT f FROM Funcionario f
        WHERE f.status <> :statusInativo
        AND f.id NOT IN (
            SELECT ee.funcionario.id FROM EquipeEvento ee WHERE ee.evento.id = :eventoId
        )
        AND f.id NOT IN (
            SELECT ee.funcionario.id FROM EquipeEvento ee
            JOIN ee.evento e
            WHERE e.id != :eventoId
            AND e.cancelado = false
            AND e.dataInicio < :dataFim
            AND e.dataFim > :dataInicio
        )
    """)
    List<Funcionario> findDisponiveis(
        @Param("eventoId") Long eventoId,
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim,
        @Param("statusInativo") FuncionarioStatus statusInativo
    );
}
