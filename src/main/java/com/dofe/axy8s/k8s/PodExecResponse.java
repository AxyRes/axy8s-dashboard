package com.dofe.axy8s.k8s;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PodExecResponse {
    private String output;
}
