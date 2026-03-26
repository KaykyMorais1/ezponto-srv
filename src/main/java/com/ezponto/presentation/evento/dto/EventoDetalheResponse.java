package com.ezponto.presentation.evento.dto;

import com.ezponto.domain.evento.EventoStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class EventoDetalheResponse {
    private Long id;
    private String nome;
    private OffsetDateTime dataInicio;
    private OffsetDateTime dataFim;
    private String endereco;
    private Double latitude;
    private Double longitude;
    private Integer raioMetros;
    private EventoStatus status;
    private List<MembroEquipeResponse> equipe;
    private OffsetDateTime createdAt;
}
