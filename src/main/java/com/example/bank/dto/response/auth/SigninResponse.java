package com.example.bank.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class SigninResponse {
    private int status;
    private String message;
    private TokenResponse tokens;
}
