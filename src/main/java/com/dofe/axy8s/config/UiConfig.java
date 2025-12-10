package com.dofe.axy8s.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UiConfig {

    @Value("${ui.enabled:false}")
    private boolean uiEnabled;

    public boolean isUiEnabled() {
        return uiEnabled;
    }
}
