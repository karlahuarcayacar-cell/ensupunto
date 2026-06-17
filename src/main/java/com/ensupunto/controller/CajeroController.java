package com.ensupunto.controller;

import com.ensupunto.entity.PagoFraccionado;
import com.ensupunto.entity.Pedido;
import com.ensupunto.entity.Usuario;
import com.ensupunto.service.MesaService;
import com.ensupunto.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.BigDecimal;

/**
 * CONTROLADOR WEB (SPRING MVC + HTMX): MÓDULO DE CAJA Y FACTURACIÓN
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Mapeo de Cobros y Comprobantes de Pago:
 *    Este controlador emite representaciones visuales de boletas o facturas tras confirmar la transacción
 *    con el servicio, pasando la información correspondiente al fragmento de Thymeleaf.
 * 
 * 2. Comunicación Asíncrona vía HTMX Triggers (HX-Trigger):
 *    Cuando un cliente paga su parte en una cuenta dividida, queremos actualizar el listado de fracciones
 *    restantes en la pantalla de caja sin forzar al cajero a recargar la página.
 *    - response.setHeader("HX-Trigger", "refresh-split-list"): Envía una cabecera de evento HTTP personalizada.
 *    HTMX en el navegador escucha este evento y dispara automáticamente una petición AJAX secundaria
 *    para redibujar el panel de saldos pendientes de forma reactiva.
 */
@Controller
@RequestMapping("/cajero")
@RequiredArgsConstructor
public class CajeroController {

    private final MesaService mesaService;
    private final PedidoService pedidoService;

    /**
     * Carga la página principal del Módulo de Caja (cajero.html) con el mapeo físico de mesas del salón.
     */
    @GetMapping
    public String cajeroPage(Model model, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        if (u == null || !u.getRol().equals("cajero")) return "redirect:/login";
        
        model.addAttribute("usuario", u);
        model.addAttribute("mesas", mesaService.listarTodas());
        return "cajero";
    }

    /**
     * Carga de forma asíncrona el panel de detalle de cuenta en el lado derecho de la pantalla (HTMX).
     * Distingue si la mesa tiene una cuenta regular o una dividida en fracciones.
     * 
     * @param id ID de la mesa seleccionada.
     */
    @GetMapping("/mesa/{id}/panel")
    public String getMesaPanel(@PathVariable Integer id, Model model) {
        Pedido p = pedidoService.buscarPedidoActivoPorMesa(id);
        if (p == null) {
            return "cajero :: #cashier-right-panel"; // Retornar vacío o aviso
        }
        
        if ("dividido".equals(p.getEstado())) {
            List<PagoFraccionado> fracciones = pedidoService.obtenerFraccionesPorPedido(p.getId());
            BigDecimal saldo = fracciones.stream()
                    .filter(f -> !f.getPagado())
                    .map(PagoFraccionado::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            model.addAttribute("fracciones", fracciones);
            model.addAttribute("saldoPendiente", saldo);
            model.addAttribute("mesaId", id);
            return "cajero/fragmentos/dividida :: detalle";
        }
        
        model.addAttribute("pedido", p);
        return "cajero/fragmentos/cuenta :: detalle";
    }

    /**
     * Procesa la facturación regular de la mesa física.
     * Mapea los datos del cliente, método de pago, calcula vueltos y retorna el comprobante impreso (HTMX).
     */
    @PostMapping("/pedidos/cobrar")
    public String cobrarPedido(
            @RequestParam Integer pedidoId,
            @RequestParam String metodoPago,
            @RequestParam String tipoComprobante,
            @RequestParam(required = false) String dniCliente,
            @RequestParam(required = false) String nombreClienteBoleta,
            @RequestParam(required = false) String rucCliente,
            @RequestParam(required = false) String razonSocial,
            @RequestParam(required = false) String direccionFactura,
            @RequestParam(required = false) BigDecimal efectivoRecibido,
            @RequestParam(required = false) BigDecimal vuelto,
            HttpSession session,
            Model model) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        Pedido p = pedidoService.cobrarPedidoRegular(pedidoId, u.getId(), metodoPago);
        
        String prefix = "boleta".equals(tipoComprobante) ? "B001-" : "F001-";
        String docNum = prefix + String.format("%06d", p.getId());
        
        model.addAttribute("pedido", p);
        model.addAttribute("tipoComprobante", tipoComprobante);
        model.addAttribute("docNum", docNum);
        model.addAttribute("fechaDocumento", p.getFechaPago());
        model.addAttribute("nombreCajero", u.getNombre());
        
        model.addAttribute("dniCliente", dniCliente);
        model.addAttribute("nombreCliente", nombreClienteBoleta);
        model.addAttribute("rucCliente", rucCliente);
        model.addAttribute("razonSocial", razonSocial);
        model.addAttribute("direccionFactura", direccionFactura);
        model.addAttribute("efectivoRecibido", efectivoRecibido != null ? efectivoRecibido : BigDecimal.ZERO);
        model.addAttribute("vuelto", vuelto != null ? vuelto : BigDecimal.ZERO);
        
        return "cajero/fragmentos/recibo :: comprobante";
    }

