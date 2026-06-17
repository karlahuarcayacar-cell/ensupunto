package com.ensupunto.service;

import com.ensupunto.entity.Pedido;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * CAPA DE NEGOCIO (SERVICE INTERFACE): INDICADORES GERENCIALES Y REPORTES PDF
 */
public interface ReporteService {
    
    // Obtiene métricas clave del día de hoy: ingresos del día, órdenes completadas, ticket promedio
    Map<String, Object> obtenerKpisDelDia();
    
    // Agrupa ingresos del día de hoy por categoría de plato (estadísticas de ventas)
    Map<String, Object> obtenerVentasPorCategoria();
    
    // Obtiene el ranking de los 5 platos más vendidos del día (análisis de popularidad)
    Map<String, Object> obtenerTopPlatos();
    
    // Obtiene la lista de pedidos cobrados hoy
    List<Pedido> obtenerHistorialHoy();
    
    // Obtiene la lista completa de todos los pedidos históricos cobrados
    List<Pedido> obtenerHistorialCompleto();
    
    // Obtiene los pedidos cobrados en un rango específico de fechas
    List<Pedido> obtenerHistorialPeriodo(LocalDate desde, LocalDate hasta);
    
    // Genera un archivo PDF con el reporte de ventas del día actual
    byte[] generarReporteVentas();
    
    // Genera un archivo PDF con el reporte histórico completo
    byte[] generarReporteVentasCompleto();
    
    // Genera un archivo PDF con el reporte filtrado en un período de tiempo
    byte[] generarReporteVentasPeriodo(LocalDate desde, LocalDate hasta);
}
