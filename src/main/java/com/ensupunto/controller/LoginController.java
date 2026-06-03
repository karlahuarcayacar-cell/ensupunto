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

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarioService;

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("usuarioLogueado") != null) {
            Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
            return "redirect:/" + u.getRol();
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioService.login(username, password);
        
        if (usuario != null) {
            session.setAttribute("usuarioLogueado", usuario);
            return "redirect:/" + usuario.getRol();
        } else {
            redirectAttributes.addFlashAttribute("error", "Credenciales incorrectas");
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
