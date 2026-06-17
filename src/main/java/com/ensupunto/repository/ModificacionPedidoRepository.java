package com.ensupunto.repository;

import com.ensupunto.entity.ModificacionPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * REPOSITORIO DE DATOS: MODIFICACIÓN DE PEDIDO (BITÁCORA)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. El Problema de las N+1 Consultas:
 *    Si hacemos un select simple de ModificacionPedido (m) y luego en un bucle o vista iteramos
 *    m.getPedido() y m.getAdministrador() (que están mapeados como FetchType.LAZY), Hibernate
 *    disparará una consulta SQL adicional por cada registro obtenido para cargar sus relaciones.
 *    Si hay N modificaciones, se ejecutarán N+1 consultas en total, destruyendo el rendimiento.
 * 
 * 2. Solución con JOIN FETCH:
 *    La anotación @Query permite definir una consulta JPQL (Java Persistence Query Language).
 *    Usamos 'JOIN FETCH' para indicarle a Hibernate que traiga de forma inmediata ('Eagerly')
 *    el pedido, el administrador y la mesa en una única consulta SQL combinada con INNER JOINs.
 *    Esto reduce el número de llamadas a la base de datos a exactamente una (1), optimizando el sistema.
 */
@Repository
public interface ModificacionPedidoRepository extends JpaRepository<ModificacionPedido, Integer> {
    
    // Obtiene las modificaciones por ID de pedido
    List<ModificacionPedido> findByPedidoId(Integer pedidoId);

    // Consulta de auditoría optimizada que carga de golpe todas las entidades Lazy
    @Query("SELECT m FROM ModificacionPedido m JOIN FETCH m.pedido p JOIN FETCH m.administrador a JOIN FETCH p.mesa order by m.fechaCreacion DESC")
    List<ModificacionPedido> findAllWithPedidoAndAdmin();
}
