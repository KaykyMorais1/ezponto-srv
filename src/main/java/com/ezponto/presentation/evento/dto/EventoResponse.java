package com.ezponto.presentation.evento.dto;

import com.ezponto.domain.evento.EventoStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class EventoResponse {
    private Long id;
    private String nome;
    private OffsetDateTime dataInicio;
    private OffsetDateTime dataFim;
    private String endereco;
    private Double latitude;
    private Double longitude;
    private Integer raioMetros;
    private EventoStatus status;
    private Integer totalMembros;
    private OffsetDateTime createdAt;
}
