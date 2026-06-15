package com.ensupunto.controller;

import com.ensupunto.entity.Mesa;
import com.ensupunto.entity.Pedido;
import com.ensupunto.entity.Plato;
import com.ensupunto.entity.Usuario;
import com.ensupunto.service.MesaService;
import com.ensupunto.service.PedidoService;
import com.ensupunto.service.PlatoService;
import com.ensupunto.service.ReporteService;
import com.ensupunto.service.UsuarioService;

import com.ensupunto.repository.ModificacionPedidoRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ReporteService reporteService;
    private final PedidoService pedidoService;
    private final PlatoService platoService;
    private final UsuarioService usuarioService;
    private final MesaService mesaService;
    private final ModificacionPedidoRepository modificacionRepository;

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
        
        // KPIs (Hoy)
        model.addAttribute("kpis", reporteService.obtenerKpisDelDia());
        
        // Ventas por defecto (Hoy)
        List<Pedido> hoy = reporteService.obtenerHistorialHoy();
        BigDecimal totalVendido = hoy.stream()
                .map(Pedido::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int page = 0;
        int size = 10;
        int totalPedidos = hoy.size();
        int totalPages = (int) Math.ceil((double) totalPedidos / size);
        if (totalPages == 0) totalPages = 1;
        
        List<Pedido> paginated = hoy.subList(0, Math.min(size, totalPedidos));
        
        model.addAttribute("pedidos", pedidoService.mapearPedidos(paginated));
        model.addAttribute("titulo", "Reporte del Día");
        
        String fechaHoy = LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new java.util.Locale("es", "PE"))
        );
        model.addAttribute("subtitulo", fechaHoy);
        model.addAttribute("downloadUrl", "/admin/reporte/ventas");
        model.addAttribute("previewUrl", "/admin/reporte/preview/dia");
        
        // Paginacion & Resumen
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalPedidos);
        model.addAttribute("totalVendido", totalVendido);
        model.addAttribute("pageSize", size);
        model.addAttribute("tipo", "hoy");
        
        return hxRequest ? "admin/reportes :: content" : "admin/layout";
    }

    @GetMapping("/reportes/ventas/filtrar")
    public String filtrarVentas(
            @RequestParam String tipo,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        List<Pedido> pedidos;
        String titulo;
        String subtitulo;
        String downloadUrl;
        String previewUrl;
        
        if ("historico".equals(tipo)) {
            pedidos = reporteService.obtenerHistorialCompleto();
            titulo = "Todo lo Vendido (Histórico)";
            subtitulo = "Historial completo de ventas registradas";
            downloadUrl = "/admin/reporte/historico";
            previewUrl = "/admin/reporte/preview/historico";
        } else if ("personalizado".equals(tipo)) {
            LocalDate fechaDesde = LocalDate.parse(desde);
            LocalDate fechaHasta = LocalDate.parse(hasta);
            pedidos = reporteService.obtenerHistorialPeriodo(fechaDesde, fechaHasta);
            titulo = "Reporte por Período";
            subtitulo = "Desde " + desde + " hasta " + hasta;
            downloadUrl = "/admin/reporte/periodo?desde=" + desde + "&hasta=" + hasta;
            previewUrl = "/admin/reporte/preview/personalizado?desde=" + desde + "&hasta=" + hasta;
        } else { // hoy
            pedidos = reporteService.obtenerHistorialHoy();
            titulo = "Reporte del Día";
            String fechaHoy = LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new java.util.Locale("es", "PE"))
            );
            subtitulo = fechaHoy;
            downloadUrl = "/admin/reporte/ventas";
            previewUrl = "/admin/reporte/preview/dia";
        }
        
        BigDecimal totalVendido = pedidos.stream()
                .map(Pedido::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalPedidos = pedidos.size();
        int totalPages = (int) Math.ceil((double) totalPedidos / size);
        if (totalPages == 0) totalPages = 1;
        
        int fromIndex = page * size;
        if (fromIndex > totalPedidos) {
            fromIndex = 0;
            page = 0;
        }
        int toIndex = Math.min(fromIndex + size, totalPedidos);
        List<Pedido> paginated = (fromIndex < totalPedidos) ? pedidos.subList(fromIndex, toIndex) : new java.util.ArrayList<>();
        
        model.addAttribute("pedidos", pedidoService.mapearPedidos(paginated));
        model.addAttribute("titulo", titulo);
        model.addAttribute("subtitulo", subtitulo);
        model.addAttribute("downloadUrl", downloadUrl);
        model.addAttribute("previewUrl", previewUrl);
        
        // Paginacion & Resumen
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalPedidos);
        model.addAttribute("totalVendido", totalVendido);
        model.addAttribute("pageSize", size);
        model.addAttribute("tipo", tipo);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        
        return "admin/reportes/tabla_pedidos :: render(pedidos=${pedidos}, titulo=${titulo}, subtitulo=${subtitulo}, downloadUrl=${downloadUrl}, previewUrl=${previewUrl})";
    }

    @GetMapping("/reportes/auditoria")
    public String reporteAuditoriaFragment(Model model) {
        model.addAttribute("modificaciones", modificacionRepository.findAllWithPedidoAndAdmin());
        return "admin/reportes/auditoria";
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
            existente.setNombreUsuario(usuario.getNombreUsuario());
            if (usuario.getContrasena() != null && !usuario.getContrasena().trim().isEmpty()) {
                existente.setContrasena(usuario.getContrasena());
            }
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

    @PutMapping("/personal/{id}/reactivar")
    public String reactivarUsuario(@PathVariable Integer id, Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        usuarioService.reactivar(id);
        return personalTab(model, u, hxRequest);
    }

    // --- MESAS TAB ---

    @GetMapping("/mesas")
    public String mesasTab(Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        model.addAttribute("usuario", u);
        model.addAttribute("activeTab", "mesas");
        model.addAttribute("contentTemplate", "mesas/list");
        model.addAttribute("mesas", mesaService.listarTodas());
        return hxRequest ? "admin/mesas/list :: content" : "admin/layout";
    }

    @GetMapping("/mesas/nuevo")
    public String nuevaMesaForm(Model model) {
        model.addAttribute("mesa", new Mesa());
        return "admin/mesas/form_modal :: form";
    }

    @GetMapping("/mesas/editar/{id}")
    public String editarMesaForm(@PathVariable Integer id, Model model) {
        model.addAttribute("mesa", mesaService.buscarPorId(id));
        return "admin/mesas/form_modal :: form";
    }

    @PostMapping("/mesas/guardar")
    public String guardarMesa(@ModelAttribute Mesa mesa, Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        try {
            mesaService.guardar(mesa);
            model.addAttribute("successMsg", "La mesa se guardó exitosamente.");
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Ocurrió un error al guardar la mesa: " + e.getMessage());
        }
        return mesasTab(model, u, hxRequest);
    }

    @DeleteMapping("/mesas/{id}")
    public String eliminarMesa(@PathVariable Integer id, Model model, @SessionAttribute("usuarioLogueado") Usuario u, @RequestHeader(value = "HX-Request", required = false) boolean hxRequest) {
        try {
            mesaService.eliminar(id);
            model.addAttribute("successMsg", "La mesa ha sido eliminada exitosamente.");
        } catch (Exception e) {
            model.addAttribute("errorMsg", "No se puede eliminar la mesa porque tiene pedidos (activos o históricos) asociados en el salón.");
        }
        return mesasTab(model, u, hxRequest);
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

    @GetMapping("/reporte/preview/{tipo}")
    public ResponseEntity<byte[]> previsualizarReporte(
            @PathVariable String tipo,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        try {
            byte[] pdf;
            switch (tipo) {
                case "dia" -> pdf = reporteService.generarReporteVentas();
                case "historico" -> pdf = reporteService.generarReporteVentasCompleto();
                case "personalizado" -> {
                    if (desde == null || hasta == null) {
                        return ResponseEntity.badRequest().build();
                    }
                    pdf = reporteService.generarReporteVentasPeriodo(
                            LocalDate.parse(desde), LocalDate.parse(hasta));
                }
                default -> {
                    return ResponseEntity.badRequest().build();
                }
            }
 
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=preview.pdf");
            return ResponseEntity.ok().headers(headers).body(pdf);
 
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
}
