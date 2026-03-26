package com.ezponto.application.ponto;

import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.funcionario.Funcionario;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.ponto.RegistroPonto;
import com.ezponto.domain.ponto.RegistroPontoRepository;
import com.ezponto.domain.shared.GeoUtils;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import com.ezponto.domain.shared.exception.RegraDeNegocioException;
import com.ezponto.presentation.ponto.dto.EventoAtivoResponse;
import com.ezponto.presentation.ponto.dto.RegistrarPontoRequest;
import com.ezponto.presentation.ponto.dto.RegistroPontoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PontoServiceImpl implements PontoService {

    private final RegistroPontoRepository registroPontoRepository;
    private final EventoRepository eventoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FotoUploadService fotoUploadService;

    @Override
    @Transactional
    public RegistroPontoResponse registrar(Long funcionarioId, RegistrarPontoRequest request) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));

        Evento evento = eventoRepository
                .findEventoAtivoDoFuncionario(funcionarioId, OffsetDateTime.now())
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Nenhum evento ativo vinculado a este funcionário"));

        boolean dentroDaArea = GeoUtils.dentroDaArea(
                request.getLatitude(), request.getLongitude(),
                evento.getLatitude(), evento.getLongitude(),
                evento.getRaioMetros());

        if (!dentroDaArea) {
            throw new RegraDeNegocioException(
                    "Registro fora da área permitida pelo evento (raio: " + evento.getRaioMetros() + "m)");
        }

        String fotoUrl = null;
        if (request.getFotoBase64() != null && !request.getFotoBase64().isBlank()) {
            fotoUrl = fotoUploadService.upload(request.getFotoBase64(), funcionarioId);
        }

        RegistroPonto registro = RegistroPonto.builder()
                .funcionario(funcionario)
                .evento(evento)
                .tipo(request.getTipo())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .fotoUrl(fotoUrl)
                .timestampServidor(OffsetDateTime.now())
                .build();

        return toResponse(registroPontoRepository.save(registro));
    }

    @Override
    public List<RegistroPontoResponse> historico(Long funcionarioId) {
        return registroPontoRepository
                .findByFuncionarioIdOrderByTimestampServidorDesc(funcionarioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EventoAtivoResponse buscarEventoAtivo(Long funcionarioId) {
        return eventoRepository
                .findEventoAtivoDoFuncionario(funcionarioId, OffsetDateTime.now())
                .map(e -> EventoAtivoResponse.builder()
                        .id(e.getId())
                        .nome(e.getNome())
                        .latitude(e.getLatitude())
                        .longitude(e.getLongitude())
                        .raioMetros(e.getRaioMetros())
                        .build())
                .orElse(null);
    }

    private RegistroPontoResponse toResponse(RegistroPonto r) {
        return RegistroPontoResponse.builder()
                .id(r.getId())
                .tipo(r.getTipo())
                .status(r.getStatus())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .fotoUrl(r.getFotoUrl())
                .timestampServidor(r.getTimestampServidor())
                .eventoNome(r.getEvento().getNome())
                .build();
    }
}
