package com.ezponto.domain.equipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EquipeEventoRepository extends JpaRepository<EquipeEvento, Long> {

    List<EquipeEvento> findByEventoId(Long eventoId);

    Optional<EquipeEvento> findByEventoIdAndFuncionarioId(Long eventoId, Long funcionarioId);

    boolean existsByEventoIdAndFuncionarioId(Long eventoId, Long funcionarioId);

    void deleteByEventoIdAndFuncionarioId(Long eventoId, Long funcionarioId);

    @Modifying
    @Query("DELETE FROM EquipeEvento e WHERE e.evento.id = :eventoId")
    void deleteAllByEventoId(@Param("eventoId") Long eventoId);

    @Modifying
    @Query("DELETE FROM EquipeEvento e WHERE e.funcionario.id = :funcionarioId")
    void deleteAllByFuncionarioId(@Param("funcionarioId") Long funcionarioId);

    @Query("SELECT e.funcionario.id FROM EquipeEvento e WHERE e.evento.id = :eventoId")
    List<Long> findFuncionarioIdsByEventoId(@Param("eventoId") Long eventoId);

    @Modifying
    @Query("DELETE FROM EquipeEvento e WHERE e.evento.id = :eventoId AND e.funcionario.id IN :funcionarioIds")
    void deleteByEventoIdAndFuncionarioIdIn(
            @Param("eventoId") Long eventoId,
            @Param("funcionarioIds") List<Long> funcionarioIds);

    @Query("""
            SELECT COUNT(e) > 0 FROM EquipeEvento e
            WHERE e.funcionario.id = :funcionarioId
              AND e.evento.id != :eventoId
              AND e.evento.dataInicio < :dataFim
              AND e.evento.dataFim > :dataInicio
            """)
    boolean existsOverlapForFuncionario(
            @Param("funcionarioId") Long funcionarioId,
            @Param("dataInicio") OffsetDateTime dataInicio,
            @Param("dataFim") OffsetDateTime dataFim,
            @Param("eventoId") Long eventoId);
}
