package com.ezponto.domain.funcionario;

import com.ezponto.domain.conta.Conta;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "funcionarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, updatable = false)
    private String cpf;

    @Column(nullable = false)
    private String cargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuncionarioStatus status = FuncionarioStatus.ATIVO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
