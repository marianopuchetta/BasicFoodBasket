/*
package com.basic_food_basket.service;

import com.basic_food_basket.model.*;
import com.basic_food_basket.repository.PrecioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CanastaService implements ICanastaService {

	@Autowired
	private PrecioRepository precioRepository;


	@Override
	public Map<String, Object> obtenerUltimosPreciosPorSupermercado() {

		Map<String, Object> respuesta = new LinkedHashMap<>();

		Optional<Precio> precioMasRecienteOpt = precioRepository.findTopByOrderByFechaDesc();

		if (precioMasRecienteOpt.isEmpty()) {
			respuesta.put("mensaje", "No hay datos cargados");
			return respuesta;
		}

		LocalDate ultimaFecha = precioMasRecienteOpt.get().getFecha();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		respuesta.put("fecha", ultimaFecha.format(formatter));

		// 🔥 UNA sola query grande optimizada
		List<Precio> preciosDelDia = precioRepository.findByFechaWithProductoAndSupermercado(ultimaFecha);

		// Agrupar en memoria (muy barato)
		Map<Supermercado, List<Precio>> agrupado = preciosDelDia.stream()
				.collect(Collectors.groupingBy(p -> p.getProducto().getSupermercado()));

		List<Map<String, Object>> supermercadosData = new ArrayList<>();

		for (Map.Entry<Supermercado, List<Precio>> entry : agrupado.entrySet()) {

			Supermercado supermercado = entry.getKey();
			List<Precio> precios = entry.getValue();

			Map<String, Object> supermercadoData = new LinkedHashMap<>();
			supermercadoData.put("nombre", supermercado.getNombre());

			List<Map<String, Object>> productosData = new ArrayList<>();
			double totalSupermercado = 0.0;

			for (Precio precio : precios) {

				Map<String, Object> productoData = new LinkedHashMap<>();
				productoData.put("nombre", precio.getProducto().getNombre());
				productoData.put("precio", precio.getValor());

				productosData.add(productoData);
				totalSupermercado += precio.getValor();
			}

			supermercadoData.put("productos", productosData);
			supermercadoData.put("total", redondear2Decimales(totalSupermercado));

			supermercadosData.add(supermercadoData);
		}

		respuesta.put("supermercados", supermercadosData);

		return respuesta;
	}

	@Override
	public Map<String, Object> obtenerResumenCanasta() {
		return obtenerResumenPorSupermercado();
	}

	private LocalDate obtenerUltimaFechaConPrecios() {
		List<LocalDate> fechas = precioRepository.findDistinctFechas();
		if (fechas == null || fechas.isEmpty()) {
			return null;
		}
		return fechas.stream().max(LocalDate::compareTo).orElse(null);
	}

	public Map<String, Object> obtenerHistorialCanasta(LocalDate desde, LocalDate hasta) {

		Map<String, Object> respuesta = new LinkedHashMap<>();

		// 1. Fechas disponibles en rango
		List<LocalDate> fechas = precioRepository.findDistinctFechas().stream()
				.filter(f -> (f.equals(desde) || f.isAfter(desde)) && (f.equals(hasta) || f.isBefore(hasta))).sorted()
				.collect(Collectors.toList());

		// 2. Supermercados ordenados por ID
		List<Supermercado> supermercados = precioRepository.findDistinctSupermercados().stream()
				.sorted(Comparator.comparing(Supermercado::getId)).collect(Collectors.toList());

		List<Map<String, Object>> data = new ArrayList<>();

		// 3. Iteración principal
		for (Supermercado supermercado : supermercados) {

			Map<String, Object> supermercadoData = new LinkedHashMap<>();
			supermercadoData.put("id", supermercado.getId());
			supermercadoData.put("nombre", supermercado.getNombre());
			supermercadoData.put("slug", supermercado.getSlug());

			List<Map<String, Object>> historial = new ArrayList<>();

			double ultimoTotal = Double.MAX_VALUE;

			// 4. Historial por fecha
			for (LocalDate fecha : fechas) {

				Map<String, Object> registro = new LinkedHashMap<>();

				// 🔥 fecha en formato ISO (mejor para apps y JSON)
				registro.put("fecha", fecha.toString());

				double total = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(fecha,
						supermercado.getId(), TipoCanasta.CBA));

				total = redondear2Decimales(total);

				registro.put("total_CBA", total);

				historial.add(registro);

				// guardar último valor
				ultimoTotal = total;
			}

			supermercadoData.put("historial", historial);

			// 🔥 campo clave para ordenar
			supermercadoData.put("ultimo_total", ultimoTotal);

			data.add(supermercadoData);
		}

		// 🔥 ORDENAR del más barato al más caro
		data.sort(Comparator.comparing(m -> (Double) m.get("ultimo_total")));

		respuesta.put("historialSupermercados", data);

		return respuesta;
	}

	@Override
	public Map<String, Object> obtenerHistorialCanasta() {
		List<LocalDate> fechas = precioRepository.findDistinctFechas();
		if (fechas.isEmpty()) {
			return Collections.singletonMap("historialSupermercados", new ArrayList<>());
		}
		LocalDate desde = fechas.stream().min(LocalDate::compareTo).orElse(LocalDate.now());
		LocalDate hasta = fechas.stream().max(LocalDate::compareTo).orElse(LocalDate.now());
		return obtenerHistorialCanasta(desde, hasta);
	}

	public Map<String, Object> obtenerHistorialGeneral(LocalDate desde, LocalDate hasta) {
		Map<String, Object> respuesta = new LinkedHashMap<>();
		List<LocalDate> fechas = precioRepository.findDistinctFechas().stream()
				.filter(f -> (f.equals(desde) || f.isAfter(desde)) && (f.equals(hasta) || f.isBefore(hasta))).sorted()
				.collect(Collectors.toList());

		List<Map<String, Object>> historial = new ArrayList<>();
		for (LocalDate fecha : fechas) {
			Map<String, Object> registro = new LinkedHashMap<>();
			registro.put("fecha", Arrays.asList(fecha.getYear(), fecha.getMonthValue(), fecha.getDayOfMonth()));

			// SOLO CBA
			for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
				List<Precio> precios = precioRepository.findByFechaAndTipoCanasta(fecha, tipo);
				double total = calcularTotal(precios);
				registro.put("total_" + tipo.name(), redondear2Decimales(total));
				registro.put("promedioProducto_" + tipo.name(),
						precios.isEmpty() ? 0 : redondear2Decimales(total / precios.size()));
			}
			historial.add(registro);
		}
		respuesta.put("historialGeneral", historial);
		return respuesta;
	}

	@Override
	public Map<String, Object> obtenerHistorialGeneral() {
		List<LocalDate> fechas = precioRepository.findDistinctFechas();
		if (fechas.isEmpty()) {
			return Collections.singletonMap("historialGeneral", new ArrayList<>());
		}
		LocalDate desde = fechas.stream().min(LocalDate::compareTo).orElse(LocalDate.now());
		LocalDate hasta = fechas.stream().max(LocalDate::compareTo).orElse(LocalDate.now());
		return obtenerHistorialGeneral(desde, hasta);
	}

	@Override
	public Map<String, Object> obtenerResumenGeneral() {
		Map<String, Object> respuesta = new LinkedHashMap<>();
		LocalDate hoy = obtenerUltimaFechaConPrecios();
		if (hoy == null) {
			respuesta.put("fecha", LocalDate.now().toString());
			respuesta.put("cantidadSupermercados", 0);
			return respuesta;
		}
		LocalDate ayer = hoy.minus(1, ChronoUnit.DAYS);
		LocalDate semanaPasada = hoy.minus(7, ChronoUnit.DAYS);
		LocalDate mesPasado = hoy.minus(30, ChronoUnit.DAYS);
		LocalDate anioPasado = hoy.minus(365, ChronoUnit.DAYS);

		List<Supermercado> supermercados = precioRepository.findDistinctSupermercados().stream()
				.sorted(Comparator.comparing(Supermercado::getId)).collect(Collectors.toList());
		Map<TipoCanasta, Double> sumaTotalesCanasta = new EnumMap<>(TipoCanasta.class);
		Map<TipoCanasta, Double> sumaTotalesCanastaAyer = new EnumMap<>(TipoCanasta.class);
		Map<TipoCanasta, Double> sumaTotalesCanastaSemanal = new EnumMap<>(TipoCanasta.class);
		Map<TipoCanasta, Double> sumaTotalesCanastaMensual = new EnumMap<>(TipoCanasta.class);
		Map<TipoCanasta, Double> sumaTotalesCanastaAnual = new EnumMap<>(TipoCanasta.class);

		// SOLO CBA
		for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
			sumaTotalesCanasta.put(tipo, 0.0);
			sumaTotalesCanastaAyer.put(tipo, 0.0);
			sumaTotalesCanastaSemanal.put(tipo, 0.0);
			sumaTotalesCanastaMensual.put(tipo, 0.0);
			sumaTotalesCanastaAnual.put(tipo, 0.0);
		}

		for (Supermercado supermercado : supermercados) {
			for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
				double totalHoy = calcularTotal(
						precioRepository.findByFechaAndSupermercadoAndTipoCanasta(hoy, supermercado.getId(), tipo));
				sumaTotalesCanasta.put(tipo, sumaTotalesCanasta.get(tipo) + totalHoy);

				double totalAyer = calcularTotal(
						precioRepository.findByFechaAndSupermercadoAndTipoCanasta(ayer, supermercado.getId(), tipo));
				sumaTotalesCanastaAyer.put(tipo, sumaTotalesCanastaAyer.get(tipo) + totalAyer);

				double totalSemanal = calcularTotal(precioRepository
						.findByFechaAndSupermercadoAndTipoCanasta(semanaPasada, supermercado.getId(), tipo));
				sumaTotalesCanastaSemanal.put(tipo, sumaTotalesCanastaSemanal.get(tipo) + totalSemanal);

				double totalMensual = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(mesPasado,
						supermercado.getId(), tipo));
				sumaTotalesCanastaMensual.put(tipo, sumaTotalesCanastaMensual.get(tipo) + totalMensual);

				double totalAnual = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(anioPasado,
						supermercado.getId(), tipo));
				sumaTotalesCanastaAnual.put(tipo, sumaTotalesCanastaAnual.get(tipo) + totalAnual);
			}
		}

		for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
			String nombre = tipo.name();
			Double promedioGeneral = supermercados.isEmpty() ? 0
					: redondear2Decimales(sumaTotalesCanasta.get(tipo) / supermercados.size());
			respuesta.put("promedioGeneral" + nombre, promedioGeneral);

			respuesta.put("variacionDiariaGeneral" + nombre,
					calcularVariacion(sumaTotalesCanasta.get(tipo), sumaTotalesCanastaAyer.get(tipo)));

			respuesta.put("variacionSemanalGeneral" + nombre,
					calcularVariacion(sumaTotalesCanasta.get(tipo), sumaTotalesCanastaSemanal.get(tipo)));

			respuesta.put("variacionMensualGeneral" + nombre,
					calcularVariacion(sumaTotalesCanasta.get(tipo), sumaTotalesCanastaMensual.get(tipo)));

			respuesta.put("variacionAnualGeneral" + nombre,
					calcularVariacion(sumaTotalesCanasta.get(tipo), sumaTotalesCanastaAnual.get(tipo)));
		}

		respuesta.put("fecha", hoy.toString());
		respuesta.put("cantidadSupermercados", supermercados.size());
		return respuesta;
	}

	private Map<String, Object> obtenerResumenPorSupermercado() {

		Map<String, Object> respuesta = new LinkedHashMap<>();

		// 1. Buscar última fecha con datos
		List<LocalDate> fechasDisponibles = precioRepository.findDistinctFechas();

		if (fechasDisponibles.isEmpty()) {
			respuesta.put("mensaje", "No hay datos cargados en el sistema");
			return respuesta;
		}

		LocalDate hoy = fechasDisponibles.stream().max(LocalDate::compareTo).orElse(LocalDate.now());

		List<Precio> preciosDelDia = precioRepository.findByFechaWithRelations(hoy);

		// Agrupar SOLO por supermercado (ya no por tipo)
		Map<Supermercado, List<Precio>> preciosAgrupados = preciosDelDia.stream()
				.filter(p -> p.getProducto().getTipoCanasta() == TipoCanasta.CBA)
				.collect(Collectors.groupingBy(p -> p.getProducto().getSupermercado()));

		// Fechas históricas
		LocalDate ayer = hoy.minusDays(1);
		LocalDate semanaPasada = hoy.minusDays(7);
		LocalDate mesPasado = hoy.minusDays(30);
		LocalDate añoPasado = hoy.minusDays(365);

		List<Map<String, Object>> supermercadosData = new ArrayList<>();
		int contadorSupermercados = 0;

		double sumaTotalesHoy = 0.0;

		double sumHoyParaAyer = 0.0;
		double sumAyer = 0.0;

		double sumHoyParaSemana = 0.0;
		double sumSemana = 0.0;

		double sumHoyParaMes = 0.0;
		double sumMes = 0.0;

		double sumHoyParaAnio = 0.0;
		double sumAnio = 0.0;

		// 2. Iterar supermercados
		for (Map.Entry<Supermercado, List<Precio>> entry : preciosAgrupados.entrySet()) {

			Supermercado supermercado = entry.getKey();
			List<Precio> precios = entry.getValue();

			double totalHoy = calcularTotal(precios);
			sumaTotalesHoy += totalHoy;

			Map<String, Object> supermercadoData = new LinkedHashMap<>();
			supermercadoData.put("id", supermercado.getId());
			supermercadoData.put("nombre", supermercado.getNombre());
			supermercadoData.put("slug", supermercado.getSlug());

			supermercadoData.put("total", redondear2Decimales(totalHoy));
			supermercadoData.put("productos", precios.size());

			// --- Variación diaria ---
			double totalAyer = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(ayer,
					supermercado.getId(), TipoCanasta.CBA));
			if (totalAyer > 0) {
				sumHoyParaAyer += totalHoy;
				sumAyer += totalAyer;
			}

			// --- Variación semanal ---
			double totalSemana = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(semanaPasada,
					supermercado.getId(), TipoCanasta.CBA));
			if (totalSemana > 0) {
				sumHoyParaSemana += totalHoy;
				sumSemana += totalSemana;
			}

			// --- Variación mensual ---
			double totalMes = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(mesPasado,
					supermercado.getId(), TipoCanasta.CBA));
			if (totalMes > 0) {
				sumHoyParaMes += totalHoy;
				sumMes += totalMes;
			}

			// --- Variación anual ---
			double totalAnio = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(añoPasado,
					supermercado.getId(), TipoCanasta.CBA));
			if (totalAnio > 0) {
				sumHoyParaAnio += totalHoy;
				sumAnio += totalAnio;
			}

			supermercadoData.put("variacionDiaria", calcularVariacion(totalHoy, totalAyer));
			supermercadoData.put("variacionSemanal", calcularVariacion(totalHoy, totalSemana));
			supermercadoData.put("variacionMensual", calcularVariacion(totalHoy, totalMes));
			supermercadoData.put("variacionAnual", calcularVariacion(totalHoy, totalAnio));

			supermercadosData.add(supermercadoData);
			contadorSupermercados++;
		}

		// 3. Respuesta final
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		respuesta.put("fecha", hoy.format(formatter));
		respuesta.put("supermercados", supermercadosData);
		respuesta.put("cantidadSupermercados", contadorSupermercados);

		// Promedio general CBA
		double promedioGeneral = contadorSupermercados == 0 ? 0
				: redondear2Decimales(sumaTotalesHoy / contadorSupermercados);

		respuesta.put("promedioGeneralCBA", promedioGeneral);

		respuesta.put("variacionDiariaGeneralCBA", calcularVariacion(sumHoyParaAyer, sumAyer));

		respuesta.put("variacionSemanalGeneralCBA", calcularVariacion(sumHoyParaSemana, sumSemana));

		respuesta.put("variacionMensualGeneralCBA", calcularVariacion(sumHoyParaMes, sumMes));

		respuesta.put("variacionAnualGeneralCBA", calcularVariacion(sumHoyParaAnio, sumAnio));

		return respuesta;
	}

	private double calcularTotalCanasta(Long supermercadoId, TipoCanasta tipoCanasta, LocalDate fecha) {
		List<Precio> precios = precioRepository.findByFechaAndSupermercadoAndTipoCanasta(fecha, supermercadoId,
				tipoCanasta);
		return calcularTotal(precios);
	}

	private double calcularTotalGeneral(LocalDate fecha) {
		return calcularTotal(precioRepository.findByFecha(fecha));
	}

	private double calcularTotal(List<Precio> precios) {
		if (precios == null || precios.isEmpty()) {
			return 0;
		}

		double total = precios.stream().filter(p -> p.getValor() != null).mapToDouble(Precio::getValor).sum();

		return total;
	}

	private Double calcularVariacion(Double actual, Double anterior) {
		if (actual == null || anterior == null || anterior == 0)
			return null;
		double variacion = ((actual - anterior) / anterior) * 100.0;
		return Math.round(variacion * 100.0) / 100.0;
	}

	
	private LocalDate obtenerUltimaFecha() {
		List<LocalDate> fechas = precioRepository.findDistinctFechas();

		if (fechas.isEmpty()) {
			return null;
		}

		return fechas.stream().max(LocalDate::compareTo).orElse(null);
	}

	@Override
	public Map<String, Object> obtenerHistorialUltimos30Dias() {

		LocalDate ultimaFecha = obtenerUltimaFecha();

		if (ultimaFecha == null) {
			return Collections.singletonMap("historialSupermercados", new ArrayList<>());
		}

		LocalDate desde = ultimaFecha.minusDays(30);

		return obtenerHistorialCanasta(desde, ultimaFecha);
	}
	
	@Override
	public Map<String, Object> obtenerHistorialCategoriasUltimos30Dias() {
		LocalDate ultimaFecha = precioRepository.obtenerUltimaFecha();
		if (ultimaFecha == null) {
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("desde", null);
			r.put("hasta", null);
			r.put("historialSupermercados", new ArrayList<>());
			return r;
		}
		LocalDate desde = ultimaFecha.minusDays(30);
		return obtenerHistorialCategorias(desde, ultimaFecha);
	}

	@Override
	public Map<String, Object> obtenerHistorialCategorias(LocalDate desde, LocalDate hasta) {

		Map<String, Object> respuesta = new LinkedHashMap<>();
		respuesta.put("desde", desde.toString());
		respuesta.put("hasta", hasta.toString());

		// Eje de fechas: mismas reglas que tu historial actual (fechas existentes en BD dentro del rango)
		List<LocalDate> fechas = precioRepository.findDistinctFechas().stream()
				.filter(f -> (f.equals(desde) || f.isAfter(desde)) && (f.equals(hasta) || f.isBefore(hasta)))
				.sorted()
				.collect(Collectors.toList());

		if (fechas.isEmpty()) {
			respuesta.put("historialSupermercados", new ArrayList<>());
			return respuesta;
		}

		// Traemos todos los supermercados que aparecen en precios (igual que tu historial actual)
		List<Supermercado> supermercados = precioRepository.findDistinctSupermercados().stream()
				.sorted(Comparator.comparing(Supermercado::getId))
				.collect(Collectors.toList());

		// Query grande: totales agregados por super+fecha+cat+subcat (solo CBA)
		List<Object[]> rows = precioRepository.obtenerTotalesPorSuperFechaCategoriaSubcategoria(fechas.get(0), fechas.get(fechas.size() - 1));

		// Index:
		// superId -> categoria -> subCategoria -> (fecha -> total)
		Map<Long, Map<String, Map<String, Map<LocalDate, Double>>>> idx = new HashMap<>();

		// ...
		for (Object[] row : rows) {
		    Long superId = ((Number) row[0]).longValue();

		    // row[3] viene como java.sql.Date en queries nativas
		    LocalDate fecha;
		    Object fechaObj = row[3];
		    if (fechaObj instanceof java.sql.Date) {
		        fecha = ((java.sql.Date) fechaObj).toLocalDate();
		    } else if (fechaObj instanceof java.sql.Timestamp) {
		        fecha = ((java.sql.Timestamp) fechaObj).toLocalDateTime().toLocalDate();
		    } else {
		        // fallback por si el driver ya lo da como LocalDate
		        fecha = (LocalDate) fechaObj;
		    }

		    String categoria = (String) row[4];
		    String subCategoria = (String) row[5];
		    Double total = row[6] != null ? ((Number) row[6]).doubleValue() : null;

		    idx
		        .computeIfAbsent(superId, __ -> new HashMap<>())
		        .computeIfAbsent(categoria, __ -> new HashMap<>())
		        .computeIfAbsent(subCategoria, __ -> new HashMap<>())
		        .put(fecha, total);
		}
		// ...
		List<Map<String, Object>> dataSupers = new ArrayList<>();

		for (Supermercado s : supermercados) {
			Map<String, Object> superData = new LinkedHashMap<>();
			superData.put("id", s.getId());
			superData.put("nombre", s.getNombre());
			superData.put("slug", s.getSlug());

			Map<String, Map<String, Map<LocalDate, Double>>> porCategoria = idx.getOrDefault(s.getId(), Collections.emptyMap());

			List<Map<String, Object>> categoriasOut = new ArrayList<>();

			// Para mantener determinismo: ordenar categorías por nombre
			List<String> categoriasOrdenadas = new ArrayList<>(porCategoria.keySet());
			Collections.sort(categoriasOrdenadas);

			for (String cat : categoriasOrdenadas) {
				Map<String, Object> catData = new LinkedHashMap<>();
				catData.put("nombre", cat);

				Map<String, Map<LocalDate, Double>> porSub = porCategoria.getOrDefault(cat, Collections.emptyMap());

				// Historial TOTAL categoría = suma de subcategorías por fecha.
				// Si no hay ninguna subcategoría con dato en esa fecha -> null
				List<Map<String, Object>> histCat = new ArrayList<>();
				for (LocalDate fecha : fechas) {
					Double suma = 0.0;
					boolean huboAlgo = false;

					for (Map<LocalDate, Double> serieSub : porSub.values()) {
						Double v = serieSub.get(fecha);
						if (v != null) {
							suma += v;
							huboAlgo = true;
						}
					}

					Map<String, Object> punto = new LinkedHashMap<>();
					punto.put("fecha", fecha.toString());
					punto.put("total", huboAlgo ? redondear2Decimales(suma) : null);
					histCat.add(punto);
				}
				catData.put("historial", histCat);

				// Subcategorías con historial propio
				List<Map<String, Object>> subOut = new ArrayList<>();

				List<String> subOrdenadas = new ArrayList<>(porSub.keySet());
				Collections.sort(subOrdenadas);

				for (String sub : subOrdenadas) {
					Map<String, Object> subData = new LinkedHashMap<>();
					subData.put("nombre", sub);

					Map<LocalDate, Double> serie = porSub.getOrDefault(sub, Collections.emptyMap());

					List<Map<String, Object>> histSub = new ArrayList<>();
					for (LocalDate fecha : fechas) {
						Map<String, Object> punto = new LinkedHashMap<>();
						punto.put("fecha", fecha.toString());
						Double v = serie.get(fecha);
						punto.put("total", v == null ? null : redondear2Decimales(v));
						histSub.add(punto);
					}

					subData.put("historial", histSub);
					subOut.add(subData);
				}

				catData.put("subcategorias", subOut);
				categoriasOut.add(catData);
			}

			superData.put("categorias", categoriasOut);

			// ultimo_total: total CBA del último día = suma de todas las categorías (usamos el mismo criterio que arriba)
			LocalDate ultima = fechas.get(fechas.size() - 1);
			Double ultimoTotal = 0.0;
			boolean huboAlgoUltimo = false;

			for (Map<String, Map<LocalDate, Double>> porSub : porCategoria.values()) {
				for (Map<LocalDate, Double> serieSub : porSub.values()) {
					Double v = serieSub.get(ultima);
					if (v != null) {
						ultimoTotal += v;
						huboAlgoUltimo = true;
					}
				}
			}

			superData.put("ultimo_total", huboAlgoUltimo ? redondear2Decimales(ultimoTotal) : null);

			dataSupers.add(superData);
		}

		// Ordenar por ultimo_total asc (como tu historial actual)
		dataSupers.sort(Comparator.comparing(m -> (Double) m.get("ultimo_total"), Comparator.nullsLast(Double::compareTo)));

		respuesta.put("historialSupermercados", dataSupers);
		return respuesta;
	}

	private double redondear2Decimales(double valor) {
		return Math.round(valor * 100.0) / 100.0;
	}
}
*/
package com.basic_food_basket.service;

