package com.basic_food_basket.dto;

import java.util.List;

public class CanastaCategoriasResumenDTO {
    private String fecha;
    private List<CategoriaResumenDTO> promedioXCategorias;
    private List<SupermercadoResumenDTO> supermercados;

    public CanastaCategoriasResumenDTO() {}

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public List<CategoriaResumenDTO> getPromedioXCategorias() { return promedioXCategorias; }
    public void setPromedioXCategorias(List<CategoriaResumenDTO> promedioXCategorias) { 
        this.promedioXCategorias = promedioXCategorias; 
    }

    public List<SupermercadoResumenDTO> getSupermercados() { return supermercados; }
    public void setSupermercados(List<SupermercadoResumenDTO> supermercados) { 
        this.supermercados = supermercados; 
    }
}
