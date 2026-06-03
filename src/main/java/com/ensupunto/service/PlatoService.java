package com.ensupunto.service;

import com.ensupunto.entity.Plato;
import java.util.List;

public interface PlatoService {
    List<Plato> listarTodos();
    List<Plato> listarActivos();
    List<Plato> listarPorCategoria(String categoria);
    Plato buscarPorId(Integer id);
    Plato guardar(Plato plato);
    void eliminar(Integer id); // baja lógica
}
