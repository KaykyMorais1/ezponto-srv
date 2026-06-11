package com.ezponto.application.ponto;

import com.ezponto.presentation.evento.dto.EventoResponse;
import com.ezponto.presentation.ponto.dto.EventoAtivoResponse;
import com.ezponto.presentation.ponto.dto.RegistrarPontoRequest;
import com.ezponto.presentation.ponto.dto.RegistroPontoResponse;

import java.time.OffsetDateTime;
import java.util.List;

public interface PontoService {
    RegistroPontoResponse registrar(Long funcionarioId, RegistrarPontoRequest request);
    List<RegistroPontoResponse> historico(Long funcionarioId, OffsetDateTime inicio, OffsetDateTime fimExclusivo);
    EventoAtivoResponse buscarEventoAtivo(Long funcionarioId);
    List<EventoResponse> listarMeusEventos(Long funcionarioId);
}
