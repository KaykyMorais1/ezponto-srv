package com.ezponto.domain.evento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    @Query("SELECT e FROM Evento e WHERE e.dataInicio <= :agora AND e.dataFim >= :agora")
    List<Evento> findEmAndamento(@Param("agora") OffsetDateTime agora);

    @Query("""
        SELECT e FROM Evento e
        JOIN EquipeEvento ee ON ee.evento.id = e.id
        WHERE ee.funcionario.id = :funcionarioId
        AND e.dataInicio <= :agora AND e.dataFim >= :agora
    """)
    Optional<Evento> findEventoAtivoDoFuncionario(
        @Param("funcionarioId") Long funcionarioId,
        @Param("agora") OffsetDateTime agora
    );

    @Query("""
        SELECT e.nome FROM Evento e
        JOIN EquipeEvento ee ON ee.evento.id = e.id
        WHERE ee.funcionario.id = :funcionarioId
        AND e.cancelado = false
        AND e.dataFim >= :agora
        ORDER BY e.dataInicio ASC
    """)
    Optional<String> findNomeEventoAtivoOuFuturoDoFuncionario(
        @Param("funcionarioId") Long funcionarioId,
        @Param("agora") OffsetDateTime agora
    );

    @Query("""
        SELECT e FROM Evento e
        JOIN EquipeEvento ee ON ee.evento.id = e.id
        WHERE ee.funcionario.id = :funcionarioId
        ORDER BY e.dataInicio ASC
    """)
    List<Evento> findByFuncionarioId(@Param("funcionarioId") Long funcionarioId);
}
