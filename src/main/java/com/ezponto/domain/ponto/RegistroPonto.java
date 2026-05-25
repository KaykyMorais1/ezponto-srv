package com.ezponto.domain.ponto;

import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.funcionario.Funcionario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "registros_ponto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistroPonto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPonto tipo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPonto status = StatusPonto.APROVADO;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Builder.Default
    @Column(name = "timestamp_servidor", nullable = false)
    private OffsetDateTime timestampServidor = OffsetDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
