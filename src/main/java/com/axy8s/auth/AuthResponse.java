package com.dofe.axy8s.auth;

import com.dofe.axy8s.user.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String username;
    private Role role;
}
