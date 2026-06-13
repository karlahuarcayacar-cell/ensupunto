package com.ensupunto.controller;

import com.ensupunto.entity.PagoFraccionado;
import com.ensupunto.entity.Pedido;
import com.ensupunto.entity.Usuario;
import com.ensupunto.service.MesaService;
import com.ensupunto.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.BigDecimal;

@Controller
@RequestMapping("/cajero")
@RequiredArgsConstructor
public class CajeroController {

    private final MesaService mesaService;
    private final PedidoService pedidoService;

    @GetMapping
    public String cajeroPage(Model model, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        if (u == null || !u.getRol().equals("cajero")) return "redirect:/login";
        
        model.addAttribute("usuario", u);
        model.addAttribute("mesas", mesaService.listarTodas());
        return "cajero";
    }

    @GetMapping("/mesa/{id}/panel")
    public String getMesaPanel(@PathVariable Integer id, Model model) {
        Pedido p = pedidoService.buscarPedidoActivoPorMesa(id);
        if (p == null) {
            return "cajero :: #cashier-right-panel"; // Retornar vacío o aviso
        }
        
        if ("dividido".equals(p.getEstado())) {
            List<PagoFraccionado> fracciones = p.getPagosFraccionados();
            BigDecimal saldo = fracciones.stream()
                    .filter(f -> !f.getPagado())
                    .map(PagoFraccionado::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            model.addAttribute("fracciones", fracciones);
            model.addAttribute("saldoPendiente", saldo);
            return "cajero/fragmentos/dividida :: detalle";
        }
        
        model.addAttribute("pedido", p);
        return "cajero/fragmentos/cuenta :: detalle";
    }

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
        model.addAttribute("metodoPago", metodoPago);
        
        model.addAttribute("dniCliente", dniCliente);
        model.addAttribute("nombreCliente", nombreClienteBoleta);
        model.addAttribute("rucCliente", rucCliente);
        model.addAttribute("razonSocial", razonSocial);
        model.addAttribute("direccionFactura", direccionFactura);
        model.addAttribute("efectivoRecibido", efectivoRecibido != null ? efectivoRecibido : BigDecimal.ZERO);
        model.addAttribute("vuelto", vuelto != null ? vuelto : BigDecimal.ZERO);
        
        return "cajero/fragmentos/recibo :: comprobante";
    }

    @GetMapping("/pedidos/{id}/dividir/prompt")
    public String promptDividir(@PathVariable Integer id, Model model) {
        model.addAttribute("pedidoId", id);
        return "cajero/fragmentos/dividir_modal :: modal";
    }

    @PostMapping("/pedidos/{id}/dividir")
    public String dividirCuenta(@PathVariable Integer id, @RequestParam int nPartes, Model model) {
        List<PagoFraccionado> fracciones = pedidoService.dividirCuenta(id, nPartes);
        BigDecimal saldo = fracciones.stream()
                .filter(f -> !f.getPagado())
                .map(PagoFraccionado::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("fracciones", fracciones);
        model.addAttribute("saldoPendiente", saldo);
        return "cajero/fragmentos/dividida :: detalle";
    }

    @PostMapping("/fracciones/{id}/pagar")
    public String pagarFraccion(@PathVariable Integer id, @RequestParam(defaultValue = "efectivo") String metodoPago, HttpSession session, Model model) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        PagoFraccionado pf = pedidoService.registrarPagoFraccion(id, metodoPago, u.getId());
        Pedido p = pf.getPedido();
        
        List<PagoFraccionado> fracciones = p.getPagosFraccionados();
        BigDecimal saldo = fracciones.stream()
                .filter(f -> !f.getPagado())
                .map(PagoFraccionado::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("fracciones", fracciones);
        model.addAttribute("saldoPendiente", saldo);
        return "cajero/fragmentos/dividida :: detalle";
    }

    @GetMapping("/notificar/mesa-liberada")
    public String notificarMesaLiberada() {
        return "cajero/fragmentos/notificacion_liberada :: modal";
    }
}
