package com.ezponto.application.funcionario;

import com.ezponto.domain.conta.Conta;
import com.ezponto.domain.conta.ContaRepository;
import com.ezponto.domain.conta.ContaRole;
import com.ezponto.domain.equipe.EquipeEventoRepository;
import com.ezponto.domain.evento.Evento;
import com.ezponto.domain.evento.EventoRepository;
import com.ezponto.domain.funcionario.Funcionario;
import com.ezponto.domain.funcionario.FuncionarioRepository;
import com.ezponto.domain.funcionario.FuncionarioStatus;
import com.ezponto.domain.ponto.RegistroPontoRepository;
import com.ezponto.domain.shared.exception.RecursoNaoEncontradoException;
import com.ezponto.domain.shared.exception.RegraDeNegocioException;
import com.ezponto.domain.shared.exception.SenhaInvalidaException;
import com.ezponto.presentation.funcionario.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FuncionarioServiceImpl implements FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final ContaRepository contaRepository;
    private final EventoRepository eventoRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistroPontoRepository registroPontoRepository;
    private final EquipeEventoRepository equipeEventoRepository;

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
                .status(FuncionarioStatus.DISPONIVEL)
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
    @Transactional
    public FuncionarioResponse atualizarStatus(Long id, AtualizarStatusFuncionarioRequest request) {
        Funcionario funcionario = buscarFuncionario(id);
        FuncionarioStatus novoStatus = request.status();

        boolean isInativando = novoStatus == FuncionarioStatus.INATIVO;
        boolean isReativando = novoStatus == FuncionarioStatus.DISPONIVEL
                && funcionario.getStatus() == FuncionarioStatus.INATIVO;

        if (!isInativando && !isReativando) {
            throw new RegraDeNegocioException(
                "Transição inválida. Use INATIVO para desativar ou DISPONIVEL para reativar."
            );
        }

        funcionario.setStatus(novoStatus);
        Conta conta = funcionario.getConta();
        if (isInativando) {
            conta.setAtivo(false);
        } else {
            conta.setAtivo(true);
        }
        contaRepository.save(conta);
        return toResponse(funcionarioRepository.save(funcionario));
    }

    @Override
    @Transactional
    public void alterarSenha(Long id, AlterarSenhaRequest request) {
        Funcionario funcionario = buscarFuncionario(id);
        Conta conta = funcionario.getConta();

        if (!passwordEncoder.matches(request.senhaAtual(), conta.getSenhaHash())) {
            throw new SenhaInvalidaException("Senha atual incorreta.");
        }

        conta.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        contaRepository.save(conta);
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        Funcionario funcionario = buscarFuncionario(id);

        if (registroPontoRepository.existsByFuncionarioId(id)) {
            throw new RegraDeNegocioException(
                "Este funcionário possui registros de ponto e não pode ser excluído. Use a opção de inativar."
            );
        }

        equipeEventoRepository.deleteAllByFuncionarioId(id);
        Conta conta = funcionario.getConta();
        funcionarioRepository.delete(funcionario);
        contaRepository.delete(conta);
    }

    @Override
    public List<FuncionarioResponse> listarDisponiveisParaEvento(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado: " + eventoId));

        return funcionarioRepository
                .findDisponiveis(eventoId, evento.getDataInicio(), evento.getDataFim(), FuncionarioStatus.INATIVO)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // --- Helpers ---

    private Funcionario buscarFuncionario(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado: " + id));
    }

    private static final ZoneId SP_ZONE = ZoneId.of("America/Sao_Paulo");

    private FuncionarioResponse toResponse(Funcionario f) {
        String eventoAtual = eventoRepository
                .findNomeEventoAtivoOuFuturoDoFuncionario(f.getId(), java.time.OffsetDateTime.now())
                .orElse(null);

        FuncionarioStatus displayStatus;
        if (f.getStatus() == FuncionarioStatus.INATIVO) {
            displayStatus = FuncionarioStatus.INATIVO;
        } else {
            LocalDate hoje = LocalDate.now(SP_ZONE);
            java.time.OffsetDateTime inicioDia = hoje.atStartOfDay(SP_ZONE).toOffsetDateTime();
            java.time.OffsetDateTime fimDia = hoje.plusDays(1).atStartOfDay(SP_ZONE).toOffsetDateTime();
            boolean presente = registroPontoRepository.estaPresente(f.getId(), inicioDia, fimDia);
            displayStatus = presente ? FuncionarioStatus.PRESENTE : FuncionarioStatus.DISPONIVEL;
        }

        return FuncionarioResponse.builder()
                .id(f.getId())
                .nome(f.getNome())
                .cpf(f.getCpf())
                .cargo(f.getCargo())
                .email(f.getConta().getEmail())
                .status(displayStatus)
                .createdAt(f.getCreatedAt())
                .eventoAtual(eventoAtual)
                .fotoPerfilUrl(f.getFotoPerfilUrl())
                .build();
    }
}
