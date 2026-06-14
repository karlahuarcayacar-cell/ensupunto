package com.ensupunto.controller;

import com.ensupunto.dto.CarritoDTO;
import com.ensupunto.dto.ItemCarritoDTO;
import com.ensupunto.entity.DetallePedido;
import com.ensupunto.entity.Mesa;
import com.ensupunto.entity.Pedido;
import com.ensupunto.entity.Plato;
import com.ensupunto.entity.Usuario;
import com.ensupunto.service.MesaService;
import com.ensupunto.service.PedidoService;
import com.ensupunto.service.PlatoService;
import com.ensupunto.service.UsuarioService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/mesero")
@RequiredArgsConstructor
@SessionAttributes("carrito")
public class MeseroController {

    private final MesaService mesaService;
    private final PlatoService platoService;
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    @ModelAttribute("carrito")
    public CarritoDTO carrito() {
        return new CarritoDTO();
    }

    @GetMapping
    public String meseroPage(Model model, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        if (u == null || !u.getRol().equals("mesero")) return "redirect:/login";
        
        model.addAttribute("usuario", u);
        return "mesero/salon";
    }

    @GetMapping("/salon/fragment")
    public String getSalonFragment(Model model) {
        getMesasFragment(model);
        return "mesero/salon :: salon-view";
    }

