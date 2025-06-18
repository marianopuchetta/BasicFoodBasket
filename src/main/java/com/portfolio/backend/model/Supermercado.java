package com.portfolio.backend.model;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Entity
public class Supermercado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	@Column(unique = true)
	private String slug;

	@OneToMany(mappedBy = "supermercado", cascade = CascadeType.ALL)
	@JsonIgnore
	private List<Producto> productos;

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

	public String getSlug() {
		return slug;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public List<Producto> getProductos() {
		return productos;
	}

	public void setProductos(List<Producto> productos) {
		this.productos = productos;
	}
}
