package com.ezponto.presentation.funcionario.dto;

import com.ezponto.domain.funcionario.FuncionarioStatus;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusFuncionarioRequest(
    @NotNull(message = "Status é obrigatório")
    FuncionarioStatus status
) {}
