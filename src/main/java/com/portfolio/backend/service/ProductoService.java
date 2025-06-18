package com.portfolio.backend.service;


import com.portfolio.backend.model.Producto;
import com.portfolio.backend.model.Supermercado;
import com.portfolio.backend.model.TipoCanasta;
import com.portfolio.backend.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService implements IProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    @Override
    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    @Override
    public List<Producto> listarPorSupermercado(Supermercado supermercado) {
        return productoRepository.findBySupermercado(supermercado);
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }
    
    @Override
    public List<Producto> listarPorSupermercadoYTipo(Supermercado supermercado, TipoCanasta tipoCanasta) {
        return productoRepository.findBySupermercadoAndTipoCanasta(supermercado, tipoCanasta);
    }
        
    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }
    
    public List<Producto> obtenerProductosNoScrapeadosEnUltimoScrap() {
        return productoRepository.findProductosNoScrapeadosEnUltimoScrap();
    }
    
}

