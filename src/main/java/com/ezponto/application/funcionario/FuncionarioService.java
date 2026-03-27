package com.ezponto.application.funcionario;

import com.ezponto.presentation.funcionario.dto.*;

import java.util.List;

public interface FuncionarioService {
    List<FuncionarioResponse> listarTodos();
    FuncionarioResponse buscarPorId(Long id);
    FuncionarioResponse criar(CriarFuncionarioRequest request);
    FuncionarioResponse atualizar(Long id, AtualizarFuncionarioRequest request);
    void desativar(Long id);
    FuncionarioResponse atualizarStatus(Long id, AtualizarStatusFuncionarioRequest request);
    void alterarSenha(Long id, AlterarSenhaRequest request);
    void deletar(Long id);
    List<FuncionarioResponse> listarDisponiveisParaEvento(Long eventoId);
}
