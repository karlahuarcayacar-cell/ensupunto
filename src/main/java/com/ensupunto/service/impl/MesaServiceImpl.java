package com.ensupunto.service.impl;

import com.ensupunto.entity.Mesa;
import com.ensupunto.repository.MesaRepository;
import com.ensupunto.service.MesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CAPA DE NEGOCIO (SERVICE IMPLEMENTATION): GESTIÓN DE MESAS
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. @Service: Registra esta clase como un Bean de Servicio de negocio en el contenedor de Spring.
 * 
 * 2. Inyección de Dependencias por Constructor (@RequiredArgsConstructor de Lombok):
 *    Genera automáticamente un constructor público que recibe la variable final 'mesaRepository'.
 *    Spring Boot detecta automáticamente ese constructor e inyecta la instancia del repositorio (DI).
 *    Esta estrategia de inyección por constructor es más recomendada académicamente que usar @Autowired directo
 *    en el campo (Field Injection), ya que permite la inmutabilidad de la variable (final) y facilita pruebas unitarias.
 */
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
        // Busca la mesa por ID, si la encuentra modifica su estado y la vuelve a persistir.
        return mesaRepository.findById(id).map(mesa -> {
            mesa.setEstado(nuevoEstado);
            return mesaRepository.save(mesa);
        }).orElse(null);
    }

    @Override
    public Mesa guardar(Mesa mesa) {
        // Aseguramos que toda mesa nueva guardada empiece en estado "libre" si no se especificó otro.
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
