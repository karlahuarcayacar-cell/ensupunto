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
import org.springframework.transaction.annotation.Transactional;
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

    @GetMapping("/api/top-platos")
    @ResponseBody
    public Map<String, Object> getTopPlatos() {
        return reporteService.obtenerTopPlatos();
    }

    @GetMapping("/api/historial-hoy")
    @ResponseBody
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistorialHoy() {
        return pedidoService.mapearPedidos(reporteService.obtenerHistorialHoy());
    }

    @GetMapping("/api/historial-completo")
    @ResponseBody
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistorialCompleto() {
        return pedidoService.mapearPedidos(reporteService.obtenerHistorialCompleto());
    }

    @GetMapping("/api/historial-periodo")
    @ResponseBody
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistorialPeriodo(@RequestParam String desde, @RequestParam String hasta) {
        LocalDate fechaDesde = LocalDate.parse(desde);
        LocalDate fechaHasta = LocalDate.parse(hasta);
        return pedidoService.mapearPedidos(reporteService.obtenerHistorialPeriodo(fechaDesde, fechaHasta));
    }

    @GetMapping("/reporte/ventas")
    public ResponseEntity<?> descargarReporteDia() {
        try {
            byte[] pdf = reporteService.generarReporteVentas();
            return responderPdf(pdf, "reporte_diario_" + LocalDate.now() + ".pdf");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @GetMapping("/reporte/historico")
    public ResponseEntity<?> descargarReporteHistorico() {
        try {
            byte[] pdf = reporteService.generarReporteVentasCompleto();
            return responderPdf(pdf, "reporte_historico_" + LocalDate.now() + ".pdf");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @GetMapping("/reporte/periodo")
    public ResponseEntity<?> descargarReportePeriodo(@RequestParam String desde, @RequestParam String hasta) {
        try {
            LocalDate fechaDesde = LocalDate.parse(desde);
            LocalDate fechaHasta = LocalDate.parse(hasta);
            byte[] pdf = reporteService.generarReporteVentasPeriodo(fechaDesde, fechaHasta);
            return responderPdf(pdf, "reporte_periodo_" + desde + "_" + hasta + ".pdf");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

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
        if (existente != null) {
            existente.setNombre(plato.getNombre());
            existente.setCategoria(plato.getCategoria());
            existente.setPrecio(plato.getPrecio());
            existente.setDescripcion(plato.getDescripcion());
            return platoService.guardar(existente);
        }
        return null;
    }

    @DeleteMapping("/api/platos/{id}")
    @ResponseBody
    public void eliminarPlato(@PathVariable Integer id) {
        platoService.eliminar(id);
    }

    @GetMapping("/api/usuarios")
    @ResponseBody
    public List<Usuario> listarUsuarios() {
        return usuarioService.listarTodos();
    }

    @PostMapping("/api/usuarios")
    @ResponseBody
    public ResponseEntity<?> crearUsuario(@RequestBody Map<String, Object> payload) {
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
        if (existente != null) {
            existente.setNombre(usuario.getNombre());
            existente.setRol(usuario.getRol());
            return usuarioService.guardar(existente);
        }
        return null;
    }

    @DeleteMapping("/api/usuarios/{id}")
    @ResponseBody
    public void eliminarUsuario(@PathVariable Integer id) {
        usuarioService.eliminar(id);
    }

    private ResponseEntity<byte[]> responderPdf(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
