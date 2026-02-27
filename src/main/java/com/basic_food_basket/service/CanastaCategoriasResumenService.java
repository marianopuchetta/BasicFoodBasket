package com.basic_food_basket.service;

import com.basic_food_basket.dto.*;
import com.basic_food_basket.model.Precio;
import com.basic_food_basket.model.Supermercado;
import com.basic_food_basket.repository.PrecioRepository;
import com.basic_food_basket.repository.SupermercadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CanastaCategoriasResumenService {

    @Autowired
    private PrecioRepository precioRepository;

    @Autowired
    private SupermercadoRepository supermercadoRepository;

    /**
     * Genera el resumen completo de categorías con promedios y variaciones
     */
    public CanastaCategoriasResumenDTO generarResumenCategorias() {
        CanastaCategoriasResumenDTO resultado = new CanastaCategoriasResumenDTO();

        // 1. OBTENER ÚLTIMA FECHA CON DATOS
        Optional<Precio> precioMasRecienteOpt = precioRepository.findTopByOrderByFechaDesc();
        if (precioMasRecienteOpt.isEmpty()) {
            resultado.setFecha("Sin datos");
            resultado.setPromedioXCategorias(new ArrayList<>());
            resultado.setSupermercados(new ArrayList<>());
            return resultado;
        }

        LocalDate hoy = precioMasRecienteOpt.get().getFecha();
        resultado.setFecha(hoy.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // 2. OBTENER TODAS LAS FECHAS DISPONIBLES
        List<LocalDate> todasLasFechas = precioRepository.findDistinctFechas();
        todasLasFechas.sort(Collections.reverseOrder());

        // 3. BUSCAR FECHAS PARA VARIACIONES
        LocalDate ayer = obtenerFechaDiasAtras(hoy, 1, todasLasFechas);
        LocalDate semanaPasada = obtenerFechaDiasAtras(hoy, 7, todasLasFechas);
        LocalDate mesPasado = obtenerFechaDiasAtras(hoy, 30, todasLasFechas);
        LocalDate anioPasado = obtenerFechaDiasAtras(hoy, 365, todasLasFechas);

        System.out.println("📅 Fecha principal: " + hoy);
        System.out.println("  - Ayer: " + (ayer != null ? ayer : "NO DISPONIBLE"));
        System.out.println("  - Hace 7 días: " + (semanaPasada != null ? semanaPasada : "NO DISPONIBLE"));
        System.out.println("  - Hace 30 días: " + (mesPasado != null ? mesPasado : "NO DISPONIBLE"));
        System.out.println("  - Hace 1 año: " + (anioPasado != null ? anioPasado : "NO DISPONIBLE"));

        // 4. OBTENER TODOS LOS PRECIOS DE HOY
        List<Precio> preciosHoy = precioRepository.findByFecha(hoy);
        System.out.println("✓ Precios de hoy: " + preciosHoy.size());

        // 5. AGRUPAR PRECIOS POR CATEGORIA + SUBCATEGORIA
        Map<String, Map<String, List<Precio>>> preciosPorCatYSubcat = agruparPorCategoriaYSubcategoria(preciosHoy);

        // 6. CONSTRUIR PROMEDIOS GLOBALES (TODOS LOS SUPERMERCADOS)
        List<CategoriaResumenDTO> promedios = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Precio>>> catEntry : preciosPorCatYSubcat.entrySet()) {
            String categoria = catEntry.getKey();
            Map<String, List<Precio>> subcategorias = catEntry.getValue();

            CategoriaResumenDTO catDTO = construirCategoriaPromedio(
                categoria, subcategorias, hoy, ayer, semanaPasada, mesPasado, anioPasado
            );
            promedios.add(catDTO);
        }
        resultado.setPromedioXCategorias(promedios);

        // 7. CONSTRUIR RESUMEN POR SUPERMERCADO
        List<Supermercado> supermercados = supermercadoRepository.findAll();
        List<SupermercadoResumenDTO> supermercadosDTO = new ArrayList<>();

        for (Supermercado super_ : supermercados) {
            SupermercadoResumenDTO superDTO = construirSupermercadoResumen(
                super_, hoy, ayer, semanaPasada, mesPasado, anioPasado
            );
            supermercadosDTO.add(superDTO);
        }
        resultado.setSupermercados(supermercadosDTO);

        return resultado;
    }

    /**
     * Agrupa precios por categoría y subcategoría
     */
    private Map<String, Map<String, List<Precio>>> agruparPorCategoriaYSubcategoria(List<Precio> precios) {
        return precios.stream()
            .collect(Collectors.groupingBy(
                p -> p.getProducto().getCategoria(),
                Collectors.groupingBy(p -> p.getProducto().getSubCategoria())
            ));
    }

    /**
     * Construye una categoría del promedio global (todos los supermercados)
     * CON PROTECTOR: Solo incluye supermercados que tienen datos EN AMBAS FECHAS
     */
    private CategoriaResumenDTO construirCategoriaPromedio(
            String categoria,
            Map<String, List<Precio>> subcategorias,
            LocalDate hoy,
            LocalDate ayer,
            LocalDate semanaPasada,
            LocalDate mesPasado,
            LocalDate anioPasado) {

        CategoriaResumenDTO catDTO = new CategoriaResumenDTO();
        catDTO.setNombre(categoria);

        List<Supermercado> supermercados = supermercadoRepository.findAll();
        int cantidadSupermercados = supermercados.size();

        Map<String, Double> subcategoriasTotales = new HashMap<>();
        List<SubCategoriaDTO> subcategoriasDetalles = new ArrayList<>();

        // SUMAS para después promediar
        double sumaTotalHoy = 0.0;
        double sumHoyParaAyer = 0.0;
        double sumAyer = 0.0;
        
        double sumHoyParaSemana = 0.0;
        double sumSemana = 0.0;
        
        double sumHoyParaMes = 0.0;
        double sumMes = 0.0;
        
        double sumHoyParaAnio = 0.0;
        double sumAnio = 0.0;

        int contadorSubcategorias = 0;

        for (Map.Entry<String, List<Precio>> subcatEntry : subcategorias.entrySet()) {
            String subcat = subcatEntry.getKey();
            List<Precio> precios = subcatEntry.getValue();

            // TOTAL HOY
            double totalHoy = calcularTotal(precios);
            
            // TOTALES otras fechas
            double totalAyer = ayer != null ? calcularTotalPorSubcategoriaYFecha(categoria, subcat, ayer) : 0;
            double totalSemana = semanaPasada != null ? calcularTotalPorSubcategoriaYFecha(categoria, subcat, semanaPasada) : 0;
            double totalMes = mesPasado != null ? calcularTotalPorSubcategoriaYFecha(categoria, subcat, mesPasado) : 0;
            double totalAnio = anioPasado != null ? calcularTotalPorSubcategoriaYFecha(categoria, subcat, anioPasado) : 0;

            sumaTotalHoy += totalHoy;
            
            // ✅ PROTECTOR: Solo sumar si hay datos EN AMBAS FECHAS
            if (totalAyer > 0) {
                sumHoyParaAyer += totalHoy;
                sumAyer += totalAyer;
            }
            if (totalSemana > 0) {
                sumHoyParaSemana += totalHoy;
                sumSemana += totalSemana;
            }
            if (totalMes > 0) {
                sumHoyParaMes += totalHoy;
                sumMes += totalMes;
            }
            if (totalAnio > 0) {
                sumHoyParaAnio += totalHoy;
                sumAnio += totalAnio;
            }

            // ✅ PROMEDIO de la subcategoría = suma total / cantidad de supermercados
            double promedioHoy = cantidadSupermercados > 0 ? totalHoy / cantidadSupermercados : 0;
            double promedioAyer = cantidadSupermercados > 0 ? totalAyer / cantidadSupermercados : 0;
            double promedioSemana = cantidadSupermercados > 0 ? totalSemana / cantidadSupermercados : 0;
            double promedioMes = cantidadSupermercados > 0 ? totalMes / cantidadSupermercados : 0;
            double promedioAnio = cantidadSupermercados > 0 ? totalAnio / cantidadSupermercados : 0;

            // Variaciones INDIVIDUALES
            Double varDiaria = calcularVariacion(promedioHoy, promedioAyer);
            Double varSemanal = calcularVariacion(promedioHoy, promedioSemana);
            Double varMensual = calcularVariacion(promedioHoy, promedioMes);
            Double varAnual = promedioAnio > 0 ? calcularVariacion(promedioHoy, promedioAnio) : null;

            SubCategoriaDTO subcatDTO = new SubCategoriaDTO(
                subcat,
                redondear2Decimales(promedioHoy),
                varDiaria,
                varSemanal,
                varMensual,
                varAnual
            );
            subcategoriasDetalles.add(subcatDTO);
            subcategoriasTotales.put(subcat, redondear2Decimales(promedioHoy));

            contadorSubcategorias++;
        }

        // ✅ PROMEDIO de la categoría
        double totalCategoriaPromedioHoy = cantidadSupermercados > 0 ? 
            redondear2Decimales(sumaTotalHoy / cantidadSupermercados) : 0;

        catDTO.setTotal(totalCategoriaPromedioHoy);
        catDTO.setSubcategorias(subcategoriasTotales);
        catDTO.setSubcategoriasDTO(subcategoriasDetalles);

        // ✅ Variaciones sobre los PROMEDIOS
        // SIEMPRE dividir entre cantidadSupermercados, NUNCA entre counts parciales
        double promedioHoy = cantidadSupermercados > 0 ? sumaTotalHoy / cantidadSupermercados : 0;
        double promedioAyer = cantidadSupermercados > 0 ? sumAyer / cantidadSupermercados : 0;
        double promedioSemana = cantidadSupermercados > 0 ? sumSemana / cantidadSupermercados : 0;
        double promedioMes = cantidadSupermercados > 0 ? sumMes / cantidadSupermercados : 0;
        double promedioAnio = cantidadSupermercados > 0 ? sumAnio / cantidadSupermercados : 0;

        Double varDiaria = calcularVariacion(promedioHoy, promedioAyer);
        Double varSemanal = calcularVariacion(promedioHoy, promedioSemana);
        Double varMensual = calcularVariacion(promedioHoy, promedioMes);
        Double varAnual = promedioAnio > 0 ? calcularVariacion(promedioHoy, promedioAnio) : null;

        if (contadorSubcategorias > 0) {
            catDTO.setVariacionDiaria(varDiaria);
            catDTO.setVariacionSemanal(varSemanal);
            catDTO.setVariacionMensual(varMensual);
        }
        catDTO.setVariacionAnual(null);

        return catDTO;
    }

    /**
     * Construye resumen de un supermercado
     */
    private SupermercadoResumenDTO construirSupermercadoResumen(
            Supermercado supermercado,
            LocalDate hoy,
            LocalDate ayer,
            LocalDate semanaPasada,
            LocalDate mesPasado,
            LocalDate anioPasado) {

        SupermercadoResumenDTO superDTO = new SupermercadoResumenDTO(supermercado.getNombre());

        List<Precio> preciosHoy = precioRepository.findByFecha(hoy).stream()
            .filter(p -> p.getProducto().getSupermercado().getId().equals(supermercado.getId()))
            .collect(Collectors.toList());

        // Agrupar por categoría
        Map<String, Map<String, List<Precio>>> preciosPorCatYSubcat = agruparPorCategoriaYSubcategoria(preciosHoy);

        List<CategoriaResumenDTO> categorias = new ArrayList<>();
        double totalSuper = 0.0;

        for (Map.Entry<String, Map<String, List<Precio>>> catEntry : preciosPorCatYSubcat.entrySet()) {
            String categoria = catEntry.getKey();
            Map<String, List<Precio>> subcategorias = catEntry.getValue();

            CategoriaResumenDTO catDTO = construirCategoriaSupermercado(
                supermercado.getId(), categoria, subcategorias,
                hoy, ayer, semanaPasada, mesPasado, anioPasado
            );
            categorias.add(catDTO);
            totalSuper += catDTO.getTotal();
        }

        superDTO.setTotal(redondear2Decimales(totalSuper));
        superDTO.setCategorias(categorias);

        return superDTO;
    }

    /**
     * Construye categoría de un supermercado específico
     * CON PROTECTOR: Si no hay datos en una fecha, no calcula esa variación
     */
    private CategoriaResumenDTO construirCategoriaSupermercado(
            Long supermercadoId,
            String categoria,
            Map<String, List<Precio>> subcategorias,
            LocalDate hoy,
            LocalDate ayer,
            LocalDate semanaPasada,
            LocalDate mesPasado,
            LocalDate anioPasado) {

        CategoriaResumenDTO catDTO = new CategoriaResumenDTO();
        catDTO.setNombre(categoria);

        Map<String, Double> subcategoriasTotales = new HashMap<>();
        List<SubCategoriaDTO> subcategoriasDetalles = new ArrayList<>();
        
        double totalCategoriaHoy = 0.0;
        double sumHoyParaAyer = 0.0;
        double sumAyer = 0.0;
        double sumHoyParaSemana = 0.0;
        double sumSemana = 0.0;
        double sumHoyParaMes = 0.0;
        double sumMes = 0.0;
        double sumHoyParaAnio = 0.0;
        double sumAnio = 0.0;
        int contador = 0;

        for (Map.Entry<String, List<Precio>> subcatEntry : subcategorias.entrySet()) {
            String subcat = subcatEntry.getKey();
            List<Precio> precios = subcatEntry.getValue();

            // TOTALES
            double totalHoy = calcularTotal(precios);
            double totalAyer = ayer != null ? calcularTotalPorSupermercadoCategoriaSubcategoriaYFecha(supermercadoId, categoria, subcat, ayer) : 0;
            double totalSemana = semanaPasada != null ? calcularTotalPorSupermercadoCategoriaSubcategoriaYFecha(supermercadoId, categoria, subcat, semanaPasada) : 0;
            double totalMes = mesPasado != null ? calcularTotalPorSupermercadoCategoriaSubcategoriaYFecha(supermercadoId, categoria, subcat, mesPasado) : 0;
            double totalAnio = anioPasado != null ? calcularTotalPorSupermercadoCategoriaSubcategoriaYFecha(supermercadoId, categoria, subcat, anioPasado) : 0;

            // ✅ PROTECTOR: Solo incluir si hay datos EN AMBAS FECHAS
            if (totalAyer > 0) {
                sumHoyParaAyer += totalHoy;
                sumAyer += totalAyer;
            }
            if (totalSemana > 0) {
                sumHoyParaSemana += totalHoy;
                sumSemana += totalSemana;
            }
            if (totalMes > 0) {
                sumHoyParaMes += totalHoy;
                sumMes += totalMes;
            }
            if (totalAnio > 0) {
                sumHoyParaAnio += totalHoy;
                sumAnio += totalAnio;
            }

            // Variaciones
            Double varDiaria = calcularVariacion(totalHoy, totalAyer);
            Double varSemanal = calcularVariacion(totalHoy, totalSemana);
            Double varMensual = calcularVariacion(totalHoy, totalMes);
            Double varAnual = totalAnio > 0 ? calcularVariacion(totalHoy, totalAnio) : null;

            // DTO
            SubCategoriaDTO subcatDTO = new SubCategoriaDTO(
                subcat,
                redondear2Decimales(totalHoy),
                varDiaria,
                varSemanal,
                varMensual,
                varAnual
            );
            subcategoriasDetalles.add(subcatDTO);

            subcategoriasTotales.put(subcat, redondear2Decimales(totalHoy));
            totalCategoriaHoy += totalHoy;
            contador++;
        }

        catDTO.setTotal(redondear2Decimales(totalCategoriaHoy));
        catDTO.setSubcategorias(subcategoriasTotales);
        catDTO.setSubcategoriasDTO(subcategoriasDetalles);

        // ✅ Variaciones globales de la categoría CON PROTECTOR
        if (sumAyer > 0) {
            catDTO.setVariacionDiaria(calcularVariacion(sumHoyParaAyer, sumAyer));
        } else {
            catDTO.setVariacionDiaria(null);
        }
        
        if (sumSemana > 0) {
            catDTO.setVariacionSemanal(calcularVariacion(sumHoyParaSemana, sumSemana));
        } else {
            catDTO.setVariacionSemanal(null);
        }
        
        if (sumMes > 0) {
            catDTO.setVariacionMensual(calcularVariacion(sumHoyParaMes, sumMes));
        } else {
            catDTO.setVariacionMensual(null);
        }
        
        if (sumAnio > 0) {
            catDTO.setVariacionAnual(calcularVariacion(sumHoyParaAnio, sumAnio));
        } else {
            catDTO.setVariacionAnual(null);
        }

        return catDTO;
    }

    /**
     * Calcula total de precios (SUM, no promedio)
     */
    private double calcularTotal(List<Precio> precios) {
        if (precios == null || precios.isEmpty()) {
            return 0;
        }
        return precios.stream()
            .filter(p -> p.getValor() != null)
            .mapToDouble(Precio::getValor)
            .sum();
    }

    /**
     * Calcula total de una subcategoría en una fecha específica
     */
    private double calcularTotalPorSubcategoriaYFecha(String categoria, String subCategoria, LocalDate fecha) {
        List<Precio> precios = precioRepository.findByFecha(fecha).stream()
            .filter(p -> p.getProducto().getCategoria().equals(categoria) &&
                        p.getProducto().getSubCategoria().equals(subCategoria))
            .collect(Collectors.toList());
        return calcularTotal(precios);
    }

    /**
     * Calcula total de una subcategoría para un supermercado en una fecha específica
     */
    private double calcularTotalPorSupermercadoCategoriaSubcategoriaYFecha(
            Long supermercadoId, String categoria, String subCategoria, LocalDate fecha) {
        List<Precio> precios = precioRepository.findByFecha(fecha).stream()
            .filter(p -> p.getProducto().getSupermercado().getId().equals(supermercadoId) &&
                        p.getProducto().getCategoria().equals(categoria) &&
                        p.getProducto().getSubCategoria().equals(subCategoria))
            .collect(Collectors.toList());
        return calcularTotal(precios);
    }

    /**
     * Obtiene la fecha más cercana que está X días atrás
     * Si no existe exactamente en X días, busca la más cercana ANTERIOR
     */
    private LocalDate obtenerFechaDiasAtras(LocalDate fechaPrincipal, int dias, List<LocalDate> todasLasFechas) {
        LocalDate fechaBuscada = fechaPrincipal.minusDays(dias);
        
        // Buscar exacta primero
        if (todasLasFechas.contains(fechaBuscada)) {
            System.out.println("    ✓ Encontrada fecha exacta: " + fechaBuscada);
            return fechaBuscada;
        }
        
        // Si no existe exacta, buscar la más cercana anterior
        LocalDate fechaMasCercana = null;
        long diferenciaMenor = Long.MAX_VALUE;
        
        for (LocalDate fecha : todasLasFechas) {
            if (fecha.isBefore(fechaBuscada)) {  // ✅ Solo fechas ANTES de la buscada
                long diferencia = java.time.temporal.ChronoUnit.DAYS.between(fecha, fechaBuscada);
                
                if (diferencia < diferenciaMenor) {
                    diferenciaMenor = diferencia;
                    fechaMasCercana = fecha;
                }
            }
        }
        
        if (fechaMasCercana != null) {
            System.out.println("    ⚠️ Fecha no exacta. Buscada: " + fechaBuscada + 
                              " → Encontrada: " + fechaMasCercana + 
                              " (" + diferenciaMenor + " días atrás)");
        } else {
            System.out.println("    ❌ NO HAY DATOS para " + dias + " días atrás");
        }
        
        return fechaMasCercana;
    }

    /**
     * Calcula variación (igual que CanastaService)
     */
    private Double calcularVariacion(Double actual, Double anterior) {
        if (actual == null || anterior == null || anterior == 0)
            return null;
        double variacion = ((actual - anterior) / anterior) * 100.0;
        return Math.round(variacion * 100.0) / 100.0;
    }

    /**
     * Redondea a 2 decimales (igual que CanastaService)
     */
    private double redondear2Decimales(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}