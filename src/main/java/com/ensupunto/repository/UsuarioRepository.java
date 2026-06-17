package com.ensupunto.repository;

import com.ensupunto.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * REPOSITORIO DE DATOS: USUARIO (EMPLEADO)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. findByNombreUsuarioAndContrasenaAndActivoTrue:
 *    Genera automáticamente una consulta SQL filtrando por nombreUsuario (username),
 *    contrasena (password), y activo = true. Esto asegura que los usuarios deshabilitados (baja lógica)
 *    no puedan iniciar sesión en el sistema.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    
    // Busca un usuario por su nombre de usuario único
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    
    // Busca un usuario activo por sus credenciales (Autenticación)
    Optional<Usuario> findByNombreUsuarioAndContrasenaAndActivoTrue(String nombreUsuario, String contrasena);
}
