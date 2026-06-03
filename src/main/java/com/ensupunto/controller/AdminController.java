package com.ensupunto.controller;

import com.ensupunto.entity.Plato;
import com.ensupunto.entity.Usuario;
import com.ensupunto.service.PedidoService;
import com.ensupunto.service.PlatoService;
import com.ensupunto.service.ReporteService;
import com.ensupunto.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ReporteService reporteService;
    private final PedidoService pedidoService;
    private final PlatoService platoService;
    private final UsuarioService usuarioService;

    @GetMapping
    public String adminPage(Model model, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        if (u == null || !u.getRol().equals("admin")) return "redirect:/login";
        
        model.addAttribute("usuario", u);
        return "admin";
    }

    @GetMapping("/api/kpis")
    @ResponseBody
    public Map<String, Object> getKpis() {
        return reporteService.obtenerKpisDelDia();
    }

    @GetMapping("/api/ventas-categoria")
    @ResponseBody
    public Map<String, Object> getVentasCategoria() {
        return reporteService.obtenerVentasPorCategoria();
    }

    // --- CRUD PLATOS ---
    @GetMapping("/api/platos")
    @ResponseBody
    public List<Plato> listarPlatos() {
        return platoService.listarTodos();
    }

    @PostMapping("/api/platos")
    @ResponseBody
    public Plato crearPlato(@RequestBody Plato plato) {
        return platoService.guardar(plato);
    }

    @PutMapping("/api/platos/{id}")
    @ResponseBody
    public Plato actualizarPlato(@PathVariable Integer id, @RequestBody Plato plato) {
        Plato existente = platoService.buscarPorId(id);
        if(existente != null) {
            existente.setNombre(plato.getNombre());
            existente.setCategoria(plato.getCategoria());
            existente.setPrecio(plato.getPrecio());
            existente.setDescripcion(plato.getDescripcion());
            return platoService.guardar(existente);
        }
        return null;
    }

    // --- CRUD USUARIOS ---
    @GetMapping("/api/usuarios")
    @ResponseBody
    public List<Usuario> listarUsuarios() {
        return usuarioService.listarTodos();
    }

    @PostMapping("/api/usuarios")
    @ResponseBody
    public Usuario crearUsuario(@RequestBody Usuario usuario) {
        return usuarioService.guardar(usuario);
    }

    @PutMapping("/api/usuarios/{id}")
    @ResponseBody
    public Usuario actualizarUsuario(@PathVariable Integer id, @RequestBody Usuario usuario) {
        Usuario existente = usuarioService.buscarPorId(id);
        if(existente != null) {
            existente.setNombre(usuario.getNombre());
            existente.setRol(usuario.getRol());
            // No actualizamos contraseña desde aquí en este flujo básico
            return usuarioService.guardar(existente);
        }
        return null;
    }
}
