package com.basic_food_basket.repository;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.basic_food_basket.model.Supermercado;

import java.util.Optional;

@Repository
public interface SupermercadoRepository extends JpaRepository<Supermercado, Long> {
    Optional<Supermercado> findBySlug(String slug);
}

