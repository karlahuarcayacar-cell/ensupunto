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

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "pedido_id", nullable = false)
	    private Pedido pedido;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "administrador_id", nullable = false)
	    private Usuario administrador;

	    @Column(nullable = false, length = 255)
	    private String detalle;

	    @Column(name = "fecha_creacion", nullable = false, updatable = false)
	    private LocalDateTime fechaCreacion;

	    @PrePersist
	    public void prePersist() {
	        this.fechaCreacion = LocalDateTime.now();
	    }
	
}
