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

/**
 * CONTROLADOR WEB (SPRING MVC + HTMX): MÓDULO DEL MESERO (ATENCIÓN EN SALÓN)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. @SessionAttributes("carrito"):
 *    Indica a Spring MVC que mantenga el atributo de modelo "carrito" guardado de forma persistente
 *    en la sesión HTTP del usuario. Esto es esencial para simular el carrito de compras (toma de pedidos)
 *    mientras el mesero navega entre categorías de platos y agrega ítems, evitando persistir datos en la BD
 *    de forma prematura.
 * 
 * 2. Integración con HTMX (Single Page Application - SPA Reactiva):
 *    En lugar de hacer recargas completas del navegador (Full Page Reload), este controlador está diseñado
 *    para responder con fragmentos HTML parciales (usando la notación "plantilla :: fragmento").
 *    HTMX intercepta las llamadas AJAX en el cliente y reemplaza de forma asíncrona únicamente el contenedor HTML
 *    correspondiente. Esto mejora la velocidad y la experiencia de usuario.
 * 
 * 3. Cabeceras de Respuesta HTMX (HX-Retarget y HX-Reswap):
 *    Permiten al backend controlar el comportamiento del cliente de forma dinámica.
 *    - 'HX-Retarget': Cambia el selector CSS destino donde HTMX inyectará el fragmento HTML de respuesta.
 *    - 'HX-Reswap': Especifica la estrategia de inyección (ej: outerHTML, innerHTML, beforeend).
 */
@Controller
@RequestMapping("/mesero")
@RequiredArgsConstructor
@SessionAttributes("carrito")
public class MeseroController {

    private final MesaService mesaService;
    private final PlatoService platoService;
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    /**
     * @ModelAttribute("carrito"):
     * Inicializa el objeto CarritoDTO en el modelo si es que no existe previamente en la sesión HTTP.
     */
    @ModelAttribute("carrito")
    public CarritoDTO carrito() {
        return new CarritoDTO();
    }

    /**
     * Carga el contenedor principal de la vista del salón del mesero (salon.html).
     */
    @GetMapping
    public String meseroPage(Model model, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        if (u == null || !u.getRol().equals("mesero")) return "redirect:/login";
        
        model.addAttribute("usuario", u);
        return "mesero/salon"; // Devuelve la vista principal
    }

    /**
     * Devuelve el fragmento HTML completo de la vista del salón (HTMX).
     */
    @GetMapping("/salon/fragment")
    public String getSalonFragment(Model model) {
        getMesasFragment(model);
        return "mesero/salon :: salon-view";
    }

    /**
     * Devuelve únicamente el listado de tarjetas de mesas del salón (HTMX).
     * Calcula dinámicamente colores y métricas de pedidos activos de cada mesa.
     */
    @GetMapping("/fragment/mesas")
    public String getMesasFragment(Model model) {
        List<Mesa> mesas = mesaService.listarTodas();
        List<Map<String, Object>> mesasStatus = new ArrayList<>();

        for (Mesa m : mesas) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("nombre", m.getNombre());
            map.put("estado", m.getEstado());
            
            // Asigna clases CSS y etiquetas en español según el estado de la mesa física
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

            // Vincula el pedido activo si la mesa no está libre
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
        return "mesero/salon :: lista-mesas"; // Retorna solo la sección de tarjetas
    }

    /**
     * Carga el modal con el detalle de consumos actuales de una mesa.
     */
    @GetMapping("/pedido/detalle/{id}")
    public String getPedidoDetalle(@PathVariable Integer id, Model model) {
        model.addAttribute("pedido", pedidoService.buscarPorId(id));
        return "mesero/pedido/detalle_modal :: modal";
    }

    /**
     * Inicializa un pedido nuevo para una mesa limpia.
     */
    @GetMapping("/pedido/nuevo/{mesaId}")
    public String nuevoPedido(@PathVariable Integer mesaId, @ModelAttribute("carrito") CarritoDTO carrito, Model model) {
        Pedido p = pedidoService.buscarPedidoActivoPorMesa(mesaId);
        
        // Restricción: no se puede alterar mesas en cola de cobro
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
        carrito.getItems().clear(); // Limpiamos el carrito en sesión
        
        return editorView(model, "entradas"); // Abre la vista de toma de pedidos
    }

    /**
     * Intenta iniciar la edición de un pedido en curso.
     * 
     * SEGURIDAD CRÍTICA (Firma de Administrador):
     * Si el pedido ya está en estado "cocina_preparacion" (cocinándose), el mesero NO puede
     * modificarlo libremente. El sistema intercepta la acción, altera los headers HTTP
     * de HTMX para inyectar al final del DOM un modal que solicita credenciales de Administrador.
     */
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
      
        // CASO DE COCINA: Si ya se está preparando en cocina, se detiene al mesero y se exige PIN de Admin
        if (p != null && "cocina_preparacion".equals(p.getEstado())) {
            model.addAttribute("pedidoId", pedidoId);
            response.setHeader("HX-Retarget", "body"); // Redirige la respuesta al body del navegador
            response.setHeader("HX-Reswap", "beforeend"); // Añade el modal al final de la página
            return "mesero/pedido/autorizacion_modal :: modal"; // Retorna el modal de autenticación
        }
      
