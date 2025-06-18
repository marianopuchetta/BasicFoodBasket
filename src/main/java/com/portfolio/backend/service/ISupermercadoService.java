package com.portfolio.backend.service;



import java.util.List;
import java.util.Optional;

import com.portfolio.backend.model.Supermercado;

public interface ISupermercadoService {
    List<Supermercado> listarTodos();
    Optional<Supermercado> buscarPorSlug(String slug);
    Supermercado guardar(Supermercado supermercado);
    Optional<Supermercado> buscarPorId(Long id);

}
