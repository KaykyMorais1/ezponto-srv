package com.ezponto.presentation.evento.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AtualizarEventoRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotNull(message = "Data de início é obrigatória")
    private OffsetDateTime dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    private OffsetDateTime dataFim;

    private String endereco;

    @NotNull(message = "Latitude é obrigatória")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    private Double longitude;

    @Min(50) @Max(500)
    private Integer raioMetros = 100;
}
