package com.portfolio.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.portfolio.backend.model.Precio;
import com.portfolio.backend.model.Producto;
import com.portfolio.backend.model.Supermercado;
import com.portfolio.backend.model.TipoCanasta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrecioRepository extends JpaRepository<Precio, Long> {
    
    // Consulta básica por producto y fecha
    Optional<Precio> findByProductoAndFecha(Producto producto, LocalDate fecha);
    
    // Histórico de precios por producto ordenado por fecha
    List<Precio> findByProductoOrderByFechaDesc(Producto producto);
    
    // Consulta optimizada que carga las relaciones necesarias con todos los campos de precio
    @Query("SELECT p FROM Precio p JOIN FETCH p.producto pr JOIN FETCH pr.supermercado WHERE p.fecha = :fecha")
    List<Precio> findByFechaWithRelations(LocalDate fecha);
    
    // Consulta para obtener precios por supermercado y tipo de canasta con campos específicos
    @Query("SELECT p FROM Precio p JOIN FETCH p.producto pr JOIN FETCH pr.supermercado " +
           "WHERE p.fecha = :fecha AND pr.supermercado.id = :supermercadoId AND pr.tipoCanasta = :tipoCanasta")
    List<Precio> findByFechaAndSupermercadoAndTipoCanasta(
        LocalDate fecha, 
        Long supermercadoId, 
        TipoCanasta tipoCanasta);
    
    // Consulta básica por fecha
    List<Precio> findByFecha(LocalDate fecha);
    
    // Nueva consulta: Precios scrapeados por fecha
    @Query("SELECT p FROM Precio p WHERE p.fecha = :fecha AND p.scrapeado = true")
    List<Precio> findScrapeadosByFecha(LocalDate fecha);
    
    // Nueva consulta: Precios por rango de fechas y producto
    @Query("SELECT p FROM Precio p WHERE p.producto = :producto AND p.fecha BETWEEN :startDate AND :endDate ORDER BY p.fecha DESC")
    List<Precio> findByProductoAndFechaBetween(
        Producto producto, 
        LocalDate startDate, 
        LocalDate endDate);
    
    @Query(value = "SELECT * FROM precio WHERE producto_id = :productoId AND scrapeado = true ORDER BY fecha DESC LIMIT 1", 
    	       nativeQuery = true)
    	Optional<Precio> findLastScrapeadoByProducto(@Param("productoId") Producto producto);
    
    // Nueva consulta: Precios por supermercado y scrapeado
    @Query("SELECT p FROM Precio p JOIN p.producto pr WHERE pr.supermercado = :supermercado AND p.scrapeado = :scrapeado AND p.fecha = :fecha")
    List<Precio> findBySupermercadoAndScrapeadoAndFecha(
        Supermercado supermercado, 
        boolean scrapeado, 
        LocalDate fecha);
    
    Optional<Precio> findFirstByProductoOrderByFechaDesc(Producto producto);
}
/*
package com.portfolio.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.portfolio.backend.model.Precio;
import com.portfolio.backend.model.Producto;
import com.portfolio.backend.model.TipoCanasta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrecioRepository extends JpaRepository<Precio, Long> {
    Optional<Precio> findByProductoAndFecha(Producto producto, LocalDate fecha);
    List<Precio> findByProductoOrderByFechaDesc(Producto producto);
    // Consulta optimizada que carga las relaciones necesarias
    @Query("SELECT p FROM Precio p JOIN FETCH p.producto pr JOIN FETCH pr.supermercado WHERE p.fecha = :fecha")
    List<Precio> findByFechaWithRelations(LocalDate fecha);
    
    // Consulta para obtener precios por supermercado y tipo de canasta
    @Query("SELECT p FROM Precio p JOIN p.producto pr WHERE p.fecha = :fecha AND pr.supermercado.id = :supermercadoId AND pr.tipoCanasta = :tipoCanasta")
    List<Precio> findByFechaAndSupermercadoAndTipoCanasta(
        LocalDate fecha, 
        Long supermercadoId, 
        TipoCanasta tipoCanasta);
    
    // Consulta básica por fecha
    List<Precio> findByFecha(LocalDate fecha);
    
}
*/
