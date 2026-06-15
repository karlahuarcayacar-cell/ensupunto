package com.ensupunto.service.impl;

import com.ensupunto.entity.Mesa;
import com.ensupunto.repository.MesaRepository;
import com.ensupunto.service.MesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MesaServiceImpl implements MesaService {

    private final MesaRepository mesaRepository;

    @Override
    public List<Mesa> listarTodas() {
        return mesaRepository.findAll();
    }

    @Override
    public Mesa buscarPorId(Integer id) {
        return mesaRepository.findById(id).orElse(null);
    }

    @Override
    public Mesa actualizarEstado(Integer id, String nuevoEstado) {
        return mesaRepository.findById(id).map(mesa -> {
            mesa.setEstado(nuevoEstado);
            return mesaRepository.save(mesa);
        }).orElse(null);
    }

    @Override
    public Mesa guardar(Mesa mesa) {
        if (mesa.getEstado() == null || mesa.getEstado().trim().isEmpty()) {
            mesa.setEstado("libre");
        }
        return mesaRepository.save(mesa);
    }

    @Override
    public void eliminar(Integer id) {
        mesaRepository.deleteById(id);
    }
}
