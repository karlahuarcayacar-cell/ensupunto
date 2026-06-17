package com.ensupunto.repository;

import com.ensupunto.entity.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * REPOSITORIO DE DATOS: DETALLE DE PEDIDO
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Persistencia de Detalles:
 *    Permite persistir de forma individual o masiva los platos y cantidades asociados a un pedido.
 *    Debido a que Pedido está configurado con CascadeType.ALL en su relación con DetallePedido,
 *    la mayor parte de las inserciones y eliminaciones se hacen directamente a través del repositorio
 *    de Pedido, pero este repositorio es útil si se requiere consultar o auditar detalles individuales.
 */
@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Integer> {
}
