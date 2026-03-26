package com.ezponto.presentation.dashboard;

import com.ezponto.domain.ponto.TipoPonto;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class UltimoRegistroResponse {
    private Long id;
    private String funcionarioNome;
    private TipoPonto tipo;
    private OffsetDateTime timestampServidor;
    private String eventoNome;
}
