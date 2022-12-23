package com.portfolio.backend.repository;

import org.springframework.data.repository.CrudRepository;

import com.portfolio.backend.model.Usuario;

public interface UsuarioRepository extends CrudRepository<Usuario, Integer> {
	
	Usuario findByUsername(String username);
	
}