package com.ezponto.presentation.ponto;

import com.ezponto.application.ponto.PontoService;
import com.ezponto.domain.conta.Conta;
import com.ezponto.domain.conta.ContaRepository;
import com.ezponto.domain.equipe.EquipeEventoRepository;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.funcionario.Funcionario;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.funcionario.FuncionarioStatus;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import com.ezponto.presentation.evento.dto.MembroEquipeResponse;
import com.ezponto.presentation.ponto.dto.EventoAtivoResponse;
import com.ezponto.presentation.ponto.dto.RegistrarPontoRequest;
import com.ezponto.presentation.ponto.dto.RegistroPontoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ponto")
@RequiredArgsConstructor
public class PontoController {

    private final PontoService pontoService;
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

    @GetMapping("/historico")
    public ResponseEntity<List<RegistroPontoResponse>> historico(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long funcionarioId = resolverFuncionarioId(userDetails.getUsername());
        return ResponseEntity.ok(pontoService.historico(funcionarioId));
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
                                    .build())
                            .toList();
                    return ResponseEntity.ok(membros);
                })
                .orElse(ResponseEntity.ok(List.of()));
    }

    private Long resolverFuncionarioId(String email) {
        Conta conta = contaRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada"));
        Funcionario funcionario = funcionarioRepository.findByContaId(conta.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));
        return funcionario.getId();
    }
}
