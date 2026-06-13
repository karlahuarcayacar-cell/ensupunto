package com.ensupunto.repository;

import com.ensupunto.entity.ModificacionPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModificacionPedidoRepository extends JpaRepository<ModificacionPedido, Integer> {
    List<ModificacionPedido> findByPedidoId(Integer pedidoId);

    @Query("SELECT m FROM ModificacionPedido m JOIN FETCH m.pedido p JOIN FETCH m.administrador a JOIN FETCH p.mesa order by m.fechaCreacion DESC")
    List<ModificacionPedido> findAllWithPedidoAndAdmin();
}
