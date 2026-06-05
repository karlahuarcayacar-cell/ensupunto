package com.ensupunto.service.impl;

import com.ensupunto.entity.DetallePedido;
import com.ensupunto.entity.Pedido;
import com.ensupunto.repository.PedidoRepository;
import com.ensupunto.service.ReporteService;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        kpis.put("satisfaccion", 95);
        
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

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReporteVentas() {
        try {
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

            List<Pedido> pedidosPagados = pedidoRepository.findByEstadoAndFechaPagoBetween("pagado", startOfDay, endOfDay);

            List<Map<String, ?>> dataSource = new ArrayList<>();
            for (Pedido p : pedidosPagados) {
                Map<String, Object> row = new HashMap<>();
                row.put("mesaNombre", p.getMesa().getNombre());
                row.put("meseroNombre", p.getMesero().getNombre());
                row.put("total", p.getTotal());
                row.put("metodoPago", p.getMetodoPago() != null ? p.getMetodoPago() : "");
                row.put("fechaPago", p.getFechaPago() != null
                        ? p.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "");
                dataSource.add(row);
            }

            Map<Integer, Map<String, Object>> topPlatosMap = new LinkedHashMap<>();
            for (Pedido p : pedidosPagados) {
                for (DetallePedido dp : p.getDetalles()) {
                    Integer platoId = dp.getPlato().getId();
                    topPlatosMap.putIfAbsent(platoId, new HashMap<>());
                    Map<String, Object> platoData = topPlatosMap.get(platoId);
                    platoData.put("nombre", dp.getPlato().getNombre());
                    platoData.put("categoria", dp.getPlato().getCategoria());
                    platoData.put("unidades", (Integer) platoData.getOrDefault("unidades", 0) + dp.getCantidad());
                    platoData.put("totalVendido", ((BigDecimal) platoData.getOrDefault("totalVendido", BigDecimal.ZERO))
                            .add(dp.getPrecioUnitario().multiply(new BigDecimal(dp.getCantidad()))));
                }
            }

            List<Map<String, Object>> topPlatos = topPlatosMap.values().stream()
                    .sorted((a, b) -> ((Integer) b.get("unidades")).compareTo((Integer) a.get("unidades")))
                    .limit(5)
                    .collect(Collectors.toList());

            Map<String, Object> params = new HashMap<>();
            params.put("TOP_PLATOS", topPlatos);
            params.put("FECHA_REPORTE", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            ClassPathResource resource = new ClassPathResource("reportes/reporte_ventas.jrxml");
            if (!resource.exists()) {
                throw new RuntimeException("No se encontró el archivo reportes/reporte_ventas.jrxml en el classpath");
            }
            InputStream reportStream = resource.getInputStream();
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            JRDataSource jrDataSource = new JRMapCollectionDataSource(dataSource);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, jrDataSource);

            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte de ventas: " + e.getMessage(), e);
        }
    }
}
