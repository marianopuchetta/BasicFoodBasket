package com.portfolio.backend.controller;



import com.portfolio.backend.model.Producto;
import com.portfolio.backend.model.Supermercado;
import com.portfolio.backend.service.IProductoService;
import com.portfolio.backend.service.ISupermercadoService;
import com.portfolio.backend.service.ProductoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private IProductoService iproductoService;

    @Autowired
    private ISupermercadoService supermercadoService;
    
    @Autowired
    private final ProductoService productoService;

    
    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }
    
    @GetMapping("/no-scrapeados-ultimo")
    public ResponseEntity<List<Producto>> getProductosNoScrapeadosEnUltimoScrap() {
        List<Producto> productos = productoService.obtenerProductosNoScrapeadosEnUltimoScrap();
        return ResponseEntity.ok(productos);
    }

    @PostMapping("/newproducto")
    public Producto crearProducto(@RequestBody Producto producto) {
        if (producto.getSupermercado() == null || producto.getSupermercado().getId() == null) {
            throw new RuntimeException("Debe especificarse el ID del supermercado en el objeto 'supermercado'");
        }

        Long supermercadoId = producto.getSupermercado().getId();

        Optional<Supermercado> supermercadoOpt = supermercadoService.buscarPorId(supermercadoId); // ← usa ID, no slug
        if (!supermercadoOpt.isPresent()) {
            throw new RuntimeException("Supermercado con ID " + supermercadoId + " no existe.");
        }

        producto.setSupermercado(supermercadoOpt.get());

        return iproductoService.guardar(producto);
    }

    
	@GetMapping("/productos")
	@ResponseBody
    public List<Producto> listarTodos() {
        return iproductoService.listarTodos();
    }

    @GetMapping("/supermercado/{slug}")
    public List<Producto> listarPorSupermercado(@PathVariable String slug) {
        Optional<Supermercado> supermercado = supermercadoService.buscarPorSlug(slug);
        if (!supermercado.isPresent()) {
            throw new RuntimeException("Supermercado no encontrado");
        }
        return iproductoService.listarPorSupermercado(supermercado.get());
    }
}
