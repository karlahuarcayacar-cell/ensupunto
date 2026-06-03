package com.ensupunto.service;

import java.util.Map;

public interface ReporteService {
    Map<String, Object> obtenerKpisDelDia();
    Map<String, Object> obtenerVentasPorCategoria();
}
