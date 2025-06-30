package com.basic_food_basket.service;


import java.util.List;
import java.util.Optional;

import com.basic_food_basket.model.Producto;
import com.basic_food_basket.model.Supermercado;
import com.basic_food_basket.model.TipoCanasta;

public interface IProductoService {
    Producto guardar(Producto producto);
    List<Producto> listarTodos();
    List<Producto> listarPorSupermercado(Supermercado supermercado);
    Optional<Producto> buscarPorId(Long id);
    List<Producto> listarPorSupermercadoYTipo(Supermercado supermercado, TipoCanasta tipoCanasta);
}
