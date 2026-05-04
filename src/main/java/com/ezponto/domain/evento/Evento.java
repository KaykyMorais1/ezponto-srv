package com.ezponto.domain.evento;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "eventos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "data_inicio", nullable = false)
    private OffsetDateTime dataInicio;

    @Column(name = "data_fim", nullable = false)
    private OffsetDateTime dataFim;

    private String endereco;

    @Builder.Default
    @Column(nullable = false)
    private Boolean cancelado = false;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Builder.Default
    @Column(name = "raio_metros", nullable = false)
    private Integer raioMetros = 100;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Transient
    public EventoStatus getStatus() {
        if (Boolean.TRUE.equals(cancelado)) return EventoStatus.CANCELADO;
        OffsetDateTime agora = OffsetDateTime.now();
        if (agora.isBefore(dataInicio)) return EventoStatus.A_ACONTECER;
        if (agora.isAfter(dataFim))     return EventoStatus.ENCERRADO;
        return EventoStatus.EM_ANDAMENTO;
    }
}
