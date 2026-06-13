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

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModificacionPedidoRepository modificacionRepository;
    private final DetallePedidoRepository detalleRepository;
    private final PagoFraccionadoRepository pagoFraccionadoRepository;

    @Override
    @Transactional
    public Pedido crearPedido(Integer mesaId, Integer meseroId, List<DetallePedido> detalles) {
        Mesa mesa = mesaRepository.findById(mesaId).orElseThrow();
        Usuario mesero = usuarioRepository.findById(meseroId).orElseThrow();

        Pedido pedido = pedidoRepository.findByMesaIdAndEstadoNot(mesaId, "pagado")
                .orElseGet(() -> {
                    Pedido p = new Pedido();
                    p.setMesa(mesa);
                    p.setMesero(mesero);
                    p.setEstado("cocina_pendiente");
                    p.setTotal(BigDecimal.ZERO);
                    return pedidoRepository.save(p);
                });

        if ("dividido".equals(pedido.getEstado()) || "cuenta_pedida".equals(pedido.getEstado())) {
            throw new IllegalStateException("No se pueden agregar platos a una mesa con cuenta dividida o en proceso de cobro.");
        }

        BigDecimal total = pedido.getTotal();
        for (DetallePedido dp : detalles) {
            dp.setPedido(pedido);
            total = total.add(dp.getPrecioUnitario().multiply(new BigDecimal(dp.getCantidad())));
            pedido.getDetalles().add(dp);
        }
        
        pedido.setTotal(total);
        mesa.setEstado("esperando_comida");
        mesaRepository.save(mesa);

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

    @Override
    @Transactional
    public Pedido terminarPedido(Integer pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        pedido.setEstado("cocina_listo");
        
        Mesa mesa = pedido.getMesa();
        mesa.setEstado("comiendo"); // O "cuenta_pedida" si ya terminó todo
        mesaRepository.save(mesa);
        
        return pedidoRepository.save(pedido);
    }

    @Override
    public boolean validarAdmin(String username, String password) {
        return usuarioRepository.findByNombreUsuario(username)
                .filter(u -> u.getRol().equals("admin") && u.getContrasena().equals(password))
                .isPresent();
    }

    @Override
    @Transactional
    public Pedido modificarPedido(Integer pedidoId, List<DetallePedido> nuevosDetalles, Integer adminId, String razonAuditoria) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        
        if ("dividido".equals(pedido.getEstado()) || "cuenta_pedida".equals(pedido.getEstado())) {
            throw new IllegalStateException("No se puede modificar un pedido con cuenta dividida o en proceso de cobro.");
        }

        // Audit trail if admin authorized something
        if (adminId != null) {
            Usuario admin = usuarioRepository.findById(adminId).orElseThrow();
            ModificacionPedido mod = ModificacionPedido.builder()
                    .pedido(pedido)
                    .administrador(admin)
                    .detalle(razonAuditoria)
                    .build();
            modificacionRepository.save(mod);
        }

        // Simplificación: reemplazamos detalles (en un sistema real se compararían IDs)
        // Pero para cumplir con el CU: agregamos o quitamos según la lógica del front
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

    @Override
    @Transactional
    public Pedido cobrarPedidoRegular(Integer pedidoId, Integer cajeroId, String metodoPago) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        Usuario cajero = usuarioRepository.findById(cajeroId).orElseThrow();

        pedido.setCajero(cajero);
        pedido.setEstado("pagado");
        pedido.setMetodoPago(metodoPago);
        pedido.setFechaPago(LocalDateTime.now());

        Mesa mesa = pedido.getMesa();
        mesa.setEstado("libre");
        mesaRepository.save(mesa);

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public List<PagoFraccionado> dividirCuenta(Integer pedidoId, int nPartes) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        pedido.setEstado("dividido");
        pedidoRepository.save(pedido);

        BigDecimal montoFraccion = pedido.getTotal().divide(new BigDecimal(nPartes), 2, java.math.RoundingMode.HALF_UP);
        
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

    @Override
    @Transactional
    public PagoFraccionado registrarPagoFraccion(Integer fraccionId, String metodoPago, Integer cajeroId) {
        PagoFraccionado pf = pagoFraccionadoRepository.findById(fraccionId).orElseThrow();
        pf.setPagado(true);
        pf.setMetodoPago(metodoPago);
        pf.setFechaPago(LocalDateTime.now());
        pf.setNumeroBoleta("B001-" + String.format("%06d", (int)(Math.random() * 1000000)));
        pagoFraccionadoRepository.save(pf);

        // Verificar si todos están pagados
        Pedido pedido = pf.getPedido();
        List<PagoFraccionado> todas = pagoFraccionadoRepository.findByPedidoId(pedido.getId());
        boolean todosPagados = todas.stream().allMatch(PagoFraccionado::getPagado);

        if (todosPagados) {
            pedido.setEstado("pagado");
            pedido.setMetodoPago("dividido");
            pedido.setFechaPago(LocalDateTime.now());
            pedido.setCajero(usuarioRepository.findById(cajeroId).orElse(null));
            pedidoRepository.save(pedido);

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
}
