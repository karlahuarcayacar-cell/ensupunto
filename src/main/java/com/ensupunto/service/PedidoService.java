package com.ensupunto.service;

import com.ensupunto.entity.Pedido;
import com.ensupunto.entity.DetallePedido;
import com.ensupunto.entity.PagoFraccionado;

import java.util.List;
import java.util.Map;

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
}
