package com.basic_food_basket.service;



import java.util.List;
import java.util.Optional;

import com.basic_food_basket.model.Supermercado;

public interface ISupermercadoService {
    List<Supermercado> listarTodos();
    Optional<Supermercado> buscarPorSlug(String slug);
    Supermercado guardar(Supermercado supermercado);
    Optional<Supermercado> buscarPorId(Long id);

}
