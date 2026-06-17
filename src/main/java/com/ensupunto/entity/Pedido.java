package com.ensupunto.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ENTIDAD ORM COMPLEJA: PEDIDO (ORDEN DE CONSUMO)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 
 * 1. Estrategias de Carga de Relaciones (FetchType):
 *    - FetchType.EAGER (Carga Temprana): Obliga a Hibernate a traer los datos de la relación mediante un JOIN HTTP
 *      inmediatamente al consultar la entidad principal. Ejemplo: la relación 'mesa' es EAGER porque siempre que
 *      se consulta un pedido, se necesita saber inmediatamente qué mesa lo solicitó.
 *    - FetchType.LAZY (Carga Diferida/Perezosa): Hibernate no consulta la relación de inmediato. En su lugar,
 *      crea un Proxy. La consulta SQL real se dispara recién cuando el código llama explícitamente a su getter
 *      (ej: getMesero().getNombre()). Es ideal para optimizar el rendimiento y no sobrecargar la memoria.
 * 
 * 2. Operaciones en Cascada (CascadeType):
 *    - CascadeType.ALL: Cualquier operación persistente (crear, actualizar, borrar) realizada sobre el Pedido
 *      se replicará automáticamente en cascada en las entidades relacionadas (sus detalles y pagos fraccionados).
 *      Por ejemplo, al guardar un Pedido con 3 DetallePedido nuevos, no hace falta guardar cada detalle de forma individual;
 *      el EntityManager los persistirá automáticamente en una única transacción.
 * 
 * 3. Eliminación de Huérfanos (orphanRemoval = true):
 *    Si desvinculamos o removemos un objeto DetallePedido de la lista 'detalles' del Pedido, JPA se encargará
 *    de borrar físicamente ese registro de la tabla 'detalles_pedido'. Esto evita que queden registros huérfanos
 *    con llaves foráneas inválidas.
 */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relación de muchos pedidos a una mesa. Se carga EAGER porque el nombre y estado de la mesa es crucial al instante.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mesa_id", nullable = false)
    private Mesa mesa;

    // Relación de muchos pedidos a un mesero. Carga LAZY porque no siempre necesitamos los datos completos del empleado mesero.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesero_id", nullable = false)
    private Usuario mesero;

    // Cajero que procesa el pago. Nuleable al inicio, se llena cuando se finaliza la transacción.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id")
    private Usuario cajero;

    // Estado del pedido (Ej: 'pendiente', 'cocina_pendiente', 'cocina_preparacion', 'cocina_listo', 'servido', 'cuenta_pedida', 'pagado', 'dividido')
    @Column(nullable = false, length = 30)
    private String estado;

    // Importe total del pedido (suma acumulada de detalles)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    // Método de pago: 'efectivo', 'tarjeta', 'yape', 'dividido' (nulo mientras no esté pagado)
    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    // Fecha y hora del registro del pedido (se autocompleta en el PrePersist)
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // Fecha y hora del cobro del pedido
    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    // Relación Bidireccional One-To-Many hacia detalles.
    // 'mappedBy = "pedido"' indica que la clave foránea está definida en la entidad 'DetallePedido' (campo 'pedido').
    @Builder.Default
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    // Relación Bidireccional One-To-Many hacia los pagos fraccionados en caso de cuenta dividida.
    // @JsonIgnore evita ciclos infinitos en serialización a JSON (Pedido -> Pagos -> Pedido -> Pagos).
    @JsonIgnore
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoFraccionado> pagosFraccionados;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.total == null) this.total = BigDecimal.ZERO;
    }
}
