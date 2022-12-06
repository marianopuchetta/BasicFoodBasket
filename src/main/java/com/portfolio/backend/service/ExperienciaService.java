package com.portfolio.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portfolio.backend.model.Experiencia;
import com.portfolio.backend.repository.ExperienciaRepository;

/**
 * @author Mariano Puchetta 5 dic. 2022
 */
@Service
public class ExperienciaService implements IExperienciaService {

	@Autowired
	private ExperienciaRepository experienciaRepository;

	@Override
	public List<Experiencia> getAllExperiencias() {
		return experienciaRepository.findAll();
	}

	@Override
	public Experiencia getExperiencia(Long id) {
		return experienciaRepository.findById(id).orElse(null);
	}

	@Override
	public void newExperiencia(Experiencia experiencia) {
		experienciaRepository.save(experiencia);
	}

	@Override
	public void deleteExperiencia(Long id) {
		experienciaRepository.deleteById(id);
	}



}
