/*
package com.portfolio.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.portfolio.backend.service.ICanastaService;

import java.util.Map;

@RestController
@RequestMapping("/canasta")
public class CanastaController {

    @Autowired
    private ICanastaService canastaService;

    @GetMapping
    public Map<String, Object> obtenerResumenCanasta() {
        return canastaService.obtenerResumenCanasta();
    }
}*/
package com.portfolio.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import com.portfolio.backend.service.ICanastaService;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/canasta")
public class CanastaController {

    @Autowired
    private ICanastaService canastaService;

    // Resumen por supermercado (último scrap, promedios, variaciones, etc)
    @GetMapping("/resumen")
    public Map<String, Object> obtenerResumenCanasta() {
        return canastaService.obtenerResumenCanasta();
    }

    // Historial de cada canasta (CBA y CPA) por supermercado (todo el historial)
    @GetMapping("/historial")
    public Map<String, Object> obtenerHistorialCanasta() {
        return canastaService.obtenerHistorialCanasta();
    }

    // Historial de cada canasta (CBA y CPA) por supermercado, filtrado por fechas (opcional)
    @GetMapping(value = "/historial/filtrado")
    public Map<String, Object> obtenerHistorialCanastaFiltrado(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return canastaService.obtenerHistorialCanasta(desde, hasta);
    }

    // Resumen general (sumando/promediando todos los supermercados juntos)
    @GetMapping("/resumen/general")
    public Map<String, Object> obtenerResumenGeneral() {
        return canastaService.obtenerResumenGeneral();
    }

    // Historial general (sumando/promediando todos los supermercados juntos)
    @GetMapping("/historial/general")
    public Map<String, Object> obtenerHistorialGeneral() {
        return canastaService.obtenerHistorialGeneral();
    }

    // Historial general (sumando/promediando todos los supermercados juntos), filtrado por fechas (opcional)
    @GetMapping(value = "/historial/general/filtrado")
    public Map<String, Object> obtenerHistorialGeneralFiltrado(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return canastaService.obtenerHistorialGeneral(desde, hasta);
    }
}