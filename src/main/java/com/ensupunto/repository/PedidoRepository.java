package com.ensupunto.repository;

import com.ensupunto.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    // Find active order for a table
    Optional<Pedido> findByMesaIdAndEstadoNot(Integer mesaId, String estado);
    Optional<Pedido> findByMesaIdAndEstadoIn(Integer mesaId, List<String> estados);

    // Find orders for Kitchen (pendientes, cocina_preparacion)
    List<Pedido> findByEstadoInOrderByFechaCreacionAsc(List<String> estados);

    // Find paid orders today for stats
    List<Pedido> findByEstadoAndFechaPagoBetween(String estado, LocalDateTime start, LocalDateTime end);
}
