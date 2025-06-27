/*
package com.portfolio.backend.service;

import com.portfolio.backend.model.*;
import com.portfolio.backend.repository.PrecioRepository;
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

    @Override
    public Map<String, Object> obtenerResumenCanasta() {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        LocalDate hoy = LocalDate.now();

        List<Precio> preciosDelDia = precioRepository.findByFechaWithRelations(hoy);

        Map<Supermercado, Map<TipoCanasta, List<Precio>>> preciosAgrupados = preciosDelDia.stream()
            .collect(Collectors.groupingBy(
                p -> p.getProducto().getSupermercado(),
                Collectors.groupingBy(p -> p.getProducto().getTipoCanasta())
            ));

        LocalDate ayer = hoy.minus(1, ChronoUnit.DAYS);
        LocalDate semanaPasada = hoy.minus(7, ChronoUnit.DAYS);
        LocalDate mesPasado = hoy.minus(30, ChronoUnit.DAYS);
        LocalDate añoPasado = hoy.minus(365, ChronoUnit.DAYS);

        List<Map<String, Object>> supermercadosData = new ArrayList<>();
        double granTotalHoy = 0;
        int contadorSupermercados = 0;

        for (Map.Entry<Supermercado, Map<TipoCanasta, List<Precio>>> entry : preciosAgrupados.entrySet()) {
            Supermercado supermercado = entry.getKey();
            Map<String, Object> supermercadoData = new LinkedHashMap<>();
            supermercadoData.put("id", supermercado.getId());
            supermercadoData.put("nombre", supermercado.getNombre());
            supermercadoData.put("slug", supermercado.getSlug());

            List<Map<String, Object>> canastasData = new ArrayList<>();
            double totalSupermercado = 0;

            for (Map.Entry<TipoCanasta, List<Precio>> canastaEntry : entry.getValue().entrySet()) {
                TipoCanasta tipoCanasta = canastaEntry.getKey();
                List<Precio> precios = canastaEntry.getValue();

                double totalCanasta = calcularTotal(precios);
                totalSupermercado += totalCanasta;

                Map<String, Object> canastaData = new LinkedHashMap<>();
                canastaData.put("tipo", tipoCanasta.name());
                canastaData.put("total", redondear2Decimales(totalCanasta));
                canastaData.put("productos", precios.size());
                canastaData.put("promedio", precios.isEmpty() ? 0 : redondear2Decimales(totalCanasta / precios.size()));

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
            supermercadoData.put("totalSupermercado", redondear2Decimales(totalSupermercado));
            supermercadoData.put("promedioSupermercado", 
                canastasData.isEmpty() ? 0 : redondear2Decimales(totalSupermercado / canastasData.size()));

            supermercadosData.add(supermercadoData);
            granTotalHoy += totalSupermercado;
            contadorSupermercados++;
        }

        double promedioGeneral = contadorSupermercados == 0 ? 0 : granTotalHoy / contadorSupermercados;

        respuesta.put("fecha", hoy.toString());
        respuesta.put("supermercados", supermercadosData);
        respuesta.put("totalGeneral", redondear2Decimales(granTotalHoy));
        respuesta.put("promedioGeneral", redondear2Decimales(promedioGeneral));
        respuesta.put("cantidadSupermercados", contadorSupermercados);

        respuesta.put("variacionDiariaGeneral", calcularVariacion(
            granTotalHoy,
            calcularTotalGeneral(ayer)
        ));

        respuesta.put("variacionSemanalGeneral", calcularVariacion(
            granTotalHoy,
            calcularTotalGeneral(semanaPasada)
        ));

        respuesta.put("variacionMensualGeneral", calcularVariacion(
            granTotalHoy,
            calcularTotalGeneral(mesPasado)
        ));

        respuesta.put("variacionAnualGeneral", calcularVariacion(
            granTotalHoy,
            calcularTotalGeneral(añoPasado)
        ));

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
        if (precios == null || precios.isEmpty()) return 0;
        return precios.stream()
                .filter(p -> p.getValor() != null)
                .mapToDouble(Precio::getValor)
                .sum();
    }

    private Double calcularVariacion(Double actual, Double anterior) {
        if (actual == null || anterior == null || anterior == 0) return null;
        double variacion = ((actual - anterior) / anterior) * 100.0;
        return Math.round(variacion * 100.0) / 100.0; // 2 decimales
    }

    private double redondear2Decimales(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}*/