import com.basic_food_basket.model.*;
import com.basic_food_basket.repository.PrecioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CanastaService implements ICanastaService {

	@Autowired
	private PrecioRepository precioRepository;


	@Override
	public Map<String, Object> obtenerUltimosPreciosPorSupermercado() {

		Map<String, Object> respuesta = new LinkedHashMap<>();

		Optional<Precio> precioMasRecienteOpt = precioRepository.findTopByOrderByFechaDesc();

		if (precioMasRecienteOpt.isEmpty()) {
			respuesta.put("mensaje", "No hay datos cargados");
			return respuesta;
		}

		LocalDate ultimaFecha = precioMasRecienteOpt.get().getFecha();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		respuesta.put("fecha", ultimaFecha.format(formatter));

		// 🔥 UNA sola query grande optimizada
		List<Precio> preciosDelDia = precioRepository.findByFechaWithProductoAndSupermercado(ultimaFecha);

		// Agrupar en memoria (muy barato)
		Map<Supermercado, List<Precio>> agrupado = preciosDelDia.stream()
				.collect(Collectors.groupingBy(p -> p.getProducto().getSupermercado()));

		List<Map<String, Object>> supermercadosData = new ArrayList<>();

		for (Map.Entry<Supermercado, List<Precio>> entry : agrupado.entrySet()) {

			Supermercado supermercado = entry.getKey();
			List<Precio> precios = entry.getValue();

			Map<String, Object> supermercadoData = new LinkedHashMap<>();
			supermercadoData.put("nombre", supermercado.getNombre());

			List<Map<String, Object>> productosData = new ArrayList<>();
			double totalSupermercado = 0.0;

			for (Precio precio : precios) {

				Map<String, Object> productoData = new LinkedHashMap<>();
				productoData.put("nombre", precio.getProducto().getNombre());
				productoData.put("precio", precio.getValor());

				productosData.add(productoData);
				totalSupermercado += precio.getValor();
			}

			supermercadoData.put("productos", productosData);
			supermercadoData.put("total", redondear2Decimales(totalSupermercado));

			supermercadosData.add(supermercadoData);
		}

		respuesta.put("supermercados", supermercadosData);

		return respuesta;
	}

	@Override
	public Map<String, Object> obtenerResumenCanasta() {
		return obtenerResumenPorSupermercado();
	}

	private LocalDate obtenerUltimaFechaConPrecios() {
		List<LocalDate> fechas = precioRepository.findDistinctFechas();
		if (fechas == null || fechas.isEmpty()) {
			return null;
		}
		return fechas.stream().max(LocalDate::compareTo).orElse(null);
	}

	public Map<String, Object> obtenerHistorialCanasta(LocalDate desde, LocalDate hasta) {

		Map<String, Object> respuesta = new LinkedHashMap<>();

		// 1. Fechas disponibles en rango
		List<LocalDate> fechas = precioRepository.findDistinctFechas().stream()
				.filter(f -> (f.equals(desde) || f.isAfter(desde)) && (f.equals(hasta) || f.isBefore(hasta))).sorted()
				.collect(Collectors.toList());

		// 2. Supermercados ordenados por ID
		List<Supermercado> supermercados = precioRepository.findDistinctSupermercados().stream()
				.sorted(Comparator.comparing(Supermercado::getId)).collect(Collectors.toList());

		List<Map<String, Object>> data = new ArrayList<>();

		// 3. Iteración principal
		for (Supermercado supermercado : supermercados) {

			Map<String, Object> supermercadoData = new LinkedHashMap<>();
			supermercadoData.put("id", supermercado.getId());
			supermercadoData.put("nombre", supermercado.getNombre());
			supermercadoData.put("slug", supermercado.getSlug());

			List<Map<String, Object>> historial = new ArrayList<>();

			double ultimoTotal = Double.MAX_VALUE;

			// 4. Historial por fecha
			for (LocalDate fecha : fechas) {

				Map<String, Object> registro = new LinkedHashMap<>();

				// 🔥 fecha en formato ISO (mejor para apps y JSON)
				registro.put("fecha", fecha.toString());

				double total = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(fecha,
						supermercado.getId(), TipoCanasta.CBA));

				total = redondear2Decimales(total);

				registro.put("total_CBA", total);

				historial.add(registro);

				// guardar último valor
				ultimoTotal = total;
			}

			supermercadoData.put("historial", historial);

			// 🔥 campo clave para ordenar
			supermercadoData.put("ultimo_total", ultimoTotal);

			data.add(supermercadoData);
		}

		// 🔥 ORDENAR del más barato al más caro
		data.sort(Comparator.comparing(m -> (Double) m.get("ultimo_total")));

		respuesta.put("historialSupermercados", data);

		return respuesta;
	}

	@Override
	public Map<String, Object> obtenerHistorialCanasta() {
		List<LocalDate> fechas = precioRepository.findDistinctFechas();
		if (fechas.isEmpty()) {
			return Collections.singletonMap("historialSupermercados", new ArrayList<>());
		}
		LocalDate desde = fechas.stream().min(LocalDate::compareTo).orElse(LocalDate.now());
		LocalDate hasta = fechas.stream().max(LocalDate::compareTo).orElse(LocalDate.now());
		return obtenerHistorialCanasta(desde, hasta);
	}

	public Map<String, Object> obtenerHistorialGeneral(LocalDate desde, LocalDate hasta) {
		Map<String, Object> respuesta = new LinkedHashMap<>();
		List<LocalDate> fechas = precioRepository.findDistinctFechas().stream()
				.filter(f -> (f.equals(desde) || f.isAfter(desde)) && (f.equals(hasta) || f.isBefore(hasta))).sorted()
				.collect(Collectors.toList());

		List<Map<String, Object>> historial = new ArrayList<>();
		for (LocalDate fecha : fechas) {
			Map<String, Object> registro = new LinkedHashMap<>();
			registro.put("fecha", Arrays.asList(fecha.getYear(), fecha.getMonthValue(), fecha.getDayOfMonth()));

			// SOLO CBA
			for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
				List<Precio> precios = precioRepository.findByFechaAndTipoCanasta(fecha, tipo);
				double total = calcularTotal(precios);
				registro.put("total_" + tipo.name(), redondear2Decimales(total));
				registro.put("promedioProducto_" + tipo.name(),
						precios.isEmpty() ? 0 : redondear2Decimales(total / precios.size()));
			}
			historial.add(registro);
		}
		respuesta.put("historialGeneral", historial);
		return respuesta;
	}

	@Override
	public Map<String, Object> obtenerHistorialGeneral() {
		List<LocalDate> fechas = precioRepository.findDistinctFechas();
		if (fechas.isEmpty()) {
			return Collections.singletonMap("historialGeneral", new ArrayList<>());
		}
		LocalDate desde = fechas.stream().min(LocalDate::compareTo).orElse(LocalDate.now());
		LocalDate hasta = fechas.stream().max(LocalDate::compareTo).orElse(LocalDate.now());
		return obtenerHistorialGeneral(desde, hasta);
	}

	@Override
	public Map<String, Object> obtenerResumenGeneral() {
		Map<String, Object> respuesta = new LinkedHashMap<>();
		LocalDate hoy = obtenerUltimaFechaConPrecios();
		if (hoy == null) {
			respuesta.put("fecha", LocalDate.now().toString());
			respuesta.put("cantidadSupermercados", 0);
			return respuesta;
		}
		LocalDate ayer = hoy.minus(1, ChronoUnit.DAYS);
		LocalDate semanaPasada = hoy.minus(7, ChronoUnit.DAYS);
		LocalDate mesPasado = hoy.minus(30, ChronoUnit.DAYS);
		LocalDate anioPasado = hoy.minus(365, ChronoUnit.DAYS);

		List<Supermercado> supermercados = precioRepository.findDistinctSupermercados().stream()
				.sorted(Comparator.comparing(Supermercado::getId)).collect(Collectors.toList());
		Map<TipoCanasta, Double> sumaTotalesCanasta = new EnumMap<>(TipoCanasta.class);
		Map<TipoCanasta, Double> sumaTotalesCanastaAyer = new EnumMap<>(TipoCanasta.class);
		Map<TipoCanasta, Double> sumaTotalesCanastaSemanal = new EnumMap<>(TipoCanasta.class);
		Map<TipoCanasta, Double> sumaTotalesCanastaMensual = new EnumMap<>(TipoCanasta.class);
		Map<TipoCanasta, Double> sumaTotalesCanastaAnual = new EnumMap<>(TipoCanasta.class);

		// SOLO CBA
		for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
			sumaTotalesCanasta.put(tipo, 0.0);
			sumaTotalesCanastaAyer.put(tipo, 0.0);
			sumaTotalesCanastaSemanal.put(tipo, 0.0);
			sumaTotalesCanastaMensual.put(tipo, 0.0);
			sumaTotalesCanastaAnual.put(tipo, 0.0);
		}

		for (Supermercado supermercado : supermercados) {
			for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
				double totalHoy = calcularTotal(
						precioRepository.findByFechaAndSupermercadoAndTipoCanasta(hoy, supermercado.getId(), tipo));
				sumaTotalesCanasta.put(tipo, sumaTotalesCanasta.get(tipo) + totalHoy);

				double totalAyer = calcularTotal(
						precioRepository.findByFechaAndSupermercadoAndTipoCanasta(ayer, supermercado.getId(), tipo));
				sumaTotalesCanastaAyer.put(tipo, sumaTotalesCanastaAyer.get(tipo) + totalAyer);

				double totalSemanal = calcularTotal(precioRepository
						.findByFechaAndSupermercadoAndTipoCanasta(semanaPasada, supermercado.getId(), tipo));
				sumaTotalesCanastaSemanal.put(tipo, sumaTotalesCanastaSemanal.get(tipo) + totalSemanal);

				double totalMensual = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(mesPasado,
						supermercado.getId(), tipo));
				sumaTotalesCanastaMensual.put(tipo, sumaTotalesCanastaMensual.get(tipo) + totalMensual);

				double totalAnual = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(anioPasado,
						supermercado.getId(), tipo));
				sumaTotalesCanastaAnual.put(tipo, sumaTotalesCanastaAnual.get(tipo) + totalAnual);
			}
		}

		for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
			String nombre = tipo.name();
			Double promedioGeneral = supermercados.isEmpty() ? 0
					: redondear2Decimales(sumaTotalesCanasta.get(tipo) / supermercados.size());
			respuesta.put("promedioGeneral" + nombre, promedioGeneral);

			respuesta.put("variacionDiariaGeneral" + nombre,
					calcularVariacion(sumaTotalesCanasta.get(tipo), sumaTotalesCanastaAyer.get(tipo)));

			respuesta.put("variacionSemanalGeneral" + nombre,
					calcularVariacion(sumaTotalesCanasta.get(tipo), sumaTotalesCanastaSemanal.get(tipo)));

			respuesta.put("variacionMensualGeneral" + nombre,
					calcularVariacion(sumaTotalesCanasta.get(tipo), sumaTotalesCanastaMensual.get(tipo)));

			respuesta.put("variacionAnualGeneral" + nombre,
					calcularVariacion(sumaTotalesCanasta.get(tipo), sumaTotalesCanastaAnual.get(tipo)));
		}

		respuesta.put("fecha", hoy.toString());
		respuesta.put("cantidadSupermercados", supermercados.size());
		return respuesta;
	}

	private Map<String, Object> obtenerResumenPorSupermercado() {

		Map<String, Object> respuesta = new LinkedHashMap<>();

		// 1. Buscar última fecha con datos
		List<LocalDate> fechasDisponibles = precioRepository.findDistinctFechas();

		if (fechasDisponibles.isEmpty()) {
			respuesta.put("mensaje", "No hay datos cargados en el sistema");
			return respuesta;
		}

		LocalDate hoy = fechasDisponibles.stream().max(LocalDate::compareTo).orElse(LocalDate.now());

		List<Precio> preciosDelDia = precioRepository.findByFechaWithRelations(hoy);

		// Agrupar SOLO por supermercado (ya no por tipo)
		Map<Supermercado, List<Precio>> preciosAgrupados = preciosDelDia.stream()
				.filter(p -> p.getProducto().getTipoCanasta() == TipoCanasta.CBA)
				.collect(Collectors.groupingBy(p -> p.getProducto().getSupermercado()));

		// Fechas históricas
		LocalDate ayer = hoy.minusDays(1);
		LocalDate semanaPasada = hoy.minusDays(7);
		LocalDate mesPasado = hoy.minusDays(30);
		LocalDate añoPasado = hoy.minusDays(365);

		List<Map<String, Object>> supermercadosData = new ArrayList<>();
		int contadorSupermercados = 0;

		double sumaTotalesHoy = 0.0;

		double sumHoyParaAyer = 0.0;
		double sumAyer = 0.0;

		double sumHoyParaSemana = 0.0;
		double sumSemana = 0.0;

		double sumHoyParaMes = 0.0;
		double sumMes = 0.0;

		double sumHoyParaAnio = 0.0;
		double sumAnio = 0.0;

		// 2. Iterar supermercados
		for (Map.Entry<Supermercado, List<Precio>> entry : preciosAgrupados.entrySet()) {

			Supermercado supermercado = entry.getKey();
			List<Precio> precios = entry.getValue();

			double totalHoy = calcularTotal(precios);
			sumaTotalesHoy += totalHoy;

			Map<String, Object> supermercadoData = new LinkedHashMap<>();
			supermercadoData.put("id", supermercado.getId());
			supermercadoData.put("nombre", supermercado.getNombre());
			supermercadoData.put("slug", supermercado.getSlug());

			supermercadoData.put("total", redondear2Decimales(totalHoy));
			supermercadoData.put("productos", precios.size());

			// --- Variación diaria ---
			double totalAyer = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(ayer,
					supermercado.getId(), TipoCanasta.CBA));
			if (totalAyer > 0) {
				sumHoyParaAyer += totalHoy;
				sumAyer += totalAyer;
			}

			// --- Variación semanal ---
			double totalSemana = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(semanaPasada,
					supermercado.getId(), TipoCanasta.CBA));
			if (totalSemana > 0) {
				sumHoyParaSemana += totalHoy;
				sumSemana += totalSemana;
			}

			// --- Variación mensual ---
			double totalMes = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(mesPasado,
					supermercado.getId(), TipoCanasta.CBA));
			if (totalMes > 0) {
				sumHoyParaMes += totalHoy;
				sumMes += totalMes;
			}

			// --- Variación anual ---
			double totalAnio = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(añoPasado,
					supermercado.getId(), TipoCanasta.CBA));
			if (totalAnio > 0) {
				sumHoyParaAnio += totalHoy;
				sumAnio += totalAnio;
			}

			supermercadoData.put("variacionDiaria", calcularVariacion(totalHoy, totalAyer));
			supermercadoData.put("variacionSemanal", calcularVariacion(totalHoy, totalSemana));
			supermercadoData.put("variacionMensual", calcularVariacion(totalHoy, totalMes));
			supermercadoData.put("variacionAnual", calcularVariacion(totalHoy, totalAnio));

			supermercadosData.add(supermercadoData);
			contadorSupermercados++;
		}

		// 3. Respuesta final
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		respuesta.put("fecha", hoy.format(formatter));
		respuesta.put("supermercados", supermercadosData);
		respuesta.put("cantidadSupermercados", contadorSupermercados);

		// Promedio general CBA
		double promedioGeneral = contadorSupermercados == 0 ? 0
				: redondear2Decimales(sumaTotalesHoy / contadorSupermercados);

		respuesta.put("promedioGeneralCBA", promedioGeneral);

		respuesta.put("variacionDiariaGeneralCBA", calcularVariacion(sumHoyParaAyer, sumAyer));

		respuesta.put("variacionSemanalGeneralCBA", calcularVariacion(sumHoyParaSemana, sumSemana));

		respuesta.put("variacionMensualGeneralCBA", calcularVariacion(sumHoyParaMes, sumMes));

		respuesta.put("variacionAnualGeneralCBA", calcularVariacion(sumHoyParaAnio, sumAnio));

		return respuesta;
	}

	private double calcularTotalCanasta(Long supermercadoId, TipoCanasta tipoCanasta, LocalDate fecha) {
		List<Precio> precios = precioRepository.findByFechaAndSupermercadoAndTipoCanasta(fecha, supermercadoId,
				tipoCanasta);
		return calcularTotal(precios);
	}

	private double calcularTotalGeneral(LocalDate fecha) {
		return calcularTotal(precioRepository.findByFecha(fecha));
	}

	private double calcularTotal(List<Precio> precios) {
		if (precios == null || precios.isEmpty()) {
			return 0;
		}

		double total = precios.stream().filter(p -> p.getValor() != null).mapToDouble(Precio::getValor).sum();

		return total;
	}

	private Double calcularVariacion(Double actual, Double anterior) {
		if (actual == null || anterior == null || anterior == 0)
			return null;
		double variacion = ((actual - anterior) / anterior) * 100.0;
		return Math.round(variacion * 100.0) / 100.0;
	}

	private LocalDate obtenerUltimaFecha() {
		List<LocalDate> fechas = precioRepository.findDistinctFechas();

		if (fechas.isEmpty()) {
			return null;
		}

		return fechas.stream().max(LocalDate::compareTo).orElse(null);
	}

	@Override
	public Map<String, Object> obtenerHistorialUltimos30Dias() {

		LocalDate ultimaFecha = obtenerUltimaFecha();

		if (ultimaFecha == null) {
			return Collections.singletonMap("historialSupermercados", new ArrayList<>());
		}

		LocalDate desde = ultimaFecha.minusDays(30);

		return obtenerHistorialCanasta(desde, ultimaFecha);
	}

	@Override
	public Map<String, Object> obtenerHistorialCategoriasUltimos30Dias() {
		LocalDate ultimaFecha = precioRepository.obtenerUltimaFecha();
		if (ultimaFecha == null) {
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("desde", null);
			r.put("hasta", null);
			r.put("historialSupermercados", new ArrayList<>());
			return r;
		}
		LocalDate desde = ultimaFecha.minusDays(30);
		return obtenerHistorialCategorias(desde, ultimaFecha);
	}

	@Override
	public Map<String, Object> obtenerHistorialCategorias(LocalDate desde, LocalDate hasta) {

		Map<String, Object> respuesta = new LinkedHashMap<>();
		respuesta.put("desde", desde.toString());
		respuesta.put("hasta", hasta.toString());

		// Eje de fechas: mismas reglas que tu historial actual (fechas existentes en BD dentro del rango)
		List<LocalDate> fechas = precioRepository.findDistinctFechas().stream()
				.filter(f -> (f.equals(desde) || f.isAfter(desde)) && (f.equals(hasta) || f.isBefore(hasta)))
				.sorted()
				.collect(Collectors.toList());

		if (fechas.isEmpty()) {
			respuesta.put("historialSupermercados", new ArrayList<>());
			return respuesta;
		}

		// NUEVO: dates único para todo el payload
		respuesta.put("dates", fechas.stream().map(LocalDate::toString).collect(Collectors.toList()));

		// Traemos todos los supermercados que aparecen en precios (igual que tu historial actual)
		List<Supermercado> supermercados = precioRepository.findDistinctSupermercados().stream()
				.sorted(Comparator.comparing(Supermercado::getId))
				.collect(Collectors.toList());

		// Query grande: totales agregados por super+fecha+cat+subcat (solo CBA)
		List<Object[]> rows = precioRepository.obtenerTotalesPorSuperFechaCategoriaSubcategoria(fechas.get(0),
				fechas.get(fechas.size() - 1));

		// Index:
		// superId -> categoria -> subCategoria -> (fecha -> total)
		Map<Long, Map<String, Map<String, Map<LocalDate, Double>>>> idx = new HashMap<>();

		for (Object[] row : rows) {
			Long superId = ((Number) row[0]).longValue();

			// row[3] viene como java.sql.Date en queries nativas
			LocalDate fecha;
			Object fechaObj = row[3];
			if (fechaObj instanceof java.sql.Date) {
				fecha = ((java.sql.Date) fechaObj).toLocalDate();
			} else if (fechaObj instanceof java.sql.Timestamp) {
				fecha = ((java.sql.Timestamp) fechaObj).toLocalDateTime().toLocalDate();
			} else {
				// fallback por si el driver ya lo da como LocalDate
				fecha = (LocalDate) fechaObj;
			}

			String categoria = (String) row[4];
			String subCategoria = (String) row[5];
			Double total = row[6] != null ? ((Number) row[6]).doubleValue() : null;

			idx
					.computeIfAbsent(superId, __ -> new HashMap<>())
					.computeIfAbsent(categoria, __ -> new HashMap<>())
					.computeIfAbsent(subCategoria, __ -> new HashMap<>())
					.put(fecha, total);
		}

		List<Map<String, Object>> dataSupers = new ArrayList<>();

		for (Supermercado s : supermercados) {
			Map<String, Object> superData = new LinkedHashMap<>();
			superData.put("id", s.getId());
			superData.put("nombre", s.getNombre());
			superData.put("slug", s.getSlug());

			Map<String, Map<String, Map<LocalDate, Double>>> porCategoria = idx.getOrDefault(s.getId(),
					Collections.emptyMap());

			List<Map<String, Object>> categoriasOut = new ArrayList<>();

			// Para mantener determinismo: ordenar categorías por nombre
			List<String> categoriasOrdenadas = new ArrayList<>(porCategoria.keySet());
			Collections.sort(categoriasOrdenadas);

			for (String cat : categoriasOrdenadas) {
				Map<String, Object> catData = new LinkedHashMap<>();
				catData.put("nombre", cat);

				Map<String, Map<LocalDate, Double>> porSub = porCategoria.getOrDefault(cat, Collections.emptyMap());

				// ---- Totales de CATEGORIA (sumatoria de subcategorías por fecha) ----
				List<Double> totalsCat = new ArrayList<>(fechas.size());
				for (LocalDate fecha : fechas) {
					Double suma = 0.0;
					boolean huboAlgo = false;

					for (Map<LocalDate, Double> serieSub : porSub.values()) {
						Double v = serieSub.get(fecha);
						if (v != null) {
							suma += v;
							huboAlgo = true;
						}
					}
					totalsCat.add(huboAlgo ? redondear2Decimales(suma) : null);
				}
				catData.put("totals", totalsCat);

				// Variaciones (día contra día) usando null para faltantes
				List<Double> varsCat = new ArrayList<>(fechas.size());
				Double prev = null;
				for (Double actual : totalsCat) {
					varsCat.add(calcularVariacion(actual, prev));
					prev = actual;
				}
				catData.put("variations", varsCat);

				// ---- Subcategorías ----
				List<Map<String, Object>> subOut = new ArrayList<>();

				List<String> subOrdenadas = new ArrayList<>(porSub.keySet());
				Collections.sort(subOrdenadas);

				for (String sub : subOrdenadas) {
					Map<String, Object> subData = new LinkedHashMap<>();
					subData.put("nombre", sub);

					Map<LocalDate, Double> serie = porSub.getOrDefault(sub, Collections.emptyMap());

					List<Double> totalsSub = new ArrayList<>(fechas.size());
					for (LocalDate fecha : fechas) {
						Double v = serie.get(fecha);
						totalsSub.add(v == null ? null : redondear2Decimales(v));
					}
					subData.put("totals", totalsSub);

					List<Double> varsSub = new ArrayList<>(fechas.size());
					Double prevSub = null;
					for (Double actual : totalsSub) {
						varsSub.add(calcularVariacion(actual, prevSub));
						prevSub = actual;
					}
					subData.put("variations", varsSub);

					subOut.add(subData);
				}

				catData.put("subcategorias", subOut);
				categoriasOut.add(catData);
			}

			superData.put("categorias", categoriasOut);

			// ultimo_total: total CBA del último día = suma de todas las categorías
			LocalDate ultima = fechas.get(fechas.size() - 1);
			Double ultimoTotal = 0.0;
			boolean huboAlgoUltimo = false;

			for (Map<String, Map<LocalDate, Double>> porSub : porCategoria.values()) {
				for (Map<LocalDate, Double> serieSub : porSub.values()) {
					Double v = serieSub.get(ultima);
					if (v != null) {
						ultimoTotal += v;
						huboAlgoUltimo = true;
					}
				}
			}

			superData.put("ultimo_total", huboAlgoUltimo ? redondear2Decimales(ultimoTotal) : null);

			dataSupers.add(superData);
		}

		// Ordenar por ultimo_total asc (como tu historial actual)
		dataSupers.sort(Comparator.comparing(m -> (Double) m.get("ultimo_total"),
				Comparator.nullsLast(Double::compareTo)));

		respuesta.put("historialSupermercados", dataSupers);
		return respuesta;
	}

	private double redondear2Decimales(double valor) {
		return Math.round(valor * 100.0) / 100.0;
	}
}