package com.basic_food_basket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.basic_food_basket.model.Supermercado;
import com.basic_food_basket.service.ISupermercadoService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/supermercados")

public class SupermercadoController {

    @Autowired
    private ISupermercadoService iSupermercadoService;

	@ResponseBody
    public List<Supermercado> listarTodos() {
        return iSupermercadoService.listarTodos();
    }

    @PostMapping("/newsupermercado")
    public Supermercado crear(@RequestBody Supermercado supermercado) {
        return iSupermercadoService.guardar(supermercado);
    }

    @GetMapping("/{slug}")
    public Supermercado buscarPorSlug(@PathVariable String slug) {
        Optional<Supermercado> supermercado = iSupermercadoService.buscarPorSlug(slug);
        return supermercado.orElseThrow(() -> new RuntimeException("Supermercado no encontrado"));
    }
}

