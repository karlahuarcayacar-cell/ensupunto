package com.ensupunto.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CarritoDTO {
    private Integer mesaId;
    private String nombreMesa;
    private Integer pedidoId;
    private boolean isModifying;
    private List<ItemCarritoDTO> items = new ArrayList<>();
    
    public BigDecimal getSubtotal() {
        return items.stream()
                .map(i -> i.getPrecio().multiply(new BigDecimal(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getIgv() {
        return getSubtotal().multiply(new BigDecimal("0.18"));
    }
    
    public BigDecimal getTotal() {
        return getSubtotal().add(getIgv());
    }
}
