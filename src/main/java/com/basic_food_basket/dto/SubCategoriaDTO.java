package com.basic_food_basket.dto;

public class SubCategoriaDTO {
    private String nombre;
    private Double total;
    private Double variacionDiaria;
    private Double variacionSemanal;
    private Double variacionMensual;
    private Double variacionAnual;

    public SubCategoriaDTO() {}

    public SubCategoriaDTO(String nombre, Double total, Double vd, Double vs, Double vm, Double va) {
        this.nombre = nombre;
        this.total = total;
        this.variacionDiaria = vd;
        this.variacionSemanal = vs;
        this.variacionMensual = vm;
        this.variacionAnual = va;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Double getVariacionDiaria() { return variacionDiaria; }
    public void setVariacionDiaria(Double variacionDiaria) { this.variacionDiaria = variacionDiaria; }

    public Double getVariacionSemanal() { return variacionSemanal; }
    public void setVariacionSemanal(Double variacionSemanal) { this.variacionSemanal = variacionSemanal; }

    public Double getVariacionMensual() { return variacionMensual; }
    public void setVariacionMensual(Double variacionMensual) { this.variacionMensual = variacionMensual; }

    public Double getVariacionAnual() { return variacionAnual; }
    public void setVariacionAnual(Double variacionAnual) { this.variacionAnual = variacionAnual; }
}