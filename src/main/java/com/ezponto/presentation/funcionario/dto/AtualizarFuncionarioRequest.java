package com.ezponto.presentation.funcionario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AtualizarFuncionarioRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Cargo é obrigatório")
    private String cargo;
}
