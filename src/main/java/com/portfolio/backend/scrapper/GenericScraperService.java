package com.portfolio.backend.scrapper;

import com.portfolio.backend.model.*;
import com.portfolio.backend.repository.ProductoRepository;
import com.portfolio.backend.service.IPrecioService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GenericScraperService implements IScraperService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private IPrecioService precioService;

    @Override
    public void scrapPrecios(Supermercado supermercado) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--remote-allow-origins=*");
        
        WebDriver driver = new ChromeDriver(options);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            ScraperConfig config = ScraperConfig.getConfigFor(supermercado.getSlug());
            
            for (TipoCanasta tipoCanasta : TipoCanasta.values()) {
                List<Producto> productos = productoRepository.findBySupermercadoAndTipoCanasta(
                    supermercado, tipoCanasta);
                
                System.out.printf("Scrapeando %d productos de %s (%s)%n", 
                    productos.size(), supermercado.getNombre(), tipoCanasta);
                
                scrapeProductos(driver, productos, config, js);
            }

        } finally {
            driver.quit();
        }
    }

    private void scrapeProductos(WebDriver driver, List<Producto> productos, 
                               ScraperConfig config, JavascriptExecutor js) {
        
        Function<By, Boolean> clickElementRobustly = (locator) -> {
            try {
                WebDriverWait clickWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                WebElement element = clickWait.until(ExpectedConditions.elementToBeClickable(locator));
                if (element.isDisplayed()) {
                    try {
                        element.click();
                        return true;
                    } catch (ElementClickInterceptedException e) {
                        js.executeScript("arguments[0].click();", element);
                        return true;
                    }
                }
            } catch (Exception e) {
                return false;
            }
            return false;
        };

        for (Producto producto : productos) {
            try {
                System.out.println("\n--- Procesando producto: " + producto.getNombre() + " ---");
                driver.get(producto.getUrl());

                handlePopups(driver, config, clickElementRobustly, js);

                WebDriverWait waitPrecio = new WebDriverWait(driver, Duration.ofSeconds(30));
                WebElement precioElement = waitPrecio.until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(config.getPriceSelector()))
                );

                String precioLimpio;
                if ("mas-online".equals(config.getSupermarketSlug())) {
                    precioLimpio = extractMasOnlinePrice(driver);
                } else {
                    String textoPrecio = precioElement.getText();
                    precioLimpio = textoPrecio.replace("$", "")
                        .replace(".", "")
                        .replace(",", ".")
                        .trim();
                }

                Double valor = Double.parseDouble(precioLimpio);
                guardarPrecio(producto, valor);

            } catch (Exception e) {
                System.err.println("Error procesando producto: " + producto.getNombre());
                e.printStackTrace();
            }
        }
    }

    private String extractMasOnlinePrice(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            WebElement priceContainer = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("span[class*='dynamicProductPrice']")
                )
            );

            List<WebElement> integerParts = priceContainer.findElements(
                By.cssSelector("span[class*='currencyInteger']")
            );
            
            WebElement fractionPart = priceContainer.findElement(
                By.cssSelector("span[class*='currencyFraction']")
            );

            String fullInteger = integerParts.stream()
                .map(WebElement::getText)
                .collect(Collectors.joining());
                
            return fullInteger + "." + fractionPart.getText();
            
        } catch (Exception e) {
            System.err.println("Error al extraer precio de Más Online: " + e.getMessage());
            
            try {
                WebElement priceElement = driver.findElement(
                    By.cssSelector("span[class*='dynamicProductPrice']")
                );
                return priceElement.getText()
                    .replace("$", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replaceAll("\\s", "")
                    .trim();
            } catch (Exception ex) {
                throw new RuntimeException("No se pudo obtener el precio de Más Online", ex);
            }
        }
    }

    private void handlePopups(WebDriver driver, ScraperConfig config, 
                            Function<By, Boolean> clickElement, JavascriptExecutor js) {
        try {
            if (config.getModalCloseSelector() != null) {
                clickElement.apply(By.cssSelector(config.getModalCloseSelector()));
            }

            if (config.getCookieBannerSelector() != null) {
                clickElement.apply(By.cssSelector(config.getCookieBannerSelector()));
            }

            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.invisibilityOfElementLocated(
                        By.cssSelector(config.getOverlaySelector())));
            } catch (Exception e) {
                // Ignorar si no hay overlay
            }
        } catch (Exception e) {
            System.err.println("Error manejando popups: " + e.getMessage());
        }
    }

    private void guardarPrecio(Producto producto, Double valor) {
        try {
            LocalDate hoy = LocalDate.now();
            
            // Intenta guardar el precio scrapeado hoy
            try {
                Optional<Precio> existente = precioService.obtenerPrecioPorProductoYFecha(producto, hoy);
                
                if (existente.isEmpty()) {
                    Precio nuevoPrecio = new Precio();
                    nuevoPrecio.setProducto(producto);
                    nuevoPrecio.setFecha(hoy);
                    nuevoPrecio.setValor(valor);
                    nuevoPrecio.setScrapeado(true);
                    precioService.guardarPrecio(nuevoPrecio);
                    
                    System.out.printf("Guardado: %s - $%.2f (%s)%n", 
                        producto.getNombre(), valor, producto.getTipoCanasta());
                    return; // Salir si se guardó correctamente
                } else {
                    System.out.println("Ya existía precio para: " + producto.getNombre());
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error al guardar precio actual, intentando con fallback...");
            }

            // Fallback: Buscar último precio disponible
            Optional<Precio> ultimoPrecio = precioService.findUltimoPrecioByProducto(producto);
            
            if (ultimoPrecio.isPresent()) {
                Precio precioFallback = new Precio();
                precioFallback.setProducto(producto);
                precioFallback.setFecha(hoy);
                precioFallback.setValor(ultimoPrecio.get().getValor());
                precioFallback.setScrapeado(false); // Marcamos como no scrapeado
                
                precioService.guardarPrecio(precioFallback);
                System.out.printf("Guardado fallback (no scrapeado): %s - $%.2f (%s)%n",
                    producto.getNombre(), ultimoPrecio.get().getValor(), producto.getTipoCanasta());
            } else {
                System.err.println("No se encontró precio anterior para: " + producto.getNombre());
            }
            
        } catch (Exception e) {
            System.err.println("Error crítico al guardar precio (incluso fallback) para: " + producto.getNombre());
        }
    }
}