    @GetMapping("/fragment/mesas")
    public String getMesasFragment(Model model) {
        List<Mesa> mesas = mesaService.listarTodas();
        List<Map<String, Object>> mesasStatus = new ArrayList<>();
        // ... rest of the method ...

        for (Mesa m : mesas) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("nombre", m.getNombre());
            map.put("estado", m.getEstado());
            
            String suffix = "free";
            String label = "Libre";
            switch (m.getEstado()) {
                case "esperando_comida": suffix = "waiting"; label = "En Cola"; break;
                case "cocina_preparacion": suffix = "cooking"; label = "Preparando"; break;
                case "comiendo": suffix = "eating"; label = "Comiendo"; break;
                case "cuenta_pedida": suffix = "ready"; label = "Cuenta Pedida"; break;
            }
            map.put("estadoSuffix", suffix);
            map.put("estadoLabel", label);

            Pedido p = pedidoService.buscarPedidoActivoPorMesa(m.getId());
            if (p != null) {
                map.put("pedidoId", p.getId());
                map.put("total", p.getTotal());
                map.put("cantidadPlatos", p.getDetalles().stream().mapToInt(DetallePedido::getCantidad).sum());
                map.put("meseroNombre", p.getMesero().getNombre());
            }
            mesasStatus.add(map);
        }
        model.addAttribute("mesasStatus", mesasStatus);
        return "mesero/salon :: lista-mesas";
    }

    @GetMapping("/pedido/detalle/{id}")
    public String getPedidoDetalle(@PathVariable Integer id, Model model) {
        model.addAttribute("pedido", pedidoService.buscarPorId(id));
        return "mesero/pedido/detalle_modal :: modal";
    }

    @GetMapping("/pedido/nuevo/{mesaId}")
    public String nuevoPedido(@PathVariable Integer mesaId, @ModelAttribute("carrito") CarritoDTO carrito, Model model) {
        Pedido p = pedidoService.buscarPedidoActivoPorMesa(mesaId);
        if (p != null && ("dividido".equals(p.getEstado()) || "cuenta_pedida".equals(p.getEstado()))) {
            getSalonFragment(model);
            model.addAttribute("errorModal", "No se pueden agregar platos a una mesa con cuenta dividida o en proceso de cobro.");
            return "mesero/salon :: salon-view";
        }

        Mesa mesa = mesaService.listarTodas().stream().filter(m -> m.getId().equals(mesaId)).findFirst().orElse(null);
        carrito.setMesaId(mesaId);
        carrito.setNombreMesa(mesa != null ? mesa.getNombre() : "Mesa " + mesaId);
        carrito.setPedidoId(null);
        carrito.setModifying(false);
        carrito.getItems().clear();
        
        return editorView(model, "entradas");
    }

    @GetMapping("/pedido/editar/{pedidoId}")
    public String editarPedido(@PathVariable Integer pedidoId,
                                @ModelAttribute("carrito") CarritoDTO carrito,
                                Model model,
                                jakarta.servlet.http.HttpServletResponse response) {
     
        Pedido p = pedidoService.buscarPorId(pedidoId);
     
        if (p != null && ("dividido".equals(p.getEstado()) || "cuenta_pedida".equals(p.getEstado()))) {
            getSalonFragment(model);
            model.addAttribute("errorModal", "No se puede modificar un pedido con cuenta dividida o en proceso de cobro.");
            response.setHeader("HX-Retarget", "#mesero-app");
            response.setHeader("HX-Reswap", "innerHTML");
            return "mesero/salon :: salon-view";
        }
     
        // Caso: pedido en preparación -> requiere autorización admin
        if (p != null && "cocina_preparacion".equals(p.getEstado())) {
            model.addAttribute("pedidoId", pedidoId);
            response.setHeader("HX-Retarget", "body");
            response.setHeader("HX-Reswap", "beforeend");
            return "mesero/pedido/autorizacion_modal :: modal";
        }
     
        // Caso normal: modificar libremente
        carrito.setMesaId(p.getMesa().getId());
        carrito.setNombreMesa(p.getMesa().getNombre());
        carrito.setPedidoId(pedidoId);
        carrito.setModifying(true);
        carrito.getItems().clear();
     
        for (DetallePedido dp : p.getDetalles()) {
            carrito.getItems().add(new ItemCarritoDTO(dp.getPlato().getId(), dp.getPlato().getNombre(), dp.getPrecioUnitario(), dp.getCantidad(), dp.getNota()));
        }
     
        response.setHeader("HX-Retarget", "#mesero-app");
        response.setHeader("HX-Reswap", "innerHTML");
        return editorView(model, "entradas");
    }

    @PostMapping("/pedido/editar/{pedidoId}/autorizar")
    public String autorizarYEditar(@PathVariable Integer pedidoId,
                                    @RequestParam String adminUsername,
                                    @RequestParam String adminPassword,
                                    @ModelAttribute("carrito") CarritoDTO carrito,
                                    Model model,
                                    jakarta.servlet.http.HttpServletResponse response) {
     
        if (!pedidoService.validarAdmin(adminUsername, adminPassword)) {
            model.addAttribute("pedidoId", pedidoId);
            model.addAttribute("error", "Usuario o contraseña de administrador incorrectos.");
            // Falla: el modal de error se reemplaza a sí mismo (overlay), no toca #mesero-app
            response.setHeader("HX-Retarget", "#autorizacion-modal");
            response.setHeader("HX-Reswap", "outerHTML");
            return "mesero/pedido/autorizacion_modal :: modal";
        }
     
        Pedido p = pedidoService.buscarPorId(pedidoId);
        if (p != null && ("dividido".equals(p.getEstado()) || "cuenta_pedida".equals(p.getEstado()))) {
            getSalonFragment(model);
            model.addAttribute("errorModal", "No se puede modificar un pedido con cuenta dividida o en proceso de cobro.");
            response.setHeader("HX-Retarget", "#mesero-app");
            response.setHeader("HX-Reswap", "innerHTML");
            return "mesero/salon :: salon-view";
        }
     
        carrito.setMesaId(p.getMesa().getId());
        carrito.setNombreMesa(p.getMesa().getNombre());
        carrito.setPedidoId(pedidoId);
        carrito.setModifying(true);
        carrito.getItems().clear();
     
        for (DetallePedido dp : p.getDetalles()) {
            carrito.getItems().add(new ItemCarritoDTO(dp.getPlato().getId(), dp.getPlato().getNombre(), dp.getPrecioUnitario(), dp.getCantidad(), dp.getNota()));
        }
     
        // Éxito: va a #mesero-app y el form's hx-on cierra el modal
        response.setHeader("HX-Retarget", "#mesero-app");
        response.setHeader("HX-Reswap", "innerHTML");
        return editorView(model, "entradas");
    }
    
    @GetMapping("/pedido/catalogo/{categoria}")
    public String getCatalogo(@PathVariable String categoria, Model model) {
        model.addAttribute("platos", platoService.listarActivos().stream()
                .filter(p -> p.getCategoria().equals(categoria)).collect(Collectors.toList()));
        return "mesero/pedido/catalogo :: lista";
    }

    @PostMapping("/pedido/carrito/agregar/{platoId}")
    public String agregarAlCarrito(@PathVariable Integer platoId, @ModelAttribute("carrito") CarritoDTO carrito, Model model) {
        ItemCarritoDTO item = carrito.getItems().stream().filter(i -> i.getPlatoId().equals(platoId)).findFirst().orElse(null);
        if (item != null) {
            item.setCantidad(item.getCantidad() + 1);
        } else {
            Plato p = platoService.buscarPorId(platoId);
            carrito.getItems().add(new ItemCarritoDTO(platoId, p.getNombre(), p.getPrecio(), 1, ""));
        }
        model.addAttribute("carrito", carrito);
        return "mesero/pedido/carrito :: detalle";
    }

    @PostMapping("/pedido/carrito/quitar/{platoId}")
    public String quitarDelCarrito(@PathVariable Integer platoId, @ModelAttribute("carrito") CarritoDTO carrito, Model model) {
        ItemCarritoDTO item = carrito.getItems().stream().filter(i -> i.getPlatoId().equals(platoId)).findFirst().orElse(null);
        if (item != null) {
            item.setCantidad(item.getCantidad() - 1);
            if (item.getCantidad() <= 0) {
                carrito.getItems().remove(item);
            }
        }
        model.addAttribute("carrito", carrito);
        return "mesero/pedido/carrito :: detalle";
    }

    @GetMapping("/pedido/carrito/nota/{platoId}")
    public String notaModal(@PathVariable Integer platoId, Model model) {
        model.addAttribute("platoId", platoId);
        return "mesero/pedido/nota_modal :: modal";
    }

    @PostMapping("/pedido/carrito/nota/{platoId}")
    public String guardarNota(@PathVariable Integer platoId, @RequestParam String nota, @ModelAttribute("carrito") CarritoDTO carrito, Model model) {
        ItemCarritoDTO item = carrito.getItems().stream().filter(i -> i.getPlatoId().equals(platoId)).findFirst().orElse(null);
        if (item != null) {
            item.setNota(nota);
        }
        model.addAttribute("carrito", carrito);
        return "mesero/pedido/carrito :: detalle";
    }

    @PostMapping("/pedido/finalizar")
    public String finalizarPedido(@ModelAttribute("carrito") CarritoDTO carrito, HttpSession session, Model model) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        
        List<DetallePedido> detalles = carrito.getItems().stream().map(i -> {
            DetallePedido dp = new DetallePedido();
            dp.setPlato(platoService.buscarPorId(i.getPlatoId()));
            dp.setCantidad(i.getCantidad());
            dp.setPrecioUnitario(i.getPrecio());
            dp.setNota(i.getNota());
            return dp;
        }).collect(Collectors.toList());

        try {
            if (carrito.isModifying()) {
                pedidoService.modificarPedido(carrito.getPedidoId(), detalles, null, "Modificación desde UI");
            } else {
                pedidoService.crearPedido(carrito.getMesaId(), u.getId(), detalles);
            }
        } catch (IllegalStateException e) {
            getSalonFragment(model);
            model.addAttribute("errorModal", e.getMessage());
            carrito.getItems().clear();
            return "mesero/salon :: salon-view";
        }
        
        carrito.getItems().clear();
        return getSalonFragment(model);
    }

    private String editorView(Model model, String activeCat) {
        CarritoDTO carrito = (CarritoDTO) model.getAttribute("carrito");
        model.addAttribute("activeCat", activeCat);
        model.addAttribute("isModifying", carrito != null && carrito.isModifying());
        model.addAttribute("platos", platoService.listarActivos().stream()
                .filter(p -> p.getCategoria().equals(activeCat)).collect(Collectors.toList()));
        return "mesero/pedido/editor :: editor";
    }
}
