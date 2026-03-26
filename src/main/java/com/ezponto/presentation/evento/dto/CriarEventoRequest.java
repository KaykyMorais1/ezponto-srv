package com.ezponto.presentation.evento.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CriarEventoRequest {

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

    @Min(value = 50, message = "Raio mínimo é 50m")
    @Max(value = 500, message = "Raio máximo é 500m")
    private Integer raioMetros = 100;
}
