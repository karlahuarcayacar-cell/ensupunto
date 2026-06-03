package com.ensupunto.controller;

import com.ensupunto.entity.Pedido;
import com.ensupunto.entity.Usuario;
import com.ensupunto.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chef")
@RequiredArgsConstructor
public class ChefController {

    private final PedidoService pedidoService;

    @GetMapping
    public String chefPage(Model model, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        if (u == null || !u.getRol().equals("chef")) return "redirect:/login";
        
        model.addAttribute("usuario", u);
        return "chef";
    }

    @GetMapping("/api/pedidos")
    @ResponseBody
    public List<Pedido> getPedidosCocina() {
        return pedidoService.listarPedidosParaCocina();
    }

    @PutMapping("/api/pedidos/{id}/preparar")
    @ResponseBody
    public ResponseEntity<?> preparar(@PathVariable Integer id) {
        Pedido p = pedidoService.prepararPedido(id);
        return ResponseEntity.ok(p);
    }

    @PutMapping("/api/pedidos/{id}/terminar")
    @ResponseBody
    public ResponseEntity<?> terminar(@PathVariable Integer id) {
        Pedido p = pedidoService.terminarPedido(id);
        return ResponseEntity.ok(p);
    }
}
