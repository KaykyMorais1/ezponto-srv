package com.ezponto.application.evento;

import com.ezponto.domain.equipe.EquipeEvento;
import com.ezponto.domain.equipe.EquipeEventoRepository;
import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.evento.EventoStatus;
import com.ezponto.domain.funcionario.Funcionario;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.ponto.RegistroPontoRepository;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import com.ezponto.domain.shared.exception.RegraDeNegocioException;
import com.ezponto.presentation.evento.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;
    private final EquipeEventoRepository equipeEventoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final RegistroPontoRepository registroPontoRepository;

    @Override
    public List<EventoResponse> listarTodos() {
        return eventoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EventoDetalheResponse buscarPorId(Long id) {
        return toDetalheResponse(buscarEvento(id));
    }

    @Override
    @Transactional
    public EventoResponse criar(CriarEventoRequest request) {
        validarDatas(request.getDataInicio(), request.getDataFim());

        Evento evento = Evento.builder()
                .nome(request.getNome())
                .dataInicio(request.getDataInicio())
                .dataFim(request.getDataFim())
                .endereco(request.getEndereco())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .raioMetros(request.getRaioMetros())
                .build();

        return toResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional
    public EventoResponse atualizar(Long id, AtualizarEventoRequest request) {
        Evento evento = buscarEvento(id);
        EventoStatus status = evento.getStatus();

        if (status == EventoStatus.EM_ANDAMENTO || status == EventoStatus.ENCERRADO) {
            throw new RegraDeNegocioException(
                    "Não é possível editar nome, datas ou localização de evento " + status.name());
        }

        validarDatas(request.getDataInicio(), request.getDataFim());

        evento.setNome(request.getNome());
        evento.setDataInicio(request.getDataInicio());
        evento.setDataFim(request.getDataFim());
        evento.setEndereco(request.getEndereco());
        evento.setLatitude(request.getLatitude());
        evento.setLongitude(request.getLongitude());
        evento.setRaioMetros(request.getRaioMetros());

        return toResponse(eventoRepository.save(evento));
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        Evento evento = buscarEvento(id);

        if (evento.getStatus() == EventoStatus.EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Não é possível deletar evento EM ANDAMENTO");
        }

        registroPontoRepository.deleteAllByEventoId(id);
        equipeEventoRepository.deleteAllByEventoId(id);
        eventoRepository.delete(evento);
    }

    @Override
    @Transactional
    public EventoDetalheResponse adicionarMembro(Long eventoId, AdicionarMembroRequest request) {
        Evento evento = buscarEvento(eventoId);

        if (evento.getStatus() == EventoStatus.ENCERRADO) {
            throw new RegraDeNegocioException("Não é possível adicionar membros a evento ENCERRADO");
        }

        if (equipeEventoRepository.existsByEventoIdAndFuncionarioId(eventoId, request.getFuncionarioId())) {
            throw new RegraDeNegocioException("Funcionário já está na equipe deste evento");
        }

        Funcionario funcionario = funcionarioRepository.findById(request.getFuncionarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));

        List<Funcionario> disponiveis = funcionarioRepository.findDisponiveis(
                eventoId, evento.getDataInicio(), evento.getDataFim());
        boolean disponivel = disponiveis.stream()
                .anyMatch(f -> f.getId().equals(funcionario.getId()));

        if (!disponivel) {
            throw new RegraDeNegocioException(
                    "Funcionário possui sobreposição de eventos no período informado");
        }

        EquipeEvento membro = EquipeEvento.builder()
                .evento(evento)
                .funcionario(funcionario)
                .dataAdicionado(OffsetDateTime.now())
                .build();

        equipeEventoRepository.save(membro);
        return toDetalheResponse(buscarEvento(eventoId));
    }

    @Override
    @Transactional
    public EventoDetalheResponse removerMembro(Long eventoId, Long funcionarioId) {
        Evento evento = buscarEvento(eventoId);

        if (evento.getStatus() == EventoStatus.ENCERRADO) {
            throw new RegraDeNegocioException("Não é possível remover membros de evento ENCERRADO");
        }

        EquipeEvento membro = equipeEventoRepository
                .findByEventoIdAndFuncionarioId(eventoId, funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado na equipe"));

        if (evento.getStatus() == EventoStatus.EM_ANDAMENTO) {
            if (!membro.getDataAdicionado().isAfter(evento.getDataInicio())) {
                throw new RegraDeNegocioException(
                        "Não é possível remover membro que já estava na equipe antes do início do evento");
            }
        }

        equipeEventoRepository.delete(membro);
        return toDetalheResponse(buscarEvento(eventoId));
    }

    // --- Helpers ---

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado: " + id));
    }

    private void validarDatas(OffsetDateTime inicio, OffsetDateTime fim) {
        if (!fim.isAfter(inicio)) {
            throw new RegraDeNegocioException("Data de fim deve ser posterior à data de início");
        }
    }

    private EventoResponse toResponse(Evento evento) {
        int totalMembros = equipeEventoRepository.findByEventoId(evento.getId()).size();
        return EventoResponse.builder()
                .id(evento.getId())
                .nome(evento.getNome())
                .dataInicio(evento.getDataInicio())
                .dataFim(evento.getDataFim())
                .endereco(evento.getEndereco())
                .latitude(evento.getLatitude())
                .longitude(evento.getLongitude())
                .raioMetros(evento.getRaioMetros())
                .status(evento.getStatus())
                .totalMembros(totalMembros)
                .createdAt(evento.getCreatedAt())
                .build();
    }

    private EventoDetalheResponse toDetalheResponse(Evento evento) {
        List<EquipeEvento> equipe = equipeEventoRepository.findByEventoId(evento.getId());
        List<MembroEquipeResponse> membros = equipe.stream()
                .map(ee -> MembroEquipeResponse.builder()
                        .funcionarioId(ee.getFuncionario().getId())
                        .nome(ee.getFuncionario().getNome())
                        .cargo(ee.getFuncionario().getCargo())
                        .dataAdicionado(ee.getDataAdicionado())
                        .presente(false)
                        .build())
                .toList();

        return EventoDetalheResponse.builder()
                .id(evento.getId())
                .nome(evento.getNome())
                .dataInicio(evento.getDataInicio())
                .dataFim(evento.getDataFim())
                .endereco(evento.getEndereco())
                .latitude(evento.getLatitude())
                .longitude(evento.getLongitude())
                .raioMetros(evento.getRaioMetros())
                .status(evento.getStatus())
                .equipe(membros)
                .createdAt(evento.getCreatedAt())
                .build();
    }
}
