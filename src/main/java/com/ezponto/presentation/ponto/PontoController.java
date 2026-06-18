package com.ezponto.presentation.ponto;

import com.ezponto.application.funcionario.FuncionarioService;
import com.ezponto.application.ponto.FotoUploadService;
import com.ezponto.application.ponto.PontoService;
import com.ezponto.domain.conta.Conta;
import com.ezponto.domain.conta.ContaRepository;
import com.ezponto.domain.equipe.EquipeEventoRepository;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.funcionario.Funcionario;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.funcionario.FuncionarioStatus;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import com.ezponto.presentation.evento.dto.EventoResponse;
import com.ezponto.presentation.evento.dto.MembroEquipeResponse;
import com.ezponto.presentation.funcionario.dto.AlterarSenhaRequest;
import com.ezponto.presentation.ponto.dto.AtualizarFotoPerfilRequest;
import com.ezponto.presentation.ponto.dto.EstadoPontoResponse;
import com.ezponto.presentation.ponto.dto.EventoAtivoResponse;
import com.ezponto.presentation.ponto.dto.FotoPerfilResponse;
import com.ezponto.presentation.ponto.dto.RegistrarPontoRequest;
import com.ezponto.presentation.ponto.dto.RegistroPontoResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ponto")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PontoController {

    private final PontoService pontoService;
    private final FuncionarioService funcionarioService;
    private final FotoUploadService fotoUploadService;
    private final ContaRepository contaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final EventoRepository eventoRepository;
    private final EquipeEventoRepository equipeEventoRepository;

    @PostMapping
    public ResponseEntity<RegistroPontoResponse> registrar(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RegistrarPontoRequest request
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pontoService.registrar(funcionarioId, request));
    }

    private static final ZoneId SP_ZONE = ZoneId.of("America/Sao_Paulo");

    @GetMapping("/historico")
    public ResponseEntity<List<RegistroPontoResponse>> historico(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        OffsetDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay(SP_ZONE).toOffsetDateTime() : null;
        OffsetDateTime fimExclusivo = dataFim != null ? dataFim.plusDays(1).atStartOfDay(SP_ZONE).toOffsetDateTime() : null;
        return ResponseEntity.ok(pontoService.historico(funcionarioId, inicio, fimExclusivo));
    }

    @GetMapping("/evento-ativo")
    public ResponseEntity<EventoAtivoResponse> eventoAtivo(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        EventoAtivoResponse response = pontoService.buscarEventoAtivo(funcionarioId);
        if (response == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/equipe")
    public ResponseEntity<List<MembroEquipeResponse>> minhaEquipe(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        return eventoRepository
                .findEventoAtivoDoFuncionario(funcionarioId, OffsetDateTime.now())
                .map(evento -> {
                    List<MembroEquipeResponse> membros = equipeEventoRepository
                            .findByEventoId(evento.getId())
                            .stream()
                            .map(ee -> MembroEquipeResponse.builder()
                                    .funcionarioId(ee.getFuncionario().getId())
                                    .nome(ee.getFuncionario().getNome())
                                    .cargo(ee.getFuncionario().getCargo())
                                    .dataAdicionado(ee.getDataAdicionado())
                                    .presente(ee.getFuncionario().getStatus() == FuncionarioStatus.PRESENTE)
                                    .fotoPerfilUrl(ee.getFuncionario().getFotoPerfilUrl())
                                    .build())
                            .toList();
                    return ResponseEntity.ok(membros);
                })
                .orElse(ResponseEntity.ok(List.of()));
    }

    @GetMapping("/meus-eventos")
    public ResponseEntity<List<EventoResponse>> meusEventos(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        return ResponseEntity.ok(pontoService.listarMeusEventos(funcionarioId));
    }

    @GetMapping("/estado-atual")
    public ResponseEntity<EstadoPontoResponse> estadoAtual(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        return ResponseEntity.ok(pontoService.estadoAtual(funcionarioId));
    }

    @GetMapping("/me/foto")
    public ResponseEntity<FotoPerfilResponse> minhaFoto(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));
        return ResponseEntity.ok(FotoPerfilResponse.builder()
                .fotoPerfilUrl(funcionario.getFotoPerfilUrl())
                .build());
    }

    @PatchMapping("/foto")
    public ResponseEntity<FotoPerfilResponse> atualizarFotoPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AtualizarFotoPerfilRequest request
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));
        String url = fotoUploadService.upload(request.getFotoBase64(), funcionarioId);
        funcionario.setFotoPerfilUrl(url);
        funcionarioRepository.save(funcionario);
        return ResponseEntity.ok(FotoPerfilResponse.builder().fotoPerfilUrl(url).build());
    }

    @PatchMapping("/senha")
    public ResponseEntity<Void> alterarSenha(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AlterarSenhaRequest request
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        funcionarioService.alterarSenha(funcionarioId, request);
        return ResponseEntity.noContent().build();
    }

    private Long resolverFuncionarioId(String email) {
        Conta conta = contaRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada"));
        Funcionario funcionario = funcionarioRepository.findByContaId(conta.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));
        return funcionario.getId();
    }
}
