package com.ensupunto.controller;

import com.ensupunto.entity.Usuario;
import com.ensupunto.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * CONTROLADOR WEB (SPRING MVC): INICIO DE SESIÓN (LOGIN)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. @Controller: Indica a Spring que esta clase es un controlador MVC.
 *    A diferencia de @RestController (que serializa las respuestas directamente a JSON),
 *    un @Controller clásico de Spring MVC retorna nombres de vistas (cadenas String)
 *    que serán resueltas y renderizadas por un motor de plantillas (en este caso, Thymeleaf).
 * 
 * 2. HttpSession: Maneja el estado conversacional del usuario a través de múltiples peticiones HTTP,
 *    ya que el protocolo HTTP es por defecto sin estado (stateless). Almacenamos el objeto 'Usuario'
 *    bajo la clave 'usuarioLogueado' tras una autenticación exitosa.
 * 
 * 3. RedirectAttributes / Flash Attributes:
 *    Permiten transferir atributos que sobrevivirán a una redirección HTTP (Código 302).
 *    Son especialmente útiles para enviar mensajes temporales de error o éxito ("Credenciales incorrectas").
 */
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarioService;

    /**
     * Redirige la petición raíz (/) hacia el panel de login.
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    /**
     * Renderiza la página de login.
     * Si ya existe un usuario logueado en la sesión HTTP actual, lo redirige a su respectivo panel.
     */
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("usuarioLogueado") != null) {
            Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
            return "redirect:/" + u.getRol();
        }
        return "login"; // Devuelve la plantilla 'login.html' (Thymeleaf)
    }

    /**
     * Procesa el formulario POST de inicio de sesión.
     * 
     * @param username Captura el campo 'username' del formulario vía @RequestParam.
     * @param password Captura el campo 'password' del formulario vía @RequestParam.
     * @param session Instancia de la sesión HTTP actual.
     */
    @PostMapping("/login")
    public String loginSubmit(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        // Consultamos al servicio la validez de las credenciales
        Usuario usuario = usuarioService.login(username, password);
        
        if (usuario != null) {
            // Guardamos el objeto usuario en la sesión para ser verificado en AuthInterceptor
            session.setAttribute("usuarioLogueado", usuario);
            // Redirige al panel correspondiente según el rol (Ej: /mesero, /chef, /admin, /cajero)
            return "redirect:/" + usuario.getRol();
        } else {
            // Almacenamos el error en Flash Attributes y redirigimos de vuelta al login
            redirectAttributes.addFlashAttribute("error", "Credenciales incorrectas");
            return "redirect:/login";
        }
    }

    /**
     * Cierra la sesión del usuario.
     * Invalida completamente el objeto de sesión HTTP y destruye las variables guardadas.
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Destruye la sesión
        return "redirect:/login";
    }
}
