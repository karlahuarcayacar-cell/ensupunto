package com.ensupunto.service;

import com.ensupunto.entity.Usuario;
import java.util.List;

public interface UsuarioService {
    Usuario login(String nombreUsuario, String contrasena);
    List<Usuario> listarTodos();
    Usuario buscarPorId(Integer id);
    Usuario guardar(Usuario usuario);
    void eliminar(Integer id); // Soft delete can be implemented
}
