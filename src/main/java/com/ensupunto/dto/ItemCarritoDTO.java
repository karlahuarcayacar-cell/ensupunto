package com.ensupunto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemCarritoDTO {
    private Integer platoId;
    private String nombre;
    private BigDecimal precio;
    private Integer cantidad;
    private String nota;
}
