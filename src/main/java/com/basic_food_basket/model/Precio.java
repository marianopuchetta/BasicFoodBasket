package com.basic_food_basket.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
public class Precio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;

    private Double valor;

    @Column(name = "scrapeado")
    private boolean scrapeado;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    // Getters y setters

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public boolean isScrapeado() {
        return scrapeado;
    }

    public void setScrapeado(boolean scrapeado) {
        this.scrapeado = scrapeado;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}
