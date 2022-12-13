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

import com.portfolio.backend.model.Skill;
import com.portfolio.backend.service.ISkillService;

@CrossOrigin(origins = "*")
@RestController
public class SkillController {
	
	@Autowired
	ISkillService iSkill;
	
	@GetMapping("/skills")
	@ResponseBody
	public List<Skill>getSkills(){
		return iSkill.getAllSkills();
	}
	
	@GetMapping("/skill/{id}")
	public Skill getSkill(@PathVariable Long id){
		return iSkill.getSkill(id);
	}
	
	@PostMapping("/newskill")
	public ResponseEntity<Skill>addSkill(@RequestBody Skill skill){
		iSkill.newSkill(skill);
		return new ResponseEntity<Skill>(skill,HttpStatus.OK);	
	}
	
	@DeleteMapping("/deleteskill/{id}")
	public void deleteSkill(@PathVariable Long id) {
		iSkill.deleteSkill(id);
	}
	
	@PutMapping("/editskill/{id}")
	public ResponseEntity<Skill>editSkill(@PathVariable Long id,
											@RequestParam("nombre") String newNombre,
											@RequestParam("nivel") String newNivel){
						 Skill skill_to_edit = iSkill.getSkill(id);
						 skill_to_edit.setNombre(newNombre);
						 skill_to_edit.setNivel(newNivel);
						 
						 iSkill.newSkill(skill_to_edit);
												
												
		return new ResponseEntity<Skill>(skill_to_edit,HttpStatus.OK);
	}
	

}
