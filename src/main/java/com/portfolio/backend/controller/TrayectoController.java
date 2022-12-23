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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.backend.model.Trayecto;
import com.portfolio.backend.service.ITrayectoService;



/**
*@author Mariano Puchetta
*23 nov. 2022
*/
@CrossOrigin(origins = "https://marianopuchetta-protfolio.web.app/", exposedHeaders = "token")
@RestController
public class TrayectoController {

@Autowired
private ITrayectoService iTrayecto;


	@GetMapping("/trayectos")
	@ResponseBody
	public List <Trayecto> AllTrayectos() {
		return iTrayecto.getAllTrayectos();
	}
	
	@GetMapping("/trayecto/{id}")
	public Trayecto getTrayecto(@PathVariable Long id) {
		return iTrayecto.getTrayecto(id);
	}
	
	@PostMapping("/newtrayecto")
	public ResponseEntity<Trayecto> addTrayecto(@RequestBody Trayecto trayecto) {
		iTrayecto.newTrayecto(trayecto);
		return new ResponseEntity<Trayecto>(trayecto, HttpStatus.OK);

	}
	
	@DeleteMapping("/deletetrayecto/{id}")
	public void deleteTrayecto(@PathVariable Long id) {
		iTrayecto.deleteTrayecto(id);
	}
	
	@PutMapping("edittrayecto/{id}")
	public ResponseEntity<Trayecto> editTrayecto(@PathVariable Long id,
							@RequestBody Trayecto trayecto) {
		
		Trayecto trayectoToEdit = iTrayecto.getTrayecto(id);
		
		trayectoToEdit.setInstitucion(trayecto.getInstitucion());
		trayectoToEdit.setTitulo(trayecto.getTitulo());
		trayectoToEdit.setDesde(trayecto.getDesde());
		trayectoToEdit.setHasta(trayecto.getHasta());
		
		iTrayecto.newTrayecto(trayectoToEdit);
		
		return new ResponseEntity<Trayecto>(trayectoToEdit,HttpStatus.OK);
	}
}


