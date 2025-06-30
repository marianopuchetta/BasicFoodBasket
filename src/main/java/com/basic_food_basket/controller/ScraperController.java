/*

package com.portfolio.backend.controller;

import com.portfolio.backend.model.Supermercado;
import com.portfolio.backend.repository.SupermercadoRepository;
import com.portfolio.backend.scrapper.IScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200/", maxAge = 3600)
@RestController
@RequestMapping("/scrap")
public class ScraperController {

    @Autowired
    private IScraperService scraperService;

    @Autowired
    private SupermercadoRepository supermercadoRepository;

    @GetMapping("/{supermarketSlug}")
    public String ejecutarScraper(@PathVariable String supermarketSlug) {
        Supermercado supermercado = supermercadoRepository.findBySlug(supermarketSlug)
            .orElseThrow(() -> new RuntimeException("Supermercado no encontrado"));
        
        scraperService.scrapPrecios(supermercado);
        return "Scraping completado para " + supermercado.getNombre();
    }
}*/
package com.basic_food_basket.controller;

import com.basic_food_basket.model.Supermercado;
import com.basic_food_basket.repository.SupermercadoRepository;
import com.basic_food_basket.scrapper.IScraperService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200/", maxAge = 3600)
@RestController
@RequestMapping("/scrap")
public class ScraperController {

    @Autowired
    private IScraperService scraperService;

    @Autowired
    private SupermercadoRepository supermercadoRepository;

    @GetMapping("/{supermarketSlug}")
    public String ejecutarScraper(@PathVariable String supermarketSlug) {
        Supermercado supermercado = supermercadoRepository.findBySlug(supermarketSlug)
            .orElseThrow(() -> new RuntimeException("Supermercado no encontrado"));
        
        scraperService.scrapPrecios(supermercado);
        return "Scraping completado para " + supermercado.getNombre();
    }

    @GetMapping("/all")
    public String ejecutarScraperTodos() {
        List<Supermercado> supermercados = supermercadoRepository.findAll();
        
        if (supermercados.isEmpty()) {
            return "No hay supermercados configurados";
        }

        for (Supermercado supermercado : supermercados) {
            try {
                scraperService.scrapPrecios(supermercado);
            } catch (Exception e) {
                System.err.println("Error al scrapear " + supermercado.getNombre() + ": " + e.getMessage());
            }
        }
        
        return "Scraping iniciado para todos los supermercados (" + supermercados.size() + ")";
    }
}

