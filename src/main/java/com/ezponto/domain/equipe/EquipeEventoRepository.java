package com.ezponto.domain.equipe;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EquipeEventoRepository extends JpaRepository<EquipeEvento, Long> {

    List<EquipeEvento> findByEventoId(Long eventoId);

    Optional<EquipeEvento> findByEventoIdAndFuncionarioId(Long eventoId, Long funcionarioId);

    boolean existsByEventoIdAndFuncionarioId(Long eventoId, Long funcionarioId);

    void deleteByEventoIdAndFuncionarioId(Long eventoId, Long funcionarioId);
}
