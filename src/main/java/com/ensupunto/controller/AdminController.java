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
        return reportesTab(model, u, false);
    }

    // --- REPORTES TAB ---

    @GetMapping("/reportes")
    public String reportesTab(Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        model.addAttribute("usuario", u);
        model.addAttribute("activeTab", "reportes");
        model.addAttribute("contentTemplate", "reportes");
        model.addAttribute("kpis", reporteService.obtenerKpisDelDia());
        model.addAttribute("ventasCategoria", reporteService.obtenerVentasPorCategoria());
        model.addAttribute("topPlatos", reporteService.obtenerTopPlatos());
        model.addAttribute("historialHoy", pedidoService.mapearPedidos(reporteService.obtenerHistorialHoy()));
        
        return hxRequest ? "admin/reportes :: content" : "admin/layout";
    }

    @GetMapping("/reportes/dashboard")
    public String dashboardFragment(Model model) {
        model.addAttribute("ventasCategoria", reporteService.obtenerVentasPorCategoria());
        model.addAttribute("topPlatos", reporteService.obtenerTopPlatos());
        model.addAttribute("historialHoy", pedidoService.mapearPedidos(reporteService.obtenerHistorialHoy()));
        return "admin/reportes/dashboard :: content";
    }

    @GetMapping("/reportes/dia")
    public String reporteDiaFragment(Model model) {
        model.addAttribute("pedidos", pedidoService.mapearPedidos(reporteService.obtenerHistorialHoy()));
        return "admin/reportes/dia";
    }

    @GetMapping("/reportes/historico")
    public String reporteHistoricoFragment(Model model) {
        model.addAttribute("pedidos", pedidoService.mapearPedidos(reporteService.obtenerHistorialCompleto()));
        return "admin/reportes/historico";
    }

    @GetMapping("/reportes/personalizado")
    public String reportePersonalizadoFragment() {
        return "admin/reportes/personalizado :: content";
    }

    @GetMapping("/reportes/personalizado/buscar")
    public String buscarPorPeriodo(@RequestParam String desde, @RequestParam String hasta, Model model) {
        LocalDate fechaDesde = LocalDate.parse(desde);
        LocalDate fechaHasta = LocalDate.parse(hasta);
        model.addAttribute("pedidos", pedidoService.mapearPedidos(reporteService.obtenerHistorialPeriodo(fechaDesde, fechaHasta)));
        return "admin/reportes/tabla_pedidos :: render(${pedidos}, 'Reporte por Período', 'Desde ' + " + desde + " + ' hasta ' + " + hasta + ", '/admin/reporte/periodo?desde=' + " + desde + " + '&hasta=' + " + hasta + ", 'personalizado')";
    }

    // --- PLATOS TAB ---

    @GetMapping("/platos")
    public String platosTab(Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        model.addAttribute("usuario", u);
        model.addAttribute("activeTab", "platos");
        model.addAttribute("contentTemplate", "platos/list");
        model.addAttribute("platos", platoService.listarTodos());
        return hxRequest ? "admin/platos/list :: content" : "admin/layout";
    }

    @GetMapping("/platos/nuevo")
    public String nuevoPlatoForm(Model model) {
        model.addAttribute("plato", new Plato());
        return "admin/platos/form_modal :: form";
    }

    @GetMapping("/platos/editar/{id}")
    public String editarPlatoForm(@PathVariable Integer id, Model model) {
        model.addAttribute("plato", platoService.buscarPorId(id));
        return "admin/platos/form_modal :: form";
    }

    @PostMapping("/platos/guardar")
    public String guardarPlato(@ModelAttribute Plato plato, Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        platoService.guardar(plato);
        return platosTab(model, u, hxRequest);
    }

    @DeleteMapping("/platos/{id}")
    public String eliminarPlato(@PathVariable Integer id, Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        platoService.eliminar(id);
        return platosTab(model, u, hxRequest);
    }

    // --- PERSONAL TAB ---

    @GetMapping("/personal")
    public String personalTab(Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        model.addAttribute("usuario", u);
        model.addAttribute("activeTab", "personal");
        model.addAttribute("contentTemplate", "personal/list");
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return hxRequest ? "admin/personal/list :: content" : "admin/layout";
    }

    @GetMapping("/personal/nuevo")
    public String nuevoUsuarioForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "admin/personal/form_modal :: form";
    }

    @GetMapping("/personal/editar/{id}")
    public String editarUsuarioForm(@PathVariable Integer id, Model model) {
        model.addAttribute("usuario", usuarioService.buscarPorId(id));
        return "admin/personal/form_modal :: form";
    }

    @PostMapping("/personal/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario, Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        Usuario existente = usuario.getId() != null ? usuarioService.buscarPorId(usuario.getId()) : null;
        if (existente != null) {
            existente.setNombre(usuario.getNombre());
            existente.setRol(usuario.getRol());
            usuarioService.guardar(existente);
        } else {
            usuario.setActivo(true);
            usuarioService.guardar(usuario);
        }
        return personalTab(model, u, hxRequest);
    }

    @DeleteMapping("/personal/{id}")
    public String eliminarUsuario(@PathVariable Integer id, Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        usuarioService.eliminar(id);
        return personalTab(model, u, hxRequest);
    }

    // --- REPORTE PDF DOWNLOADS ---

    @GetMapping("/reporte/ventas")
    public ResponseEntity<?> descargarReporteDia() {
        try {
            byte[] pdf = reporteService.generarReporteVentas();
            return responderPdf(pdf, "reporte_diario_" + LocalDate.now() + ".pdf");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/reporte/historico")
    public ResponseEntity<?> descargarReporteHistorico() {
        try {
            byte[] pdf = reporteService.generarReporteVentasCompleto();
            return responderPdf(pdf, "reporte_historico_" + LocalDate.now() + ".pdf");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
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
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private ResponseEntity<byte[]> responderPdf(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // Mantener APIs antiguas por si acaso para compatibilidad parcial (opcional)
    @GetMapping("/api/kpis") @ResponseBody public Map<String, Object> getKpis() { return reporteService.obtenerKpisDelDia(); }
    @GetMapping("/api/ventas-categoria") @ResponseBody public Map<String, Object> getVentasCategoria() { return reporteService.obtenerVentasPorCategoria(); }
    @GetMapping("/api/top-platos") @ResponseBody public Map<String, Object> getTopPlatos() { return reporteService.obtenerTopPlatos(); }
}
