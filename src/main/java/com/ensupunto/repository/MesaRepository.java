package com.ensupunto.repository;

import com.ensupunto.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * REPOSITORIO DE DATOS: MESA
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Acceso a Datos Simple:
 *    Hereda de JpaRepository. Dado que la gestión física del salón (CRUD de mesas) no
 *    requiere consultas de filtrado complejas más allá de buscar por ID y listar todas,
 *    se mantiene como una interfaz limpia y minimalista, delegando toda la complejidad a Spring.
 */
@Repository
public interface MesaRepository extends JpaRepository<Mesa, Integer> {
}
