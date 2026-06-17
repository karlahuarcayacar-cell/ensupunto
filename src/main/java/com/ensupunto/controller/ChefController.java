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

/**
 * CONTROLADOR WEB (SPRING MVC + HTMX): MÓDULO DE COCINA (CHEF MONITOR)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Monitor de Cola de Trabajo (FIFO):
 *    Este controlador maneja las interacciones del Chef para visualizar los pedidos pendientes
 *    y cambiar su estado en tiempo real.
 * 
 * 2. Anotaciones REST en Controlador MVC:
 *    - @PutMapping: Usamos PUT para denotar actualizaciones de recursos existentes (cambio de estado de cocina).
 *    - @ResponseBody: Indica que el retorno del método debe ser serializado directamente como el cuerpo de la
 *      respuesta HTTP (por ejemplo, en formato JSON), en lugar de buscar una vista de Thymeleaf.
 *      Es ideal para exponer APIs internas ligeras.
 */
@Controller
@RequestMapping("/chef")
@RequiredArgsConstructor
public class ChefController {

    private final PedidoService pedidoService;

    /**
     * Renderiza la página contenedora del Monitor de Cocina (monitor.html).
     */
    @GetMapping
    public String chefPage(Model model, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        if (u == null || !u.getRol().equals("chef")) return "redirect:/login";
        
        model.addAttribute("usuario", u);
        return "chef/monitor"; // Vista principal
    }

    /**
     * Retorna únicamente la lista fragmentada de pedidos activos en cocina (HTMX).
     */
    @GetMapping("/fragment/pedidos")
    public String getPedidosFragment(Model model) {
        model.addAttribute("pedidos", pedidoService.listarPedidosParaCocina());
        return "chef/monitor :: lista-pedidos"; // Renderiza el fragmento de la cola
    }

    /**
     * Pone el pedido en estado 'cocina_preparacion' (HTMX PUT).
     */
    @PutMapping("/api/pedidos/{id}/preparar")
    public String preparar(@PathVariable Integer id, Model model) {
        pedidoService.prepararPedido(id);
        return getPedidosFragment(model); // Refresca y devuelve el fragmento HTML actualizado
    }

    /**
     * Finaliza la preparación del pedido ('cocina_listo') para ser servido por el mesero (HTMX PUT).
     */
    @PutMapping("/api/pedidos/{id}/terminar")
    public String terminar(@PathVariable Integer id, Model model) {
        pedidoService.terminarPedido(id);
        return getPedidosFragment(model); // Refresca y devuelve el fragmento HTML actualizado
    }

    /**
     * API REST interna que expone los pedidos en formato JSON.
     * Útil si quisiéramos conectar un panel externo en React/Vue o hacer sondeo (polling) mediante JSON.
     */
    @GetMapping("/api/pedidos")
    @ResponseBody
    public List<Pedido> getPedidosCocina() {
        return pedidoService.listarPedidosParaCocina();
    }
}
