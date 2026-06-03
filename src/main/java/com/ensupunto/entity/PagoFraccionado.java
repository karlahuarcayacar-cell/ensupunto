package com.ensupunto.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pagos_fraccionados")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoFraccionado {
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Integer id;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "pedido_id", nullable = false)
	    private Pedido pedido;

	    @Column(name = "numero_cliente", nullable = false)
	    private Integer numeroCliente;

	    @Column(nullable = false, precision = 10, scale = 2)
	    private BigDecimal monto;

	    @Column(name = "metodo_pago", length = 30)
	    private String metodoPago; // 'efectivo', 'tarjeta', 'yape'

	    @Column(name = "numero_boleta", length = 30)
	    private String numeroBoleta;

	    @Column(nullable = false)
	    private Boolean pagado = false;

	    @Column(name = "fecha_pago")
	    private LocalDateTime fechaPago;

}
