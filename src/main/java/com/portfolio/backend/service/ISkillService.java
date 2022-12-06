package com.portfolio.backend.service;

import java.util.List;

import com.portfolio.backend.model.Skill;

public interface ISkillService {

	public List<Skill>getAllSkills();
	public Skill getSkill(Long id);
	public void newSkill(Skill skill);
	public void deleteSkill(Long id);
}
