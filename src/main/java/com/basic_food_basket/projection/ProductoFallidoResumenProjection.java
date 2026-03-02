package com.basic_food_basket.projection;

public interface ProductoFallidoResumenProjection {
    Long getProductoId();
    String getNombreProducto();
    String getUrlProducto();
    Long getTotalDiasNoScrapeados();
}