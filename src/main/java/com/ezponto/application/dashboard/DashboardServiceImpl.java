package com.ezponto.application.dashboard;

import com.ezponto.domain.equipe.EquipeEventoRepository;
import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.funcionario.FuncionarioStatus;
import com.ezponto.domain.ponto.RegistroPonto;
import com.ezponto.domain.ponto.RegistroPontoRepository;
import com.ezponto.domain.ponto.StatusPonto;
import com.ezponto.presentation.dashboard.DashboardResponse;
import com.ezponto.presentation.dashboard.UltimoRegistroResponse;
import com.ezponto.presentation.evento.dto.EventoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final FuncionarioRepository funcionarioRepository;
    private final EventoRepository eventoRepository;
    private final RegistroPontoRepository registroPontoRepository;
    private final EquipeEventoRepository equipeEventoRepository;

    @Override
    public DashboardResponse buscar() {
        OffsetDateTime agora = OffsetDateTime.now();

        int total = (int) funcionarioRepository.count();
        int presentes = (int) funcionarioRepository.findAll().stream()
                .filter(f -> f.getStatus() == FuncionarioStatus.PRESENTE)
                .count();

        List<Evento> eventosAtivos = eventoRepository.findEmAndamento(agora);

        List<RegistroPonto> ultimosRegistros = registroPontoRepository
                .findUltimosRegistros(PageRequest.of(0, 5));

        int pendentes = (int) registroPontoRepository.findAll().stream()
                .filter(r -> r.getStatus() == StatusPonto.PENDENTE)
                .count();

        List<EventoResponse> eventosDoDia = eventosAtivos.stream()
                .map(e -> {
                    int membros = equipeEventoRepository.findByEventoId(e.getId()).size();
                    return EventoResponse.builder()
                            .id(e.getId())
                            .nome(e.getNome())
                            .dataInicio(e.getDataInicio())
                            .dataFim(e.getDataFim())
                            .status(e.getStatus())
                            .totalMembros(membros)
                            .build();
                })
                .toList();

        List<UltimoRegistroResponse> ultimos = ultimosRegistros.stream()
                .map(r -> UltimoRegistroResponse.builder()
                        .id(r.getId())
                        .funcionarioNome(r.getFuncionario().getNome())
                        .tipo(r.getTipo())
                        .timestampServidor(r.getTimestampServidor())
                        .eventoNome(r.getEvento().getNome())
                        .build())
                .toList();

        return DashboardResponse.builder()
                .totalFuncionarios(total)
                .presentes(presentes)
                .eventosAtivos(eventosAtivos.size())
                .registrosPendentes(pendentes)
                .eventosDoDia(eventosDoDia)
                .ultimosRegistros(ultimos)
                .build();
    }
}
