package com.dance101.steptodance.auth.service;

import com.dance101.steptodance.auth.data.response.TokenResponse;

public interface AuthService {
    TokenResponse kakaoLogin(String code);
}