package com.ezponto.domain.equipe;

import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.funcionario.Funcionario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "equipe_evento")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipeEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "data_adicionado", nullable = false)
    private OffsetDateTime dataAdicionado = OffsetDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
