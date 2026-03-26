package com.ezponto.presentation.evento.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdicionarMembroRequest {
    @NotNull(message = "ID do funcionário é obrigatório")
    private Long funcionarioId;
}
