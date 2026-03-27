package com.ezponto.presentation.funcionario.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CriarFuncionarioRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "CPF inválido")
    private String cpf;

    @NotBlank(message = "Cargo é obrigatório")
    private String cargo;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
        message = "Senha deve conter ao menos uma maiúscula, um número e um caractere especial"
    )
    private String senha;
}
