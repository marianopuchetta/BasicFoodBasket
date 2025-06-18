/*package com.portfolio.backend.controller;


import com.portfolio.backend.scrapper.LaAnonimaScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200/", maxAge = 3600)

@RestController
@RequestMapping("/scrap")
public class ScraperController {

    @Autowired
    private LaAnonimaScraperService scraper;

    @GetMapping("/la-anonima")
    public String ejecutarScraper() {
        scraper.scrapPrecios();
        return "Scraping completado.";
    }
}*/

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
}

