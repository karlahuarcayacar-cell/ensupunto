package com.ensupunto.repository;

import com.ensupunto.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REPOSITORIO DE DATOS: PEDIDO
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. @Repository: Anotación de estereotipo que indica a Spring que esta interfaz es un
 *    componente de acceso a datos (DAO - Data Access Object). Además, habilita la traducción
 *    automática de excepciones de bases de datos a excepciones legibles por Spring (@Repository translation).
 * 
 * 2. JpaRepository<Pedido, Integer>: Extiende JpaRepository pasándole la Entidad y el tipo de dato
 *    de su clave primaria. Al hacerlo, Spring Data JPA autogenera en tiempo de ejecución toda la
 *    implementación con los métodos básicos del CRUD: save(), findById(), findAll(), deleteById(), etc.
 * 
 * 3. Derivación de Consultas por Nombre de Método (Query Methods):
 *    Spring analiza los nombres de los métodos y construye dinámicamente la consulta SQL.
 *    - 'findByMesaIdAndEstadoNot': Filtra pedidos por mesa y excluye un estado específico.
 *    - 'findByEstadoAndFechaPagoBetween': Filtra pedidos de un estado en un rango de fechas.
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    
    // Consulta para encontrar el pedido activo de una mesa (aquel cuyo estado NO sea 'pagado')
    Optional<Pedido> findByMesaIdAndEstadoNot(Integer mesaId, String estado);
    
    // Consulta para encontrar pedidos en mesas filtrados por varios estados
    Optional<Pedido> findByMesaIdAndEstadoIn(Integer mesaId, List<String> estados);

    // Consulta para el Monitor de Cocina del Chef: pedidos pendientes u ordenados por fecha ascendente (cola FIFO)
    List<Pedido> findByEstadoInOrderByFechaCreacionAsc(List<String> estados);

    // Encuentra todos los pedidos según su estado
    List<Pedido> findByEstado(String estado);

    // Filtro para el Reporte de Ventas del día: busca pedidos pagados en un rango de horas (min/max del día)
    List<Pedido> findByEstadoAndFechaPagoBetween(String estado, LocalDateTime start, LocalDateTime end);
}
