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
}

