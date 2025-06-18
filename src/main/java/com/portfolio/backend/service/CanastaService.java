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
}

/*package com.portfolio.backend.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.portfolio.backend.model.Precio;
import com.portfolio.backend.repository.PrecioRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CanastaService implements ICanastaService {

    @Autowired
    private PrecioRepository precioRepository;

    @Override
    public Map<String, Object> obtenerResumenCanasta() {
        Map<String, Object> respuesta = new HashMap<>();

        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minus(1, ChronoUnit.DAYS);
        LocalDate semana = hoy.minus(7, ChronoUnit.DAYS);
        LocalDate mes = hoy.minus(30, ChronoUnit.DAYS);
        LocalDate año = hoy.minus(365, ChronoUnit.DAYS);

        double totalHoy = calcularTotal(precioRepository.findByFecha(hoy));
        Double totalAyer = total(precioRepository.findByFecha(ayer));
        Double totalSemana = total(precioRepository.findByFecha(semana));
        Double totalMes = total(precioRepository.findByFecha(mes));
        Double totalAnual = total(precioRepository.findByFecha(año));

        respuesta.put("fecha", hoy.toString());
        respuesta.put("total", totalHoy);
        respuesta.put("variacionDiaria", calcularVariacion(totalHoy, totalAyer));
        respuesta.put("variacionSemanal", calcularVariacion(totalHoy, totalSemana));
        respuesta.put("variacionMensual", calcularVariacion(totalHoy, totalMes));
        respuesta.put("variacionAnual", calcularVariacion(totalHoy, totalAnual));

        return respuesta;
    }

    private double calcularTotal(List<Precio> precios) {
        return precios.stream()
                .filter(p -> p.getValor() != null)
                .mapToDouble(Precio::getValor)
                .sum();
    }

    private Double total(List<Precio> precios) {
        if (precios == null || precios.isEmpty()) return null;
        return calcularTotal(precios);
    }

    private Double calcularVariacion(Double actual, Double anterior) {
        if (actual == null || anterior == null || anterior == 0) return null;
        return Math.round(((actual - anterior) / anterior) * 100.0 * 100.0) / 100.0;
    }
}
*/


