package com.dofe.axy8s.ui;

import com.dofe.axy8s.config.UiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiController {

    @Autowired
    private UiConfig uiConfig;

    @GetMapping("/ui")
    public String uiIndex() {
        if (!uiConfig.isUiEnabled()) {
            throw new RuntimeException("UI is disabled");
        }
        return "ui/index"; // sau này anh build frontend copy file html vào đây
    }
}
