package com.portfolio.backend.repository;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portfolio.backend.model.Supermercado;

import java.util.Optional;

@Repository
public interface SupermercadoRepository extends JpaRepository<Supermercado, Long> {
    Optional<Supermercado> findBySlug(String slug);
}

