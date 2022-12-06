package com.portfolio.backend.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
*@author Mariano Puchetta
*5 dic. 2022
*/

@Entity
public class Experiencia {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private long id;
	private String empresa;
	private String puesto;
	private String desde;
	private String hasta;
	
	public Experiencia() {
		
	}
	public Experiencia(String empresa,String puesto,String desde,String hasta) {
		this.empresa = empresa;
		this.puesto = puesto;
		this.desde = desde;
		this.hasta = hasta;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getEmpresa() {
		return empresa;
	}
	public void setEmpresa(String empresa) {
		this.empresa = empresa;
	}
	public String getPuesto() {
		return puesto;
	}
	public void setPuesto(String puesto) {
		this.puesto = puesto;
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
