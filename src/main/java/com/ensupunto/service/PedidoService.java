package com.ensupunto.service;

import com.ensupunto.entity.Pedido;
import com.ensupunto.entity.DetallePedido;
import com.ensupunto.entity.PagoFraccionado;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CAPA DE NEGOCIO (SERVICE INTERFACE): CONTROL DE PEDIDOS Y TRANSACCIONES
 * 
 * Esta interfaz define todas las operaciones de flujo de pedidos, cocina, auditoría y cobro.
 */
public interface PedidoService {
    
    // Registra un nuevo pedido para una mesa, inicializándolo en la cola de cocina
    Pedido crearPedido(Integer mesaId, Integer meseroId, List<DetallePedido> detalles);
    
    // Busca un pedido por ID
    Pedido buscarPorId(Integer id);
    
    // Busca el pedido activo de una mesa determinada (aquel que aún no ha sido pagado)
    Pedido buscarPedidoActivoPorMesa(Integer mesaId);
    
    // Cocina (Chef): Lista pedidos listos para cocinar o preparándose
    List<Pedido> listarPedidosParaCocina();
    
    // Cambia el estado del pedido a "En preparación" y actualiza el estado de la mesa física
    Pedido prepararPedido(Integer pedidoId);
    
    // Finaliza la cocción del pedido cambiándolo a "Listo para servir"
    Pedido terminarPedido(Integer pedidoId);
    
    // Modificaciones (Mesero): Registra cambios en pedidos en preparación guardando rastro de auditoría
    Pedido modificarPedido(Integer pedidoId, List<DetallePedido> nuevosDetalles, Integer adminId, String razonAuditoria);
    
    // Valida credenciales de administrador para autorizar modificaciones de pedidos en cocina
    boolean validarAdmin(String username, String password);

    // Cobro Regular: Procesa el pago total de la cuenta, emite comprobante y libera la mesa
    Pedido cobrarPedidoRegular(Integer pedidoId, Integer cajeroId, String metodoPago);
    
    // Cuenta Dividida: Divide el importe total entre N partes iguales, creando registros fraccionados
    List<PagoFraccionado> dividirCuenta(Integer pedidoId, int nPartes);
    
    // Procesa el pago de una fracción de cuenta independiente y evalúa si todo el pedido fue cancelado
    PagoFraccionado registrarPagoFraccion(Integer fraccionId, String metodoPago, Integer cajeroId);
    
    // Busca una fracción por su identificador único
    PagoFraccionado buscarFraccionPorId(Integer id);
    
    // Lista todas las fracciones asociadas a un pedido
    List<PagoFraccionado> obtenerFraccionesPorPedido(Integer pedidoId);

    /**
     * Mapeador auxiliar (Helper) para estructurar datos y evitar LazyInitializationExceptions en la vista.
     * Convierte entidades JPA complejas a mapas planos con tipos de datos nativos legibles en Thymeleaf.
     */
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
