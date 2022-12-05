package com.portfolio.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portfolio.backend.model.Trayecto;
import com.portfolio.backend.repository.TrayectoRepository;


/**
 * @author Mariano Puchetta 22 nov. 2022
 */
@Service
public class TrayectoService implements ITrayectoService {
	@Autowired
	private TrayectoRepository trayectoRepository;

	@Override
	public List<Trayecto> getAllTrayectos() {
		return trayectoRepository.findAll();
	}

	@Override
	public void newTrayecto(Trayecto trayecto) {
		trayectoRepository.save(trayecto);
	}

	@Override
	public void deleteTrayecto(Long id) {
		trayectoRepository.deleteById(id);
	}


	@Override
	public Trayecto getTrayecto(Long id) {
		return trayectoRepository.findById(id).orElse(null);
	}



}
