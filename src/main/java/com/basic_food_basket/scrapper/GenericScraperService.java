/*
package com.basic_food_basket.scrapper;

import com.basic_food_basket.model.*;
import com.basic_food_basket.repository.ProductoRepository;
import com.basic_food_basket.service.IPrecioService;

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
import java.util.Set;
import org.openqa.selenium.NoSuchElementException; // Asegúrate de tener este import
import java.util.function.Function;

@Service
public class GenericScraperService implements IScraperService {

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private IPrecioService precioService;

	private volatile boolean scrapingEnabled = true;
	private WebDriver currentDriver;

	// Lista de productos DIA cuyo precio debe multiplicarse x2
    // (Se mantiene la lista de tu "código complejo")
	private static final Set<String> DIA_DUPLICATE_PRICE_PRODUCTS = Set.of("Café Clásico La Morenita", "Té en saquitos Crysf");

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
		options.addArguments(
				"--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

		try {
			currentDriver = new ChromeDriver(options);
			JavascriptExecutor js = (JavascriptExecutor) currentDriver;

			ScraperConfig config = ScraperConfig.getConfigFor(supermercado.getSlug());

			for (TipoCanasta tipoCanasta : TipoCanasta.values()) {
				if (!scrapingEnabled)
					break;

				try {
					List<Producto> productos = productoRepository
							.findBySupermercadoIdAndTipoCanasta(supermercado.getId(), tipoCanasta);

					if (productos.isEmpty()) {
						System.out.printf("No hay productos para %s (%s)%n", supermercado.getNombre(), tipoCanasta);
						continue;
					}

					System.out.printf("Scrapeando %d productos de %s (%s)%n", productos.size(),
							supermercado.getNombre(), tipoCanasta);

					scrapeProductos(currentDriver, productos, config, js, supermercado.getSlug());
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

	// Método especializado para Disco (Existente)
	private boolean isProductoSinStockDisco(WebDriver driver) {
		try {
			WebElement sinStockElement = driver.findElement(By.cssSelector("p.vtex-outOfStockFlag__text"));
			String texto = sinStockElement.getText().trim().toLowerCase();
			return texto.contains("sin stock");
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	private void scrapeProductos(WebDriver driver, List<Producto> productos, ScraperConfig config,
			JavascriptExecutor js, String supermercadoSlug) {

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
			if (!scrapingEnabled)
				break;

			try {
                // Se agrega el ID al log para facilitar la depuración
				System.out.println("\n--- Procesando producto: " + producto.getNombre() + " (ID: " + producto.getId() + ") ---");
				driver.get(producto.getUrl());

                // Chequeo inmediato de "sin stock" para Disco
				if ("disco".equalsIgnoreCase(supermercadoSlug) && isProductoSinStockDisco(driver)) {
					System.out.println("Producto sin stock, usando precio del día anterior.");
					guardarPrecioFallback(producto);
					continue;
				}

				String precioLimpio;
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // WebDriverWait genérico

                // --- INICIO DE LÓGICA FUSIONADA ---

                // CASO 1: Lógica especial para Jumbo productos 471 y 472 (NUEVO)
                if ("jumbo".equalsIgnoreCase(supermercadoSlug) &&
                    (producto.getId() == 471 || producto.getId() == 472)) {

                    System.out.println("Aplicando lógica de scraping especial para Jumbo ID: " + producto.getId());
                    By jumboSpecialSelector = By.cssSelector("span.vtex-custom-unit-price"); 
                    WebElement precioElement = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(jumboSpecialSelector)
                    );

                    String allText = precioElement.getText();
                    String parentTextOnly;
                    try {
                         WebElement childDiv = precioElement.findElement(By.cssSelector("div[class*='jumboargentinaio-store-theme']"));
                         String childText = childDiv.getText();
                         parentTextOnly = allText.replace(childText, "").trim();
                    } catch (NoSuchElementException e) {
                         parentTextOnly = allText.trim();
                    }
                    
                    String[] parts = parentTextOnly.split("\\$");
                    if (parts.length > 1) {
                        String precioStr = parts[parts.length - 1].trim();
                        precioLimpio = precioStr.replace(".", "")
                            .replace(",", ".")
                            .replaceAll("\\s", "")
                            .trim();
                    } else {
                        throw new RuntimeException("No se pudo parsear el precio especial de Jumbo (no se encontró '$'): " + parentTextOnly);
                    }
                
                // CASO 2: Lógica especial para DIA ID 434 (NUEVO)
                } else if ("dia".equalsIgnoreCase(supermercadoSlug) && producto.getId() == 434) {
                    
                    System.out.println("Aplicando lógica de scraping especial para DIA ID: 434 (Por Kg)");
                    By diaSpecialSelector = By.cssSelector("div.diaio-store-5-x-custom_specification_wrapper"); 
                    WebElement precioElement = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(diaSpecialSelector)
                    );
                    String textoCompleto = precioElement.getText(); 
                    String[] parts = textoCompleto.split("\\$");
                    if (parts.length > 1) {
                        String precioStr = parts[parts.length - 1].trim(); // "10.000"
                        precioLimpio = precioStr.replace(".", "")
                            .replace(",", ".")
                            .replaceAll("\\s", "")
                            .trim(); // "10000"
                    } else {
                        throw new RuntimeException("No se pudo parsear el precio por Kg de DIA (no se encontró '$'): " + textoCompleto);
                    }

                // CASO 3: Lógica general de DIA, priorizando precio regular (NUEVO)
                } else if ("dia".equalsIgnoreCase(supermercadoSlug)) {
                    System.out.println("Aplicando lógica general de DIA (buscando precio regular primero)");
                    
                    List<WebElement> regularPriceElements = driver.findElements(
                        By.cssSelector("span.diaio-store-5-x-listPriceValue.strike")
                    );

                    if (!regularPriceElements.isEmpty()) {
                        String textoPrecio = regularPriceElements.get(0).getText();
                        System.out.println("Precio regular (tachado) encontrado: " + textoPrecio);
                        precioLimpio = textoPrecio.replace("$", "")
                            .replace(".", "")
                            .replace(",", ".")
                            .replaceAll("\\s", "")
                            .trim();
                    } else {
                        System.out.println("No se encontró precio regular. Buscando precio principal (oferta/único).");
                        WebElement precioElement = wait.until(
                            ExpectedConditions.visibilityOfElementLocated(By.cssSelector(config.getPriceSelector()))
                        );
                        // Llama a extractPrice (que usará la lógica "else" genérica)
                        precioLimpio = extractPrice(precioElement, config, driver); 
                    }

                // CASO 4: Lógica existente para Disco (EXISTENTE)
                } else if ("disco".equalsIgnoreCase(supermercadoSlug)) {
					precioLimpio = extractDiscoPrice(driver);
                
                // CASO 5: Lógica genérica (incluye Coto, Jumbo genérico, Mas-Online, etc.) (EXISTENTE)
				} else {
					WebElement precioElement = wait.until(
							ExpectedConditions.visibilityOfElementLocated(By.cssSelector(config.getPriceSelector())));
                    // Llama a extractPrice (que tiene la lógica compleja para Coto, Jumbo, etc.)
					precioLimpio = extractPrice(precioElement, config, driver);
				}
                // --- FIN DE LÓGICA FUSIONADA ---


                // Validar precio antes de parsear y guardar (Lógica existente)
				if (precioLimpio == null || precioLimpio.isEmpty()) {
					System.out.println("Precio vacío, usando fallback");
					guardarPrecioFallback(producto);
					continue;
				}

				Double valor;
				try {
					valor = Double.parseDouble(precioLimpio);
                    // Modificación: cambiado a <= 0 para capturar también precios negativos
					if (valor <= 0) { 
						System.out.println("Precio es 0 o inválido, usando fallback");
						guardarPrecioFallback(producto);
						continue;
					}
				} catch (NumberFormatException nfe) {
					System.out.println("Precio inválido (" + precioLimpio + "), usando fallback");
					guardarPrecioFallback(producto);
					continue;
				}

                // Ajuste para DIA: Multiplica por 2 si corresponde (Lógica existente)
				if ("dia".equalsIgnoreCase(supermercadoSlug) && shouldDoubleDiaProduct(producto.getNombre())) {
                    System.out.println("Aplicando duplicación de precio para producto DIA: " + producto.getNombre());
					valor = valor * 2;
				}

				guardarPrecio(producto, valor);

			} catch (Exception e) {
				System.err.println("Error procesando producto: " + producto.getNombre() + " (ID: " + producto.getId() + ")");
				e.printStackTrace();
				guardarPrecioFallback(producto);
			}
		}
	}

	// --- MÉTODOS EXISTENTES (LIMPIADOS DE CARACTERES INVÁLIDOS) ---

    private String extractDiscoPrice(WebDriver driver) {
        try {
            Thread.sleep(3000); // Espera explícita de tu código
            WebElement priceContainer = driver.findElement(By.cssSelector("#priceContainer"));
            String offerText = priceContainer.getText();
            double offerValue = parseDiscoPrice(offerText);
            System.out.println("[DEBUG] Oferta (priceContainer): '" + offerText + "' => " + offerValue);

            // Intentar encontrar el <span> ancestro y regular tachado
            try {
                WebElement spanContainer = priceContainer.findElement(By.xpath("./ancestor::span[1]"));
                List<WebElement> regularDivs = spanContainer.findElements(By.xpath("following-sibling::div[contains(text(), '$')]"));

                if (!regularDivs.isEmpty()) {
                    String regText = regularDivs.get(0).getText();
                    double regVal = parseDiscoPrice(regText);
                    System.out.println("[DEBUG] Regular tachado: '" + regText + "' => " + regVal);
                    if (regVal > offerValue) {
                        System.out.println("[DEBUG] Seleccionado precio REGULAR: '" + regText + "' => " + regVal);
                        return String.valueOf(regVal);
                    }
                }
            } catch (Exception ex) {
                // No hay <span> ancestro o regular tachado, usar priceContainer
                System.out.println("[DEBUG] No se encontró regular tachado, uso priceContainer.");
            }

            // Solo llega aquí si no hay regular tachado, usar offer/único
            System.out.println("[DEBUG] Seleccionado precio OFERTA/ÚNICO: '" + offerText + "' => " + offerValue);
            return String.valueOf(offerValue);

        } catch (Exception e) {
            System.err.println("[DEBUG] Error en extractDiscoPrice: " + e.getMessage());
            throw new RuntimeException("No se pudo extraer el precio de Disco", e);
        }
    }

    private double parseDiscoPrice(String s) {
        try {
            if (s == null) return 0;
            String limpio = s.replace("$", "").replace(".", "").replace(",", ".").replaceAll("\\s", "").trim();
            double resultado = Double.parseDouble(limpio);
            System.out.println("[DEBUG] parseDiscoPrice: '" + s + "' -> '" + limpio + "' -> " + resultado);
            return resultado;
        } catch (Exception e) {
            System.err.println("[DEBUG] parseDiscoPrice error para '" + s + "'");
            return 0;
        }
    }


	private boolean shouldDoubleDiaProduct(String nombreProducto) {
		return DIA_DUPLICATE_PRICE_PRODUCTS.contains(nombreProducto.trim());
	}

	private String extractPrice(WebElement priceElement, ScraperConfig config, WebDriver driver) {
        // La lógica de "jumbo" aquí ahora SÓLO se aplica a productos que NO son 471 o 472
		if ("jumbo".equals(config.getSupermarketSlug())) {
            // Esta lógica ahora se aplica a todos los productos de Jumbo EXCEPTO 471 y 472
			// 1. Buscar precio real (sin rebaja)
			List<WebElement> reales = driver
					.findElements(By.cssSelector("div.jumboargentinaio-store-theme-2t-mVsKNpKjmCAEM_AMCQH"));
			if (!reales.isEmpty()) {
				String texto = reales.get(0).getText().replace("$", "").replace(".", "").replace(",", ".")
						.replaceAll("\\s", "").trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// 2. Si no hay precio real, buscar precio rebajado (oferta)
			List<WebElement> rebajas = driver.findElements(By
					.cssSelector("div.jumboargentinaio-store-theme-1dCOMij_MzTzZOCohX1K7w.vtex-price-format-gallery"));
			if (!rebajas.isEmpty()) {
				String texto = rebajas.get(0).getText().replace("$", "").replace(".", "").replace(",", ".")
						.replaceAll("\\s", "").trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// Si no encontró ningún precio, devuelve "0"
			return "0";
		} else if ("coto".equals(config.getSupermarketSlug())) {
			try {
				Thread.sleep(3000); // Espera explícita de tu código
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// 1. Buscar precio regular
			List<WebElement> preciosRegulares = driver.findElements(By.cssSelector("div.mt-2.small.ng-star-inserted"));
			for (WebElement elem : preciosRegulares) {
				String textoCompleto = elem.getText();
				if (textoCompleto.contains("Precio regular")) {
					// Extraer solo el número de precio (puede venir con espacios)
					String texto = textoCompleto.replace("Precio regular :", "").replace("$", "").replace(".", "")
							.replace(",", ".").replaceAll("\\s", "").trim();
					try {
						double val = Double.parseDouble(texto);
						if (val > 0)
							return String.valueOf(val);
					} catch (NumberFormatException ignored) {
					}
				}
			}
			// 2. Fallback: precio de oferta
			List<WebElement> preciosOferta = driver.findElements(By.cssSelector("span.sale-price"));
			for (WebElement elem : preciosOferta) {
				String texto = elem.getText().replace("$", "").replace(".", "").replace(",", ".").replaceAll("\\s", "")
						.trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// 3. Fallback: precio normal antiguo
			List<WebElement> preciosAntiguos = driver.findElements(By.cssSelector("var.price.h3.ng-star-inserted"));
			for (WebElement elem : preciosAntiguos) {
				String texto = elem.getText().replace("$", "").replace(".", "").replace(",", ".").replaceAll("\\s", "")
						.trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// Si no encontró ningún precio, devuelve "0"
			return "0";
		} else if ("disco".equals(config.getSupermarketSlug())) {
			try {
				Thread.sleep(3000); // Espera explícita de tu código
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// 1. Buscar por id
			List<WebElement> porId = driver.findElements(By.cssSelector("#priceContainer"));
			if (!porId.isEmpty()) {
				String texto = porId.get(0).getText().replace("$", "").replace(".", "").replace(",", ".")
						.replaceAll("\\s", "").trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// 2. Buscar por clase con oferta
			List<WebElement> realesConOferta = driver
					.findElements(By.cssSelector("div.discoargentina-store-theme-2t-mVsKNpKjmCAEM_AMCQH"));
			if (!realesConOferta.isEmpty()) {
				String texto = realesConOferta.get(0).getText().replace("$", "").replace(".", "").replace(",", ".")
						.replaceAll("\\s", "").trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// 3. Buscar por clase sin oferta
			List<WebElement> realesSinOferta = driver.findElements(By.cssSelector(
					"div.discoargentina-store-theme-1dCOMij_MzTzZOCohX1K7w:not(.vtex-price-format-gallery)"));
			if (!realesSinOferta.isEmpty()) {
				String texto = realesSinOferta.get(0).getText().replace("$", "").replace(".", "").replace(",", ".")
						.replaceAll("\\s", "").trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// Si no encontró ningún precio real, devuelve "0" y se usa el fallback del día
			// anterior.
			return "0";
		} else if ("mas-online".equals(config.getSupermarketSlug())) {
			WebDriver d = ((RemoteWebElement) priceElement).getWrappedDriver();
			return extractMasOnlinePrice(d);
        } else {
            // Lógica genérica (usada por el fallback de DIA)
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

			// 1. Espera a que TODOS los posibles contenedores de precio estén presentes.
			List<WebElement> priceContainers = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("span.valtech-gdn-dynamic-product-1-x-currencyContainer")));

			// 2. Si no se encuentra ningún contenedor, devuelve "0".
			if (priceContainers.isEmpty()) {
				System.err.println("No se encontró ningún contenedor de precio para MasOnline.");
				return "0";
			}

			// 3. Determina qué contenedor usar.
			WebElement targetPriceElement = priceContainers.size() > 1 ? priceContainers.get(1)
					: priceContainers.get(0);

			// 4. Obtiene el texto completo del contenedor seleccionado.
			String textoCompleto = targetPriceElement.getText();

			// 5. Limpia el texto.
			String precioLimpio = textoCompleto.split(",")[0].replaceAll("[^\\d]", "");

			// 6. Devuelve el resultado.
			return precioLimpio.isEmpty() ? "0" : precioLimpio;

		} catch (Exception e) {
			System.err.println("Error al extraer precio MasOnline: " + e.getMessage());
			return "0";
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
				System.out.printf("Guardado fallback (no scrapeado): %s - $%.2f (%s)%n", producto.getNombre(),
						ultimoPrecio.get().getValor(), producto.getTipoCanasta());
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

				System.out.printf("Guardado: %s - $%.2f (%s)%n", producto.getNombre(), valor,
						producto.getTipoCanasta());
			} else {
				System.out.println("Ya existía precio para: " + producto.getNombre());
			}
		} catch (Exception e) {
			System.err.println("Error al guardar precio para: " + producto.getNombre());
			guardarPrecioFallback(producto);
		}
	}
}
*/
package com.basic_food_basket.scrapper;

