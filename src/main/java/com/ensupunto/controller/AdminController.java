package com.ensupunto.controller;

import com.ensupunto.entity.Plato;
import com.ensupunto.entity.Usuario;
import com.ensupunto.service.PedidoService;
import com.ensupunto.service.PlatoService;
import com.ensupunto.service.ReporteService;
import com.ensupunto.service.UsuarioService;


import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
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
    public ResponseEntity<?> crearUsuario(@RequestBody Map<String, Object> payload) {
        System.out.println(">>> PAYLOAD RECIBIDO: " + payload);  // ← agrega esto
        try {
            Usuario usuario = new Usuario();
            usuario.setNombre(payload.get("nombre").toString());
            usuario.setNombreUsuario(payload.get("nombreUsuario").toString());
            usuario.setContrasena(payload.get("contrasena").toString());
            usuario.setRol(payload.get("rol").toString());
            usuario.setActivo(true);
            Usuario saved = usuarioService.guardar(usuario);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
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

    @GetMapping("/api/reporte/descargar")
    public ResponseEntity<?> descargarReporte() {
        try {
            byte[] pdf = reporteService.generarReporteVentas();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=reporte_ventas_" + LocalDate.now() + ".pdf");

            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al generar PDF: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
    
}
