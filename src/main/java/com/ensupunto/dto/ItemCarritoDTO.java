package com.ensupunto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * PATRÓN DE DISEÑO DTO: ÍTEM DE CARRITO DTO
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Encapsulación de fila del carrito:
 *    Representa una línea individual agregada al carrito con sus datos mínimos: plato, precio,
 *    cantidad y la nota de preparación especial.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemCarritoDTO {
    // ID del plato referenciado
    private Integer platoId;
    
    // Nombre del plato
    private String nombre;
    
    // Precio unitario al momento del pedido
    private BigDecimal precio;
    
    // Cantidad agregada por el mesero
    private Integer cantidad;
    
    // Comentario / indicación especial para cocina (Ej. "sin picante")
    private String nota;
}
