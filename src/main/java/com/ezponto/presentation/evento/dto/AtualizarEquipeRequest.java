package com.ezponto.presentation.evento.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AtualizarEquipeRequest(
        @NotNull(message = "Lista de membros é obrigatória")
        List<Long> funcionarioIds
) {}
