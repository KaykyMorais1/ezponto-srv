package com.ezponto.presentation.ponto.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventoAtivoResponse {
    private Long id;
    private String nome;
    private Double latitude;
    private Double longitude;
    private Integer raioMetros;
}
