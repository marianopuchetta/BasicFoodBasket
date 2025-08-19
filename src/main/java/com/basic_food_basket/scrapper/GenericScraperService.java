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

@Service
public class GenericScraperService implements IScraperService {

	@Autowired
	private ProductoRepository productoRepository;

	@Autowired
	private IPrecioService precioService;

	private volatile boolean scrapingEnabled = true;
	private WebDriver currentDriver;

	// Lista de productos DIA cuyo precio debe multiplicarse x2
	private static final Set<String> DIA_DUPLICATE_PRICE_PRODUCTS = Set.of("Café Clásico La Morenita",
			"Café Sensaciones Bonafide Torrado Intenso", "Té en saquitos Crysf", "Té Saquitos en Sobre Green Hills",
			"Carne Picada común Atmósfera Modificada 600 Gr.", "Carne Picada de Nalga x 500 Gr.");

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

	private void scrapeProductos(WebDriver driver, List<Producto> productos, ScraperConfig config,
			JavascriptExecutor js, String supermercadoSlug) {

		for (Producto producto : productos) {
			if (!scrapingEnabled)
				break;

			try {
				System.out.println("\n--- Procesando producto: " + producto.getNombre() + " ---");
				driver.get(producto.getUrl());

				WebDriverWait initialWait = new WebDriverWait(driver, Duration.ofSeconds(config.getTimeoutSeconds()));
				initialWait.until(webDriver -> ((JavascriptExecutor) webDriver)
						.executeScript("return document.readyState").equals("complete"));

				WebDriverWait waitPrecio = new WebDriverWait(driver, Duration.ofSeconds(20));
				WebElement precioElement = null;

				try {
					precioElement = waitPrecio.until(
							ExpectedConditions.visibilityOfElementLocated(By.cssSelector(config.getPriceSelector())));
				} catch (TimeoutException e1) {
					if ("disco".equalsIgnoreCase(supermercadoSlug)) {
						try {
							precioElement = waitPrecio.until(ExpectedConditions
									.visibilityOfElementLocated(By.cssSelector("span[class*='sellingPrice']")));
							System.out.println("Precio encontrado usando selector alternativo para Disco.");
						} catch (TimeoutException e2) {
							System.err.println("No se encontró el precio con ninguno de los selectores para Disco.");
							throw e1;
						}
					} else {
						throw e1;
					}
				}

				String precioLimpio = extractPrice(precioElement, config, driver);
				Double valor = Double.parseDouble(precioLimpio);

				if ("dia".equalsIgnoreCase(supermercadoSlug) && shouldDoubleDiaProduct(producto.getNombre())) {
					valor = valor * 2;
				}

				if (valor == 0.0) {
					Optional<Precio> ultimoPrecio = precioService.findUltimoPrecioByProducto(producto);
					if (ultimoPrecio.isPresent()) {
						Precio precioFallback = new Precio();
						precioFallback.setProducto(producto);
						precioFallback.setFecha(LocalDate.now());
						precioFallback.setValor(ultimoPrecio.get().getValor());
						precioFallback.setScrapeado(false);
						precioService.guardarPrecio(precioFallback);
						System.out.printf("Guardado fallback por valor 0: %s - $%.2f (%s)%n", producto.getNombre(),
								ultimoPrecio.get().getValor(), producto.getTipoCanasta());
					} else {
						guardarPrecio(producto, valor);
					}
				} else {
					guardarPrecio(producto, valor);
				}

			} catch (Exception e) {
				System.err.println("Error procesando producto: " + producto.getNombre());
				e.printStackTrace();
				guardarPrecioFallback(producto);
			}
		}
	}

	private boolean shouldDoubleDiaProduct(String nombreProducto) {
		return DIA_DUPLICATE_PRICE_PRODUCTS.contains(nombreProducto.trim());
	}

	private String extractPrice(WebElement priceElement, ScraperConfig config, WebDriver driver) {
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
		}

		else if ("disco".equals(config.getSupermarketSlug())) {
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
			String textoPrecio = priceElement.getText();
			return textoPrecio.replace("$", "").replace(".", "").replace(",", ".").replaceAll("\\s", "").trim();
		}

	}

	private String extractMasOnlinePrice(WebDriver driver) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			List<WebElement> containers = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
					By.cssSelector("span.valtech-gdn-dynamic-product-1-x-currencyContainer")));
			// Si hay más de uno, priorizá el segundo (precio real). Si solo hay uno, usá
			// ese.
			WebElement priceContainer = containers.size() > 1 ? containers.get(1) : containers.get(0);

			StringBuilder precioBuilder = new StringBuilder();
			List<WebElement> partes = priceContainer.findElements(By.xpath("./span"));
			for (WebElement parte : partes) {
				String clase = parte.getAttribute("class");
				String texto = parte.getText().replaceAll("[^\\d]", "");
				if (clase.contains("currencyInteger") || clase.contains("currencyGroup")) {
					precioBuilder.append(texto);
				}
			}
			// El resultado será algo como "2249" para "2.249,00"
			return precioBuilder.toString().isEmpty() ? "0" : precioBuilder.toString();
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