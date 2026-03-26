package com.ezponto.application.funcionario;

import com.ezponto.domain.conta.Conta;
import com.ezponto.domain.conta.ContaRepository;
import com.ezponto.domain.conta.ContaRole;
import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.funcionario.Funcionario;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.funcionario.FuncionarioStatus;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import com.ezponto.domain.shared.exception.RegraDeNegocioException;
import com.ezponto.presentation.funcionario.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FuncionarioServiceImpl implements FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final ContaRepository contaRepository;
    private final EventoRepository eventoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<FuncionarioResponse> listarTodos() {
        return funcionarioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public FuncionarioResponse buscarPorId(Long id) {
        return toResponse(buscarFuncionario(id));
    }

    @Override
    @Transactional
    public FuncionarioResponse criar(CriarFuncionarioRequest request) {
        if (funcionarioRepository.existsByCpf(request.getCpf())) {
            throw new RegraDeNegocioException("CPF já cadastrado: " + request.getCpf());
        }
        if (contaRepository.existsByEmail(request.getEmail())) {
            throw new RegraDeNegocioException("E-mail já cadastrado: " + request.getEmail());
        }

        Conta conta = Conta.builder()
                .email(request.getEmail())
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .role(ContaRole.FUNCIONARIO)
                .ativo(true)
                .build();
        contaRepository.save(conta);

        Funcionario funcionario = Funcionario.builder()
                .conta(conta)
                .nome(request.getNome())
                .cpf(request.getCpf())
                .cargo(request.getCargo())
                .status(FuncionarioStatus.ATIVO)
                .build();

        return toResponse(funcionarioRepository.save(funcionario));
    }

    @Override
    @Transactional
    public FuncionarioResponse atualizar(Long id, AtualizarFuncionarioRequest request) {
        Funcionario funcionario = buscarFuncionario(id);
        funcionario.setNome(request.getNome());
        funcionario.setCargo(request.getCargo());
        return toResponse(funcionarioRepository.save(funcionario));
    }

    @Override
    @Transactional
    public void desativar(Long id) {
        Funcionario funcionario = buscarFuncionario(id);
        funcionario.setStatus(FuncionarioStatus.INATIVO);
        Conta conta = funcionario.getConta();
        conta.setAtivo(false);
        contaRepository.save(conta);
        funcionarioRepository.save(funcionario);
    }

    @Override
    public List<FuncionarioResponse> listarDisponiveisParaEvento(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado: " + eventoId));

        return funcionarioRepository
                .findDisponiveis(eventoId, evento.getDataInicio(), evento.getDataFim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // --- Helpers ---

    private Funcionario buscarFuncionario(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado: " + id));
    }

    private FuncionarioResponse toResponse(Funcionario f) {
        return FuncionarioResponse.builder()
                .id(f.getId())
                .nome(f.getNome())
                .cpf(f.getCpf())
                .cargo(f.getCargo())
                .email(f.getConta().getEmail())
                .status(f.getStatus())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
