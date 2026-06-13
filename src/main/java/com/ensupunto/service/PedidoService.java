package com.ensupunto.service;

import com.ensupunto.entity.Pedido;
import com.ensupunto.entity.DetallePedido;
import com.ensupunto.entity.PagoFraccionado;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface PedidoService {
    Pedido crearPedido(Integer mesaId, Integer meseroId, List<DetallePedido> detalles);
    Pedido buscarPorId(Integer id);
    Pedido buscarPedidoActivoPorMesa(Integer mesaId);
    
    // Cocina (Chef)
    List<Pedido> listarPedidosParaCocina();
    Pedido prepararPedido(Integer pedidoId); // Nuevo
    Pedido terminarPedido(Integer pedidoId); // Nuevo
    
    // Modificaciones (Mesero)
    Pedido modificarPedido(Integer pedidoId, List<DetallePedido> nuevosDetalles, Integer adminId, String razonAuditoria);
    boolean validarAdmin(String username, String password);

    // Cobro y División (Cajero)
    Pedido cobrarPedidoRegular(Integer pedidoId, Integer cajeroId, String metodoPago);
    List<PagoFraccionado> dividirCuenta(Integer pedidoId, int nPartes);
    PagoFraccionado registrarPagoFraccion(Integer fraccionId, String metodoPago, Integer cajeroId);
    PagoFraccionado buscarFraccionPorId(Integer id);
    List<PagoFraccionado> obtenerFraccionesPorPedido(Integer pedidoId);

    // Mapping helper
    default List<Map<String, Object>> mapearPedidos(List<Pedido> pedidos) {
        return pedidos.stream().map(p -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", p.getId());
            map.put("total", p.getTotal());
            map.put("estado", p.getEstado());
            map.put("metodoPago", p.getMetodoPago());
            map.put("fechaCreacion", p.getFechaCreacion());
            map.put("fechaPago", p.getFechaPago());
            map.put("mesa", p.getMesa());
            map.put("mesero", p.getMesero());
            map.put("detalles", p.getDetalles().stream().map(d -> {
                Map<String, Object> dm = new java.util.HashMap<>();
                dm.put("id", d.getId());
                dm.put("cantidad", d.getCantidad());
                dm.put("precioUnitario", d.getPrecioUnitario());
                dm.put("nota", d.getNota());
                dm.put("plato", d.getPlato());
                return dm;
            }).collect(Collectors.toList()));
            return map;
        }).collect(Collectors.toList());
    }
}
