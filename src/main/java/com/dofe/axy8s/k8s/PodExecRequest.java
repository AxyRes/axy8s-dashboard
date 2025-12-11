package com.dofe.axy8s.k8s;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PodExecRequest {

    @NotBlank
    private String command;

    // optional, nếu null/blank thì dùng container default
    private String container;
}
