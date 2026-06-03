package com.ensupunto.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/mesero")
@RequiredArgsConstructor
public class MeseroController {

    private final MesaService mesaService;
    private final PlatoService platoService;
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    @GetMapping
    public String meseroPage(Model model, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        if (u == null || !u.getRol().equals("mesero")) return "redirect:/login";
        
        model.addAttribute("usuario", u);
        model.addAttribute("mesas", mesaService.listarTodas());
        model.addAttribute("platos", platoService.listarActivos());
        return "mesero";
    }

    @GetMapping("/api/mesas-status")
    @ResponseBody
    public List<Map<String, Object>> getMesasStatus() {
        List<Mesa> mesas = mesaService.listarTodas();
        List<Map<String, Object>> response = new ArrayList<>();

        for (Mesa m : mesas) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("nombre", m.getNombre());
            map.put("estado", m.getEstado());

            Pedido p = pedidoService.buscarPedidoActivoPorMesa(m.getId());
            if (p != null) {
                map.put("pedidoId", p.getId());
                map.put("total", p.getTotal());
                map.put("cantidadPlatos", p.getDetalles().stream().mapToInt(d -> d.getCantidad()).sum());
                map.put("meseroNombre", p.getMesero().getNombre());
            }
            response.add(map);
        }
        return response;
    }

    @GetMapping("/api/pedidos/{id}")
    @ResponseBody
    public ResponseEntity<?> getPedido(@PathVariable Integer id) {
        Pedido p = pedidoService.buscarPorId(id);
        return ResponseEntity.ok(p);
    }

    @PostMapping("/api/pedidos")
    @ResponseBody
    public ResponseEntity<?> crearPedido(@RequestBody Map<String, Object> payload, HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        if (u == null) return ResponseEntity.status(401).build();

        Integer mesaId = Integer.parseInt(payload.get("mesaId").toString());
        List<Map<String, Object>> detallesMap = (List<Map<String, Object>>) payload.get("detalles");

        List<DetallePedido> detalles = new ArrayList<>();
        for (Map<String, Object> map : detallesMap) {
            Integer platoId = Integer.parseInt(map.get("platoId").toString());
            Plato plato = platoService.buscarPorId(platoId);
            DetallePedido dp = new DetallePedido();
            dp.setPlato(plato);
            dp.setCantidad(Integer.parseInt(map.get("cantidad").toString()));
            dp.setPrecioUnitario(plato.getPrecio());
            dp.setNota(map.get("nota") != null ? map.get("nota").toString() : null);
            detalles.add(dp);
        }

        Pedido pedido = pedidoService.crearPedido(mesaId, u.getId(), detalles);
        return ResponseEntity.ok(pedido);
    }

    @PostMapping("/api/pedidos/{id}/modificar")
    @ResponseBody
    public ResponseEntity<?> modificarPedido(@PathVariable Integer id, 
                                             @RequestBody Map<String, Object> payload, 
                                             HttpSession session) {
        Usuario u = (Usuario) session.getAttribute("usuarioLogueado");
        
        List<Map<String, Object>> detallesMap = (List<Map<String, Object>>) payload.get("detalles");
        Integer adminId = payload.get("adminId") != null ? Integer.parseInt(payload.get("adminId").toString()) : null;
        String razon = payload.get("razon") != null ? payload.get("razon").toString() : null;

        List<DetallePedido> detalles = new ArrayList<>();
        for (Map<String, Object> map : detallesMap) {
            Integer platoId = Integer.parseInt(map.get("platoId").toString());
            Plato plato = platoService.buscarPorId(platoId);
            DetallePedido dp = new DetallePedido();
            dp.setPlato(plato);
            dp.setCantidad(Integer.parseInt(map.get("cantidad").toString()));
            dp.setPrecioUnitario(plato.getPrecio());
            dp.setNota(map.get("nota") != null ? map.get("nota").toString() : null);
            detalles.add(dp);
        }

        Pedido pedido = pedidoService.modificarPedido(id, detalles, adminId, razon);
        return ResponseEntity.ok(pedido);
    }

    @PostMapping("/api/validar-admin")
    @ResponseBody
    public ResponseEntity<?> validarAdmin(@RequestBody Map<String, String> creds) {
        boolean valid = pedidoService.validarAdmin(creds.get("username"), creds.get("password"));
        if (valid) {
            Usuario admin = usuarioService.listarTodos().stream()
                    .filter(u -> u.getNombreUsuario().equals(creds.get("username")))
                    .findFirst().orElse(null);
            return ResponseEntity.ok(admin);
        }
        return ResponseEntity.status(401).build();
    }
}
