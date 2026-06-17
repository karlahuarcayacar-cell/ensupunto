package com.ensupunto.service;

import com.ensupunto.entity.Usuario;
import java.util.List;

/**
 * CAPA DE NEGOCIO (SERVICE INTERFACE): GESTIÓN DE USUARIOS / PERSONAL
 */
public interface UsuarioService {
    
    // Autenticación de usuarios por username y password (inicios de sesión)
    Usuario login(String nombreUsuario, String contrasena);
    
    // Lista a todos los usuarios del sistema (activos e inactivos)
    List<Usuario> listarTodos();
    
    // Busca un usuario por ID
    Usuario buscarPorId(Integer id);
    
    // Guarda o actualiza un usuario (CRUD Admin)
    Usuario guardar(Usuario usuario);
    
    // Baja lógica de un empleado (pone 'activo = false')
    void eliminar(Integer id);
    
    // Reactivación lógica de un empleado dado de baja (pone 'activo = true')
    void reactivar(Integer id);
}
