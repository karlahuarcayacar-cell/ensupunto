package com.ensupunto.service;

import com.ensupunto.entity.Mesa;
import java.util.List;

/**
 * CAPA DE NEGOCIO (SERVICE INTERFACE): GESTIÓN DE MESAS
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Desacoplamiento (Loose Coupling):
 *    Definir una interfaz antes de la implementación es una buena práctica (Patrón de Inversión de Dependencias).
 *    Permite cambiar la implementación concreta (por ejemplo, de JPA a NoSQL o mockups para pruebas unitarias)
 *    sin alterar los controladores que consumen el servicio.
 */
public interface MesaService {
    
    // Recupera la lista completa de mesas del restaurante
    List<Mesa> listarTodas();
    
    // Busca una mesa específica por su ID
    Mesa buscarPorId(Integer id);
    
    // Cambia el estado físico de la mesa (Ej: de 'cocina_preparacion' a 'comiendo')
    Mesa actualizarEstado(Integer id, String nuevoEstado);
    
    // Guarda o actualiza los datos físicos de una mesa (CRUD Admin)
    Mesa guardar(Mesa mesa);
    
    // Elimina físicamente una mesa por ID
    void eliminar(Integer id);
}
