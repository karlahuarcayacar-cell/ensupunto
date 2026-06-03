package com.ensupunto.config;

import com.ensupunto.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // Allow static resources, login and logout
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.equals("/login") || uri.equals("/logout") || uri.equals("/") || uri.equals("/error")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect("/login");
            return false;
        }

        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        String role = u.getRol();

        // Check path starts with role (e.g. /mesero, /chef)
        if (uri.startsWith("/" + role)) {
            return true;
        }

        // If trying to access another role's area, redirect back
        response.sendRedirect("/" + role);
        return false;
    }
}
