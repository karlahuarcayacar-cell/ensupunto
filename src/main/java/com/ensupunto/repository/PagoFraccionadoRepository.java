package com.ensupunto.repository;

import com.ensupunto.entity.PagoFraccionado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoFraccionadoRepository extends JpaRepository<PagoFraccionado, Integer> {
    List<PagoFraccionado> findByPedidoId(Integer pedidoId);
}
