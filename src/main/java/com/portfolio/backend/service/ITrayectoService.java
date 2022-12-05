package com.portfolio.backend.service;

import java.util.List;

import com.portfolio.backend.model.Trayecto;


/**
*@author Mariano Puchetta
*22 nov. 2022
*/
public interface ITrayectoService {

	public List<Trayecto>getAllTrayectos();
	public Trayecto getTrayecto(Long id);
	public void newTrayecto(Trayecto trayecto);
	public void deleteTrayecto(Long id);
}