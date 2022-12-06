package com.portfolio.backend.service;

import java.util.List;

import com.portfolio.backend.model.Experiencia;

/**
*@author Mariano Puchetta
*5 dic. 2022
*/
public interface IExperienciaService {

	public List<Experiencia>getAllExperiencias();
	public Experiencia getExperiencia(Long id);
	public void newExperiencia(Experiencia experiencia);
	public void deleteExperiencia(Long id);
}
