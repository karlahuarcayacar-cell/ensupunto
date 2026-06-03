package com.ensupunto.service.impl;

import com.ensupunto.entity.DetallePedido;
import com.ensupunto.entity.Pedido;
import com.ensupunto.repository.PedidoRepository;
import com.ensupunto.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final PedidoRepository pedidoRepository;

    @Override
    public Map<String, Object> obtenerKpisDelDia() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
        List<Pedido> pedidosPagados = pedidoRepository.findByEstadoAndFechaPagoBetween("pagado", startOfDay, endOfDay);
        
        BigDecimal ingresosTotales = BigDecimal.ZERO;
        int clientesAtendidos = pedidosPagados.size();
        
        for (Pedido p : pedidosPagados) {
            ingresosTotales = ingresosTotales.add(p.getTotal());
        }

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("ingresosDia", ingresosTotales);
        kpis.put("ordenesCompletadas", clientesAtendidos);
        kpis.put("ticketPromedio", clientesAtendidos > 0 ? ingresosTotales.divide(new BigDecimal(clientesAtendidos), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        kpis.put("satisfaccion", 95); // Mocked for UI
        
        return kpis;
    }

    @Override
    public Map<String, Object> obtenerVentasPorCategoria() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
        List<Pedido> pedidosPagados = pedidoRepository.findByEstadoAndFechaPagoBetween("pagado", startOfDay, endOfDay);
        
        Map<String, BigDecimal> ventasPorCategoria = new HashMap<>();
        
        for(Pedido p : pedidosPagados) {
            for(DetallePedido dp : p.getDetalles()) {
                String cat = dp.getPlato().getCategoria();
                BigDecimal subtotal = dp.getPrecioUnitario().multiply(new BigDecimal(dp.getCantidad()));
                ventasPorCategoria.put(cat, ventasPorCategoria.getOrDefault(cat, BigDecimal.ZERO).add(subtotal));
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("categorias", ventasPorCategoria.keySet());
        response.put("valores", ventasPorCategoria.values());
        
        return response;
    }
}
