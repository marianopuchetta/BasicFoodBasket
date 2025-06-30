package com.basic_food_basket.scrapper;

public class ScraperConfig {
    private String supermarketSlug;
    private String priceSelector;
    private String modalCloseSelector;
    private String cookieBannerSelector;
    private String overlaySelector;
    private int timeoutSeconds;

    public ScraperConfig(String supermarketSlug, String priceSelector, String modalCloseSelector, 
                        String cookieBannerSelector, String overlaySelector, int timeoutSeconds) {
        this.supermarketSlug = supermarketSlug;
        this.priceSelector = priceSelector;
        this.modalCloseSelector = modalCloseSelector;
        this.cookieBannerSelector = cookieBannerSelector;
        this.overlaySelector = overlaySelector;
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getPriceSelector() {
        return priceSelector;
    }

    public String getModalCloseSelector() {
        return modalCloseSelector;
    }

    public String getCookieBannerSelector() {
        return cookieBannerSelector;
    }

    public String getOverlaySelector() {
        return overlaySelector;
    }

    public String getSupermarketSlug() {
        return supermarketSlug;
    }

    public static ScraperConfig getConfigFor(String supermarketSlug) {
        switch(supermarketSlug.toLowerCase()) {
            case "coto":
                // El selector para el precio puede ser "span.sale-price" (oferta) o "var.price.h3.ng-star-inserted" (precio normal)
                // Se recomienda manejar ambos en el scraper, pero aquí dejamos el principal para referencia
                return new ScraperConfig(
                		"coto",
                        "var.price.h3",
                        "button.close-modal",
                        "#cookie-banner-accept",
                        ".modal-backdrop",
                        20
                );
            case "disco":
                return new ScraperConfig(
                    "disco",
                    "#priceContainer",
                    "button.modal-close",
                    "#btn-cookie-allow",
                    "div.overlay",
                    15
                );
            case "mas-online":
                return new ScraperConfig(
                    "mas-online",
                    "span[class*='dynamicProductPrice']",
                    "button.modal-close",
                    "#cookie-consent-button",
                    "div.overlay",
                    15
                );
            case "dia":
                // Selector de precio: span.diaio-store-5-x-sellingPriceValue
                return new ScraperConfig(
                    "dia",
                    "span.diaio-store-5-x-sellingPriceValue",
                    null, // Si no hay modal de cierre, dejar null o el selector correcto si existe
                    null, // Si no hay banner de cookies, dejar null o el selector correcto si existe
                    null, // Si no hay overlay modal, dejar null o el selector correcto si existe
                    15    // Timeout sugerido
                );
            default:
                throw new IllegalArgumentException("Supermercado no configurado: " + supermarketSlug);
        }
    }
}