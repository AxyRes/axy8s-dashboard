package com.dofe.axy8s.ui;

import com.dofe.axy8s.ui.config.UiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiController {

    @Autowired
    private UiConfig uiConfig;

    private void ensureEnabled() {
        if (!uiConfig.isUiEnabled()) {
            throw new RuntimeException("UI is disabled");
        }
    }

    @GetMapping("/ui/login")
    public String loginPage() {
        ensureEnabled();
        return "ui/login";
    }

    @GetMapping("/ui")
    public String dashboard() {
        ensureEnabled();
        return "ui/dashboard";
    }
}
