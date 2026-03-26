package com.ezponto.domain.ponto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
