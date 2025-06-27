/*
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

    // Trae todos los precios de una fecha con relaciones cargadas
    @Query("SELECT p FROM Precio p JOIN FETCH p.producto pr WHERE p.fecha = :fecha")
    List<Precio> findByFechaWithRelations(@Param("fecha") LocalDate fecha);

    // Trae todos los precios de una fecha y tipo de canasta
    @Query("SELECT p FROM Precio p JOIN p.producto pr WHERE p.fecha = :fecha AND pr.tipoCanasta = :tipoCanasta")
    List<Precio> findByFechaAndTipoCanasta(@Param("fecha") LocalDate fecha, @Param("tipoCanasta") TipoCanasta tipoCanasta);

    // Trae precios por fecha, supermercado y tipo de canasta
    @Query("SELECT p FROM Precio p JOIN FETCH p.producto pr " +
           "WHERE p.fecha = :fecha AND pr.supermercado.id = :supermercadoId AND pr.tipoCanasta = :tipoCanasta")
    List<Precio> findByFechaAndSupermercadoAndTipoCanasta(
            @Param("fecha") LocalDate fecha,
            @Param("supermercadoId") Long supermercadoId,
            @Param("tipoCanasta") TipoCanasta tipoCanasta);

    // Consulta básica por fecha
    List<Precio> findByFecha(LocalDate fecha);

    // Precios scrapeados por fecha
    @Query("SELECT p FROM Precio p WHERE p.fecha = :fecha AND p.scrapeado = true")
    List<Precio> findScrapeadosByFecha(@Param("fecha") LocalDate fecha);

    // Precios por rango de fechas y producto
    @Query("SELECT p FROM Precio p WHERE p.producto = :producto AND p.fecha BETWEEN :startDate AND :endDate ORDER BY p.fecha DESC")
    List<Precio> findByProductoAndFechaBetween(
            @Param("producto") Producto producto,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Último precio scrapeado para un producto (consulta nativa)
    @Query(value = "SELECT * FROM precio WHERE producto_id = :productoId AND scrapeado = true ORDER BY fecha DESC LIMIT 1", nativeQuery = true)
    Optional<Precio> findLastScrapeadoByProducto(@Param("productoId") Long productoId);

    // Precios por supermercado, scrapeado y fecha
    @Query("SELECT p FROM Precio p JOIN p.producto pr WHERE pr.supermercado = :supermercado AND p.scrapeado = :scrapeado AND p.fecha = :fecha")
    List<Precio> findBySupermercadoAndScrapeadoAndFecha(
            @Param("supermercado") Supermercado supermercado,
            @Param("scrapeado") boolean scrapeado,
            @Param("fecha") LocalDate fecha);

    // Primer precio de un producto (el más reciente)
    Optional<Precio> findFirstByProductoOrderByFechaDesc(Producto producto);

    // Fechas distintas de precios
    @Query("SELECT DISTINCT p.fecha FROM Precio p ORDER BY p.fecha DESC")
    List<LocalDate> findDistinctFechas();

    // Supermercados distintos en precios
    @Query("SELECT DISTINCT pr.supermercado FROM Precio p JOIN p.producto pr")
    List<Supermercado> findDistinctSupermercados();
}
*/

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

    Optional<Precio> findByProductoAndFecha(Producto producto, LocalDate fecha);

    List<Precio> findByProductoOrderByFechaDesc(Producto producto);

    @Query("SELECT p FROM Precio p JOIN FETCH p.producto pr WHERE p.fecha = :fecha")
    List<Precio> findByFechaWithRelations(@Param("fecha") LocalDate fecha);

    @Query("SELECT p FROM Precio p JOIN p.producto pr WHERE p.fecha = :fecha AND pr.tipoCanasta = :tipoCanasta")
    List<Precio> findByFechaAndTipoCanasta(@Param("fecha") LocalDate fecha, @Param("tipoCanasta") TipoCanasta tipoCanasta);

    @Query("SELECT p FROM Precio p JOIN FETCH p.producto pr " +
           "WHERE p.fecha = :fecha AND pr.supermercado.id = :supermercadoId AND pr.tipoCanasta = :tipoCanasta")
    List<Precio> findByFechaAndSupermercadoAndTipoCanasta(
            @Param("fecha") LocalDate fecha,
            @Param("supermercadoId") Long supermercadoId,
            @Param("tipoCanasta") TipoCanasta tipoCanasta);

    List<Precio> findByFecha(LocalDate fecha);

    @Query("SELECT p FROM Precio p WHERE p.fecha = :fecha AND p.scrapeado = true")
    List<Precio> findScrapeadosByFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT p FROM Precio p WHERE p.producto = :producto AND p.fecha BETWEEN :startDate AND :endDate ORDER BY p.fecha DESC")
    List<Precio> findByProductoAndFechaBetween(
            @Param("producto") Producto producto,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Aquí el método recibe SIEMPRE el ID del producto (Long)
    @Query(value = "SELECT * FROM precio WHERE producto_id = :productoId AND scrapeado = true ORDER BY fecha DESC LIMIT 1", nativeQuery = true)
    Optional<Precio> findLastScrapeadoByProducto(@Param("productoId") Long productoId);

    @Query("SELECT p FROM Precio p JOIN p.producto pr WHERE pr.supermercado = :supermercado AND p.scrapeado = :scrapeado AND p.fecha = :fecha")
    List<Precio> findBySupermercadoAndScrapeadoAndFecha(
            @Param("supermercado") Supermercado supermercado,
            @Param("scrapeado") boolean scrapeado,
            @Param("fecha") LocalDate fecha);

    Optional<Precio> findFirstByProductoOrderByFechaDesc(Producto producto);

    @Query("SELECT DISTINCT p.fecha FROM Precio p ORDER BY p.fecha DESC")
    List<LocalDate> findDistinctFechas();

    @Query("SELECT DISTINCT pr.supermercado FROM Precio p JOIN p.producto pr")
    List<Supermercado> findDistinctSupermercados();
}