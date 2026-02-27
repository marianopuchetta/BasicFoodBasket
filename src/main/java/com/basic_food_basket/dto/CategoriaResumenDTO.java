package com.basic_food_basket.dto;

import java.util.List;
import java.util.Map;

public class CategoriaResumenDTO {
    private String nombre;
    private Double total;
    private Map<String, Double> subcategorias;
    private List<SubCategoriaDTO> subcategoriasDTO;
    private Double variacionDiaria;
    private Double variacionSemanal;
    private Double variacionMensual;
    private Double variacionAnual;

    public CategoriaResumenDTO() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Map<String, Double> getSubcategorias() { return subcategorias; }
    public void setSubcategorias(Map<String, Double> subcategorias) { this.subcategorias = subcategorias; }

    public List<SubCategoriaDTO> getSubcategoriasDTO() { return subcategoriasDTO; }
    public void setSubcategoriasDTO(List<SubCategoriaDTO> subcategoriasDTO) { this.subcategoriasDTO = subcategoriasDTO; }

    public Double getVariacionDiaria() { return variacionDiaria; }
    public void setVariacionDiaria(Double variacionDiaria) { this.variacionDiaria = variacionDiaria; }

    public Double getVariacionSemanal() { return variacionSemanal; }
    public void setVariacionSemanal(Double variacionSemanal) { this.variacionSemanal = variacionSemanal; }

    public Double getVariacionMensual() { return variacionMensual; }
    public void setVariacionMensual(Double variacionMensual) { this.variacionMensual = variacionMensual; }

    public Double getVariacionAnual() { return variacionAnual; }
    public void setVariacionAnual(Double variacionAnual) { this.variacionAnual = variacionAnual; }
}