import com.basic_food_basket.model.*;
import com.basic_food_basket.repository.ProductoRepository;
import com.basic_food_basket.service.IPrecioService;

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
import java.util.Set;
import org.openqa.selenium.NoSuchElementException; // Asegúrate de tener este import
import java.util.function.Function;
import java.util.stream.Collectors; // Import necesario para la nueva lógica de MasOnline

@Service
public class GenericScraperService implements IScraperService {

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private IPrecioService precioService;

	private volatile boolean scrapingEnabled = true;
	private WebDriver currentDriver;

	// Lista de productos DIA cuyo precio debe multiplicarse x2
    // (Se mantiene la lista de tu "código complejo")
	private static final Set<String> DIA_DUPLICATE_PRICE_PRODUCTS = Set.of("Café Clásico La Morenita", "Té en saquitos Crysf");

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
		options.addArguments(
				"--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

		try {
			currentDriver = new ChromeDriver(options);
			JavascriptExecutor js = (JavascriptExecutor) currentDriver;

			ScraperConfig config = ScraperConfig.getConfigFor(supermercado.getSlug());

			for (TipoCanasta tipoCanasta : TipoCanasta.values()) {
				if (!scrapingEnabled)
					break;

				try {
					List<Producto> productos = productoRepository
							.findBySupermercadoIdAndTipoCanasta(supermercado.getId(), tipoCanasta);

					if (productos.isEmpty()) {
						System.out.printf("No hay productos para %s (%s)%n", supermercado.getNombre(), tipoCanasta);
						continue;
					}

					System.out.printf("Scrapeando %d productos de %s (%s)%n", productos.size(),
							supermercado.getNombre(), tipoCanasta);

					scrapeProductos(currentDriver, productos, config, js, supermercado.getSlug());
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

	// Método especializado para Disco (Existente)
	private boolean isProductoSinStockDisco(WebDriver driver) {
		try {
			WebElement sinStockElement = driver.findElement(By.cssSelector("p.vtex-outOfStockFlag__text"));
			String texto = sinStockElement.getText().trim().toLowerCase();
			return texto.contains("sin stock");
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	private void scrapeProductos(WebDriver driver, List<Producto> productos, ScraperConfig config,
			JavascriptExecutor js, String supermercadoSlug) {

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
			if (!scrapingEnabled)
				break;

			try {
                // Se agrega el ID al log para facilitar la depuración
				System.out.println("\n--- Procesando producto: " + producto.getNombre() + " (ID: " + producto.getId() + ") ---");
				driver.get(producto.getUrl());

                // Chequeo inmediato de "sin stock" para Disco
				if ("disco".equalsIgnoreCase(supermercadoSlug) && isProductoSinStockDisco(driver)) {
					System.out.println("Producto sin stock, usando precio del día anterior.");
					guardarPrecioFallback(producto);
					continue;
				}

				String precioLimpio;
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // WebDriverWait genérico

                // --- INICIO DE LÓGICA FUSIONADA ---

                // CASO 1: Lógica especial para Jumbo productos 471 y 472
                if ("jumbo".equalsIgnoreCase(supermercadoSlug) &&
                    (producto.getId() == 471 || producto.getId() == 472)) {

                    System.out.println("Aplicando lógica de scraping especial para Jumbo ID: " + producto.getId());
                    By jumboSpecialSelector = By.cssSelector("span.vtex-custom-unit-price"); 
                    WebElement precioElement = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(jumboSpecialSelector)
                    );

                    String allText = precioElement.getText();
                    String parentTextOnly;
                    try {
                         WebElement childDiv = precioElement.findElement(By.cssSelector("div[class*='jumboargentinaio-store-theme']"));
                         String childText = childDiv.getText();
                         parentTextOnly = allText.replace(childText, "").trim();
                    } catch (NoSuchElementException e) {
                         parentTextOnly = allText.trim();
                    }
                    
                    String[] parts = parentTextOnly.split("\\$");
                    if (parts.length > 1) {
                        String precioStr = parts[parts.length - 1].trim();
                        precioLimpio = precioStr.replace(".", "")
                            .replace(",", ".")
                            .replaceAll("\\s", "")
                            .trim();
                    } else {
                        throw new RuntimeException("No se pudo parsear el precio especial de Jumbo (no se encontró '$'): " + parentTextOnly);
                    }
                
                // CASO 2: Lógica especial para DIA ID 434
                } else if ("dia".equalsIgnoreCase(supermercadoSlug) && producto.getId() == 434) {
                    
                    System.out.println("Aplicando lógica de scraping especial para DIA ID: 434 (Por Kg)");
                    By diaSpecialSelector = By.cssSelector("div.diaio-store-5-x-custom_specification_wrapper"); 
                    WebElement precioElement = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(diaSpecialSelector)
                    );
                    String textoCompleto = precioElement.getText(); 
                    String[] parts = textoCompleto.split("\\$");
                    if (parts.length > 1) {
                        String precioStr = parts[parts.length - 1].trim(); // "10.000"
                        precioLimpio = precioStr.replace(".", "")
                            .replace(",", ".")
                            .replaceAll("\\s", "")
                            .trim(); // "10000"
                    } else {
                        throw new RuntimeException("No se pudo parsear el precio por Kg de DIA (no se encontró '$'): " + textoCompleto);
                    }

                // CASO 3: Lógica general de DIA, priorizando precio regular
                } else if ("dia".equalsIgnoreCase(supermercadoSlug)) {
                    System.out.println("Aplicando lógica general de DIA (buscando precio regular primero)");
                    
                    List<WebElement> regularPriceElements = driver.findElements(
                        By.cssSelector("span.diaio-store-5-x-listPriceValue.strike")
                    );

                    if (!regularPriceElements.isEmpty()) {
                        String textoPrecio = regularPriceElements.get(0).getText();
                        System.out.println("Precio regular (tachado) encontrado: " + textoPrecio);
                        precioLimpio = textoPrecio.replace("$", "")
                            .replace(".", "")
                            .replace(",", ".")
                            .replaceAll("\\s", "")
                            .trim();
                    } else {
                        System.out.println("No se encontró precio regular. Buscando precio principal (oferta/único).");
                        WebElement precioElement = wait.until(
                            ExpectedConditions.visibilityOfElementLocated(By.cssSelector(config.getPriceSelector()))
                        );
                        // Llama a extractPrice (que usará la lógica "else" genérica)
                        precioLimpio = extractPrice(precioElement, config, driver); 
                    }

                // CASO 4: Lógica existente para Disco
                } else if ("disco".equalsIgnoreCase(supermercadoSlug)) {
					precioLimpio = extractDiscoPrice(driver);
                
                // CASO 5: Lógica genérica (incluye Coto, Jumbo genérico, Mas-Online, etc.)
				} else {
					WebElement precioElement = wait.until(
							ExpectedConditions.visibilityOfElementLocated(By.cssSelector(config.getPriceSelector())));
                    // Llama a extractPrice (que tiene la lógica compleja para Coto, Jumbo, y el NUEVO Mas-Online)
					precioLimpio = extractPrice(precioElement, config, driver);
				}
                // --- FIN DE LÓGICA FUSIONADA ---


                // Validar precio antes de parsear y guardar (Lógica existente)
				if (precioLimpio == null || precioLimpio.isEmpty()) {
					System.out.println("Precio vacío, usando fallback");
					guardarPrecioFallback(producto);
					continue;
				}

				Double valor;
				try {
					valor = Double.parseDouble(precioLimpio);
                    // Modificación: cambiado a <= 0 para capturar también precios negativos
					if (valor <= 0) { 
						System.out.println("Precio es 0 o inválido, usando fallback");
						guardarPrecioFallback(producto);
						continue;
					}
				} catch (NumberFormatException nfe) {
					System.out.println("Precio inválido (" + precioLimpio + "), usando fallback");
					guardarPrecioFallback(producto);
					continue;
				}

                // Ajuste para DIA: Multiplica por 2 si corresponde (Lógica existente)
				if ("dia".equalsIgnoreCase(supermercadoSlug) && shouldDoubleDiaProduct(producto.getNombre())) {
                    System.out.println("Aplicando duplicación de precio para producto DIA: " + producto.getNombre());
					valor = valor * 2;
				}

				guardarPrecio(producto, valor);

			} catch (Exception e) {
				System.err.println("Error procesando producto: " + producto.getNombre() + " (ID: " + producto.getId() + ")");
				e.printStackTrace();
				guardarPrecioFallback(producto);
			}
		}
	}

	// --- MÉTODOS EXISTENTES (COMPLEJOS) ---

    private String extractDiscoPrice(WebDriver driver) {
        try {
            Thread.sleep(3000); // Espera explícita de tu código
            WebElement priceContainer = driver.findElement(By.cssSelector("#priceContainer"));
            String offerText = priceContainer.getText();
            double offerValue = parseDiscoPrice(offerText);
            System.out.println("[DEBUG] Oferta (priceContainer): '" + offerText + "' => " + offerValue);

            // Intentar encontrar el <span> ancestro y regular tachado
            try {
                WebElement spanContainer = priceContainer.findElement(By.xpath("./ancestor::span[1]"));
                List<WebElement> regularDivs = spanContainer.findElements(By.xpath("following-sibling::div[contains(text(), '$')]"));

                if (!regularDivs.isEmpty()) {
                    String regText = regularDivs.get(0).getText();
                    double regVal = parseDiscoPrice(regText);
                    System.out.println("[DEBUG] Regular tachado: '" + regText + "' => " + regVal);
                    if (regVal > offerValue) {
                        System.out.println("[DEBUG] Seleccionado precio REGULAR: '" + regText + "' => " + regVal);
                        return String.valueOf(regVal);
                    }
                }
            } catch (Exception ex) {
                // No hay <span> ancestro o regular tachado, usar priceContainer
                System.out.println("[DEBUG] No se encontró regular tachado, uso priceContainer.");
            }

            // Solo llega aquí si no hay regular tachado, usar offer/único
            System.out.println("[DEBUG] Seleccionado precio OFERTA/ÚNICO: '" + offerText + "' => " + offerValue);
            return String.valueOf(offerValue);

        } catch (Exception e) {
            System.err.println("[DEBUG] Error en extractDiscoPrice: " + e.getMessage());
            throw new RuntimeException("No se pudo extraer el precio de Disco", e);
        }
    }

    private double parseDiscoPrice(String s) {
        try {
            if (s == null) return 0;
            String limpio = s.replace("$", "").replace(".", "").replace(",", ".").replaceAll("\\s", "").trim();
            double resultado = Double.parseDouble(limpio);
            System.out.println("[DEBUG] parseDiscoPrice: '" + s + "' -> '" + limpio + "' -> " + resultado);
            return resultado;
        } catch (Exception e) {
            System.err.println("[DEBUG] parseDiscoPrice error para '" + s + "'");
            return 0;
        }
    }


	private boolean shouldDoubleDiaProduct(String nombreProducto) {
		return DIA_DUPLICATE_PRICE_PRODUCTS.contains(nombreProducto.trim());
	}

	private String extractPrice(WebElement priceElement, ScraperConfig config, WebDriver driver) {
       
		
		// La lógica de "jumbo" aquí ahora SÓLO se aplica a productos que NO son 471 o 472
		
		/*
		if ("jumbo".equals(config.getSupermarketSlug())) {
			// 1. Buscar precio real (sin rebaja)
			List<WebElement> reales = driver
					.findElements(By.cssSelector("div.jumboargentinaio-store-theme-2t-mVsKNpKjmCAEM_AMCQH"));
			if (!reales.isEmpty()) {
				String texto = reales.get(0).getText().replace("$", "").replace(".", "").replace(",", ".")
						.replaceAll("\\s", "").trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			List<WebElement> rebajas = driver.findElements(By
					.cssSelector("div.jumboargentinaio-store-theme-1dCOMij_MzTzZOCohX1K7w.vtex-price-format-gallery"));
			if (!rebajas.isEmpty()) {
				String texto = rebajas.get(0).getText().replace("$", "").replace(".", "").replace(",", ".")
						.replaceAll("\\s", "").trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			return "0";
		}*/
		// Reemplaza la rama "jumbo" dentro de extractPrice(...) por este bloque
		if ("jumbo".equals(config.getSupermarketSlug())) {

		    // 1) Intentar encontrar el contenedor del producto (productRoot) y guardarlo en un AtomicReference
		    java.util.concurrent.atomic.AtomicReference<WebElement> productRootRef = new java.util.concurrent.atomic.AtomicReference<>(null);
		    try {
		        WebElement pr = priceElement.findElement(By.xpath(
		            "./ancestor::div[contains(@class,'product') or contains(@class,'productCard') or contains(@class,'productDetail') or contains(@class,'product-wrapper')][1]"
		        ));
		        productRootRef.set(pr);
		        System.out.println("[DEBUG] Jumbo: productRoot encontrado via ancestor.");
		    } catch (Exception e) {
		        productRootRef.set(null);
		        System.out.println("[DEBUG] Jumbo: productRoot no encontrado, usaré driver como fallback.");
		    }

		    // helper para buscar dentro de productRoot si existe, sino en driver
		    java.util.function.Function<String, List<WebElement>> findInRoot = (css) -> {
		        WebElement productRoot = productRootRef.get();
		        if (productRoot != null) return productRoot.findElements(By.cssSelector(css));
		        return driver.findElements(By.cssSelector(css));
		    };

		    // normalizador de texto a número
		    java.util.function.Function<String, String> normalize = (txt) -> {
		        if (txt == null) return "";
		        return txt.replace("$", "").replace(".", "").replace(",", ".").replaceAll("\\s", "").trim();
		    };

		    // 2) PRIORIDAD: PRECIO TACHADO / "Precio regular" dentro del productRoot
		    List<WebElement> tachados = findInRoot.apply("span.strike, span.diaio-store-5-x-listPriceValue.strike, div[class*='listPrice'], span[class*='weighableListPrice'], del");
		    if (!tachados.isEmpty()) {
		        for (WebElement t : tachados) {
		            String txt = normalize.apply(t.getText());
		            if (!txt.isEmpty()) {
		                try {
		                    double v = Double.parseDouble(txt);
		                    if (v > 0) {
		                        System.out.println("[DEBUG] Jumbo: precio TACHADO encontrado en productRoot: " + v);
		                        return String.valueOf(v);
		                    }
		                } catch (NumberFormatException ignored) {}
		            }
		        }
		    }

		    // 3) Detectar etiquetas tipo "Llevando 2" (si es necesario para lógica)
		    boolean hasLlevando2 = false;
		    try {
		        // XPath con contains(text()) es más robusto para encontrar textos como "Llevando 2"
		        List<WebElement> llevandoElems = productRootRef.get() != null
		            ? productRootRef.get().findElements(By.xpath(".//div[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), 'llevando 2') or contains(@class,'14k7D0cUQ_45k_MeZ_yfFo')]"))
		            : driver.findElements(By.xpath("//div[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), 'llevando 2') or contains(@class,'14k7D0cUQ_45k_MeZ_yfFo')]"));
		        hasLlevando2 = !llevandoElems.isEmpty();
		    } catch (Exception ignored) {}

		    // 4) Buscar precio "final" visible dentro del productRoot (ej. div.jumboargentinaio-store-theme-2t-...)
		    List<WebElement> precioFinales = findInRoot.apply("div.jumboargentinaio-store-theme-2t-mVsKNpKjmCAEM_AMCQH, div[class*='priceFinal'], span[class*='vtex-custom-unit-price']");
		    if (!precioFinales.isEmpty()) {
		        for (WebElement pf : precioFinales) {
		            String txt = normalize.apply(pf.getText());
		            if (!txt.isEmpty()) {
		                try {
		                    double v = Double.parseDouble(txt);
		                    if (v > 0) {
		                        System.out.println("[DEBUG] Jumbo: precio FINAL encontrado en productRoot: " + v);
		                        return String.valueOf(v);
		                    }
		                } catch (NumberFormatException ignored) {}
		            }
		        }
		    }

		    // 5) Buscar precios de oferta/galería (ej. unitario c/u) dentro del productRoot
		    List<WebElement> ofertas = findInRoot.apply("div.jumboargentinaio-store-theme-1dCOMij_MzTzZOCohX1K7w.vtex-price-format-gallery, span[class*='sale'], div[class*='salePrice']");
		    if (!ofertas.isEmpty()) {
		        for (WebElement o : ofertas) {
		            String txt = normalize.apply(o.getText());
		            if (!txt.isEmpty()) {
		                try {
		                    double v = Double.parseDouble(txt);
		                    if (v > 0) {
		                        System.out.println("[DEBUG] Jumbo: precio OFERTA encontrado en productRoot: " + v);
		                        return String.valueOf(v);
		                    }
		                } catch (NumberFormatException ignored) {}
		            }
		        }
		    }

		    // 6) fallback al priceElement recibido (por si priceElement ya apunta al contenedor correcto)
		    try {
		        String txt = normalize.apply(priceElement.getText());
		        if (!txt.isEmpty()) {
		            double v = Double.parseDouble(txt);
		            if (v > 0) {
		                System.out.println("[DEBUG] Jumbo: usando priceElement como fallback: " + v);
		                return String.valueOf(v);
		            }
		        }
		    } catch (Exception ignored) {}

		    // 7) Búsqueda global como último recurso (primero finales, luego ofertas, luego tachados)
		    try {
		        List<WebElement> globalFinal = driver.findElements(By.cssSelector("div.jumboargentinaio-store-theme-2t-mVsKNpKjmCAEM_AMCQH, div[class*='priceFinal']"));
		        for (WebElement e : globalFinal) {
		            String txt = normalize.apply(e.getText());
		            try { double v = Double.parseDouble(txt); if (v > 0) { System.out.println("[DEBUG] Jumbo: global final fallback: " + v); return String.valueOf(v); } }
		            catch (NumberFormatException ignored) {}
		        }
		        List<WebElement> globalOfertas = driver.findElements(By.cssSelector("div.jumboargentinaio-store-theme-1dCOMij_MzTzZOCohX1K7w.vtex-price-format-gallery, span[class*='sale']"));
		        for (WebElement e : globalOfertas) {
		            String txt = normalize.apply(e.getText());
		            try { double v = Double.parseDouble(txt); if (v > 0) { System.out.println("[DEBUG] Jumbo: global oferta fallback: " + v); return String.valueOf(v); } }
		            catch (NumberFormatException ignored) {}
		        }
		        List<WebElement> globalTachados = driver.findElements(By.cssSelector("span.strike, del, span[class*='listPrice']"));
		        for (WebElement e : globalTachados) {
		            String txt = normalize.apply(e.getText());
		            try { double v = Double.parseDouble(txt); if (v > 0) { System.out.println("[DEBUG] Jumbo: global tachado (último recurso): " + v); return String.valueOf(v); } }
		            catch (NumberFormatException ignored) {}
		        }
		    } catch (Exception ignored) {}

		    return "0";
		}
		else if ("coto".equals(config.getSupermarketSlug())) {
			try {
				Thread.sleep(3000); // Espera explícita de tu código
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// 1. Buscar precio regular
			List<WebElement> preciosRegulares = driver.findElements(By.cssSelector("div.mt-2.small.ng-star-inserted"));
			for (WebElement elem : preciosRegulares) {
				String textoCompleto = elem.getText();
				if (textoCompleto.contains("Precio regular")) {
					String texto = textoCompleto.replace("Precio regular :", "").replace("$", "").replace(".", "")
							.replace(",", ".").replaceAll("\\s", "").trim();
					try {
						double val = Double.parseDouble(texto);
						if (val > 0)
							return String.valueOf(val);
					} catch (NumberFormatException ignored) {
					}
				}
			}
			// 2. Fallback: precio de oferta
			List<WebElement> preciosOferta = driver.findElements(By.cssSelector("span.sale-price"));
			for (WebElement elem : preciosOferta) {
				String texto = elem.getText().replace("$", "").replace(".", "").replace(",", ".").replaceAll("\\s", "")
						.trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// 3. Fallback: precio normal antiguo
			List<WebElement> preciosAntiguos = driver.findElements(By.cssSelector("var.price.h3.ng-star-inserted"));
			for (WebElement elem : preciosAntiguos) {
				String texto = elem.getText().replace("$", "").replace(".", "").replace(",", ".").replaceAll("\\s", "")
						.trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// Si no encontró ningún precio, devuelve "0"
			return "0";
		} else if ("disco".equals(config.getSupermarketSlug())) {
			try {
				Thread.sleep(3000); // Espera explícita de tu código
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// 1. Buscar por id
			List<WebElement> porId = driver.findElements(By.cssSelector("#priceContainer"));
			if (!porId.isEmpty()) {
				String texto = porId.get(0).getText().replace("$", "").replace(".", "").replace(",", ".")
						.replaceAll("\\s", "").trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// 2. Buscar por clase con oferta
			List<WebElement> realesConOferta = driver
					.findElements(By.cssSelector("div.discoargentina-store-theme-2t-mVsKNpKjmCAEM_AMCQH"));
			if (!realesConOferta.isEmpty()) {
				String texto = realesConOferta.get(0).getText().replace("$", "").replace(".", "").replace(",", ".")
						.replaceAll("\\s", "").trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// 3. Buscar por clase sin oferta
			List<WebElement> realesSinOferta = driver.findElements(By.cssSelector(
					"div.discoargentina-store-theme-1dCOMij_MzTzZOCohX1K7w:not(.vtex-price-format-gallery)"));
			if (!realesSinOferta.isEmpty()) {
				String texto = realesSinOferta.get(0).getText().replace("$", "").replace(".", "").replace(",", ".")
						.replaceAll("\\s", "").trim();
				try {
					double val = Double.parseDouble(texto);
					if (val > 0)
						return String.valueOf(val);
				} catch (NumberFormatException ignored) {
				}
			}
			// Si no encontró ningún precio real, devuelve "0"
			return "0";
		
        // --- INICIO DE MODIFICACIÓN MASONLINE ---
        } else if ("mas-online".equals(config.getSupermarketSlug())) {
			WebDriver d = ((RemoteWebElement) priceElement).getWrappedDriver();
            // Llama al NUEVO método extractMasOnlinePrice
			return extractMasOnlinePrice(d);
        // --- FIN DE MODIFICACIÓN MASONLINE ---

        } else {
            // Lógica genérica (usada por el fallback de DIA)
			String textoPrecio = priceElement.getText();
			return textoPrecio.replace("$", "")
                .replace(".", "")
                .replace(",", ".")
                .replaceAll("\\s", "")
                .trim();
		}

	}

	/*
	// --- MÉTODO 'extractMasOnlinePrice' ACTUALIZADO ---
    // Esta es la nueva versión que prioriza el precio regular
	private String extractMasOnlinePrice(WebDriver driver) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            WebElement priceContainer;

            // 1. Intentar encontrar el PRECIO REGULAR (lista/tachado) primero.
            //    (Este es el caso del "LLEVANDO 2" - $239,00)
            List<WebElement> regularPriceElements = driver.findElements(
                By.cssSelector("span.valtech-gdn-dynamic-product-1-x-weighableListPrice")
            );

            if (!regularPriceElements.isEmpty()) {
                // 1a. Si existe, usa el precio regular.
                System.out.println("[DEBUG] MasOnline: Precio regular (weighableListPrice) encontrado.");
                priceContainer = regularPriceElements.get(0);
            } else {
                // 1b. Si no existe (es una oferta "normal"), usa la lógica anterior
                //     (Este es el caso de tus "otros productos con oferta")
                System.out.println("[DEBUG] MasOnline: No se encontró precio regular. Buscando precio principal (dynamicProductPrice).");
                
                // La lógica original usaba "span.valtech-gdn-dynamic-product-1-x-currencyContainer"
                // Pero el 'priceElement' que se pasa a extractPrice ya apunta a 'dynamicProductPrice'
                // así que esperamos por ese contenedor general.
                priceContainer = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("span[class*='dynamicProductPrice']")
                    )
                );

                // Esta es la lógica del "get(1)" que tenías, pero es más segura
                // si buscamos *dentro* del dynamicProductPrice
                List<WebElement> currencyContainers = priceContainer.findElements(By.cssSelector("span.valtech-gdn-dynamic-product-1-x-currencyContainer"));
                if (currencyContainers.size() > 1) {
                    priceContainer = currencyContainers.get(1); // Tomar el segundo (regular)
                } else if (!currencyContainers.isEmpty()) {
                    priceContainer = currencyContainers.get(0); // Tomar el primero (oferta/único)
                }
                // Si 'currencyContainers' está vacío, 'priceContainer' sigue siendo el 'dynamicProductPrice'
            }

            // 2. Extraer las partes del precio del contenedor seleccionado.
            // Se usa findElements para ser más robusto si no hay 'currencyInteger' (ej. precio es 0)
            List<WebElement> integerParts = priceContainer.findElements(
                By.cssSelector("span[class*='currencyInteger']")
            );
            WebElement fractionPart = priceContainer.findElement(
                By.cssSelector("span[class*='currencyFraction']")
            );

            String fullInteger = integerParts.stream()
                .map(WebElement::getText)
                .collect(Collectors.joining());

            // Devuelve el formato "INTEGER.FRACTION" (ej: "239.00")
            // O limpia el texto completo si la estructura es inesperada
            if (fullInteger.isEmpty()) {
                System.out.println("[DEBUG] MasOnline: No se encontró currencyInteger, limpiando texto completo.");
                String textoCompleto = priceContainer.getText();
                String precioLimpio = textoCompleto.split(",")[0].replaceAll("[^\\d]", "");
                return precioLimpio.isEmpty() ? "0" : precioLimpio;
            }

            return fullInteger + "." + fractionPart.getText();


		} catch (Exception e) {
			System.err.println("Error al extraer precio MasOnline: " + e.getMessage());
			return "0"; // Devuelve 0 para activar el fallback
		}
	}
    // --- FIN DEL MÉTODO ACTUALIZADO ---
*/
	// --- MÉTODO 'extractMasOnlinePrice' ACTUALIZADO ---
	// Esta es la nueva versión que prioriza el precio regular
	private String extractMasOnlinePrice(WebDriver driver) {
	    try {
	        // Creamos dos esperas:
	        // Una corta para el precio regular (opcional)
	        WebDriverWait optionalWait = new WebDriverWait(driver, Duration.ofSeconds(5)); 
	        // Una larga para el precio principal (obligatorio)
	        WebDriverWait mandatoryWait = new WebDriverWait(driver, Duration.ofSeconds(15));
	        
	        WebElement priceContainer;

	        try {
	            // 1. INTENTAR encontrar el PRECIO REGULAR (lista/tachado) primero.
	            //    (Este es el caso del "LLEVANDO 2" o "2da al 70%")
	            System.out.println("[DEBUG] MasOnline: Intentando encontrar precio regular (weighableListPrice)...");
	            priceContainer = optionalWait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                    By.cssSelector("span.valtech-gdn-dynamic-product-1-x-weighableListPrice")
	                )
	            );
	            System.out.println("[DEBUG] MasOnline: Precio regular (weighableListPrice) encontrado.");

	        } catch (TimeoutException e) {
	            // 2. FALLBACK: Si no lo encuentra (porque es una oferta "normal" o no tiene oferta)
	            //    Usamos la lógica que ya funcionaba.
	            System.out.println("[DEBUG] MasOnline: No se encontró 'weighableListPrice'. Buscando precio principal (dynamicProductPrice)...");
	            
	            WebElement dynamicPriceContainer = mandatoryWait.until(
	                ExpectedConditions.presenceOfElementLocated(
	                    By.cssSelector("span[class*='dynamicProductPrice']")
	                )
	            );

	            // Esta es la lógica que tenías para la oferta "normal" (buscar el 2do contenedor)
	            List<WebElement> currencyContainers = dynamicPriceContainer.findElements(
	                By.cssSelector("span.valtech-gdn-dynamic-product-1-x-currencyContainer")
	            );
	            
	            if (currencyContainers.size() > 1) {
	                System.out.println("[DEBUG] MasOnline: Encontrados múltiples 'currencyContainer', usando el segundo (precio regular).");
	                priceContainer = currencyContainers.get(1); // Tomar el segundo (regular)
	            } else if (!currencyContainers.isEmpty()) {
	                System.out.println("[DEBUG] MasOnline: Encontrado un solo 'currencyContainer', usando el primero (oferta/único).");
	                priceContainer = currencyContainers.get(0); // Tomar el primero (oferta/único)
	            } else {
	                System.out.println("[DEBUG] MasOnline: No se encontró 'currencyContainer' hijo, usando 'dynamicProductPrice' padre.");
	                priceContainer = dynamicPriceContainer; // Fallback al contenedor padre
	            }
	        }

	        // 3. Extraer las partes del precio del contenedor seleccionado.
	        //    (Esta lógica es la misma para todos los casos)
	        List<WebElement> integerParts = priceContainer.findElements(
	            By.cssSelector("span[class*='currencyInteger']")
	        );
	        WebElement fractionPart = priceContainer.findElement(
	            By.cssSelector("span[class*='currencyFraction']")
	        );

	        String fullInteger = integerParts.stream()
	            .map(WebElement::getText)
	            .collect(Collectors.joining());

	        // Si no encontró "integerParts" (ej. precio es "0"), intenta limpiar el texto completo.
	        if (fullInteger.isEmpty()) {
	            System.out.println("[DEBUG] MasOnline: No se encontró 'currencyInteger', limpiando texto completo del contenedor.");
	            String textoCompleto = priceContainer.getText();
	            // Limpia el texto: "$ 2.699,00" -> "2699"
	            String precioLimpio = textoCompleto.split(",")[0].replaceAll("[^\\d]", "");
	            
	            // Devuelve el precio o "0" si está vacío
	            return precioLimpio.isEmpty() ? "0" : precioLimpio;
	        }

	        // Devuelve el formato "INTEGER.FRACTION" (ej: "2699.00")
	        return fullInteger + "." + fractionPart.getText();


	    } catch (Exception e) {
	        System.err.println("Error al extraer precio MasOnline: " + e.getMessage());
	        e.printStackTrace(); // Imprime el error para ver qué falló
	        return "0"; // Devuelve 0 para activar el fallback
	    }
	}
	// --- FIN DEL MÉTODO ACTUALIZADO ---

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
				System.out.printf("Guardado fallback (no scrapeado): %s - $%.2f (%s)%n", producto.getNombre(),
						ultimoPrecio.get().getValor(), producto.getTipoCanasta());
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

				System.out.printf("Guardado: %s - $%.2f (%s)%n", producto.getNombre(), valor,
						producto.getTipoCanasta());
			} else {
				System.out.println("Ya existía precio para: " + producto.getNombre());
			}
		} catch (Exception e) {
			System.err.println("Error al guardar precio para: " + producto.getNombre());
			guardarPrecioFallback(producto);
		}
	}
}