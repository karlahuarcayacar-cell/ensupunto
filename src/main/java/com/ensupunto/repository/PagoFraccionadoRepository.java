package com.ensupunto.repository;

import com.ensupunto.entity.PagoFraccionado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * REPOSITORIO DE DATOS: PAGO FRACCIONADO
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. findByPedidoId:
 *    Retorna todas las fracciones asociadas a un pedido determinado para poder comprobar
 *    si se saldó la totalidad de la cuenta dividida antes de liberar la mesa física.
 */
@Repository
public interface PagoFraccionadoRepository extends JpaRepository<PagoFraccionado, Integer> {
    
    // Obtiene el listado de fracciones del cobro de un pedido
    List<PagoFraccionado> findByPedidoId(Integer pedidoId);
}
