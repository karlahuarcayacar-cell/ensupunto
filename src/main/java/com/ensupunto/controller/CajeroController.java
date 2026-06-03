package com.ensupunto.controller;

import com.ensupunto.entity.Mesa;
import com.ensupunto.entity.PagoFraccionado;
import com.ensupunto.entity.Pedido;
import com.ensupunto.entity.Usuario;
import com.ensupunto.service.MesaService;
import com.ensupunto.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/api/mesas/{id}/pedido")
    @ResponseBody
    public ResponseEntity<?> getPedidoMesa(@PathVariable Integer id) {
        Pedido p = pedidoService.buscarPedidoActivoPorMesa(id);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @PostMapping("/api/pedidos/{id}/cobrar")
    @ResponseBody
    public ResponseEntity<?> cobrarPedido(@PathVariable Integer id, @RequestParam String metodoPago, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        Pedido p = pedidoService.cobrarPedidoRegular(id, u.getId(), metodoPago);
        return ResponseEntity.ok(p);
    }

    @PostMapping("/api/pedidos/{id}/dividir")
    @ResponseBody
    public List<PagoFraccionado> dividirCuenta(@PathVariable Integer id, @RequestParam int nPartes) {
        return pedidoService.dividirCuenta(id, nPartes);
    }

    @GetMapping("/api/pedidos/{id}/fracciones")
    @ResponseBody
    public List<PagoFraccionado> getFracciones(@PathVariable Integer id) {
        return pedidoService.buscarPorId(id).getPagosFraccionados();
    }

    @PostMapping("/api/fracciones/{id}/pagar")
    @ResponseBody
    public PagoFraccionado pagarFraccion(@PathVariable Integer id, @RequestParam String metodoPago, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        return pedidoService.registrarPagoFraccion(id, metodoPago, u.getId());
    }
}
