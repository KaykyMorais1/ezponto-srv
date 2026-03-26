package com.ezponto.config.security;

import com.ezponto.domain.conta.Conta;
import com.ezponto.domain.conta.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContaUserDetailsService implements UserDetailsService {

    private final ContaRepository contaRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Conta conta = contaRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada: " + email));

        return new org.springframework.security.core.userdetails.User(
                conta.getEmail(),
                conta.getSenhaHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + conta.getRole().name()))
        );
    }
}
