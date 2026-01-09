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
import org.openqa.selenium.NoSuchElementException;
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

	private static final Set<String> DIA_DUPLICATE_PRICE_PRODUCTS = Set.of("Café Clásico La Morenita", "Té en saquitos DIA");

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
                System.out.println("\n--- Procesando producto: " + producto.getNombre() + " (ID: " + producto.getId() + ") ---");
				driver.get(producto.getUrl());

                if ("jumbo".equalsIgnoreCase(supermercadoSlug) || "disco".equalsIgnoreCase(supermercadoSlug)) {
                    try {
                        WebDriverWait gondolaWait = new WebDriverWait(driver, Duration.ofSeconds(3));
                        gondolaWait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("div.vtex-flex-layout-0-x-flexRowContent--row-opss-notfound")
                        ));
                        System.out.println("Producto no encontrado (Góndola Vacía). Usando fallback.");
                        guardarPrecioFallback(producto);
                        continue;
                    } catch (TimeoutException e) {
                        System.out.println("[DEBUG] Góndola Vacía no detectada, continuando.");
                    }
                }

				if ("disco".equalsIgnoreCase(supermercadoSlug) && isProductoSinStockDisco(driver)) {
					System.out.println("Producto sin stock, usando precio del día anterior.");
					guardarPrecioFallback(producto);
					continue;
				}

				String precioLimpio;
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

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
                
                } else if ("dia".equalsIgnoreCase(supermercadoSlug) && producto.getId() == 434) {
                    
					 System.out.println("Aplicando lógica de scraping especial para DIA ID: 434 (Por Kg)");
                    By diaSpecialSelector = By.cssSelector("div.diaio-store-5-x-custom_specification_wrapper");
					 WebElement precioElement = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(diaSpecialSelector)
                    );
					 String textoCompleto = precioElement.getText();
                    String[] parts = textoCompleto.split("\\$");
                    if (parts.length > 1) {
                        String precioStr = parts[parts.length - 1].trim();
						precioLimpio = precioStr.replace(".", "")
                            .replace(",", ".")
                            .replaceAll("\\s", "")
                            .trim();
                    } else {
                        throw new RuntimeException("No se pudo parsear el precio por Kg de DIA (no se encontró '$'): " + textoCompleto);
					}

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
						precioLimpio = extractPrice(precioElement, config, driver);
					}

                } else if ("disco".equalsIgnoreCase(supermercadoSlug)) {
					precioLimpio = extractDiscoPrice(driver);
				} else {
					WebElement precioElement = wait.until(
							ExpectedConditions.visibilityOfElementLocated(By.cssSelector(config.getPriceSelector())));
					precioLimpio = extractPrice(precioElement, config, driver);
				}

				if (precioLimpio == null || precioLimpio.isEmpty()) {
					System.out.println("Precio vacío, usando fallback");
					guardarPrecioFallback(producto);
					continue;
				}

				Double valor;
				try {
					valor = Double.parseDouble(precioLimpio);
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

    private String extractDiscoPrice(WebDriver driver) {
        try {
            Thread.sleep(3000);
            WebElement priceContainer = driver.findElement(By.cssSelector("#priceContainer"));
			 String offerText = priceContainer.getText();
            double offerValue = parseDiscoPrice(offerText);
            System.out.println("[DEBUG] Oferta (priceContainer): '" + offerText + "' => " + offerValue);
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
                System.out.println("[DEBUG] No se encontró regular tachado, uso priceContainer.");
			}

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
	    
	    if ("jumbo".equals(config.getSupermarketSlug())) {

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

	        java.util.function.Function<String, List<WebElement>> findInRoot = (css) -> {
	            WebElement productRoot = productRootRef.get();
	            if (productRoot != null) return productRoot.findElements(By.cssSelector(css));
	            return driver.findElements(By.cssSelector(css));
	        };

	        java.util.function.Function<String, String> normalize = (txt) -> {
	            if (txt == null) return "";
	            return txt.replace("$", "").replace(".", "").replace(",", ".").replaceAll("\\s", "").trim();
	        };

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

	        boolean hasLlevando2 = false;
	        try {
	            List<WebElement> llevandoElems = productRootRef.get() != null
	                ? 
	                productRootRef.get().findElements(By.xpath(".//div[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), 'llevando 2') or contains(@class,'14k7D0cUQ_45k_MeZ_yfFo')]"))
	                : driver.findElements(By.xpath("//div[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), 'llevando 2') or contains(@class,'14k7D0cUQ_45k_MeZ_yfFo')]"));
	            hasLlevando2 = !llevandoElems.isEmpty();
	        } catch (Exception ignored) {}

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

	        // ❌ ELIMINADO: Búsqueda global problemática
	        // ✅ En su lugar, devolver 0 para activar el fallback controlado
	        System.out.println("[DEBUG] Jumbo: No se pudo extraer precio específico del producto, activando fallback");
	        return "0";
	    }
	    else if ("coto".equals(config.getSupermarketSlug())) {
	        try {
	            Thread.sleep(3000);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
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
	        return "0";
	    } else if ("disco".equals(config.getSupermarketSlug())) {
	        try {
	            Thread.sleep(3000);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
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
	        return "0";
	    } else if ("mas-online".equals(config.getSupermarketSlug())) {
	        WebDriver d = ((RemoteWebElement) priceElement).getWrappedDriver();
	        return extractMasOnlinePrice(d);
	    } else if ("carrefour".equals(config.getSupermarketSlug())) {
            // No usamos 'priceElement' directo porque necesitamos buscar en toda la página 
            // para priorizar el precio tachado.
            return extractCarrefourPrice(driver);
        // --- FIN AGREGADO CARREFOUR ---
       }
	    else {
	        String textoPrecio = priceElement.getText();
	        return textoPrecio.replace("$", "")
	            .replace(".", "")
	            .replace(",", ".")
	            .replaceAll("\\s", "")
	            .trim();
	    }
	}
	// --- NUEVO MÉTODO PARA CARREFOUR ---
		private String extractCarrefourPrice(WebDriver driver) {
			try {
				WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
				WebElement priceContainer = null;

				// 1. PRIORIDAD: Buscar PRECIO DE LISTA (Tachado/Regular)
				// Usamos findElements para no lanzar excepción si no existe (caso producto sin oferta)
				List<WebElement> listPriceElements = driver.findElements(
					By.cssSelector("span[class*='valtech-carrefourar-product-price-0-x-listPriceValue']")
				);

				if (!listPriceElements.isEmpty()) {
					// Si encontramos elementos tachados, tomamos el primero visible
					for (WebElement element : listPriceElements) {
						if (element.isDisplayed()) {
							System.out.println("[DEBUG] Carrefour: Precio de lista (tachado) encontrado.");
							priceContainer = element;
							break;
						}
					}
				}

				// 2. FALLBACK: Si no hay precio tachado, buscar el PRECIO DE VENTA (Normal)
				if (priceContainer == null) {
					System.out.println("[DEBUG] Carrefour: No hay oferta. Buscando precio normal.");
					// Buscamos el sellingPriceValue o el currencyContainer genérico que no sea hijo de un listPrice
					// Esperamos explícitamente porque es el dato obligatorio
					priceContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
						By.cssSelector("span[class*='valtech-carrefourar-product-price-0-x-currencyContainer']")
					));
				}

				// 3. Extracción y Limpieza del texto
				// El formato de Carrefour es: $ 1.990,00
				String texto = priceContainer.getText();
				
				// Limpieza estándar: quitar $, quitar puntos de miles, cambiar coma decimal por punto
				String precioLimpio = texto.replace("$", "")
										   .replace(".", "")
										   .replace(",", ".")
										   .replaceAll("\\s", "")
										   .trim();

				System.out.println("[DEBUG] Carrefour: Precio extraído final: " + precioLimpio);
				
				// Validación básica
				if (precioLimpio.isEmpty()) return "0";
				
				return precioLimpio;

			} catch (Exception e) {
				System.err.println("Error al extraer precio Carrefour: " + e.getMessage());
				return "0"; // Fallback para que el sistema intente usar el precio histórico
			}
		}
	
	private String extractMasOnlinePrice(WebDriver driver) {
	    try {
	        WebDriverWait optionalWait = new WebDriverWait(driver, Duration.ofSeconds(5));
	        WebDriverWait mandatoryWait = new WebDriverWait(driver, Duration.ofSeconds(15));
			 WebElement priceContainer;

	        try {
	            System.out.println("[DEBUG] MasOnline: Intentando encontrar precio regular (weighableListPrice)...");
				 priceContainer = optionalWait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                    By.cssSelector("span.valtech-gdn-dynamic-product-1-x-weighableListPrice")
	                )
	            );
				 System.out.println("[DEBUG] MasOnline: Precio regular (weighableListPrice) encontrado.");

	        } catch (TimeoutException e) {
				 System.out.println("[DEBUG] MasOnline: No se encontró 'weighableListPrice'. Buscando precio principal (dynamicProductPrice)...");
				 WebElement dynamicPriceContainer = mandatoryWait.until(
	                ExpectedConditions.presenceOfElementLocated(
	                    By.cssSelector("span[class*='dynamicProductPrice']")
	                )
	            );
				List<WebElement> currencyContainers = dynamicPriceContainer.findElements(
	                By.cssSelector("span.valtech-gdn-dynamic-product-1-x-currencyContainer")
	            );
				 if (currencyContainers.size() > 1) {
	                System.out.println("[DEBUG] MasOnline: Encontrados múltiples 'currencyContainer', usando el segundo (precio regular).");
					 priceContainer = currencyContainers.get(1);
	            } else if (!currencyContainers.isEmpty()) {
	                System.out.println("[DEBUG] MasOnline: Encontrado un solo 'currencyContainer', usando el primero (oferta/único).");
					 priceContainer = currencyContainers.get(0);
	            } else {
	                System.out.println("[DEBUG] MasOnline: No se encontró 'currencyContainer' hijo, usando 'dynamicProductPrice' padre.");
					 priceContainer = dynamicPriceContainer;
	            }
	        }

	        List<WebElement> integerParts = priceContainer.findElements(
	            By.cssSelector("span[class*='currencyInteger']")
	        );
			 WebElement fractionPart = priceContainer.findElement(
	            By.cssSelector("span[class*='currencyFraction']")
	        );
			 String fullInteger = integerParts.stream()
	            .map(WebElement::getText)
	            .collect(Collectors.joining());
			 if (fullInteger.isEmpty()) {
	            System.out.println("[DEBUG] MasOnline: No se encontró 'currencyInteger', limpiando texto completo del contenedor.");
				 String textoCompleto = priceContainer.getText();
	            String precioLimpio = textoCompleto.split(",")[0].replaceAll("[^\\d]", "");
	            return precioLimpio.isEmpty() ?
					 "0" : precioLimpio;
	        }

	        return fullInteger + "."
				 + fractionPart.getText();

	    } catch (Exception e) {
	        System.err.println("Error al extraer precio MasOnline: " + e.getMessage());
			 e.printStackTrace();
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