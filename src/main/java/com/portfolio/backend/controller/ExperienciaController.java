package com.portfolio.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.backend.model.Experiencia;
import com.portfolio.backend.service.IExperienciaService;

/**
*@author Mariano Puchetta
*5 dic. 2022
*/

@RestController
public class ExperienciaController {
	
	@Autowired
	private IExperienciaService iExperiencia;

	@GetMapping("/experiencias")
	@ResponseBody
	public List<Experiencia>getAllExperiencias(){
		return iExperiencia.getAllExperiencias();
	}
	
	@GetMapping("/experiencia/{id}")
	public Experiencia getExperiencia(@PathVariable Long id) {
		return iExperiencia.getExperiencia(id);
	}
	
	@PostMapping("/newexperiencia")
	public ResponseEntity<Experiencia> addExperiencia(@RequestBody Experiencia experiencia){
		iExperiencia.newExperiencia(experiencia);
		return new ResponseEntity<Experiencia>(experiencia,HttpStatus.OK);
	}
	
	@DeleteMapping("/deleteexperiencia/{id}")
	public void deleteExperiencia(@PathVariable Long id) {
		 iExperiencia.deleteExperiencia(id);
	}
	
	@PutMapping("/editexperiencia/{id}")
	public ResponseEntity<Experiencia> editExperiencia(@PathVariable Long id,
											@RequestParam("empresa") String new_empresa,
											@RequestParam("puesto") String new_puesto,
											@RequestParam("desde") String new_desde,
											@RequestParam("hasta") String new_hasta)	{
		Experiencia experiencia_to_edit = iExperiencia.getExperiencia(id);
		
		experiencia_to_edit.setEmpresa(new_empresa);
		experiencia_to_edit.setPuesto(new_puesto);
		experiencia_to_edit.setDesde(new_desde);
		experiencia_to_edit.setHasta(new_hasta);
		
		iExperiencia.newExperiencia(experiencia_to_edit);
		
		return new ResponseEntity<Experiencia>(experiencia_to_edit,HttpStatus.OK);
	}
												
	
}
