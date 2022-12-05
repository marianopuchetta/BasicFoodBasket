package com.portfolio.backend.model;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

/**
*@author Mariano Puchetta
*22 nov. 2022
*/

@Getter @Setter
@Entity
public class Trayecto {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private long id;
	
	private String institucion;
	private String titulo;
	private String desde;
	private String hasta;
	
	public Trayecto() {
		
	}
	
	public Trayecto(String institucion,String titulo,
						String desde, String hasta) {
		this.institucion = institucion;
		this.titulo = titulo;
		this.desde = desde;
		this.hasta = hasta;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getInstitucion() {
		return institucion;
	}

	public void setInstitucion(String institucion) {
		this.institucion = institucion;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getDesde() {
		return desde;
	}

	public void setDesde(String desde) {
		this.desde = desde;
	}

	public String getHasta() {
		return hasta;
	}

	public void setHasta(String hasta) {
		this.hasta = hasta;
	}
	
	
	
}