        // CASO NORMAL (cocina_pendiente): Se cargan los platos actuales en el carrito de sesión
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

    /**
     * Procesa la autorización administrativa en Ajax (HTMX).
     * Si las credenciales de administrador son correctas, permite habilitar la interfaz
     * de edición del pedido e inyecta la bitácora de auditoría.
     */
    @PostMapping("/pedido/editar/{pedidoId}/autorizar")
    public String autorizarYEditar(@PathVariable Integer pedidoId,
                                    @RequestParam String adminUsername,
                                    @RequestParam String adminPassword,
                                    @ModelAttribute("carrito") CarritoDTO carrito,
                                    Model model,
                                    jakarta.servlet.http.HttpServletResponse response) {
      
        // Validamos la cuenta administrativa
        if (!pedidoService.validarAdmin(adminUsername, adminPassword)) {
            model.addAttribute("pedidoId", pedidoId);
            model.addAttribute("error", "Usuario o contraseña de administrador incorrectos.");
            // Si falla, volvemos a renderizar el modal con el aviso de error
            response.setHeader("HX-Retarget", "#autorizacion-modal");
            response.setHeader("HX-Reswap", "outerHTML");
            return "mesero/pedido/autorizacion_modal :: modal";
        }
      
        // Si la validación es correcta, cargamos el carrito para proceder con los cambios autorizados
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
      
        response.setHeader("HX-Retarget", "#mesero-app");
        response.setHeader("HX-Reswap", "innerHTML");
        return editorView(model, "entradas");
    }
    
    /**
     * Carga el catálogo de platos activos filtrados por categoría (HTMX).
     */
    @GetMapping("/pedido/catalogo/{categoria}")
    public String getCatalogo(@PathVariable String categoria, Model model) {
        model.addAttribute("platos", platoService.listarActivos().stream()
                .filter(p -> p.getCategoria().equals(categoria)).collect(Collectors.toList()));
        return "mesero/pedido/catalogo :: lista";
    }

    /**
     * Agrega un plato al carrito temporal en la sesión (HTMX).
     */
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
        return "mesero/pedido/carrito :: detalle"; // Actualiza asíncronamente el panel derecho
    }

    /**
     * Quita o decrementa un plato del carrito de sesión (HTMX).
     */
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

    /**
     * Abre el modal de notas de cocina para un ítem del menú.
     */
    @GetMapping("/pedido/carrito/nota/{platoId}")
    public String notaModal(@PathVariable Integer platoId, Model model) {
        model.addAttribute("platoId", platoId);
        return "mesero/pedido/nota_modal :: modal";
    }

    /**
     * Guarda la nota de preparación especial en el ítem del DTO del carrito (HTMX).
     */
    @PostMapping("/pedido/carrito/nota/{platoId}")
    public String guardarNota(@PathVariable Integer platoId, @RequestParam String nota, @ModelAttribute("carrito") CarritoDTO carrito, Model model) {
        ItemCarritoDTO item = carrito.getItems().stream().filter(i -> i.getPlatoId().equals(platoId)).findFirst().orElse(null);
        if (item != null) {
            item.setNota(nota);
        }
        model.addAttribute("carrito", carrito);
        return "mesero/pedido/carrito :: detalle";
    }

    /**
     * Finaliza la toma de pedido, guardándolo definitivamente en la base de datos (HTMX).
     * Mapea los ítems del DTO Carrito a objetos de la entidad DetallePedido y delega la inserción.
     */
    @PostMapping("/pedido/finalizar")
    public String finalizarPedido(@ModelAttribute("carrito") CarritoDTO carrito, HttpSession session, Model model) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        
        // Transformamos los DTOs temporales de sesión en entidades persistentes
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
                // Si modificamos, auditamos indicando la alteración
                pedidoService.modificarPedido(carrito.getPedidoId(), detalles, null, "Modificación desde UI");
            } else {
                // Si es nuevo, inicializamos el pedido
                pedidoService.crearPedido(carrito.getMesaId(), u.getId(), detalles);
            }
        } catch (IllegalStateException e) {
            getSalonFragment(model);
            model.addAttribute("errorModal", e.getMessage());
            carrito.getItems().clear();
            return "mesero/salon :: salon-view";
        }
        
        carrito.getItems().clear(); // Limpiamos el carrito para la próxima toma
        return getSalonFragment(model); // Retornamos al plano del salón principal
    }

    /**
     * Generador de la vista del editor (Thymeleaf layout helper).
     */
    private String editorView(Model model, String activeCat) {
        CarritoDTO carrito = (CarritoDTO) model.getAttribute("carrito");
        model.addAttribute("activeCat", activeCat);
        model.addAttribute("isModifying", carrito != null && carrito.isModifying());
        model.addAttribute("platos", platoService.listarActivos().stream()
                .filter(p -> p.getCategoria().equals(activeCat)).collect(Collectors.toList()));
        return "mesero/pedido/editor :: editor";
    }
}