package com.portfolio.backend.service;

import com.portfolio.backend.model.*;
import com.portfolio.backend.repository.PrecioRepository;
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

    @Override
    public Map<String, Object> obtenerResumenCanasta() {
        return obtenerResumenPorSupermercado();
    }

    // Historial supermercado con rango de fechas
    public Map<String, Object> obtenerHistorialCanasta(LocalDate desde, LocalDate hasta) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        List<LocalDate> fechas = precioRepository.findDistinctFechas().stream()
                .filter(f -> (f.equals(desde) || f.isAfter(desde)) && (f.equals(hasta) || f.isBefore(hasta)))
                .sorted()
                .collect(Collectors.toList());
        List<Supermercado> supermercados = precioRepository.findDistinctSupermercados();

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

                for (TipoCanasta tipo : TipoCanasta.values()) {
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
        // Llama al nuevo método con todo el rango de fechas disponibles
        List<LocalDate> fechas = precioRepository.findDistinctFechas();
        if (fechas.isEmpty()) {
            return Collections.singletonMap("historialSupermercados", new ArrayList<>());
        }
        LocalDate desde = fechas.stream().min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate hasta = fechas.stream().max(LocalDate::compareTo).orElse(LocalDate.now());
        return obtenerHistorialCanasta(desde, hasta);
    }

    // Historial general con rango de fechas
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

            for (TipoCanasta tipo : TipoCanasta.values()) {
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
        // Llama al nuevo método con todo el rango de fechas disponibles
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
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minus(1, ChronoUnit.DAYS);
        LocalDate semanaPasada = hoy.minus(7, ChronoUnit.DAYS);
        LocalDate mesPasado = hoy.minus(30, ChronoUnit.DAYS);
        LocalDate anioPasado = hoy.minus(365, ChronoUnit.DAYS);

        List<Supermercado> supermercados = precioRepository.findDistinctSupermercados();
        Map<TipoCanasta, Double> sumaTotalesCanasta = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesCanastaAyer = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesCanastaSemanal = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesCanastaMensual = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesCanastaAnual = new EnumMap<>(TipoCanasta.class);

        for (TipoCanasta tipo : TipoCanasta.values()) {
            sumaTotalesCanasta.put(tipo, 0.0);
            sumaTotalesCanastaAyer.put(tipo, 0.0);
            sumaTotalesCanastaSemanal.put(tipo, 0.0);
            sumaTotalesCanastaMensual.put(tipo, 0.0);
            sumaTotalesCanastaAnual.put(tipo, 0.0);
        }

        for (Supermercado supermercado : supermercados) {
            for (TipoCanasta tipo : TipoCanasta.values()) {
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

        for (TipoCanasta tipo : TipoCanasta.values()) {
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
        LocalDate hoy = LocalDate.now();

        List<Precio> preciosDelDia = precioRepository.findByFechaWithRelations(hoy);

        Map<Supermercado, Map<TipoCanasta, List<Precio>>> preciosAgrupados = preciosDelDia.stream()
            .collect(Collectors.groupingBy(
                p -> p.getProducto().getSupermercado(),
                Collectors.groupingBy(p -> p.getProducto().getTipoCanasta())
            ));

        LocalDate ayer = hoy.minus(1, ChronoUnit.DAYS);
        LocalDate semanaPasada = hoy.minus(7, ChronoUnit.DAYS);
        LocalDate mesPasado = hoy.minus(30, ChronoUnit.DAYS);
        LocalDate añoPasado = hoy.minus(365, ChronoUnit.DAYS);

        List<Map<String, Object>> supermercadosData = new ArrayList<>();
        int contadorSupermercados = 0;

        // Suma de totales de cada canasta globales y para variaciones
        Map<TipoCanasta, Double> sumaTotalesPorCanasta = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesPorCanastaAyer = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesPorCanastaSem = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesPorCanastaMes = new EnumMap<>(TipoCanasta.class);
        Map<TipoCanasta, Double> sumaTotalesPorCanastaAnio = new EnumMap<>(TipoCanasta.class);

        for (TipoCanasta tipo : TipoCanasta.values()) {
            sumaTotalesPorCanasta.put(tipo, 0.0);
            sumaTotalesPorCanastaAyer.put(tipo, 0.0);
            sumaTotalesPorCanastaSem.put(tipo, 0.0);
            sumaTotalesPorCanastaMes.put(tipo, 0.0);
            sumaTotalesPorCanastaAnio.put(tipo, 0.0);
        }

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

                double totalCanasta = calcularTotal(precios);
                sumaTotalesCanastasSuper += totalCanasta;

                // Suma globales por canasta para HOY
                sumaTotalesPorCanasta.put(tipoCanasta,
                    sumaTotalesPorCanasta.getOrDefault(tipoCanasta, 0.0) + totalCanasta);

                // Suma globales por canasta para AYER, SEMANA, MES, AÑO
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

        respuesta.put("fecha", hoy.toString());
        respuesta.put("supermercados", supermercadosData);
        respuesta.put("cantidadSupermercados", contadorSupermercados);

        // Promedios generales por canasta: suma de totales de canasta / cantidad de supermercados
        for (TipoCanasta tipoCanasta : TipoCanasta.values()) {
            int cuenta = contadorSupermercados; // dividir por supermercados
            respuesta.put("promedioGeneral" + tipoCanasta.name(),
                cuenta == 0 ? null : redondear2Decimales(sumaTotalesPorCanasta.get(tipoCanasta) / cuenta));
        }

        // Variaciones generales por canasta
        respuesta.put("variacionDiariaGeneralCBA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CBA, 0.0), sumaTotalesPorCanastaAyer.getOrDefault(TipoCanasta.CBA, 0.0)));
        respuesta.put("variacionSemanalGeneralCBA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CBA, 0.0), sumaTotalesPorCanastaSem.getOrDefault(TipoCanasta.CBA, 0.0)));
        respuesta.put("variacionMensualGeneralCBA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CBA, 0.0), sumaTotalesPorCanastaMes.getOrDefault(TipoCanasta.CBA, 0.0)));
        respuesta.put("variacionAnualGeneralCBA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CBA, 0.0), sumaTotalesPorCanastaAnio.getOrDefault(TipoCanasta.CBA, 0.0)));

        respuesta.put("variacionDiariaGeneralCPA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CPA, 0.0), sumaTotalesPorCanastaAyer.getOrDefault(TipoCanasta.CPA, 0.0)));
        respuesta.put("variacionSemanalGeneralCPA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CPA, 0.0), sumaTotalesPorCanastaSem.getOrDefault(TipoCanasta.CPA, 0.0)));
        respuesta.put("variacionMensualGeneralCPA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CPA, 0.0), sumaTotalesPorCanastaMes.getOrDefault(TipoCanasta.CPA, 0.0)));
        respuesta.put("variacionAnualGeneralCPA",
            calcularVariacion(sumaTotalesPorCanasta.getOrDefault(TipoCanasta.CPA, 0.0), sumaTotalesPorCanastaAnio.getOrDefault(TipoCanasta.CPA, 0.0)));

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
        if (precios == null || precios.isEmpty()) return 0;
        return precios.stream()
                .filter(p -> p.getValor() != null)
                .mapToDouble(Precio::getValor)
                .sum();
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