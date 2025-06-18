package com.portfolio.backend.service;


import com.portfolio.backend.model.Producto;
import com.portfolio.backend.model.Supermercado;
import com.portfolio.backend.model.TipoCanasta;

import java.util.List;
import java.util.Optional;

public interface IProductoService {
    Producto guardar(Producto producto);
    List<Producto> listarTodos();
    List<Producto> listarPorSupermercado(Supermercado supermercado);
    Optional<Producto> buscarPorId(Long id);
    List<Producto> listarPorSupermercadoYTipo(Supermercado supermercado, TipoCanasta tipoCanasta);
}
