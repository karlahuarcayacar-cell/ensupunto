package com.ensupunto.repository;

import com.ensupunto.entity.Plato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * REPOSITORIO DE DATOS: PLATO
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Mantenimiento y Filtrado del Menú:
 *    Provee consultas para obtener únicamente los platos marcados como 'activo = true'.
 *    Esto evita mostrar en la carta aquellos platos descontinuados, pero mantiene
 *    su registro para consultas e integridad histórica.
 */
@Repository
public interface PlatoRepository extends JpaRepository<Plato, Integer> {
    
    // Lista los platos habilitados para el salón
    List<Plato> findByActivoTrue();
    
    // Lista los platos activos de una categoría particular (ej: 'entradas')
    List<Plato> findByCategoriaAndActivoTrue(String categoria);
}
