package com.ezponto.domain.shared.exception;

public class SenhaInvalidaException extends RuntimeException {
    public SenhaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
