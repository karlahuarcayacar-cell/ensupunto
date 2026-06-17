package com.ensupunto.service.impl;

import com.ensupunto.entity.Usuario;
import com.ensupunto.repository.UsuarioRepository;
import com.ensupunto.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CAPA DE NEGOCIO (SERVICE IMPLEMENTATION): GESTIÓN DE USUARIOS
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. Autenticación Básica con Soft Delete:
 *    El método `login` valida que las credenciales de acceso coincidan con un registro de la base de datos,
 *    pero añade la precondición `activo = true` (`findByNombreUsuarioAndContrasenaAndActivoTrue`).
 *    Esto bloquea inmediatamente a cualquier empleado cuyo contrato haya vencido o cuya cuenta
 *    haya sido desactivada por el administrador, garantizando la seguridad en el acceso.
 * 
 * 2. Reactivación Lógica (`reactivar`):
 *    Permite volver a habilitar (`activo = true`) una cuenta previamente dada de baja,
 *    una característica común en paneles de administración escolar/empresarial.
 */
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public Usuario login(String nombreUsuario, String contrasena) {
        // Ejecuta la consulta de autenticación filtrando solo usuarios activos
        return usuarioRepository.findByNombreUsuarioAndContrasenaAndActivoTrue(nombreUsuario, contrasena)
                .orElse(null);
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Override
    public void eliminar(Integer id) {
        // Implementación de la baja lógica del empleado
        usuarioRepository.findById(id).ifPresent(usuario -> {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
        });
    }

    @Override
    public void reactivar(Integer id) {
        // Habilita nuevamente al empleado
        usuarioRepository.findById(id).ifPresent(usuario -> {
            usuario.setActivo(true);
            usuarioRepository.save(usuario);
        });
    }
}
