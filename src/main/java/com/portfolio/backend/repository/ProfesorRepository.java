package com.portfolio.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;


import com.portfolio.backend.model.Profesor;

public interface ProfesorRepository extends JpaRepository<Profesor, Long> {
	
}
