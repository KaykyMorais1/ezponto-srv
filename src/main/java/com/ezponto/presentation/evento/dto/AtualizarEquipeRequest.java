package com.ezponto.presentation.evento.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AtualizarEquipeRequest(
        @JsonProperty("employeeIds")
        @NotNull(message = "Lista de membros é obrigatória")
        List<Long> funcionarioIds
) {}
