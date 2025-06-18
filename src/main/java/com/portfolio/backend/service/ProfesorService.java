package com.portfolio.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.portfolio.backend.model.Profesor;
import com.portfolio.backend.repository.ProfesorRepository;

/**
 * @author Mariano Puchetta 28 may. 2024
 */

@Service
public class ProfesorService  implements IProfesorService{
	

	
	@Autowired
	private ProfesorRepository  profesorRepository;

	@Override
	public List<Profesor> getAllProfesores() {
		return profesorRepository.findAll();
	}

	@Override
	public Profesor getProfesor(Long id) {
		return profesorRepository.findById(id).orElse(null);
	}

	@Override
	public void newProfesor(Profesor profesor) {
		profesorRepository.save(profesor);
	}

	@Override
	public void deleteProfesor(Long id) {
		profesorRepository.deleteById(id);
	}
	
	 @Override
	    public List<Profesor> findProfesoresByFilters(Integer cupof, Integer dni, String fechaNacimiento, String nombre, 
	                                                  String sitRev, String modalidad, String materia, Integer cantMod, 
	                                                  Integer anio, Integer division, String turno, String dia, String horario) {
	        List<Profesor> profesores = profesorRepository.findAll();
	        List<Profesor> filteredProfesores = new ArrayList<>();

	        for (Profesor profesor : profesores) {
	            boolean matches = true;

	            if (cupof != null && profesor.getCupof() != cupof) {
	                matches = false;
	            }
	            if (dni != null && profesor.getDni() != dni) {
	                matches = false;
	            }
	            if (fechaNacimiento != null && !fechaNacimiento.isEmpty() && !profesor.getFechaNacimiento().equals(fechaNacimiento)) {
	                matches = false;
	            }
	            if (nombre != null && !nombre.isEmpty() && !profesor.getNombre().equals(nombre)) {
	                matches = false;
	            }
	            if (sitRev != null && !sitRev.isEmpty() && !profesor.getSitRev().equals(sitRev)) {
	                matches = false;
	            }
	            if (modalidad != null && !modalidad.isEmpty() && !profesor.getModalidad().equals(modalidad)) {
	                matches = false;
	            }
	            if (materia != null && !materia.isEmpty() && !profesor.getMateria().equals(materia)) {
	                matches = false;
	            }
	            if (cantMod != null && profesor.getCantMod() != cantMod) {
	                matches = false;
	            }
	            if (anio != null && profesor.getAnio() != anio) {
	                matches = false;
	            }
	            if (division != null && profesor.getDivision() != division) {
	                matches = false;
	            }
	            if (turno != null && !turno.isEmpty() && !profesor.getTurno().equals(turno)) {
	                matches = false;
	            }
	            if (dia != null && !dia.isEmpty() && !Objects.equals(profesor.getDia(), dia)) {
	                matches = false;
	            }
	            if (horario != null && !horario.isEmpty() && !profesor.getHorario().equals(horario)) {
	                matches = false;
	            }

	            if (matches) {
	                filteredProfesores.add(profesor);
	            }
	        } 
	 
	        filteredProfesores.sort(Comparator.comparing(Profesor::getDni));
	        return filteredProfesores;
	    }
	
}
