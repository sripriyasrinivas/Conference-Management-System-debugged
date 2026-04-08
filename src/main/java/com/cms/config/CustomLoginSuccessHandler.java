package com.cms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String redirectUrl = "/dashboard"; // Default fallback

        // Priority-based redirection
        if (roles.contains("ROLE_ADMIN")) {
            redirectUrl = "/admin/dashboard";
        } else if (roles.contains("ROLE_AUTHOR")) {
            redirectUrl = "/author/dashboard";
        } else if (roles.contains("ROLE_REVIEWER")) {
            redirectUrl = "/reviewer/dashboard";
        } else if (roles.contains("ROLE_SPEAKER")) {
            redirectUrl = "/speaker/dashboard";
        } else if (roles.contains("ROLE_ATTENDEE")) {
            redirectUrl = "/attendee/dashboard";
        }

        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}