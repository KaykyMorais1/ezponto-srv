package com.ezponto.application.evento;

import com.ezponto.presentation.evento.dto.*;

import java.util.List;

public interface EventoService {
    List<EventoResponse> listarTodos();
    EventoDetalheResponse buscarPorId(Long id);
    EventoResponse criar(CriarEventoRequest request);
    EventoResponse atualizar(Long id, AtualizarEventoRequest request);
    void deletar(Long id);
    EventoDetalheResponse adicionarMembro(Long eventoId, AdicionarMembroRequest request);
    EventoDetalheResponse removerMembro(Long eventoId, Long funcionarioId);
    void atualizarEquipe(Long eventoId, AtualizarEquipeRequest request);
    void cancelarEvento(Long eventoId);
}
