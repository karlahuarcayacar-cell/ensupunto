package com.ensupunto.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENTIDAD ORM: MODIFICACIÓN DE PEDIDO (BITÁCORA DE AUDITORÍA)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Pistas de Auditoría (Audit Trail) y Seguridad:
 *    Esta entidad registra y audita cualquier cambio crítico realizado en los pedidos que ya
 *    se encuentran en cocina (Ej: anulación de platos). Cumple un rol fundamental para
 *    evitar fraudes, mermas inexplicables o manipulación no autorizada del flujo de pedidos.
 * 
 * 2. Relaciones @ManyToOne de Auditoría:
 *    - 'pedido': Vincula la bitácora al pedido que sufrió la alteración.
 *    - 'administrador': Registra al usuario con privilegios de Administrador que firmó y
 *       autorizó físicamente el cambio en el salón (ingresando su usuario y clave en el modal).
 * 
 * 3. Atributo 'detalle':
 *    Explicación detallada del cambio (ej. "Se anuló 1 Cebiche Clásico por demora").
 */
@Entity
@Table(name = "modificaciones_pedido")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModificacionPedido {

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Integer id;

	    // Pedido que fue modificado
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "pedido_id", nullable = false)
	    private Pedido pedido;

	    // Administrador que autorizó la acción
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "administrador_id", nullable = false)
	    private Usuario administrador;

	    // Texto descriptivo de la acción autorizada
	    @Column(nullable = false, length = 255)
	    private String detalle;

	    // Fecha y hora del registro del cambio
	    @Column(name = "fecha_creacion", nullable = false, updatable = false)
	    private LocalDateTime fechaCreacion;

	    /**
	     * @PrePersist: Interceptor de JPA para asegurar la inmutabilidad de la fecha.
	     * Garantiza que, justo antes de insertar el registro en base de datos, se asigne
	     * la marca de tiempo (timestamp) del sistema actual.
	     */
	    @PrePersist
	    public void prePersist() {
	        this.fechaCreacion = LocalDateTime.now();
	    }
	
}
