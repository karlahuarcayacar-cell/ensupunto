package com.ensupunto.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ENTIDAD ORM: USUARIO (EMPLEADO)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. @Getter y @Setter: En lugar de usar @Data (que autogenera también equals, hashCode y toString),
 *    muchas veces en entidades complejas se prefiere usar únicamente getters y setters explícitos
 *    para evitar problemas de recursividad cíclica en relaciones bidireccionales.
 * 
 * 2. Eventos de Ciclo de Vida JPA (@PrePersist):
 *    Permite ejecutar lógica de negocio automáticamente en la entidad justo antes de que
 *    se inserte el registro en la base de datos (operación INSERT). Aquí lo usamos para
 *    grabar la fecha de creación actual y forzar el estado activo por defecto.
 * 
 * 3. Roles de usuario: Gestiona la jerarquía de accesos ('mesero', 'chef', 'cajero', 'admin').
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre completo del empleado
    @Column(nullable = false, length = 100)
    private String nombre;

    // Rol del sistema: 'mesero', 'chef', 'cajero', 'admin' (evaluado en el AuthInterceptor)
    @Column(nullable = false, length = 20)
    private String rol;

    // Nombre de usuario único para inicio de sesión (Login)
    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 50)
    private String nombreUsuario;

    // Contraseña. En un sistema real se almacena encriptada (ej: BCrypt)
    @Column(name = "contrasena", nullable = false, length = 255)
    private String contrasena;

    // Estado del empleado (activo/inactivo) para implementar baja lógica en el CRUD de personal
    @Column(nullable = false)
    private Boolean activo = true;

    // Fecha de creación del registro. 'updatable = false' impide que sea alterada en actualizaciones futuras
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Callbacks de ciclo de vida JPA.
     * Se ejecuta automáticamente antes de persistir (INSERT) el registro en la BD.
     */
    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.activo == null) this.activo = true;
    }
}
