package com.ensupunto.repository;

import com.ensupunto.entity.ModificacionPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModificacionPedidoRepository extends JpaRepository<ModificacionPedido, Integer> {
    List<ModificacionPedido> findByPedidoId(Integer pedidoId);
}
