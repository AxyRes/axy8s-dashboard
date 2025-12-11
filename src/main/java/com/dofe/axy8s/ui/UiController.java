package com.dofe.axy8s.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiController {

    @GetMapping("/ui/login")
    public String loginPage() {
        return "ui/login";
    }

    @GetMapping("/ui")
    public String dashboard() {
        return "ui/dashboard";
    }
}
