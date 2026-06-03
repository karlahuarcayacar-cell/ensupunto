package com.ensupunto.repository;

import com.ensupunto.entity.Plato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatoRepository extends JpaRepository<Plato, Integer> {
    List<Plato> findByActivoTrue();
    List<Plato> findByCategoriaAndActivoTrue(String categoria);
}
