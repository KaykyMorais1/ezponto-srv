package com.ezponto.presentation.funcionario;

import com.ezponto.application.funcionario.FuncionarioService;
import com.ezponto.presentation.funcionario.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/funcionarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    @GetMapping
    public ResponseEntity<List<FuncionarioResponse>> listar() {
        return ResponseEntity.ok(funcionarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<FuncionarioResponse> criar(@Valid @RequestBody CriarFuncionarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionarioService.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FuncionarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarFuncionarioRequest request
    ) {
        return ResponseEntity.ok(funcionarioService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FuncionarioResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusFuncionarioRequest request
    ) {
        return ResponseEntity.ok(funcionarioService.atualizarStatus(id, request));
    }

    @PatchMapping("/{id}/senha")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable Long id,
            @Valid @RequestBody AlterarSenhaRequest request
    ) {
        funcionarioService.alterarSenha(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        funcionarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/disponiveis/{eventoId}")
    public ResponseEntity<List<FuncionarioResponse>> disponiveis(@PathVariable Long eventoId) {
        return ResponseEntity.ok(funcionarioService.listarDisponiveisParaEvento(eventoId));
    }
}
