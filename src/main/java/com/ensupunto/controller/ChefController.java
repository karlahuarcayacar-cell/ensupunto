package com.ensupunto.controller;

import com.ensupunto.entity.Pedido;
import com.ensupunto.entity.Usuario;
import com.ensupunto.service.PedidoService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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
        return "chef/monitor";
    }

    @GetMapping("/fragment/pedidos")
    public String getPedidosFragment(Model model) {
        model.addAttribute("pedidos", pedidoService.listarPedidosParaCocina());
        return "chef/monitor :: lista-pedidos";
    }

    @PutMapping("/api/pedidos/{id}/preparar")
    public String preparar(@PathVariable Integer id, Model model) {
        pedidoService.prepararPedido(id);
        return getPedidosFragment(model);
    }

    @PutMapping("/api/pedidos/{id}/terminar")
    public String terminar(@PathVariable Integer id, Model model) {
        pedidoService.terminarPedido(id);
        return getPedidosFragment(model);
    }

    @GetMapping("/api/pedidos")
    @ResponseBody
    public List<Pedido> getPedidosCocina() {
        return pedidoService.listarPedidosParaCocina();
    }
}
