package com.basic_food_basket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.basic_food_basket.model.Producto;
import com.basic_food_basket.model.Supermercado;
import com.basic_food_basket.model.TipoCanasta;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findBySupermercado(Supermercado supermercado);
    List<Producto> findBySupermercadoAndTipoCanasta(Supermercado supermercado, TipoCanasta tipoCanasta);
    @Query("SELECT DISTINCT p FROM Producto p " +
            "WHERE NOT EXISTS (" +
            "   SELECT pr FROM Precio pr " +
            "   WHERE pr.producto = p " +
            "   AND pr.scrapeado = true " +
            "   AND pr.fecha = (SELECT MAX(pr2.fecha) FROM Precio pr2 WHERE pr2.scrapeado = true)" +
            ")")
     List<Producto> findProductosNoScrapeadosEnUltimoScrap();
}
