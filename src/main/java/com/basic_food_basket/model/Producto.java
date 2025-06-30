package com.basic_food_basket.model;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Entity
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	private String url;

	@ManyToOne
	@JoinColumn(name = "supermercado_id")
	private Supermercado supermercado;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_canasta")
	private TipoCanasta tipoCanasta;

	@OneToMany(mappedBy = "producto", cascade = CascadeType.ALL)
	@JsonIgnore
	private List<Precio> precios;

	/*
	 * @Column(name = "tipo_canasta") private String tipoCanasta; // "CBA", "CPA",
	 * etc.
	 */

	// Getters y setters

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getUrl() {
		return url;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Supermercado getSupermercado() {
		return supermercado;
	}

	public void setSupermercado(Supermercado supermercado) {
		this.supermercado = supermercado;
	}

	public List<Precio> getPrecios() {
		return precios;
	}

	public void setPrecios(List<Precio> precios) {
		this.precios = precios;
	}

	/*
	 * public String getTipoCanasta() { return tipoCanasta; }
	 * 
	 * public void setTipoCanasta(String tipoCanasta) { this.tipoCanasta =
	 * tipoCanasta; }
	 */

	public TipoCanasta getTipoCanasta() {
		return tipoCanasta;
	}

	public void setTipoCanasta(TipoCanasta tipoCanasta) {
		this.tipoCanasta = tipoCanasta;
	}
}
