package com.portfolio.backend.service;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.portfolio.backend.model.Profesor;

/**
 * @author Mariano Puchetta 28 may. 2024
 */

public interface IProfesorService {

	public List<Profesor> getAllProfesores();

	public Profesor getProfesor(Long id);

	public void newProfesor(Profesor profesor);

	public void deleteProfesor(Long id);
	
    // Nuevo método para buscar profesores con filtros opcionales
    public List<Profesor> findProfesoresByFilters(Integer cupof, Integer dni, String fechaNacimiento, String nombre, 
                                                  String sitRev, String modalidad, String materia, Integer cantMod, 
                                                  Integer año, Integer division, String turno, String dia, String horario);

}