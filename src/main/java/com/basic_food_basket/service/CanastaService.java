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


    /*
    private Map<String, Object> obtenerResumenPorSupermercado() {
        Map<String, Object> respuesta = new LinkedHashMap<>();

        // 1. Lógica para encontrar la última fecha con datos (Evita el error de "todo vacío" si hoy no es el día de carga)
        List<LocalDate> fechasDisponibles = precioRepository.findDistinctFechas();
        
        if (fechasDisponibles.isEmpty()) {
            respuesta.put("mensaje", "No hay datos cargados en el sistema");
            return respuesta;
        }

        // Tomamos la fecha más reciente disponible
        LocalDate hoy = fechasDisponibles.stream()
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        List<Precio> preciosDelDia = precioRepository.findByFechaWithRelations(hoy);

        Map<Supermercado, Map<TipoCanasta, List<Precio>>> preciosAgrupados = preciosDelDia.stream()
            .collect(Collectors.groupingBy(
                p -> p.getProducto().getSupermercado(),
                Collectors.groupingBy(p -> p.getProducto().getTipoCanasta())
            ));

        // 2. Definición de fechas históricas relativas a la fecha encontrada
        LocalDate ayer = hoy.minus(1, ChronoUnit.DAYS);
        LocalDate semanaPasada = hoy.minus(7, ChronoUnit.DAYS);
        LocalDate mesPasado = hoy.minus(30, ChronoUnit.DAYS);
        LocalDate añoPasado = hoy.minus(365, ChronoUnit.DAYS);

        List<Map<String, Object>> supermercadosData = new ArrayList<>();
        int contadorSupermercados = 0;

        // 3. Inicialización de Acumuladores
        // Suma total absoluta (para promedios de precios actuales)
        Map<TipoCanasta, Double> sumaTotalesHoy = new EnumMap<>(TipoCanasta.class);

        // Acumuladores "Espejo" para variaciones (Solo suman si el super existe en ambas fechas)
        Map<TipoCanasta, Double> sumHoyParaAyer = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumAyer = new EnumMap<>(TipoCanasta.class);

        Map<TipoCanasta, Double> sumHoyParaSemana = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumSemana = new EnumMap<>(TipoCanasta.class);

        Map<TipoCanasta, Double> sumHoyParaMes = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumMes = new EnumMap<>(TipoCanasta.class);

        Map<TipoCanasta, Double> sumHoyParaAnio = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumAnio = new EnumMap<>(TipoCanasta.class);

        // Poner todo en 0.0 para evitar NullPointer
        for (TipoCanasta tipo : TipoCanasta.values()) {
            sumaTotalesHoy.put(tipo, 0.0);
            
            sumHoyParaAyer.put(tipo, 0.0); sumAyer.put(tipo, 0.0);
            sumHoyParaSemana.put(tipo, 0.0); sumSemana.put(tipo, 0.0);
            sumHoyParaMes.put(tipo, 0.0); sumMes.put(tipo, 0.0);
            sumHoyParaAnio.put(tipo, 0.0); sumAnio.put(tipo, 0.0);
        }

        // 4. Iteración de Supermercados
        for (Map.Entry<Supermercado, Map<TipoCanasta, List<Precio>>> entry : preciosAgrupados.entrySet()) {
            Supermercado supermercado = entry.getKey();
            Map<String, Object> supermercadoData = new LinkedHashMap<>();
            supermercadoData.put("id", supermercado.getId());
            supermercadoData.put("nombre", supermercado.getNombre());
            supermercadoData.put("slug", supermercado.getSlug());

            List<Map<String, Object>> canastasData = new ArrayList<>();
            double sumaTotalesCanastasSuper = 0;

            for (Map.Entry<TipoCanasta, List<Precio>> canastaEntry : entry.getValue().entrySet()) {
                TipoCanasta tipoCanasta = canastaEntry.getKey();
                List<Precio> precios = canastaEntry.getValue();

                double totalCanastaHoy = calcularTotal(precios);
                sumaTotalesCanastasSuper += totalCanastaHoy;

                // A) Sumar al total general de HOY (Independientemente de si es nuevo o viejo)
                sumaTotalesHoy.put(tipoCanasta, sumaTotalesHoy.get(tipoCanasta) + totalCanastaHoy);

                // B) Lógica de Filtrado para Variaciones (Intersección de fechas)
                
                // --- Variación Diaria ---
                double totalAyer = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(ayer, supermercado.getId(), tipoCanasta));
                if (totalAyer > 0) { // Solo si existía ayer
                    sumHoyParaAyer.put(tipoCanasta, sumHoyParaAyer.get(tipoCanasta) + totalCanastaHoy);
                    sumAyer.put(tipoCanasta, sumAyer.get(tipoCanasta) + totalAyer);
                }

                // --- Variación Semanal ---
                double totalSem = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(semanaPasada, supermercado.getId(), tipoCanasta));
                if (totalSem > 0) { // Solo si existía la semana pasada (Carrefour no entrará aquí)
                    sumHoyParaSemana.put(tipoCanasta, sumHoyParaSemana.get(tipoCanasta) + totalCanastaHoy);
                    sumSemana.put(tipoCanasta, sumSemana.get(tipoCanasta) + totalSem);
                }

                // --- Variación Mensual ---
                double totalMes = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(mesPasado, supermercado.getId(), tipoCanasta));
                if (totalMes > 0) { 
                    sumHoyParaMes.put(tipoCanasta, sumHoyParaMes.get(tipoCanasta) + totalCanastaHoy);
                    sumMes.put(tipoCanasta, sumMes.get(tipoCanasta) + totalMes);
                }

                // --- Variación Anual ---
                double totalAnio = calcularTotal(precioRepository.findByFechaAndSupermercadoAndTipoCanasta(añoPasado, supermercado.getId(), tipoCanasta));
                if (totalAnio > 0) {
                    sumHoyParaAnio.put(tipoCanasta, sumHoyParaAnio.get(tipoCanasta) + totalCanastaHoy);
                    sumAnio.put(tipoCanasta, sumAnio.get(tipoCanasta) + totalAnio);
                }

                // C) Datos individuales del supermercado (Aquí sí mostramos sus variaciones propias o null)
                Map<String, Object> canastaData = new LinkedHashMap<>();
               // canastaData.put("tipo", tipoCanasta.name());
                canastaData.put("total", redondear2Decimales(totalCanastaHoy));
                canastaData.put("productos", precios.size());
               // canastaData.put("promedioProducto", precios.isEmpty() ? 0 : redondear2Decimales(totalCanastaHoy / precios.size()));

                canastaData.put("variacionDiaria", calcularVariacion(totalCanastaHoy, totalAyer));
                canastaData.put("variacionSemanal", calcularVariacion(totalCanastaHoy, totalSem));
                canastaData.put("variacionMensual", calcularVariacion(totalCanastaHoy, totalMes));
                canastaData.put("variacionAnual", calcularVariacion(totalCanastaHoy, totalAnio));

                canastasData.add(canastaData);
            }

            supermercadoData.put("canastas", canastasData);
            
            supermercadoData.put("promedioSupermercado",
                canastasData.isEmpty() ? 0 :
                    redondear2Decimales(sumaTotalesCanastasSuper / canastasData.size()));
            
            supermercadosData.add(supermercadoData);
            contadorSupermercados++;
        }

        respuesta.put("fecha", hoy.toString());
        respuesta.put("supermercados", supermercadosData);
        respuesta.put("cantidadSupermercados", contadorSupermercados);

        // 5. Cálculos Generales Finales
        for (TipoCanasta tipo : TipoCanasta.values()) {
            String nombre = tipo.name();

            // Promedio General de Precios (Usa todos los supers disponibles hoy)
            Double promedioGeneral = contadorSupermercados == 0 ? 0 : 
                                     redondear2Decimales(sumaTotalesHoy.get(tipo) / contadorSupermercados);
            
            respuesta.put("promedioGeneral" + nombre, promedioGeneral);

            // Variaciones Generales (Usan los acumuladores filtrados)
            respuesta.put("variacionDiariaGeneral" + nombre, 
                calcularVariacion(sumHoyParaAyer.get(tipo), sumAyer.get(tipo)));
                
            respuesta.put("variacionSemanalGeneral" + nombre, 
                calcularVariacion(sumHoyParaSemana.get(tipo), sumSemana.get(tipo)));
                
            respuesta.put("variacionMensualGeneral" + nombre, 
                calcularVariacion(sumHoyParaMes.get(tipo), sumMes.get(tipo)));
                
            respuesta.put("variacionAnualGeneral" + nombre, 
                calcularVariacion(sumHoyParaAnio.get(tipo), sumAnio.get(tipo)));
        }

        return respuesta;
    }
    */
    private Map<String, Object> obtenerResumenPorSupermercado() {

        Map<String, Object> respuesta = new LinkedHashMap<>();

        // 1. Buscar última fecha con datos
        List<LocalDate> fechasDisponibles = precioRepository.findDistinctFechas();

        if (fechasDisponibles.isEmpty()) {
            respuesta.put("mensaje", "No hay datos cargados en el sistema");
            return respuesta;
        }

        LocalDate hoy = fechasDisponibles.stream()
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        List<Precio> preciosDelDia = precioRepository.findByFechaWithRelations(hoy);

        // Agrupar SOLO por supermercado (ya no por tipo)
        Map<Supermercado, List<Precio>> preciosAgrupados = preciosDelDia.stream()
                .filter(p -> p.getProducto().getTipoCanasta() == TipoCanasta.CBA)
                .collect(Collectors.groupingBy(
                        p -> p.getProducto().getSupermercado()
                ));

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
            double totalAyer = calcularTotal(
                    precioRepository.findByFechaAndSupermercadoAndTipoCanasta(
                            ayer, supermercado.getId(), TipoCanasta.CBA
                    )
            );
            if (totalAyer > 0) {
                sumHoyParaAyer += totalHoy;
                sumAyer += totalAyer;
            }

            // --- Variación semanal ---
            double totalSemana = calcularTotal(
                    precioRepository.findByFechaAndSupermercadoAndTipoCanasta(
                            semanaPasada, supermercado.getId(), TipoCanasta.CBA
                    )
            );
            if (totalSemana > 0) {
                sumHoyParaSemana += totalHoy;
                sumSemana += totalSemana;
            }

            // --- Variación mensual ---
            double totalMes = calcularTotal(
                    precioRepository.findByFechaAndSupermercadoAndTipoCanasta(
                            mesPasado, supermercado.getId(), TipoCanasta.CBA
                    )
            );
            if (totalMes > 0) {
                sumHoyParaMes += totalHoy;
                sumMes += totalMes;
            }

            // --- Variación anual ---
            double totalAnio = calcularTotal(
                    precioRepository.findByFechaAndSupermercadoAndTipoCanasta(
                            añoPasado, supermercado.getId(), TipoCanasta.CBA
                    )
            );
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
        respuesta.put("fecha", hoy.toString());
        respuesta.put("supermercados", supermercadosData);
        respuesta.put("cantidadSupermercados", contadorSupermercados);

        // Promedio general CBA
        double promedioGeneral = contadorSupermercados == 0
                ? 0
                : redondear2Decimales(sumaTotalesHoy / contadorSupermercados);

        respuesta.put("promedioGeneralCBA", promedioGeneral);

        respuesta.put("variacionDiariaGeneralCBA",
                calcularVariacion(sumHoyParaAyer, sumAyer));

        respuesta.put("variacionSemanalGeneralCBA",
                calcularVariacion(sumHoyParaSemana, sumSemana));

        respuesta.put("variacionMensualGeneralCBA",
                calcularVariacion(sumHoyParaMes, sumMes));

        respuesta.put("variacionAnualGeneralCBA",
                calcularVariacion(sumHoyParaAnio, sumAnio));

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
