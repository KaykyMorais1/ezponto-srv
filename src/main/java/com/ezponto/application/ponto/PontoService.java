package com.ezponto.application.ponto;

import com.ezponto.presentation.ponto.dto.EventoAtivoResponse;
import com.ezponto.presentation.ponto.dto.RegistrarPontoRequest;
import com.ezponto.presentation.ponto.dto.RegistroPontoResponse;

import java.util.List;

public interface PontoService {
    RegistroPontoResponse registrar(Long funcionarioId, RegistrarPontoRequest request);
    List<RegistroPontoResponse> historico(Long funcionarioId);
    EventoAtivoResponse buscarEventoAtivo(Long funcionarioId);
}
