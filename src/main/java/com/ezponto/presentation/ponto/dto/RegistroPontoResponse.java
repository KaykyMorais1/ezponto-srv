package com.ezponto.presentation.ponto.dto;

import com.ezponto.domain.ponto.StatusPonto;
import com.ezponto.domain.ponto.TipoPonto;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class RegistroPontoResponse {
    private Long id;
    private TipoPonto tipo;
    private StatusPonto status;
    private Double latitude;
    private Double longitude;
    private String fotoUrl;
    private OffsetDateTime timestampServidor;
    private String eventoNome;
}
