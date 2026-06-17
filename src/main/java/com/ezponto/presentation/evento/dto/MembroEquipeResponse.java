package com.ezponto.presentation.evento.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class MembroEquipeResponse {
    private Long funcionarioId;
    private String nome;
    private String cargo;
    private OffsetDateTime dataAdicionado;
    private boolean presente;
    private String fotoPerfilUrl;
}
