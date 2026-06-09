package com.ezponto.application.auth;

import com.ezponto.config.security.JwtService;
import com.ezponto.domain.conta.Conta;
import com.ezponto.domain.conta.ContaRepository;
import com.ezponto.domain.conta.ContaRole;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.shared.exception.AcessoNegadoException;
import com.ezponto.presentation.auth.dto.LoginRequest;
import com.ezponto.presentation.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ContaRepository contaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        Conta conta = contaRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AcessoNegadoException("Credenciais inválidas"));

        if (!conta.getAtivo()) {
            throw new AcessoNegadoException("Conta inativa");
        }

        if (!passwordEncoder.matches(request.getSenha(), conta.getSenhaHash())) {
            throw new AcessoNegadoException("Credenciais inválidas");
        }

        String token = jwtService.gerarToken(conta);

        LoginResponse.LoginResponseBuilder builder = LoginResponse.builder()
                .token(token)
                .role(conta.getRole().name())
                .contaId(conta.getId())
                .email(conta.getEmail());

        if (conta.getRole() == ContaRole.FUNCIONARIO) {
            funcionarioRepository.findByContaId(conta.getId()).ifPresent(f -> {
                builder.nome(f.getNome());
                builder.funcionarioId(f.getId());
                builder.cpf(f.getCpf());
            });
        }

        return builder.build();
    }
}
