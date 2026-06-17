package com.ensupunto.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * ENTIDAD ORM: DETALLE DE PEDIDO (ITEMS DE LA ORDEN)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 
 * 1. Mapeo de Relaciones de Muchos a Uno (@ManyToOne):
 *    - Cada detalle pertenece a un único 'Pedido' (@ManyToOne).
 *    - Cada detalle hace referencia a un único 'Plato' (@ManyToOne).
 * 
 * 2. @JoinColumn: Especifica la columna de unión física (foreign key) en la tabla relacional.
 *    - 'pedido_id' en la tabla detalles_pedido apunta a 'id' de la tabla pedidos.
 *    - 'plato_id' en la tabla detalles_pedido apunta a 'id' de la tabla platos.
 * 
 * 3. Evitar el Bucle de Serialización Infinita (@JsonIgnore):
 *    Al serializar un Pedido a JSON, se serializan sus detalles. Pero cada detalle tiene una referencia
 *    de retorno al Pedido. Sin @JsonIgnore en la propiedad 'pedido' de esta clase, la librería de serialización
 *    (Jackson) entraría en un bucle infinito recursivo que agotaría la pila de llamadas (StackOverflowError).
 * 
 * 4. Precio Unitario Histórico ('precioUnitario'):
 *    Es una buena práctica en bases de datos de ventas. Guardamos el precio unitario del plato en el instante
 *    que se tomó el pedido. Si en el futuro el administrador edita el precio del plato en el menú,
 *    las boletas y reportes antiguos seguirán mostrando el total correcto histórico sin verse alterados.
 */
@Entity
@Table(name = "detalles_pedido")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relación de retorno al pedido principal (LAZY para no cargar el pedido de nuevo al consultar detalles por separado)
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    // Relación al plato seleccionado (EAGER porque al ver el detalle es indispensable saber qué plato es)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plato_id", nullable = false)
    private Plato plato;

    // Cantidad ordenada del plato
    @Column(nullable = false)
    private Integer cantidad;

    // Precio unitario al momento del pedido (previene distorsiones por cambios de precio futuros)
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    // Indicación especial para preparación (Ej: 'sin cebolla', 'término medio')
    @Column(length = 255)
    private String nota;
}
