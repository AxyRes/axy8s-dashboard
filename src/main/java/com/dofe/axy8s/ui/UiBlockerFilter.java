package com.dofe.axy8s.ui;

import com.dofe.axy8s.config.UiConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UiBlockerFilter extends HttpFilter {

    private final UiConfig uiConfig;

    public UiBlockerFilter(UiConfig uiConfig) {
        this.uiConfig = uiConfig;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String path = request.getRequestURI();

        if (!uiConfig.isUiEnabled() && path.startsWith("/ui")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "UI disabled");
            return;
        }

        chain.doFilter(request, response);
    }
}
