package com.ensupunto.service;

import com.ensupunto.entity.Pedido;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReporteService {
    Map<String, Object> obtenerKpisDelDia();
    Map<String, Object> obtenerVentasPorCategoria();
    Map<String, Object> obtenerTopPlatos();
    List<Pedido> obtenerHistorialHoy();
    List<Pedido> obtenerHistorialCompleto();
    List<Pedido> obtenerHistorialPeriodo(LocalDate desde, LocalDate hasta);
    byte[] generarReporteVentas();
    byte[] generarReporteVentasCompleto();
    byte[] generarReporteVentasPeriodo(LocalDate desde, LocalDate hasta);
}
