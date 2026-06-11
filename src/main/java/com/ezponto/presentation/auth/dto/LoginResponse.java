package com.ezponto.presentation.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class LoginResponse {
    private String token;
    private String role;
    private Long contaId;
    private String email;
    private String nome;
    private Long funcionarioId;
    private String cpf;
    private String fotoPerfilUrl;
}
