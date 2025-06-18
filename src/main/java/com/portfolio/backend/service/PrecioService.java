package com.portfolio.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portfolio.backend.model.Precio;
import com.portfolio.backend.model.Producto;
import com.portfolio.backend.model.Supermercado;
import com.portfolio.backend.repository.PrecioRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PrecioService implements IPrecioService {

    @Autowired
    private PrecioRepository precioRepository;

    @Override
    public void guardarPrecio(Precio precio) {
        precioRepository.save(precio);
    }

    @Override
    public Optional<Precio> obtenerPrecioPorProductoYFecha(Producto producto, LocalDate fecha) {
        return precioRepository.findByProductoAndFecha(producto, fecha);
    }

    @Override
    public List<Precio> obtenerPreciosPorFecha(LocalDate fecha) {
        return precioRepository.findByFecha(fecha);
    }

    @Override
    public List<Precio> obtenerHistorialDeProducto(Producto producto) {
        return precioRepository.findByProductoOrderByFechaDesc(producto);
    }

    // Implementación de nuevos métodos
    @Override
    public List<Precio> obtenerPreciosScrapeadosPorFecha(LocalDate fecha) {
        return precioRepository.findScrapeadosByFecha(fecha);
    }

    @Override
    public List<Precio> obtenerPreciosPorProductoYRangoFechas(Producto producto, LocalDate startDate, LocalDate endDate) {
        return precioRepository.findByProductoAndFechaBetween(producto, startDate, endDate);
    }

    @Override
    public Optional<Precio> obtenerUltimoPrecioScrapeadoPorProducto(Producto producto) {
        return precioRepository.findLastScrapeadoByProducto(producto);
    }

    @Override
    public List<Precio> obtenerPreciosPorSupermercadoYScrapeado(Supermercado supermercado, boolean scrapeado, LocalDate fecha) {
        return precioRepository.findBySupermercadoAndScrapeadoAndFecha(supermercado, scrapeado, fecha);
    }

    @Override
    public List<Precio> obtenerUltimosPreciosScrapeados() {
        return precioRepository.findScrapeadosByFecha(LocalDate.now());
    }
    @Override
    public Optional<Precio> findUltimoPrecioByProducto(Producto producto) {
        return precioRepository.findFirstByProductoOrderByFechaDesc(producto);
    }
}
/*
package com.portfolio.backend.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portfolio.backend.model.Precio;
import com.portfolio.backend.model.Producto;
import com.portfolio.backend.repository.PrecioRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PrecioService implements IPrecioService {

    @Autowired
    private PrecioRepository precioRepository;

    @Override
    public void guardarPrecio(Precio precio) {
        precioRepository.save(precio);
    }

    @Override
    public Optional<Precio> obtenerPrecioPorProductoYFecha(Producto producto, LocalDate fecha) {
        return precioRepository.findByProductoAndFecha(producto, fecha);
    }

    @Override
    public List<Precio> obtenerPreciosPorFecha(LocalDate fecha) {
        return precioRepository.findByFecha(fecha);
    }

    @Override
    public List<Precio> obtenerHistorialDeProducto(Producto producto) {
        return precioRepository.findByProductoOrderByFechaDesc(producto);
    }
}
*/