    /**
     * Abre el modal para configurar el número de comensales entre los que se dividirá la cuenta.
     */
    @GetMapping("/pedidos/{id}/dividir/prompt")
    public String promptDividir(@PathVariable Integer id, Model model) {
        model.addAttribute("pedidoId", id);
        return "cajero/fragmentos/dividir_modal :: modal";
    }

    /**
     * Confirma la división del total de la orden en N fracciones (HTMX POST).
     */
    @PostMapping("/pedidos/{id}/dividir")
    public String dividirCuenta(@PathVariable Integer id, @RequestParam int nPartes, Model model) {
        List<PagoFraccionado> fracciones = pedidoService.dividirCuenta(id, nPartes);
        BigDecimal saldo = fracciones.stream()
                .filter(f -> !f.getPagado())
                .map(PagoFraccionado::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("fracciones", fracciones);
        model.addAttribute("saldoPendiente", saldo);
        if (!fracciones.isEmpty()) {
            model.addAttribute("mesaId", fracciones.get(0).getPedido().getMesa().getId());
        }
        return "cajero/fragmentos/dividida :: detalle";
    }

    /**
     * Abre el modal para cobrar una fracción de cuenta particular (Cliente X).
     */
    @GetMapping("/fracciones/{id}/pagar/prompt")
    public String promptPagarFraccion(@PathVariable Integer id, Model model) {
        PagoFraccionado pf = pedidoService.buscarFraccionPorId(id);
        model.addAttribute("fraccion", pf);
        return "cajero/fragmentos/cobrar_fraccion_modal :: modal";
    }

    /**
     * Registra el pago asíncrono de una fracción de cuenta (HTMX POST).
     * Si no es la última fracción, añade la cabecera 'HX-Trigger' para refrescar los saldos en pantalla.
     * Si es la última fracción, el servicio se encarga de liberar la mesa de forma automática.
     */
    @PostMapping("/fracciones/{id}/pagar")
    public String pagarFraccion(
            @PathVariable Integer id,
            @RequestParam String metodoPago,
            @RequestParam String tipoComprobante,
            @RequestParam(required = false) String dniCliente,
            @RequestParam(required = false) String nombreClienteBoleta,
            @RequestParam(required = false) String rucCliente,
            @RequestParam(required = false) String razonSocial,
            @RequestParam(required = false) String direccionFactura,
            @RequestParam(required = false) BigDecimal efectivoRecibido,
            @RequestParam(required = false) BigDecimal vuelto,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        PagoFraccionado pf = pedidoService.registrarPagoFraccion(id, metodoPago, u.getId());
        Pedido p = pf.getPedido();
        
        String prefix = "boleta".equals(tipoComprobante) ? "B001-" : "F001-";
        String docNum = prefix + String.format("F%05d", pf.getId());
        
        List<PagoFraccionado> todas = pedidoService.obtenerFraccionesPorPedido(p.getId());
        boolean todosPagados = todas.stream().allMatch(PagoFraccionado::getPagado);
        
        model.addAttribute("pedido", p);
        model.addAttribute("tipoComprobante", tipoComprobante);
        model.addAttribute("docNum", docNum);
        model.addAttribute("metodoPago", metodoPago);
        model.addAttribute("fechaDocumento", pf.getFechaPago());
        model.addAttribute("nombreCajero", u.getNombre());
        
        model.addAttribute("dniCliente", dniCliente);
        model.addAttribute("nombreCliente", nombreClienteBoleta);
        model.addAttribute("rucCliente", rucCliente);
        model.addAttribute("razonSocial", razonSocial);
        model.addAttribute("direccionFactura", direccionFactura);
        model.addAttribute("efectivoRecibido", efectivoRecibido != null ? efectivoRecibido : BigDecimal.ZERO);
        model.addAttribute("vuelto", vuelto != null ? vuelto : BigDecimal.ZERO);
        
        model.addAttribute("esFraccion", true);
        model.addAttribute("esUltimaFraccion", todosPagados);
        model.addAttribute("fraccionMonto", pf.getMonto());
        model.addAttribute("numeroCliente", pf.getNumeroCliente());
        
        if (!todosPagados) {
            response.setHeader("HX-Trigger", "refresh-split-list");
        }
        
        return "cajero/fragmentos/recibo :: comprobante";
    }

    /**
     * Carga el aviso de mesa liberada con éxito al saldar el 100% de la cuenta.
     */
    @GetMapping("/notificar/mesa-liberada")
    public String notificarMesaLiberada() {
        return "cajero/fragmentos/notificacion_liberada :: modal";
    }
}
