package com.basic_food_basket.scrapper;
/*
import com.portfolio.backend.model.Producto;
import com.portfolio.backend.model.Precio;
import com.portfolio.backend.repository.ProductoRepository;
import com.portfolio.backend.repository.SupermercadoRepository;
import com.portfolio.backend.service.IPrecioService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
public class LaAnonimaScraperService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private SupermercadoRepository supermercadoRepository;

    @Autowired
    private IPrecioService precioService;

    public void scrapPrecios() {
        WebDriverManager.chromedriver().setup();
        System.setProperty("webdriver.chrome.driver", "drivers/chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized"); // Deja esto para depurar y ver el navegador
        // options.addArguments("--headless"); // Comenta esta línea para ejecutar sin interfaz gráfica
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new ChromeDriver(options);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            List<Producto> productos = productoRepository.findBySupermercado(
                    supermercadoRepository.findBySlug("la-anonima").orElseThrow()
            );

            Function<By, Boolean> clickElementRobustly = (locator) -> {
                try {
                    WebDriverWait clickWait = new WebDriverWait(driver, java.time.Duration.ofSeconds(5));
                    WebElement elementToClick = clickWait.until(ExpectedConditions.elementToBeClickable(locator));
                    if (elementToClick.isDisplayed()) {
                        try {
                            elementToClick.click();
                            System.out.println("Clic normal en: " + locator.toString());
                            return true;
                        } catch (ElementClickInterceptedException e) {
                            System.out.println("Clic interceptado para: " + locator.toString() + ". Intentando con JS.");
                            js.executeScript("arguments[0].click();", elementToClick);
                            System.out.println("Clic JS en: " + locator.toString());
                            return true;
                        }
                    }
                } catch (TimeoutException | NoSuchElementException e) {
                    // Elemento no encontrado o no clickeable, no es un error crítico si el modal ya no está.
                } catch (StaleElementReferenceException e) {
                    System.out.println("StaleElementReferenceException para: " + locator.toString() + ". Reintentando clic con JS.");
                    try {
                        WebElement elementToClick = driver.findElement(locator);
                        js.executeScript("arguments[0].click();", elementToClick);
                        System.out.println("Clic JS (reintento) en: " + locator.toString());
                        return true;
                    } catch (Exception re) {
                        System.err.println("Fallo el reintento de clic JS para: " + locator.toString() + ": " + re.getMessage());
                    }
                }
                return false;
            };

            for (Producto producto : productos) {
                System.out.println("\n--- Procesando producto: " + producto.getNombre() + " - URL: " + producto.getUrl() + " ---");
                try {
                    driver.get(producto.getUrl());

                    WebDriverWait waitPopups = new WebDriverWait(driver, java.time.Duration.ofSeconds(15));

                    // --- Lógica de cierre de modales (llamada al método auxiliar) ---
                    attemptToCloseModals(driver, waitPopups, js, clickElementRobustly);

                    // --- Comprobación de Iframes ---
                    List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
                    if (!iframes.isEmpty()) {
                        System.out.println("Se encontraron " + iframes.size() + " iframes. Intentando cambiar y cerrar modales dentro de ellos.");
                        for (int i = 0; i < iframes.size(); i++) {
                            try {
                                driver.switchTo().frame(i);
                                System.out.println("Cambiado al iframe con índice: " + i);
                                attemptToCloseModals(driver, waitPopups, js, clickElementRobustly);
                                driver.switchTo().defaultContent();
                                System.out.println("Regresado al contexto principal después del iframe: " + i);
                            } catch (Exception e) {
                                System.err.println("Error al procesar iframe " + i + ": " + e.getMessage());
                                driver.switchTo().defaultContent();
                            }
                        }
                    } else {
                        System.out.println("No se encontraron iframes en la página.");
                    }

                    // --- Fin de la lógica de manejo de popups ---

                    // --- AHORA, la sección CRÍTICA: ESPERA Y CAPTURA EL ELEMENTO DEL PRECIO ---
                    System.out.println("Paso: Intentando localizar y capturar el elemento del precio...");
                    final int PRECIO_WAIT_SECONDS = 30;
                    WebDriverWait waitPrecio = new WebDriverWait(driver, java.time.Duration.ofSeconds(PRECIO_WAIT_SECONDS));

                    WebElement precioElement = null;
                    String textoPrecio = null;

                    try {
                        System.out.println("Paso: Buscando 'div.precio.destacado' con visibilidad. Tiempo de espera configurado: " + PRECIO_WAIT_SECONDS + "s");
                        // *** LA CORRECCIÓN CLAVE AQUÍ ***
                        precioElement = waitPrecio.until(ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("div.precio.destacado")
                        ));
                        System.out.println("Paso: Elemento 'div.precio.destacado' encontrado y visible.");

                        textoPrecio = precioElement.getText();
                        System.out.println("Paso: Texto crudo del precio obtenido: '" + textoPrecio + "'");

                        // Manejo de precios iniciales no válidos (0.00, vacío, Consultar, $-)
                        if (textoPrecio.trim().isEmpty() || textoPrecio.contains("0.00") || textoPrecio.contains("Consultar") || textoPrecio.contains("$-")) {
                            System.out.println("Paso: Precio inicial es vacío, '0.00', 'Consultar' o '$-', esperando que se actualice...");
                            System.out.println("Paso: Esperando texto válido en 'div.precio.destacado'.");
                            waitPrecio.until(ExpectedConditions.not(ExpectedConditions.or(
                                ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("div.precio.destacado"), "0.00"),
                                ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("div.precio.destacado"), "Consultar"),
                                ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("div.precio.destacado"), "$-"),
                                ExpectedConditions.textToBePresentInElement(precioElement, "")
                            )));
                            textoPrecio = precioElement.getText();
                            System.out.println("Paso: Texto del precio actualizado después de la espera: '" + textoPrecio + "'");
                        }

                        // Verificación final para asegurar que el precio obtenido es válido antes de parsear
                        if (textoPrecio.trim().isEmpty() || textoPrecio.contains("0.00") || textoPrecio.contains("Consultar") || textoPrecio.contains("$-")) {
                            System.err.println("Advertencia: El precio final para '" + producto.getNombre() + "' sigue siendo no válido: '" + textoPrecio + "'. No se guardará este producto.");
                            continue;
                        }

                        // El span de los decimales está *dentro* del div, por lo que getText() debería capturar todo.
                        // El replace(".","") es para los miles (1.550 -> 1550) y replace(",",".") es para los decimales (1550,00 -> 1550.00)
                        String precioLimpio = textoPrecio.replace("$", "")
                                .replace(".", "").replace(",", ".").trim();

                        Double valor = Double.parseDouble(precioLimpio);
                        LocalDate hoy = LocalDate.now();

                        System.out.println("Paso: Valor del precio parseado: $" + valor);

                        Optional<Precio> existente = precioService.obtenerPrecioPorProductoYFecha(producto, hoy);
                        if (!existente.isPresent()) {
                            Precio nuevoPrecio = new Precio();
                            nuevoPrecio.setProducto(producto);
                            nuevoPrecio.setFecha(hoy);
                            nuevoPrecio.setValor(valor);
                            precioService.guardarPrecio(nuevoPrecio);
                            System.out.println("Paso: Guardado: " + producto.getNombre() + " - $" + valor);
                        } else {
                            System.out.println("Paso: Ya existía precio para: " + producto.getNombre());
                        }

                    } catch (TimeoutException e) {
                        System.err.println("¡ERROR CRÍTICO!: El elemento del precio 'div.precio.destacado' NO SE ENCONTRÓ VISIBLE O NO SE ACTUALIZÓ en el tiempo esperado (" + PRECIO_WAIT_SECONDS + "s) para: " + producto.getNombre());
                        System.err.println("URL del producto con error: " + producto.getUrl());
                        System.err.println("Posibles causas: 1. El selector 'div.precio.destacado' es incorrecto (¡revisa de nuevo!). 2. El elemento está oculto, no carga a tiempo, o hay un overlay invisible.");
                        e.printStackTrace();
                    } catch (NumberFormatException e) {
                        System.err.println("ERROR al parsear el precio '" + textoPrecio + "' a número para: " + producto.getNombre() + ". Verifique el formato.");
                        System.err.println("URL del producto con error: " + producto.getUrl());
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.err.println("ERROR inesperado durante la extracción del precio para: " + producto.getNombre());
                        System.err.println("URL del producto con error: " + producto.getUrl());
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    System.err.println("Error general al procesar el producto: " + producto.getNombre());
                    System.err.println("URL del producto: " + (producto != null ? producto.getUrl() : "URL no disponible"));
                    e.printStackTrace();
                }
            }

        } finally {
            driver.quit();
        }
    }

    // --- Método auxiliar para cerrar modales ---
    private void attemptToCloseModals(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, Function<By, Boolean> clickElementRobustly) {
        // Modal 1: Ubicación/Provincia (Revisa estos selectores si los modales no se cierran automáticamente)
        By firstModalConfirmButton = By.cssSelector("button.button-primary.btn-confirm.submit-button.pull-right");
        By firstModalContainer = By.cssSelector("div.modal-dialog.modal-lg");

        System.out.println("Intentando cerrar el modal de ubicación/provincia...");
        try {
            if (clickElementRobustly.apply(firstModalConfirmButton)) {
                System.out.println("Esperando que el modal de ubicación/provincia desaparezca...");
                wait.until(ExpectedConditions.invisibilityOfElementLocated(firstModalContainer));
                System.out.println("Modal de ubicación/provincia desaparecido.");
            }
        } catch (TimeoutException | NoSuchElementException e) {
            System.out.println("Primer modal (ubicación/provincia) no encontrado o no pudo ser cerrado. Continuar.");
        }

        // Modal 2: Promoción/Bienvenida (Revisa estos selectores si los modales no se cierran automáticamente)
        By secondModalCloseButton = By.cssSelector("button.close-button.qa-close-modal");
        By secondModalContainer = By.cssSelector("div.modal-dialog.modal-md");

        System.out.println("Intentando cerrar el modal de promoción/bienvenida...");
        try {
            if (clickElementRobustly.apply(secondModalCloseButton)) {
                System.out.println("Esperando que el modal de promoción/bienvenida desaparezca...");
                wait.until(ExpectedConditions.invisibilityOfElementLocated(secondModalContainer));
                System.out.println("Modal de promoción/bienvenida desaparecido.");
            }
        } catch (TimeoutException | NoSuchElementException e) {
            System.out.println("Segundo modal (promoción/bienvenida) no encontrado o no pudo ser cerrado. Continuar.");
        }

        // Bloque genérico de respaldo para otros popups (cookies, etc.)
        System.out.println("Intentando cierre genérico de popups restantes.");
        String[] genericCloseSelectors = {
            "button[aria-label*='cerrar']",
            "button[aria-label*='Close']",
            "button[class*='close']",
            "span.close",
            "a.close",
            ".onetrust-close-btn-handler",
            "#cookie-banner-close",
            "#cookie-consent-button",
            "button.cookie-button.accept",
            "button[data-dismiss='modal']",
            "button.btn.btn-secondary[data-dismiss='modal']",
            "div.modal-backdrop"
        };

        for (String selector : genericCloseSelectors) {
            try {
                By genericLocator = By.cssSelector(selector);
                if (clickElementRobustly.apply(genericLocator)) {
                    System.out.println("Popup genérico cerrado usando selector: " + selector);
                    try {
                        WebElement clickedGenericElement = driver.findElement(genericLocator);
                        wait.until(ExpectedConditions.invisibilityOf(clickedGenericElement));
                        System.out.println("Elemento genérico desaparecido: " + selector);
                    } catch (NoSuchElementException | StaleElementReferenceException e) {
                        // Puede que ya haya desaparecido
                    } catch (TimeoutException e) {
                        System.out.println("El elemento genérico " + selector + " no desapareció en el tiempo esperado.");
                    }
                }
            } catch (Exception e) {
                // No loguear errores específicos.
            }
        }
    }
}*/

