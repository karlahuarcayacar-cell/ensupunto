package com.ensupunto.config;

import com.ensupunto.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * INTERCEPTOR DE AUTORIZACIÓN Y SEGURIDAD (HANDLER INTERCEPTOR)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. HandlerInterceptor: Es una interfaz provista por Spring MVC que actúa como un filtro o middleware.
 *    Permite interceptar las peticiones HTTP entrantes antes de que lleguen al controlador (Controller),
 *    o las respuestas antes de volver al cliente.
 * 
 * 2. @Component: Registra esta clase como un Bean gestionado por el contenedor IoC de Spring.
 * 
 * 3. Método preHandle: Se ejecuta antes de que la petición sea procesada por el Controller.
 *    - Si retorna 'true': Permite que el flujo de la petición continúe (pasa al controlador o al siguiente interceptor).
 *    - Si retorna 'false': Bloquea la petición y finaliza el procesamiento. Normalmente se acompaña
 *      de una redirección HTTP o envío de código de error.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // EXCEPCIÓN DE RUTAS PÚBLICAS: 
        // Permitimos la carga libre de recursos estáticos (CSS/JS), las peticiones a la raíz (/),
        // y los endpoints de login/logout/errores para evitar bucles infinitos de redirección.
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.equals("/login") || uri.equals("/logout") || uri.equals("/") || uri.equals("/error")) {
            return true;
        }

        // CONTROL DE SESIÓN ACTIVAS:
        // Se intenta obtener la sesión HTTP actual sin crear una nueva (request.getSession(false)).
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            // Si no hay sesión iniciada, redirige al Login y corta el flujo
            response.sendRedirect("/login");
            return false;
        }

        // CONTROL DE ACCESO SEGÚN ROL (AUTORIZACIÓN):
        // Obtenemos el objeto de usuario guardado en la sesión
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        String role = u.getRol(); // 'admin', 'mesero', 'chef', 'cajero'

        // Convención de Rutas: Se asume que cada rol tiene su área aislada (/mesero/**, /chef/**, etc.)
        // Si el URI actual coincide con la carpeta correspondiente a su rol, se concede acceso.
        if (uri.startsWith("/" + role)) {
            return true;
        }

        // Si intenta ingresar a la ruta de otro rol (ej: mesero intentando acceder a /admin),
        // se le deniega el acceso y se le redirige a su panel principal correspondiente.
        response.sendRedirect("/" + role);
        return false;
    }
}
