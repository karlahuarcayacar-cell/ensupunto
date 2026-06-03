package com.ensupunto.service.impl;

import com.ensupunto.entity.Plato;
import com.ensupunto.repository.PlatoRepository;
import com.ensupunto.service.PlatoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
        platoRepository.findById(id).ifPresent(plato -> {
            plato.setActivo(false);
            platoRepository.save(plato);
        });
    }
}
