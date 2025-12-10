package com.dofe.axy8s.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotNull
    private Role role;

    /**
     * allowedNamespaces:
     *  - "*" hoặc "ns1,ns2"
     *  - KHÔNG tự động thêm "default"
     */
    private String allowedNamespaces;

    private boolean active = true;
}
