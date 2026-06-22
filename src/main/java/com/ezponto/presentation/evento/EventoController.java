package com.ezponto.presentation.evento;

import com.ezponto.application.evento.EventoService;
import com.ezponto.domain.ponto.RegistroPontoRepository;
import com.ezponto.presentation.evento.dto.*;
import com.ezponto.presentation.ponto.dto.RegistroPontoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/eventos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class EventoController {

    private final EventoService eventoService;
    private final RegistroPontoRepository registroPontoRepository;

    @GetMapping
    public ResponseEntity<List<EventoResponse>> listar() {
        return ResponseEntity.ok(eventoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoDetalheResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<EventoResponse> criar(@Valid @RequestBody CriarEventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarEventoRequest request
    ) {
        return ResponseEntity.ok(eventoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        eventoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/equipe")
    public ResponseEntity<EventoDetalheResponse> adicionarMembro(
            @PathVariable Long id,
            @Valid @RequestBody AdicionarMembroRequest request
    ) {
        return ResponseEntity.ok(eventoService.adicionarMembro(id, request));
    }

    @DeleteMapping("/{id}/equipe/{funcionarioId}")
    public ResponseEntity<EventoDetalheResponse> removerMembro(
            @PathVariable Long id,
            @PathVariable Long funcionarioId
    ) {
        return ResponseEntity.ok(eventoService.removerMembro(id, funcionarioId));
    }

    @PutMapping("/{id}/equipe")
    public ResponseEntity<Void> atualizarEquipe(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarEquipeRequest request
    ) {
        eventoService.atualizarEquipe(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        eventoService.cancelarEvento(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventoId}/equipe/{funcionarioId}/registros")
    public ResponseEntity<List<RegistroPontoResponse>> registrosMembro(
            @PathVariable Long eventoId,
            @PathVariable Long funcionarioId
    ) {
        List<RegistroPontoResponse> registros = registroPontoRepository
                .findByFuncionarioIdAndEventoIdOrderByTimestampServidorDesc(funcionarioId, eventoId)
                .stream()
                .map(r -> RegistroPontoResponse.builder()
                        .id(r.getId())
                        .tipo(r.getTipo())
                        .status(r.getStatus())
                        .latitude(r.getLatitude())
                        .longitude(r.getLongitude())
                        .fotoUrl(r.getFotoUrl())
                        .timestampServidor(r.getTimestampServidor())
                        .eventoNome(r.getEvento().getNome())
                        .build())
                .toList();
        return ResponseEntity.ok(registros);
    }
}
