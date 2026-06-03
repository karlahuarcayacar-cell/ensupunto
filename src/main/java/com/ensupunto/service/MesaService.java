package com.ensupunto.service;

import com.ensupunto.entity.Mesa;
import java.util.List;

public interface MesaService {
    List<Mesa> listarTodas();
    Mesa buscarPorId(Integer id);
    Mesa actualizarEstado(Integer id, String nuevoEstado);
}
