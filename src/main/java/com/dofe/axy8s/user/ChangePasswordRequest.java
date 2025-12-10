package com.dofe.axy8s.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String newPassword;
}
