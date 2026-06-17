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

/**
 * ENTIDAD ORM: PAGO FRACCIONADO
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Soporte de Negocio para Cuentas Divididas:
 *    Esta entidad almacena cada una de las "partes" en las que se fracciona una cuenta.
 *    Por ejemplo, si un pedido de total S/. 150.00 se divide entre 3 personas, el sistema creará
 *    3 registros de PagoFraccionado con un monto de S/. 50.00 cada uno.
 * 
 * 2. Relación @ManyToOne (FetchType.LAZY):
 *    Vincula cada fracción a su pedido principal. Usamos carga diferida (LAZY) porque
 *    no siempre se requiere instanciar el objeto completo Pedido al listar o actualizar las fracciones.
 * 
 * 3. Atributo 'numeroCliente':
 *    Indica a qué cliente de la división corresponde (Cliente 1, Cliente 2, etc.).
 * 
 * 4. Atributo 'pagado':
 *    Permite al cajero ir cobrando de manera independiente y asíncrona cada fracción,
 *    liberando la mesa física únicamente cuando el último pago fraccionado sea saldado.
 */
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

	    // Relación al Pedido padre de la división
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "pedido_id", nullable = false)
	    private Pedido pedido;

	    // Identificador secuencial del cliente dentro del grupo de pago (Ej: 1, 2, 3...)
	    @Column(name = "numero_cliente", nullable = false)
	    private Integer numeroCliente;

	    // Monto fraccionado a pagar por esta persona
	    @Column(nullable = false, precision = 10, scale = 2)
	    private BigDecimal monto;

	    // Método de pago usado por este cliente: 'efectivo', 'tarjeta', 'yape'
	    @Column(name = "metodo_pago", length = 30)
	    private String metodoPago;

	    // Número de comprobante independiente emitido para esta fracción (Ej: 'B001-000123')
	    @Column(name = "numero_boleta", length = 30)
	    private String numeroBoleta;

	    // Estado de cobro de esta fracción en particular
	    @Column(nullable = false)
	    private Boolean pagado = false;

	    // Fecha y hora del pago de esta fracción
	    @Column(name = "fecha_pago")
	    private LocalDateTime fechaPago;

}
