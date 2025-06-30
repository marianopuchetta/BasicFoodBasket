package com.basic_food_basket.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.basic_food_basket.model.Supermercado;
import com.basic_food_basket.repository.SupermercadoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class SupermercadoService implements ISupermercadoService {

    @Autowired
    private SupermercadoRepository supermercadoRepository;

    @Override
    public List<Supermercado> listarTodos() {
        return supermercadoRepository.findAll();
    }

    @Override
    public Optional<Supermercado> buscarPorSlug(String slug) {
        return supermercadoRepository.findBySlug(slug);
    }

    @Override
    public Supermercado guardar(Supermercado supermercado) {
        return supermercadoRepository.save(supermercado);
    }
    
    @Override
    public Optional<Supermercado> buscarPorId(Long id) {
        return supermercadoRepository.findById(id);
    }

}

