package com.ensupunto.service.impl;

import com.ensupunto.entity.Plato;
import com.ensupunto.repository.PlatoRepository;
import com.ensupunto.service.PlatoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CAPA DE NEGOCIO (SERVICE IMPLEMENTATION): GESTIÓN DE PLATOS
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Implementación de Baja Lógica (Soft Delete):
 *    En lugar de invocar `deleteById` para remover físicamente el plato de la BD, se busca el plato,
 *    se altera su campo `activo` a `false` y se vuelve a guardar. Esto protege la integridad referencial.
 *    Si elimináramos físicamente un plato, todos los registros de la tabla `detalles_pedido`
 *    asociados históricamente a ese plato quedarían con llaves foráneas rotas o serían eliminados en cascada,
 *    alterando la contabilidad histórica de ventas.
 */
@Service
@RequiredArgsConstructor
public class PlatoServiceImpl implements PlatoService {

    private final PlatoRepository platoRepository;

    @Override
    public List<Plato> listarTodos() {
        return platoRepository.findAll();
    }

    @Override
    public List<Plato> listarActivos() {
        return platoRepository.findByActivoTrue();
    }

    @Override
    public List<Plato> listarPorCategoria(String categoria) {
        return platoRepository.findByCategoriaAndActivoTrue(categoria);
    }

    @Override
    public Plato buscarPorId(Integer id) {
        return platoRepository.findById(id).orElse(null);
    }

    @Override
    public Plato guardar(Plato plato) {
        return platoRepository.save(plato);
    }

    @Override
    public void eliminar(Integer id) {
        // Implementación de la baja lógica
        platoRepository.findById(id).ifPresent(plato -> {
            plato.setActivo(false);
            platoRepository.save(plato);
        });
    }
}
