package com.ezponto.domain.ponto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface RegistroPontoRepository extends JpaRepository<RegistroPonto, Long> {

    List<RegistroPonto> findByFuncionarioIdOrderByTimestampServidorDesc(Long funcionarioId);

    List<RegistroPonto> findByEventoIdOrderByTimestampServidorDesc(Long eventoId);

    @Query("""
        SELECT r FROM RegistroPonto r
        JOIN FETCH r.funcionario
        ORDER BY r.timestampServidor DESC
    """)
    List<RegistroPonto> findUltimosRegistros(Pageable pageable);

    @Query("""
        SELECT r FROM RegistroPonto r
        WHERE r.timestampServidor >= :inicioDia
        AND r.timestampServidor < :fimDia
    """)
    List<RegistroPonto> findRegistrosDoDia(
        @Param("inicioDia") OffsetDateTime inicioDia,
        @Param("fimDia") OffsetDateTime fimDia
    );

    @Query("""
        SELECT COUNT(r) > 0 FROM RegistroPonto r
        WHERE r.funcionario.id = :funcionarioId
        AND r.evento.id = :eventoId
        AND r.tipo = :tipo
        AND r.timestampServidor >= :inicioDia
    """)
    boolean existeRegistroHoje(
        @Param("funcionarioId") Long funcionarioId,
        @Param("eventoId") Long eventoId,
        @Param("tipo") TipoPonto tipo,
        @Param("inicioDia") OffsetDateTime inicioDia
    );

    boolean existsByFuncionarioId(Long funcionarioId);

    @Query("""
        SELECT COUNT(r) > 0 FROM RegistroPonto r
        WHERE r.funcionario.id = :funcionarioId
        AND r.tipo = com.ezponto.domain.ponto.TipoPonto.ENTRADA
        AND r.timestampServidor >= :inicioDia
        AND r.timestampServidor < :fimDia
        AND NOT EXISTS (
            SELECT 1 FROM RegistroPonto r2
            WHERE r2.funcionario.id = :funcionarioId
            AND r2.evento = r.evento
            AND r2.tipo = com.ezponto.domain.ponto.TipoPonto.SAIDA
            AND r2.timestampServidor >= :inicioDia
            AND r2.timestampServidor < :fimDia
        )
    """)
    boolean estaPresente(
        @Param("funcionarioId") Long funcionarioId,
        @Param("inicioDia") OffsetDateTime inicioDia,
        @Param("fimDia") OffsetDateTime fimDia
    );

    @Query("""
        SELECT r FROM RegistroPonto r
        WHERE r.funcionario.id = :funcionarioId
        AND r.timestampServidor >= :inicio
        AND r.timestampServidor < :fimExclusivo
        ORDER BY r.timestampServidor DESC
    """)
    List<RegistroPonto> findHistoricoEntreDatas(
        @Param("funcionarioId") Long funcionarioId,
        @Param("inicio") OffsetDateTime inicio,
        @Param("fimExclusivo") OffsetDateTime fimExclusivo
    );

    @Query("""
        SELECT r FROM RegistroPonto r
        WHERE r.funcionario.id = :funcionarioId
        AND r.timestampServidor >= :inicioDia
        AND r.timestampServidor < :fimDia
        ORDER BY r.timestampServidor DESC
        LIMIT 1
    """)
    java.util.Optional<RegistroPonto> findUltimoRegistroDoDia(
        @Param("funcionarioId") Long funcionarioId,
        @Param("inicioDia") OffsetDateTime inicioDia,
        @Param("fimDia") OffsetDateTime fimDia
    );

    @Modifying
    @Query("DELETE FROM RegistroPonto r WHERE r.evento.id = :eventoId")
    void deleteAllByEventoId(@Param("eventoId") Long eventoId);
}
