package com.basic_food_basket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.basic_food_basket.model.Precio;
import com.basic_food_basket.model.Producto;
import com.basic_food_basket.model.Supermercado;
import com.basic_food_basket.model.TipoCanasta;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PrecioRepository extends JpaRepository<Precio, Long> {

	Optional<Precio> findByProductoAndFecha(Producto producto, LocalDate fecha);

	List<Precio> findByProductoOrderByFechaDesc(Producto producto);

	@Query("SELECT p FROM Precio p JOIN FETCH p.producto pr WHERE p.fecha = :fecha")
	List<Precio> findByFechaWithRelations(@Param("fecha") LocalDate fecha);

	@Query("SELECT p FROM Precio p JOIN p.producto pr WHERE p.fecha = :fecha AND pr.tipoCanasta = :tipoCanasta")
	List<Precio> findByFechaAndTipoCanasta(@Param("fecha") LocalDate fecha,
			@Param("tipoCanasta") TipoCanasta tipoCanasta);

	@Query("SELECT p FROM Precio p JOIN FETCH p.producto pr "
			+ "WHERE p.fecha = :fecha AND pr.supermercado.id = :supermercadoId AND pr.tipoCanasta = :tipoCanasta")
	List<Precio> findByFechaAndSupermercadoAndTipoCanasta(@Param("fecha") LocalDate fecha,
			@Param("supermercadoId") Long supermercadoId, @Param("tipoCanasta") TipoCanasta tipoCanasta);

	List<Precio> findByFecha(LocalDate fecha);

	@Query("SELECT p FROM Precio p WHERE p.fecha = :fecha AND p.scrapeado = true")
	List<Precio> findScrapeadosByFecha(@Param("fecha") LocalDate fecha);

	@Query("SELECT p FROM Precio p WHERE p.producto = :producto AND p.fecha BETWEEN :startDate AND :endDate ORDER BY p.fecha DESC")
	List<Precio> findByProductoAndFechaBetween(@Param("producto") Producto producto,
			@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

	// Aquí el método recibe SIEMPRE el ID del producto (Long)
	@Query(value = "SELECT * FROM precio WHERE producto_id = :productoId AND scrapeado = true ORDER BY fecha DESC LIMIT 1", nativeQuery = true)
	Optional<Precio> findLastScrapeadoByProducto(@Param("productoId") Long productoId);

	@Query("SELECT p FROM Precio p JOIN p.producto pr WHERE pr.supermercado = :supermercado AND p.scrapeado = :scrapeado AND p.fecha = :fecha")
	List<Precio> findBySupermercadoAndScrapeadoAndFecha(@Param("supermercado") Supermercado supermercado,
			@Param("scrapeado") boolean scrapeado, @Param("fecha") LocalDate fecha);

	Optional<Precio> findFirstByProductoOrderByFechaDesc(Producto producto);

	@Query("SELECT DISTINCT p.fecha FROM Precio p ORDER BY p.fecha DESC")
	List<LocalDate> findDistinctFechas();

	@Query("SELECT DISTINCT pr.supermercado FROM Precio p JOIN p.producto pr")
	List<Supermercado> findDistinctSupermercados();

	@Query("SELECT p FROM Precio p WHERE p.producto = :producto AND p.scrapeado = true ORDER BY p.fecha DESC")
	List<Precio> findScrapeadosByProductoOrderByFechaDesc(@Param("producto") Producto producto);

	@Query("SELECT p FROM Precio p " + "JOIN FETCH p.producto prod " + "JOIN FETCH prod.supermercado s "
			+ "WHERE p.fecha = :fecha " + "ORDER BY s.id, prod.ordenListado")
	List<Precio> findByFechaWithProductoAndSupermercado(@Param("fecha") LocalDate fecha);

	Optional<Precio> findTopByOrderByFechaDesc();

	@Query("SELECT new map(" + "   p.fecha as fecha, " + "   SUM(p.valor) as total " + ") " + "FROM Precio p "
			+ "WHERE p.fecha BETWEEN :desde AND :hasta " + "AND p.scrapeado = true " + "GROUP BY p.fecha "
			+ "ORDER BY p.fecha")
	List<Map<String, Object>> obtenerHistorialTotales(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

	@Query("SELECT p FROM Precio p " + "WHERE p.fecha = (SELECT MAX(p2.fecha) FROM Precio p2)")
	List<Precio> findUltimoScrapeo();


	@Query(value =
			"SELECT s.id, s.nombre, pr.categoria, pr.sub_categoria, SUM(p.valor) " +
			"FROM precio p " +
			"JOIN producto pr ON pr.id = p.producto_id " +
			"JOIN supermercado s ON s.id = pr.supermercado_id " +
			"WHERE p.fecha = :fecha " +
			"GROUP BY s.id, s.nombre, pr.categoria, pr.sub_categoria",
			nativeQuery = true)
			List<Object[]> obtenerTotalesPorSuper(LocalDate fecha);

	@Query("SELECT MAX(p.fecha) FROM Precio p")
	LocalDate obtenerUltimaFecha();
	
	@Query(value = "SELECT pr.supermercado_id, " +
		       "s.nombre, " +
		       "pr.categoria, " +
		       "SUM(p.valor) total " +
		       "FROM precio p " +
		       "JOIN producto pr ON p.producto_id = pr.id " +
		       "JOIN supermercado s ON pr.supermercado_id = s.id " +
		       "WHERE p.fecha = :fecha " +
		       "GROUP BY pr.supermercado_id, s.nombre, pr.categoria",
		       nativeQuery = true)
		List<Object[]> obtenerTotalesSuperCategoria(@Param("fecha") LocalDate fecha);
		
		@Query(value =
			    "SELECT t.categoria, t.sub_categoria, AVG(t.total_super) " +
			    "FROM ( " +
			    "   SELECT pr.supermercado_id, pr.categoria, pr.sub_categoria, SUM(p.valor) total_super " +
			    "   FROM precio p " +
			    "   JOIN producto pr ON p.producto_id = pr.id " +
			    "   WHERE p.fecha = :fecha " +
			    "   GROUP BY pr.supermercado_id, pr.categoria, pr.sub_categoria " +
			    ") t " +
			    "GROUP BY t.categoria, t.sub_categoria",
			    nativeQuery = true)
			List<Object[]> obtenerPromedioSubcategorias(@Param("fecha") LocalDate fecha);
			
			@Query("SELECT s.id, pr.categoria, pr.subCategoria, AVG(p.valor) " +
				       "FROM Precio p " +
				       "JOIN p.producto pr " +
				       "JOIN pr.supermercado s " +
				       "WHERE p.fecha = :fecha " +
				       "GROUP BY s.id, pr.categoria, pr.subCategoria")
				List<Object[]> promedioSubcategoriasPorSuper(@Param("fecha") LocalDate fecha);
				
				// Agregar estos métodos a PrecioRepository
				@Query("SELECT p FROM Precio p WHERE p.fecha = :fecha ORDER BY p.id")
				List<Precio> findAllByFecha(@Param("fecha") LocalDate fecha);

				@Query("SELECT p FROM Precio p WHERE p.fecha = :fecha AND p.producto.supermercado.id = :supermercadoId")
				List<Precio> findByFechaAndSupermercadoId(@Param("fecha") LocalDate fecha, @Param("supermercadoId") Long supermercadoId);

			
}
