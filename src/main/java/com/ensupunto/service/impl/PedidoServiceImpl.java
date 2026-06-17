package com.ensupunto.service.impl;

import com.ensupunto.entity.*;
import com.ensupunto.repository.*;
import com.ensupunto.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * CAPA DE NEGOCIO (SERVICE IMPLEMENTATION): CONTROL TRANSACCIONAL DE PEDIDOS
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Transaccionalidad de Spring Boot (@Transactional):
 *    - La anotación @Transactional envuelve el método en una transacción de base de datos administrada por Spring.
 *    - Si el método se ejecuta con éxito, Spring realiza un COMMIT físico en la BD, guardando de forma atómica todos los cambios.
 *    - Si ocurre alguna RuntimeException (o error de validación), Spring automáticamente realiza un ROLLBACK, deshaciendo
 *      todos los cambios pendientes en las tablas para asegurar la consistencia total del negocio.
 * 
 * 2. Lógica de Negocio y Estados Cruzados:
 *    Esta clase contiene reglas lógicas estrictas para coordinar el estado de las mesas físicas y el estado de los pedidos.
 *    Ejemplo: al pagar, la mesa cambia a "libre". Al crear un pedido, la mesa pasa a "esperando_comida".
 */
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModificacionPedidoRepository modificacionRepository;
    private final DetallePedidoRepository detalleRepository;
    private final PagoFraccionadoRepository pagoFraccionadoRepository;

    /**
     * REGISTRO DE NUEVO PEDIDO O AGREGAR PLATOS A PEDIDO EXISTENTE.
     * 
     * Se ejecuta dentro de una transacción. Si la mesa ya tiene un pedido activo (no pagado),
     * se añaden los platos a ese mismo pedido; en caso contrario, se inicializa uno nuevo.
     */
    @Override
    @Transactional
    public Pedido crearPedido(Integer mesaId, Integer meseroId, List<DetallePedido> detalles) {
        Mesa mesa = mesaRepository.findById(mesaId).orElseThrow();
        Usuario mesero = usuarioRepository.findById(meseroId).orElseThrow();

        // 1. Buscamos si existe un pedido en curso en esa mesa física (estado != 'pagado')
        Pedido pedido = pedidoRepository.findByMesaIdAndEstadoNot(mesaId, "pagado")
                .orElseGet(() -> {
                    // Si no existe, creamos el Pedido inicial
                    Pedido p = new Pedido();
                    p.setMesa(mesa);
                    p.setMesero(mesero);
                    p.setEstado("cocina_pendiente");
                    p.setTotal(BigDecimal.ZERO);
                    return pedidoRepository.save(p);
                });

        // 2. Restricción lógica: no se puede añadir comida a mesas en proceso de facturación
        if ("dividido".equals(pedido.getEstado()) || "cuenta_pedida".equals(pedido.getEstado())) {
            throw new IllegalStateException("No se pueden agregar platos a una mesa con cuenta dividida o en proceso de cobro.");
        }

        // 3. Calculamos acumulados e insertamos detalles de los platos elegidos
        BigDecimal total = pedido.getTotal();
        for (DetallePedido dp : detalles) {
            dp.setPedido(pedido); // Establecemos la relación bidireccional (clave foránea)
            total = total.add(dp.getPrecioUnitario().multiply(new BigDecimal(dp.getCantidad())));
            pedido.getDetalles().add(dp); // Agregamos a la colección en cascada
        }
        
        pedido.setTotal(total);
        
        // 4. Actualizamos el estado visual de la mesa en el salón
        mesa.setEstado("esperando_comida");
        mesaRepository.save(mesa);

        // Al retornar, Spring hace commit y persiste todos los detalles debido a CascadeType.ALL
        return pedidoRepository.save(pedido);
    }

    @Override
    public Pedido buscarPorId(Integer id) {
        return pedidoRepository.findById(id).orElse(null);
    }

    @Override
    public Pedido buscarPedidoActivoPorMesa(Integer mesaId) {
        return pedidoRepository.findByMesaIdAndEstadoNot(mesaId, "pagado").orElse(null);
    }

    @Override
    public List<Pedido> listarPedidosParaCocina() {
        return pedidoRepository.findByEstadoInOrderByFechaCreacionAsc(Arrays.asList("cocina_pendiente", "cocina_preparacion"));
    }

    /**
     * CHEF ENTRA EN ACCIÓN: El pedido pasa a preparándose en cocina.
     */
    @Override
    @Transactional
    public Pedido prepararPedido(Integer pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        pedido.setEstado("cocina_preparacion");
        
        Mesa mesa = pedido.getMesa();
        mesa.setEstado("cocina_preparacion");
        mesaRepository.save(mesa);
        
        return pedidoRepository.save(pedido);
    }

    /**
     * CHEF TERMINA DE COCINAR: Comida lista para servir.
     */
    @Override
    @Transactional
    public Pedido terminarPedido(Integer pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        pedido.setEstado("cocina_listo");
        
        Mesa mesa = pedido.getMesa();
        mesa.setEstado("comiendo"); // El mesero lleva los platos y los clientes empiezan a consumir
        mesaRepository.save(mesa);
        
        return pedidoRepository.save(pedido);
    }

    /**
     * VALIDACIÓN DE PRIVILEGIOS: Compara datos con administrador activo.
     */
    @Override
    public boolean validarAdmin(String username, String password) {
        return usuarioRepository.findByNombreUsuario(username)
                .filter(u -> u.getRol().equals("admin") && u.getContrasena().equals(password))
                .isPresent();
    }

    /**
     * MODIFICACIÓN DE PEDIDOS EN COCINA (Autorización Administrativa y Bitácora).
     * 
     * Si los platos ya entraron en cocción y el cliente desea cancelar o cambiar un plato,
     * se requiere la firma del Administrador. Este método registra la bitácora de auditoría
     * de forma atómica junto a la alteración del pedido.
     */
    @Override
    @Transactional
    public Pedido modificarPedido(Integer pedidoId, List<DetallePedido> nuevosDetalles, Integer adminId, String razonAuditoria) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        
        if ("dividido".equals(pedido.getEstado()) || "cuenta_pedida".equals(pedido.getEstado())) {
            throw new IllegalStateException("No se puede modificar un pedido con cuenta dividida o en proceso de cobro.");
        }

        // 1. Si se proveyó la firma del Administrador, registramos la bitácora de auditoría
        if (adminId != null) {
            Usuario admin = usuarioRepository.findById(adminId).orElseThrow();
            ModificacionPedido mod = ModificacionPedido.builder()
                    .pedido(pedido)
                    .administrador(admin)
                    .detalle(razonAuditoria)
                    .build();
            modificacionRepository.save(mod);
        }

        // 2. Reemplazamos los detalles viejos con los nuevos limpiando la colección en cascada
        // (JPA/Hibernate detecta 'orphanRemoval=true' y borrará los huérfanos de la base de datos de inmediato)
        pedido.getDetalles().clear();
        BigDecimal nuevoTotal = BigDecimal.ZERO;
        
        for (DetallePedido dp : nuevosDetalles) {
            dp.setPedido(pedido);
            nuevoTotal = nuevoTotal.add(dp.getPrecioUnitario().multiply(new BigDecimal(dp.getCantidad())));
            pedido.getDetalles().add(dp);
        }
        
        pedido.setTotal(nuevoTotal);
        return pedidoRepository.save(pedido);
    }

    /**
     * COBRO REGULAR DE LA CUENTA (Cajero).
     * 
     * Actualiza el pedido a 'pagado', asocia al cajero que cobró, registra la fecha/hora actual
     * y libera físicamente la mesa en el mapa del salón ('libre').
     */
    @Override
    @Transactional
    public Pedido cobrarPedidoRegular(Integer pedidoId, Integer cajeroId, String metodoPago) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        Usuario cajero = usuarioRepository.findById(cajeroId).orElseThrow();

        pedido.setCajero(cajero);
        pedido.setEstado("pagado");
        pedido.setMetodoPago(metodoPago);
        pedido.setFechaPago(LocalDateTime.now());

        // Liberación de la mesa física en el mapa
        Mesa mesa = pedido.getMesa();
        mesa.setEstado("libre");
        mesaRepository.save(mesa);

        return pedidoRepository.save(pedido);
    }

    /**
     * INICIAR PROCESO DE DIVISION DE CUENTAS (Cuentas Divididas).
     * 
     * Divide el total de la orden en N partes iguales, crea N registros individuales en la
     * tabla 'pagos_fraccionados' y cambia el estado del pedido a 'dividido' para pausar
     * modificaciones adicionales.
     */
    @Override
    @Transactional
    public List<PagoFraccionado> dividirCuenta(Integer pedidoId, int nPartes) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        pedido.setEstado("dividido");
        pedidoRepository.save(pedido);

        // Divide el importe total acumulando redondeos en centavos (HALF_UP)
        BigDecimal montoFraccion = pedido.getTotal().divide(new BigDecimal(nPartes), 2, java.math.RoundingMode.HALF_UP);
        
        // Registramos cada una de las partes de forma individual
        for (int i = 1; i <= nPartes; i++) {
            PagoFraccionado pf = PagoFraccionado.builder()
                    .pedido(pedido)
                    .numeroCliente(i)
                    .monto(montoFraccion)
                    .pagado(false)
                    .build();
            pagoFraccionadoRepository.save(pf);
        }
        
        return pagoFraccionadoRepository.findByPedidoId(pedidoId);
    }

    /**
     * PROCESAR PAGO INDEPENDIENTE DE UNA FRACCIÓN DE CUENTA.
     * 
     * Marca una fracción individual como cobrada. Acto seguido, comprueba si todas las fracciones
     * asociadas a la orden ya fueron pagadas. Si es así, finaliza el Pedido completo marcándolo
     * como 'pagado' y liberando la mesa física ('libre').
     */
    @Override
    @Transactional
    public PagoFraccionado registrarPagoFraccion(Integer fraccionId, String metodoPago, Integer cajeroId) {
        PagoFraccionado pf = pagoFraccionadoRepository.findById(fraccionId).orElseThrow();
        pf.setPagado(true);
        pf.setMetodoPago(metodoPago);
        pf.setFechaPago(LocalDateTime.now());
        // Generamos un número correlativo aleatorio simulado para la boleta
        pf.setNumeroBoleta("B001-" + String.format("%06d", (int)(Math.random() * 1000000)));
        pagoFraccionadoRepository.save(pf);

        // Verificamos si todos los amigos de la mesa ya cancelaron sus respectivas partes
        Pedido pedido = pf.getPedido();
        List<PagoFraccionado> todas = pagoFraccionadoRepository.findByPedidoId(pedido.getId());
        boolean todosPagados = todas.stream().allMatch(PagoFraccionado::getPagado);

        // Si ya no quedan saldos pendientes de cobro
        if (todosPagados) {
            pedido.setEstado("pagado");
            pedido.setMetodoPago("dividido");
            pedido.setFechaPago(LocalDateTime.now());
            pedido.setCajero(usuarioRepository.findById(cajeroId).orElse(null));
            pedidoRepository.save(pedido);

            // Liberación de la mesa física de forma atómica
            Mesa mesa = pedido.getMesa();
            mesa.setEstado("libre");
            mesaRepository.save(mesa);
        }

        return pf;
    }

    @Override
    public PagoFraccionado buscarFraccionPorId(Integer id) {
        return pagoFraccionadoRepository.findById(id).orElseThrow();
    }

    @Override
    public List<PagoFraccionado> obtenerFraccionesPorPedido(Integer pedidoId) {
        return pagoFraccionadoRepository.findByPedidoId(pedidoId);
    }
}
