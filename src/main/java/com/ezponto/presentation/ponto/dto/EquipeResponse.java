package com.ezponto.presentation.ponto.dto;

import com.ezponto.presentation.evento.dto.MembroEquipeResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EquipeResponse {
    private String eventoNome;
    private List<MembroEquipeResponse> membros;
}
