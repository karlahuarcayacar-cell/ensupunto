package com.ensupunto.service;

import com.ensupunto.entity.Plato;
import java.util.List;

/**
 * CAPA DE NEGOCIO (SERVICE INTERFACE): GESTIÓN DE PLATOS DE LA CARTA
 */
public interface PlatoService {
    
    // Lista todos los platos de la carta (incluyendo activos e inactivos)
    List<Plato> listarTodos();
    
    // Lista los platos activos (habilitados para los meseros en el salón)
    List<Plato> listarActivos();
    
    // Filtra los platos activos por su categoría (Ej: 'entradas', 'segundos')
    List<Plato> listarPorCategoria(String categoria);
    
    // Busca un plato por su identificador único
    Plato buscarPorId(Integer id);
    
    // Guarda los datos del plato (creación o actualización)
    Plato guardar(Plato plato);
    
    // Baja lógica del plato (pone 'activo = false')
    void eliminar(Integer id);
}
