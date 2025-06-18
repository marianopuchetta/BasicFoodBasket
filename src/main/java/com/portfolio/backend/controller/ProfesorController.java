package com.portfolio.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.backend.model.Profesor;
import com.portfolio.backend.service.IProfesorService;

/**
 * @author Mariano Puchetta 28 may. 2024
 */

//@CrossOrigin(origins = "https://marianopuchetta-protfolio.web.app/", maxAge = 3600)
@CrossOrigin(origins = "http://localhost:4200/", maxAge = 3600)
@RestController
public class ProfesorController {

	@Autowired
	IProfesorService iProfesorService;

	@GetMapping("/profesores")
	@ResponseBody
	public List<Profesor> getAllProfesores() {
		return iProfesorService.getAllProfesores();
	}

	@GetMapping("/profesor/{id}")
	public Profesor getProfesor(@PathVariable Long id) {
		System.out.print("controller");

		return iProfesorService.getProfesor(id);
	}


	@PostMapping("/newprofesor")
	public ResponseEntity<Profesor> addProfesor(@RequestBody Profesor profesor) {
		iProfesorService.newProfesor(profesor);
		return new ResponseEntity<Profesor>(profesor, HttpStatus.OK);
	}

	@DeleteMapping("/deleteprofesor/{id}")
	public void deleteProfesor(@PathVariable Long id, @RequestBody Profesor profesor) {
		iProfesorService.deleteProfesor(id);
	}

	@PutMapping("/editprofesor/{id}")
	public ResponseEntity<Profesor> editProfesor(@PathVariable Long id, @RequestBody Profesor profesor) {

		Profesor profesor_to_edit = iProfesorService.getProfesor(id);

		profesor_to_edit.setCupof(profesor.getCupof());
		profesor_to_edit.setDni(profesor.getDni());
		profesor_to_edit.setFechaNacimiento(profesor.getFechaNacimiento());
		profesor_to_edit.setNombre(profesor.getNombre());
		profesor_to_edit.setSitRev(profesor.getSitRev());
		profesor_to_edit.setModalidad(profesor.getModalidad());
		profesor_to_edit.setMateria(profesor.getMateria());
		profesor_to_edit.setCantMod(profesor.getCantMod());
		profesor_to_edit.setAnio(profesor.getAnio());
		profesor_to_edit.setDivision(profesor.getDivision());
		profesor_to_edit.setTurno(profesor.getTurno());
		profesor_to_edit.setDia(profesor.getDia());
		profesor_to_edit.setHorario(profesor.getHorario());

		iProfesorService.newProfesor(profesor_to_edit);

		return new ResponseEntity<Profesor>(profesor_to_edit, HttpStatus.OK);

	}
	
	  @GetMapping("/profesores/search")
	    @ResponseBody
	    public List<Profesor> findProfesoresByFilters(
	            @RequestParam(required = false) Integer cupof,
	            @RequestParam(required = false) Integer dni,
	            @RequestParam(required = false) String fechaNacimiento,
	            @RequestParam(required = false) String nombre,
	            @RequestParam(required = false) String sitRev,
	            @RequestParam(required = false) String modalidad,
	            @RequestParam(required = false) String materia,
	            @RequestParam(required = false) Integer cantMod,
	            @RequestParam(required = false) Integer anio,
	            @RequestParam(required = false) Integer division,
	            @RequestParam(required = false) String turno,
	            @RequestParam(required = false) String dia,
	            @RequestParam(required = false) String horario) {
	        return iProfesorService.findProfesoresByFilters(cupof, dni, fechaNacimiento, nombre, sitRev, modalidad, materia, cantMod, anio, division, turno, dia, horario);
	    }

}
