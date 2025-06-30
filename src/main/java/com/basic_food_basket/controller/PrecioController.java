package com.basic_food_basket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.basic_food_basket.model.Precio;
import com.basic_food_basket.model.Producto;
import com.basic_food_basket.model.Supermercado;
import com.basic_food_basket.repository.ProductoRepository;
import com.basic_food_basket.repository.SupermercadoRepository;
import com.basic_food_basket.service.IPrecioService;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/precios")
public class PrecioController {

    @Autowired
    private SupermercadoRepository supermercadoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private IPrecioService precioService;

    @GetMapping("/{slug}")
    public Map<String, Object> obtenerPreciosActuales(@PathVariable String slug) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        Optional<Supermercado> optionalSuper = supermercadoRepository.findBySlug(slug);
        if (!optionalSuper.isPresent()) {
            respuesta.put("error", "Supermercado no encontrado");
            return respuesta;
        }

        Supermercado supermercado = optionalSuper.get();
        List<Producto> productos = productoRepository.findBySupermercado(supermercado);

        LocalDate hoy = LocalDate.now();
        Map<String, Double> precios = new LinkedHashMap<>();

        for (Producto producto : productos) {
            precioService.obtenerPrecioPorProductoYFecha(producto, hoy)
                    .ifPresent(precio -> precios.put(producto.getNombre(), precio.getValor()));
        }

        respuesta.put("supermercado", supermercado.getNombre());
        respuesta.put("fecha", hoy.toString());
        respuesta.put("precios", precios);

        return respuesta;
    }

    // Nuevos endpoints
    @GetMapping("/scrapeados")
    public List<Precio> obtenerPreciosScrapeados(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return precioService.obtenerPreciosScrapeadosPorFecha(fecha != null ? fecha : LocalDate.now());
    }
    
    

    @GetMapping("/historial")
    public List<Precio> obtenerHistorialPrecios(
            @RequestParam Long productoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        Producto producto = new Producto();
        producto.setId(productoId);
        return precioService.obtenerPreciosPorProductoYRangoFechas(producto, inicio, fin);
    }

    @GetMapping("/ultimo-scrapeado/{productoId}")
    public ResponseEntity<Precio> obtenerUltimoPrecioScrapeado(@PathVariable Long productoId) {
        Producto producto = new Producto();
        producto.setId(productoId);
        Optional<Precio> precio = precioService.obtenerUltimoPrecioScrapeadoPorProducto(producto);
        return precio.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/supermercado/{supermercadoId}/scrapeado")
    public List<Precio> obtenerPreciosPorSupermercadoYScrapeado(
            @PathVariable Long supermercadoId,
            @RequestParam boolean scrapeado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Supermercado supermercado = new Supermercado();
        supermercado.setId(supermercadoId);
        return precioService.obtenerPreciosPorSupermercadoYScrapeado(
            supermercado, 
            scrapeado, 
            fecha != null ? fecha : LocalDate.now()
        );
    }
}
/*
package com.portfolio.backend.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.portfolio.backend.model.Producto;
import com.portfolio.backend.model.Supermercado;
import com.portfolio.backend.repository.ProductoRepository;
import com.portfolio.backend.repository.SupermercadoRepository;
import com.portfolio.backend.service.IPrecioService;
import com.portfolio.backend.service.ISupermercadoService;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/precios")

public class PrecioController {

    @Autowired
    private SupermercadoRepository supermercadoRepository;
    @Autowired
    private ISupermercadoService iSupermercadoService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private IPrecioService precioService;

    @GetMapping("/{slug}")
    public Map<String, Object> obtenerPreciosActuales(@PathVariable String slug) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        Optional<Supermercado> optionalSuper = supermercadoRepository.findBySlug(slug);
        if (!optionalSuper.isPresent()) {
            respuesta.put("error", "Supermercado no encontrado");
            return respuesta;
        }

        Supermercado supermercado = optionalSuper.get();
        List<Producto> productos = productoRepository.findBySupermercado(supermercado);

        LocalDate hoy = LocalDate.now();
        Map<String, Double> precios = new LinkedHashMap<>();

        for (Producto producto : productos) {
            precioService.obtenerPrecioPorProductoYFecha(producto, hoy)
                    .ifPresent(precio -> precios.put(producto.getNombre(), precio.getValor()));
        }

        respuesta.put("supermercado", supermercado.getNombre());
        respuesta.put("fecha", hoy.toString());
        respuesta.put("precios", precios);

        return respuesta;
    }
}

*/