
/*package com.portfolio.backend.service;



import java.util.Map;

public interface ICanastaService {
    Map<String, Object> obtenerResumenCanasta();
}*/

package com.basic_food_basket.service;

import java.time.LocalDate;
import java.util.Map;

public interface ICanastaService {
    Map<String, Object> obtenerResumenCanasta(); // Por supermercado

    Map<String, Object> obtenerHistorialCanasta(); // Por supermercado

    // Nuevo: historial por supermercado con rango de fechas
    Map<String, Object> obtenerHistorialCanasta(LocalDate desde, LocalDate hasta);

    Map<String, Object> obtenerResumenGeneral(); // Todas las canastas y supermercados juntos

    Map<String, Object> obtenerHistorialGeneral(); // Historial sumado/promediado de todos los supermercados

    // Nuevo: historial general con rango de fechas
    Map<String, Object> obtenerHistorialGeneral(LocalDate desde, LocalDate hasta);
    
    Map<String, Object> obtenerUltimosPreciosPorSupermercado();
}