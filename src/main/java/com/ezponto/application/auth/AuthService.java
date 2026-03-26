package com.ezponto.application.auth;

import com.ezponto.presentation.auth.dto.LoginRequest;
import com.ezponto.presentation.auth.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
