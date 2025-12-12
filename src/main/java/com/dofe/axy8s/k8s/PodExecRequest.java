package com.dofe.axy8s.k8s;

import jakarta.validation.constraints.NotBlank;

public class PodExecRequest {

    @NotBlank
    private String command;

    // optional, nếu null/blank thì dùng container default
    private String container;

    public PodExecRequest() {
    }

    public PodExecRequest(String command, String container) {
        this.command = command;
        this.container = container;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }
}
