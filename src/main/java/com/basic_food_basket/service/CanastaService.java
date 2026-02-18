package com.basic_food_basket.service;

import com.basic_food_basket.model.*;
import com.basic_food_basket.repository.PrecioRepository;
import com.basic_food_basket.repository.ProductoRepository;

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
    
    @Autowired
    private ProductoRepository productoRepository;
        
    
   
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
        List<Precio> preciosDelDia =
                precioRepository.findByFechaWithProductoAndSupermercado(ultimaFecha);

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
/*
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
*/

    public Map<String, Object> obtenerHistorialCanasta(LocalDate desde, LocalDate hasta) {

    Map<String, Object> respuesta = new LinkedHashMap<>();

    // 1. Fechas disponibles en rango
    List<LocalDate> fechas = precioRepository.findDistinctFechas().stream()
            .filter(f -> (f.equals(desde) || f.isAfter(desde)) &&
                         (f.equals(hasta) || f.isBefore(hasta)))
            .sorted()
            .collect(Collectors.toList());

    // 2. Supermercados ordenados por ID
    List<Supermercado> supermercados = precioRepository.findDistinctSupermercados()
            .stream()
            .sorted(Comparator.comparing(Supermercado::getId))
            .collect(Collectors.toList());

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

            double total = calcularTotal(
                    precioRepository.findByFechaAndSupermercadoAndTipoCanasta(
                            fecha,
                            supermercado.getId(),
                            TipoCanasta.CBA
                    )
            );

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        respuesta.put("fecha", hoy.format(formatter));
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
    private LocalDate obtenerUltimaFecha() {
    List<LocalDate> fechas = precioRepository.findDistinctFechas();

    if (fechas.isEmpty()) {
        return null;
    }

    return fechas.stream()
            .max(LocalDate::compareTo)
            .orElse(null);
}
public List<Map<String, Object>> obtenerHistorialUltimos30Dias() {

    LocalDate ultimaFecha = obtenerUltimaFecha();

    if (ultimaFecha == null) {
        return Collections.emptyList();
    }

    LocalDate desde = ultimaFecha.minusDays(30);

    return precioRepository.obtenerHistorialTotales(desde, ultimaFecha);
}


}
