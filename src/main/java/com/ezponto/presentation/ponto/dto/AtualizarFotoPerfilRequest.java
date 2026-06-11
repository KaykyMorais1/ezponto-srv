package com.ezponto.presentation.ponto.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AtualizarFotoPerfilRequest {
    @NotBlank
    private String fotoBase64;
}
