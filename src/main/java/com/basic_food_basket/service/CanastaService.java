package com.basic_food_basket.service;

import com.basic_food_basket.model.*;
import com.basic_food_basket.repository.PrecioRepository;
import com.basic_food_basket.repository.ProductoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CanastaService implements ICanastaService {

    @Autowired
    private PrecioRepository precioRepository;
    
    @Autowired
    private ProductoRepository productoRepository;
    
    @Override
    public Map<String, Object> obtenerUltimosPreciosPorSupermercado() {
        Map<String, Object> respuesta = new LinkedHashMap<>();

        Optional<Precio> precioMasRecienteOpt = precioRepository.findTopByOrderByFechaDesc();
        String ultimaFecha = precioMasRecienteOpt.map(precio -> precio.getFecha().toString()).orElse(null);

        List<Map<String, Object>> fechas = new ArrayList<>();
        Map<String, Object> fechaMap = new LinkedHashMap<>();
        fechaMap.put("fecha", ultimaFecha);
        fechas.add(fechaMap);

        respuesta.put("fecha", fechas);

        List<Supermercado> supermercados = productoRepository.findAll()
                .stream().map(Producto::getSupermercado)
                .distinct()
                .sorted(Comparator.comparing(Supermercado::getId))
                .collect(Collectors.toList());

        List<Map<String, Object>> dataSupermercados = new ArrayList<>();

        for (Supermercado supermercado : supermercados) {
            Map<String, Object> supermercadoData = new LinkedHashMap<>();
            supermercadoData.put("nombre", supermercado.getNombre());

            List<Producto> productos = productoRepository.findBySupermercado(supermercado);

            List<Map<String, Object>> productosData = new ArrayList<>();
            double totalSupermercado = 0.0;
            
            for (Producto producto : productos) {
                Optional<Precio> precioOpt = precioRepository.findLastScrapeadoByProducto(producto.getId());
                if (precioOpt.isPresent()) {
                    Precio precio = precioOpt.get();
                    Map<String, Object> productoData = new LinkedHashMap<>();
                    productoData.put("nombre", producto.getNombre());
                    productoData.put("precio", precio.getValor());
                    productosData.add(productoData);
                    
                    // Sumar al total del supermercado
                    totalSupermercado += precio.getValor();
                }
            }
            
            supermercadoData.put("productos", productosData);
            supermercadoData.put("total", totalSupermercado); // Agregar el total al supermercado
            dataSupermercados.add(supermercadoData);
        }
        respuesta.put("supermercados", dataSupermercados);
        return respuesta;
    }
   /* @Override
    public Map<String, Object> obtenerUltimosPreciosPorSupermercado() {
        Map<String, Object> respuesta = new LinkedHashMap<>();

        Optional<Precio> precioMasRecienteOpt = precioRepository.findTopByOrderByFechaDesc();
        String ultimaFecha = precioMasRecienteOpt.map(precio -> precio.getFecha().toString()).orElse(null);

        List<Map<String, Object>> fechas = new ArrayList<>();
        Map<String, Object> fechaMap = new LinkedHashMap<>();
        fechaMap.put("fecha", ultimaFecha);
        fechas.add(fechaMap);

        respuesta.put("fecha", fechas);

        List<Supermercado> supermercados = productoRepository.findAll()
                .stream().map(Producto::getSupermercado)
                .distinct()
                .sorted(Comparator.comparing(Supermercado::getId))
                .collect(Collectors.toList());

        List<Map<String, Object>> dataSupermercados = new ArrayList<>();

        for (Supermercado supermercado : supermercados) {
            Map<String, Object> supermercadoData = new LinkedHashMap<>();
            supermercadoData.put("nombre", supermercado.getNombre());

            List<Producto> productos = productoRepository.findBySupermercado(supermercado);

            List<Map<String, Object>> productosData = new ArrayList<>();
            for (Producto producto : productos) {
                Optional<Precio> precioOpt = precioRepository.findLastScrapeadoByProducto(producto.getId());
                if (precioOpt.isPresent()) {
                    Precio precio = precioOpt.get();
                    Map<String, Object> productoData = new LinkedHashMap<>();
                    productoData.put("nombre", producto.getNombre());
                    productoData.put("precio", precio.getValor());
                    productosData.add(productoData);
                }
            }
            supermercadoData.put("productos", productosData);
            dataSupermercados.add(supermercadoData);
        }
        respuesta.put("supermercados", dataSupermercados);
        return respuesta;
    }*/
    

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
        List<LocalDate> fechas = precioRepository.findDistinctFechas().stream()
                .filter(f -> (f.equals(desde) || f.isAfter(desde)) && (f.equals(hasta) || f.isBefore(hasta)))
                .sorted()
                .collect(Collectors.toList());
        
        List<Supermercado> supermercados = precioRepository.findDistinctSupermercados().stream()
                                                .sorted(Comparator.comparing(Supermercado::getId))
                                                .collect(Collectors.toList());

        List<Map<String, Object>> data = new ArrayList<>();

        for (Supermercado supermercado : supermercados) {
            Map<String, Object> supermercadoData = new LinkedHashMap<>();
            supermercadoData.put("id", supermercado.getId());
            supermercadoData.put("nombre", supermercado.getNombre());
            supermercadoData.put("slug", supermercado.getSlug());

            List<Map<String, Object>> historial = new ArrayList<>();

            for (LocalDate fecha : fechas) {
                Map<String, Object> registro = new LinkedHashMap<>();
                registro.put("fecha", Arrays.asList(fecha.getYear(), fecha.getMonthValue(), fecha.getDayOfMonth()));

                // SOLO CBA
                for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
                    double total = calcularTotal(
                        precioRepository.findByFechaAndSupermercadoAndTipoCanasta(
                            fecha, supermercado.getId(), tipo
                        )
                    );
                    registro.put("total_" + tipo.name(), redondear2Decimales(total));
                }
                historial.add(registro);
            }
            supermercadoData.put("historial", historial);
            data.add(supermercadoData);
        }
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
                .filter(f -> (f.equals(desde) || f.isAfter(desde)) && (f.equals(hasta) || f.isBefore(hasta)))
                .sorted()
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
                registro.put("promedioProducto_" + tipo.name(), precios.isEmpty() ? 0 : redondear2Decimales(total / precios.size()));
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
                                                .sorted(Comparator.comparing(Supermercado::getId))
                                                .collect(Collectors.toList());
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
                double totalHoy = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(hoy, supermercado.getId(), tipo));
                sumaTotalesCanasta.put(tipo, sumaTotalesCanasta.get(tipo) + totalHoy);

                double totalAyer = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(ayer, supermercado.getId(), tipo));
                sumaTotalesCanastaAyer.put(tipo, sumaTotalesCanastaAyer.get(tipo) + totalAyer);

                double totalSemanal = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(semanaPasada, supermercado.getId(), tipo));
                sumaTotalesCanastaSemanal.put(tipo, sumaTotalesCanastaSemanal.get(tipo) + totalSemanal);

                double totalMensual = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(mesPasado, supermercado.getId(), tipo));
                sumaTotalesCanastaMensual.put(tipo, sumaTotalesCanastaMensual.get(tipo) + totalMensual);

                double totalAnual = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(anioPasado, supermercado.getId(), tipo));
                sumaTotalesCanastaAnual.put(tipo, sumaTotalesCanastaAnual.get(tipo) + totalAnual);
            }
        }

        for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
            String nombre = tipo.name();
            Double promedioGeneral = supermercados.isEmpty() ? 0 : redondear2Decimales(sumaTotalesCanasta.get(tipo) / supermercados.size());
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
        LocalDate fecha = obtenerUltimaFechaConPrecios();
        if (fecha == null) {
            respuesta.put("fecha", LocalDate.now().toString());
            respuesta.put("supermercados", new ArrayList<>());
            respuesta.put("cantidadSupermercados", 0);
            return respuesta;
        }

        List<Precio> preciosDelDia = precioRepository.findByFechaWithRelations(fecha);

        List<Supermercado> supermercadosOrdenados = precioRepository.findDistinctSupermercados().stream()
                                                    .sorted(Comparator.comparing(Supermercado::getId))
                                                    .collect(Collectors.toList());

        Map<Supermercado, Map<TipoCanasta, List<Precio>>> preciosAgrupados = preciosDelDia.stream()
            .collect(Collectors.groupingBy(
                p -> p.getProducto().getSupermercado(),
                Collectors.groupingBy(p -> p.getProducto().getTipoCanasta())
            ));

        LocalDate ayer = fecha.minus(1, ChronoUnit.DAYS);
        LocalDate semanaPasada = fecha.minus(7, ChronoUnit.DAYS);
        LocalDate mesPasado = fecha.minus(30, ChronoUnit.DAYS);
        LocalDate añoPasado = fecha.minus(365, ChronoUnit.DAYS);

        List<Map<String, Object>> supermercadosData = new ArrayList<>();
        int contadorSupermercados = 0;

        Map<TipoCanasta, Double> sumaTotalesPorCanasta = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesPorCanastaAyer = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesPorCanastaSem = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesPorCanastaMes = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesPorCanastaAnio = new EnumMap<>(TipoCanasta.class);

        // SOLO CBA
        for (TipoCanasta tipo : Collections.singletonList(TipoCanasta.CBA)) {
            sumaTotalesPorCanasta.put(tipo, 0.0);
            sumaTotalesPorCanastaAyer.put(tipo, 0.0);
            sumaTotalesPorCanastaSem.put(tipo, 0.0);
            sumaTotalesPorCanastaMes.put(tipo, 0.0);
            sumaTotalesPorCanastaAnio.put(tipo, 0.0);
        }

        for (Supermercado supermercado : supermercadosOrdenados) {
            Map<String, Object> supermercadoData = new LinkedHashMap<>();
            supermercadoData.put("id", supermercado.getId());
            supermercadoData.put("nombre", supermercado.getNombre());
            supermercadoData.put("slug", supermercado.getSlug());

            List<Map<String, Object>> canastasData = new ArrayList<>();
            double sumaTotalesCanastasSuper = 0;

            Map<TipoCanasta, List<Precio>> canastasDelSupermercado = preciosAgrupados.getOrDefault(supermercado, Collections.emptyMap());

            // SOLO CBA
            for (TipoCanasta tipoCanasta : Collections.singletonList(TipoCanasta.CBA)) {
                List<Precio> precios = canastasDelSupermercado.getOrDefault(tipoCanasta, Collections.emptyList());

                double totalCanasta = calcularTotal(precios);
                sumaTotalesCanastasSuper += totalCanasta;

                sumaTotalesPorCanasta.put(tipoCanasta,
                    sumaTotalesPorCanasta.getOrDefault(tipoCanasta, 0.0) + totalCanasta);

                double totalAyer = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(ayer, supermercado.getId(), tipoCanasta));
                sumaTotalesPorCanastaAyer.put(tipoCanasta,
                    sumaTotalesPorCanastaAyer.getOrDefault(tipoCanasta, 0.0) + totalAyer);

                double totalSem = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(semanaPasada, supermercado.getId(), tipoCanasta));
                sumaTotalesPorCanastaSem.put(tipoCanasta,
                    sumaTotalesPorCanastaSem.getOrDefault(tipoCanasta, 0.0) + totalSem);

                double totalMes = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(mesPasado, supermercado.getId(), tipoCanasta));
                sumaTotalesPorCanastaMes.put(tipoCanasta,
                    sumaTotalesPorCanastaMes.getOrDefault(tipoCanasta, 0.0) + totalMes);

                double totalAnio = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(añoPasado, supermercado.getId(), tipoCanasta));
                sumaTotalesPorCanastaAnio.put(tipoCanasta,
                    sumaTotalesPorCanastaAnio.getOrDefault(tipoCanasta, 0.0) + totalAnio);

                Map<String, Object> canastaData = new LinkedHashMap<>();
                canastaData.put("tipo", tipoCanasta.name());
                canastaData.put("total", redondear2Decimales(totalCanasta));
                canastaData.put("productos", precios.size());
                canastaData.put("promedioProducto", precios.isEmpty() ? 0 : redondear2Decimales(totalCanasta / precios.size()));

                canastaData.put("variacionDiaria", calcularVariacion(
                    totalCanasta,
                    calcularTotalCanasta(supermercado.getId(), tipoCanasta, ayer)
                ));

                canastaData.put("variacionSemanal", calcularVariacion(
                    totalCanasta,
                    calcularTotalCanasta(supermercado.getId(), tipoCanasta, semanaPasada)
                ));

                canastaData.put("variacionMensual", calcularVariacion(
                    totalCanasta,
                    calcularTotalCanasta(supermercado.getId(), tipoCanasta, mesPasado)
                ));

                canastaData.put("variacionAnual", calcularVariacion(
                    totalCanasta,
                    calcularTotalCanasta(supermercado.getId(), tipoCanasta, añoPasado)
                ));

                canastasData.add(canastaData);
            }

            supermercadoData.put("canastas", canastasData);
            supermercadoData.put("promedioSupermercado",
                canastasData.isEmpty() ? 0 :
                    redondear2Decimales(sumaTotalesCanastasSuper / canastasData.size()));

            supermercadosData.add(supermercadoData);
            contadorSupermercados++;
        }

        respuesta.put("fecha", fecha.toString());
        respuesta.put("supermercados", supermercadosData);
        respuesta.put("cantidadSupermercados", contadorSupermercados);

        // Promedios generales por canasta: suma de totales de canasta / cantidad de supermercados
        for (TipoCanasta tipoCanasta : Collections.singletonList(TipoCanasta.CBA)) {
            int cuenta = contadorSupermercados;
            respuesta.put("promedioGeneral" + tipoCanasta.name(),
                cuenta == 0 ? null : redondear2Decimales(sumaTotalesPorCanasta.get(tipoCanasta) / cuenta));
        }

        // Variaciones generales por canasta (SOLO CBA)
        respuesta.put("variacionDiariaGeneralCBA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CBA, 0.0), sumaTotalesPorCanastaAyer.getOrDefault(TipoCanasta.CBA, 0.0)));
        respuesta.put("variacionSemanalGeneralCBA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CBA, 0.0), sumaTotalesPorCanastaSem.getOrDefault(TipoCanasta.CBA, 0.0)));
        respuesta.put("variacionMensualGeneralCBA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CBA, 0.0), sumaTotalesPorCanastaMes.getOrDefault(TipoCanasta.CBA, 0.0)));
        respuesta.put("variacionAnualGeneralCBA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CBA, 0.0), sumaTotalesPorCanastaAnio.getOrDefault(TipoCanasta.CBA, 0.0)));

        // Eliminar los campos CPA

        return respuesta;
    }

    private double calcularTotalCanasta(Long supermercadoId, TipoCanasta tipoCanasta, LocalDate fecha) {
        List<Precio> precios = precioRepository.findByFechaAndSupermercadoAndTipoCanasta(
            fecha, supermercadoId, tipoCanasta);
        return calcularTotal(precios);
    }

    private double calcularTotalGeneral(LocalDate fecha) {
        return calcularTotal(precioRepository.findByFecha(fecha));
    }

    private double calcularTotal(List<Precio> precios) {
        if (precios == null || precios.isEmpty()) {
            return 0;
        }

        double total = precios.stream()
                .filter(p -> p.getValor() != null)
                .mapToDouble(Precio::getValor)
                .sum();

        return total;
    }

    private Double calcularVariacion(Double actual, Double anterior) {
        if (actual == null || anterior == null || anterior == 0) return null;
        double variacion = ((actual - anterior) / anterior) * 100.0;
        return Math.round(variacion * 100.0) / 100.0;
    }

    private double redondear2Decimales(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}