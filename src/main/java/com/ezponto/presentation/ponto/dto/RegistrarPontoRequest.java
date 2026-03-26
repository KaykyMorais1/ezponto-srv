package com.ezponto.presentation.ponto.dto;

import com.ezponto.domain.ponto.TipoPonto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistrarPontoRequest {

    @NotNull(message = "Tipo de ponto é obrigatório")
    private TipoPonto tipo;

    @NotNull(message = "Latitude é obrigatória")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    private Double longitude;

    private String fotoBase64;
}
