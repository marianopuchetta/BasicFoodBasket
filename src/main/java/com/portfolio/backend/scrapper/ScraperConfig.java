package com.portfolio.backend.scrapper;

public class ScraperConfig {
    private String supermarketSlug;
    private String priceSelector;
    private String modalCloseSelector;
    private String cookieBannerSelector;
    private String overlaySelector;
    private int timeoutSeconds;


    public ScraperConfig(String supermarketSlug, String priceSelector, String modalCloseSelector, 
                        String cookieBannerSelector, String overlaySelector,int timeoutSeconds) {
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
            case "la-anonima":
                return new ScraperConfig(
                    "la-anonima",
                    "div.precio.destacado",
                    "button.close-button.qa-close-modal",
                    "button.cookie-button.accept",
                    "div.modal-backdrop",
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
                
            default:
                throw new IllegalArgumentException("Supermercado no configurado: " + supermarketSlug);
        }
    }
}
