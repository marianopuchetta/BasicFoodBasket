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
import org.openqa.selenium.remote.RemoteWebElement;

import javax.annotation.PreDestroy;
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

    private volatile boolean scrapingEnabled = true;
    private WebDriver currentDriver;

    @PreDestroy
    public void cleanUp() {
        scrapingEnabled = false;
        if (currentDriver != null) {
            currentDriver.quit();
        }
    }

    @Override
    public void scrapPrecios(Supermercado supermercado) {
        if (!scrapingEnabled) {
            System.out.println("Scraping no iniciado - servicio deshabilitado");
            return;
        }

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        // Configuración headless optimizada
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        try {
            currentDriver = new ChromeDriver(options);
            JavascriptExecutor js = (JavascriptExecutor) currentDriver;

            ScraperConfig config = ScraperConfig.getConfigFor(supermercado.getSlug());

            for (TipoCanasta tipoCanasta : TipoCanasta.values()) {
                if (!scrapingEnabled) break;

                try {
                    List<Producto> productos = productoRepository.findBySupermercadoAndTipoCanasta(
                        supermercado, tipoCanasta);

                    if (productos.isEmpty()) {
                        System.out.printf("No hay productos para %s (%s)%n",
                            supermercado.getNombre(), tipoCanasta);
                        continue;
                    }

                    System.out.printf("Scrapeando %d productos de %s (%s)%n",
                        productos.size(), supermercado.getNombre(), tipoCanasta);

                    scrapeProductos(currentDriver, productos, config, js);
                } catch (Exception e) {
                    System.err.println("Error al obtener productos: " + e.getMessage());
                    // Continuar con el siguiente tipo de canasta
                }
            }
        } catch (Exception e) {
            System.err.println("Error crítico en el scraping: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (currentDriver != null) {
                currentDriver.quit();
                currentDriver = null;
            }
        }
    }

    private void scrapeProductos(WebDriver driver, List<Producto> productos,
                                 ScraperConfig config, JavascriptExecutor js) {

        Function<By, Boolean> clickElementRobustly = (locator) -> {
            try {
                WebDriverWait clickWait = new WebDriverWait(driver, Duration.ofSeconds(10));
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
            if (!scrapingEnabled) break;

            try {
                System.out.println("\n--- Procesando producto: " + producto.getNombre() + " ---");
                driver.get(producto.getUrl());

                // Espera inicial para asegurar carga de página
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                WebDriverWait waitPrecio = new WebDriverWait(driver, Duration.ofSeconds(30));
                WebElement precioElement = waitPrecio.until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(config.getPriceSelector()))
                );

                String precioLimpio = extractPrice(precioElement, config, driver);
                Double valor = Double.parseDouble(precioLimpio);
                guardarPrecio(producto, valor);

            } catch (Exception e) {
                System.err.println("Error procesando producto: " + producto.getNombre());
                e.printStackTrace();
                guardarPrecioFallback(producto);
            }
        }
    }

    private String extractPrice(WebElement priceElement, ScraperConfig config, WebDriver driver) {
        if ("mas-online".equals(config.getSupermarketSlug())) {
            WebDriver d = ((RemoteWebElement) priceElement).getWrappedDriver();
            return extractMasOnlinePrice(d);
        } else {
            String textoPrecio = priceElement.getText();
            return textoPrecio.replace("$", "")
                .replace(".", "")
                .replace(",", ".")
                .replaceAll("\\s", "")
                .trim();
        }
    }

    private String extractMasOnlinePrice(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

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

    private void guardarPrecioFallback(Producto producto) {
        try {
            LocalDate hoy = LocalDate.now();
            Optional<Precio> ultimoPrecio = precioService.findUltimoPrecioByProducto(producto);

            if (ultimoPrecio.isPresent()) {
                Precio precioFallback = new Precio();
                precioFallback.setProducto(producto);
                precioFallback.setFecha(hoy);
                precioFallback.setValor(ultimoPrecio.get().getValor());
                precioFallback.setScrapeado(false);

                precioService.guardarPrecio(precioFallback);
                System.out.printf("Guardado fallback (no scrapeado): %s - $%.2f (%s)%n",
                    producto.getNombre(), ultimoPrecio.get().getValor(), producto.getTipoCanasta());
            } else {
                System.err.println("No se encontró precio anterior para fallback: " + producto.getNombre());
            }
        } catch (Exception e) {
            System.err.println("Error al guardar precio fallback para: " + producto.getNombre());
        }
    }

    private void guardarPrecio(Producto producto, Double valor) {
        try {
            LocalDate hoy = LocalDate.now();
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
            } else {
                System.out.println("Ya existía precio para: " + producto.getNombre());
            }
        } catch (Exception e) {
            System.err.println("Error al guardar precio para: " + producto.getNombre());
            guardarPrecioFallback(producto);
        }
    }
}