package com.ezponto.presentation.funcionario.dto;

import com.ezponto.domain.funcionario.FuncionarioStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class FuncionarioResponse {
    private Long id;
    private String nome;
    private String cpf;
    private String cargo;
    private String email;
    private FuncionarioStatus status;
    private OffsetDateTime createdAt;
    private String eventoAtual;
    private String fotoPerfilUrl;
}
