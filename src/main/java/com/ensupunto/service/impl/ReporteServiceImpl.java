package com.ensupunto.service.impl;

import com.ensupunto.entity.DetallePedido;
import com.ensupunto.entity.Pedido;
import com.ensupunto.repository.PedidoRepository;
import com.ensupunto.service.ReporteService;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRDataSource;

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
import java.util.*;
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

        for (Pedido p : pedidosPagados) {
            for (DetallePedido dp : p.getDetalles()) {
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
    public Map<String, Object> obtenerTopPlatos() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<Pedido> pedidosPagados = pedidoRepository.findByEstadoAndFechaPagoBetween("pagado", startOfDay, endOfDay);

        Map<Integer, Map<String, Object>> topMap = new LinkedHashMap<>();
        for (Pedido p : pedidosPagados) {
            for (DetallePedido dp : p.getDetalles()) {
                Integer platoId = dp.getPlato().getId();
                topMap.putIfAbsent(platoId, new HashMap<>());
                Map<String, Object> data = topMap.get(platoId);
                data.put("nombre", dp.getPlato().getNombre());
                data.put("cantidad", (Integer) data.getOrDefault("cantidad", 0) + dp.getCantidad());
            }
        }

        List<Map<String, Object>> sorted = topMap.values().stream()
                .sorted((a, b) -> ((Integer) b.get("cantidad")).compareTo((Integer) a.get("cantidad")))
                .limit(5)
                .collect(Collectors.toList());

        List<String> nombres = sorted.stream().map(m -> (String) m.get("nombre")).collect(Collectors.toList());
        List<Integer> cantidades = sorted.stream().map(m -> (Integer) m.get("cantidad")).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("nombres", nombres);
        response.put("cantidades", cantidades);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerHistorialHoy() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        return pedidoRepository.findByEstadoAndFechaPagoBetween("pagado", startOfDay, endOfDay);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerHistorialCompleto() {
        return pedidoRepository.findByEstado("pagado");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerHistorialPeriodo(LocalDate desde, LocalDate hasta) {
        LocalDateTime start = LocalDateTime.of(desde, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(hasta, LocalTime.MAX);
        return pedidoRepository.findByEstadoAndFechaPagoBetween("pagado", start, end);
    }

    private byte[] generarReporteDesdePedidos(List<Pedido> pedidos, String titulo, String fechaLabel) {
        try {
            List<Map<String, ?>> dataSource = new ArrayList<>();
            for (Pedido p : pedidos) {
                Map<String, Object> row = new HashMap<>();
                row.put("mesaNombre", p.getMesa().getNombre());
                row.put("meseroNombre", p.getMesero() != null ? p.getMesero().getNombre() : "-");
                row.put("total", p.getTotal());
                row.put("metodoPago", p.getMetodoPago() != null ? p.getMetodoPago() : "");
                row.put("fechaPago", p.getFechaPago() != null
                        ? p.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "");
                dataSource.add(row);
            }

            Map<Integer, Map<String, Object>> topPlatosMap = new LinkedHashMap<>();
            for (Pedido p : pedidos) {
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
            params.put("FECHA_REPORTE", fechaLabel);

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
            throw new RuntimeException("Error al generar reporte PDF: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReporteVentas() {
        String fechaLabel = "Fecha: " + LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES")));
        return generarReporteDesdePedidos(obtenerHistorialHoy(),
                "Reporte de Ventas del Día",
                fechaLabel);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReporteVentasCompleto() {
        return generarReporteDesdePedidos(obtenerHistorialCompleto(),
                "Reporte Histórico de Ventas",
                "Historial completo - generado el "
                        + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReporteVentasPeriodo(LocalDate desde, LocalDate hasta) {
        String fechaLabel = "Período: "
                + desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " al "
                + hasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return generarReporteDesdePedidos(obtenerHistorialPeriodo(desde, hasta),
                "Reporte de Ventas por Período",
                fechaLabel);
    }
}