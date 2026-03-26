package com.ezponto.presentation.dashboard;

import com.ezponto.presentation.evento.dto.EventoResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private int totalFuncionarios;
    private int presentes;
    private int eventosAtivos;
    private int registrosPendentes;
    private List<EventoResponse> eventosDoDia;
    private List<UltimoRegistroResponse> ultimosRegistros;
}
