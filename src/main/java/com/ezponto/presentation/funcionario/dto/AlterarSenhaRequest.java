package com.ezponto.presentation.funcionario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlterarSenhaRequest(
    @NotBlank(message = "Senha atual é obrigatória")
    String senhaAtual,

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "Nova senha deve ter no mínimo 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
        message = "Nova senha deve conter ao menos uma maiúscula, um número e um caractere especial"
    )
    String novaSenha
) {}
