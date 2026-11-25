package br.unisagrado.p2.e2e;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Base dos testes de sistema/interface do grupo P2.
 * <p>
 * Requer o back-end (porta 9966) e o front-end Angular (porta 4200) em execução; caso contrário os
 * testes são ignorados. Propriedades: {@code -Dbrowser=chrome|edge|firefox}, {@code -Dheadless=true},
 * {@code -DfrontUrl=http://localhost:4200}. Em caso de falha, salva captura de tela em
 * {@code target/e2e-screenshots}.
 */
@Tag("e2e")
abstract class E2eTestBase {

    protected static final String FRONT_URL = System.getProperty("frontUrl", "http://localhost:4200");

    protected WebDriver driver;

    @RegisterExtension
    final AfterTestExecutionCallback screenshotOnFailure = context -> {
        if (context.getExecutionException().isPresent() && driver != null) {
            Path dir = Path.of("target", "e2e-screenshots");
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path file = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
            Files.copy(file, dir.resolve(context.getDisplayName().replaceAll("[^\\w-]", "_") + "_" + stamp + ".png"),
                StandardCopyOption.REPLACE_EXISTING);
        }
    };

    @BeforeEach
    void startBrowser() {
        Assumptions.assumeTrue(isUp(FRONT_URL), "Front-end não está em execução em " + FRONT_URL);
        boolean headless = Boolean.getBoolean("headless");
        driver = switch (System.getProperty("browser", "chrome")) {
            case "edge" -> new EdgeDriver(headless ? new EdgeOptions().addArguments("--headless=new") : new EdgeOptions());
            case "firefox" -> new FirefoxDriver(headless ? new FirefoxOptions().addArguments("-headless") : new FirefoxOptions());
            default -> new ChromeDriver(headless ? new ChromeOptions().addArguments("--headless=new") : new ChromeOptions());
        };
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1366, 768));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Verifica se o front-end responde. Testa todos os endereços do host (IPv4 e IPv6), porque o
     * servidor de desenvolvimento do Angular pode escutar somente em {@code [::1]}.
     */
    private static boolean isUp(String url) {
        URI uri = URI.create(url);
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(address, uri.getPort() == -1 ? 80 : uri.getPort()), 2000);
                    return true;
                } catch (IOException e) {
                    // tenta o próximo endereço
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }
}
