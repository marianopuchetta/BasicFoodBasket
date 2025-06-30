package com.basic_food_basket.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.basic_food_basket.model.Precio;
import com.basic_food_basket.model.Producto;
import com.basic_food_basket.model.Supermercado;

public interface IPrecioService {
    void guardarPrecio(Precio precio);
    Optional<Precio> obtenerPrecioPorProductoYFecha(Producto producto, LocalDate fecha);
    List<Precio> obtenerPreciosPorFecha(LocalDate fecha);
    List<Precio> obtenerHistorialDeProducto(Producto producto);
    
    // Nuevos métodos
    List<Precio> obtenerPreciosScrapeadosPorFecha(LocalDate fecha);
    List<Precio> obtenerPreciosPorProductoYRangoFechas(Producto producto, LocalDate startDate, LocalDate endDate);
    Optional<Precio> obtenerUltimoPrecioScrapeadoPorProducto(Producto producto);
    List<Precio> obtenerPreciosPorSupermercadoYScrapeado(Supermercado supermercado, boolean scrapeado, LocalDate fecha);
    List<Precio> obtenerUltimosPreciosScrapeados();
    Optional<Precio> findUltimoPrecioByProducto(Producto producto);
}
/*
package com.portfolio.backend.service;



import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.portfolio.backend.model.Precio;
import com.portfolio.backend.model.Producto;

public interface IPrecioService {

    void guardarPrecio(Precio precio);

    Optional<Precio> obtenerPrecioPorProductoYFecha(Producto producto, LocalDate fecha);

    List<Precio> obtenerPreciosPorFecha(LocalDate fecha);

    List<Precio> obtenerHistorialDeProducto(Producto producto);
}
*/