import com.basic_food_basket.model.Precio;
import com.basic_food_basket.model.Producto;
import com.basic_food_basket.repository.ProductoRepository;
import com.basic_food_basket.repository.SupermercadoRepository;
import com.basic_food_basket.service.IPrecioService;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class LaAnonimaScraperService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private SupermercadoRepository supermercadoRepository;

    @Autowired
    private IPrecioService precioService;

    public void scrapPrecios() {
    	WebDriverManager.chromedriver().setup();
    	System.setProperty("webdriver.chrome.driver", "drivers/chromedriver.exe");
    	ChromeOptions options = new ChromeOptions();
    	options.addArguments("--start-maximized"); // mejor para debug
        //options.addArguments("--headless");
    	options.addArguments("--remote-allow-origins=*"); // importante en nuevas versiones
    	
    	WebDriver driver = new ChromeDriver(options);
    	 

        try {
            List<Producto> productos = productoRepository.findBySupermercado(
                    supermercadoRepository.findBySlug("la-anonima").orElseThrow()
            );

            for (Producto producto : productos) {
                try {
                    driver.get(producto.getUrl());

                    WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
                    WebElement precioElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("div.precio.destacado")
                    ));

                    String textoPrecio = precioElement.getText().replace("$", "")
                            .replace(".", "").replace(",", ".").trim();

                    Double valor = Double.parseDouble(textoPrecio);
                    LocalDate hoy = LocalDate.now();

                    Optional<Precio> existente = precioService.obtenerPrecioPorProductoYFecha(producto, hoy);
                    if (!existente.isPresent()) {
                        Precio nuevoPrecio = new Precio();
                        nuevoPrecio.setProducto(producto);
                        nuevoPrecio.setFecha(hoy);
                        nuevoPrecio.setValor(valor);
                        nuevoPrecio.setScrapeado(true);
                        precioService.guardarPrecio(nuevoPrecio);
                        System.out.println("Guardado: " + producto.getNombre() + " - $" + valor);
                    } else {
                        System.out.println("Ya existía precio para: " + producto.getNombre());
                    }

                } catch (Exception e) {
                    System.err.println("Error al procesar producto: " + producto.getNombre());
                    e.printStackTrace();
                }
            }

        } finally {
            driver.quit();
        }
    }
}