package com.ensupunto.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * PATRÓN DE DISEÑO DTO: CARRITO DTO (DATA TRANSFER OBJECT)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Data Transfer Object (DTO): Es un patrón de diseño que consiste en objetos simples
 *    cuya única finalidad es transportar datos entre las diferentes capas de la aplicación
 *    (desde la interfaz web de Thymeleaf hacia los controladores de Spring).
 * 
 * 2. Diferencia con Entidades JPA:
 *    A diferencia de 'Pedido', 'CarritoDTO' no es una clase persistente mapeada en la BD.
 *    Se mantiene puramente en memoria y en la Sesión HTTP del mesero, acumulando los platos y notas
 *    antes de persistir el pedido formalmente. Esto aísla la lógica web de la base de datos.
 * 
 * 3. Cálculos de Importes Centralizados:
 *    Proporciona métodos auxiliares para calcular subtotales, IGV (18%) e importes totales.
 *    Esto mantiene los controladores limpios de código matemático ("Thin Controllers").
 */
@Data
public class CarritoDTO {
    // ID de la mesa a la cual se le está tomando el pedido
    private Integer mesaId;
    
    // Nombre de la mesa
    private String nombreMesa;
    
    // ID del pedido si estamos editando uno existente (nulo si es pedido nuevo)
    private Integer pedidoId;
    
    // Indica si la acción es modificación (true) o creación (false)
    private boolean isModifying;
    
    // Lista de ítems temporales en el carrito
    private List<ItemCarritoDTO> items = new ArrayList<>();
    
    /**
     * Calcula el subtotal multiplicando cantidad por precio unitario para cada ítem.
     * 
     * @return BigDecimal subtotal acumulado.
     */
    public BigDecimal getSubtotal() {
        return items.stream()
                .map(i -> i.getPrecio().multiply(new BigDecimal(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcula el Impuesto General a las Ventas (IGV) peruano (18%).
     * 
     * @return BigDecimal monto del IGV.
     */
    public BigDecimal getIgv() {
        return getSubtotal().multiply(new BigDecimal("0.18"));
    }
    
    /**
     * Calcula el importe total sumando el subtotal y el IGV.
     * 
     * @return BigDecimal total final.
     */
    public BigDecimal getTotal() {
        return getSubtotal().add(getIgv());
    }
}
