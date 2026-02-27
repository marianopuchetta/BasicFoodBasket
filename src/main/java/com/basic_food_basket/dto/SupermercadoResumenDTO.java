package com.basic_food_basket.dto;

import java.util.List;

public class SupermercadoResumenDTO {
    private String nombre;
    private Double total;
    private List<CategoriaResumenDTO> categorias;

    public SupermercadoResumenDTO() {}

    public SupermercadoResumenDTO(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public List<CategoriaResumenDTO> getCategorias() { return categorias; }
    public void setCategorias(List<CategoriaResumenDTO> categorias) { this.categorias = categorias; }
}