package com.dofe.axy8s.k8s;

public class PodExecResponse {
    private String output;

    public PodExecResponse() {
    }

    public PodExecResponse(String output) {
        this.output = output;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
