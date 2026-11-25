package br.unisagrado.p2.e2e;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Objects das telas do PetClinic Angular usadas pelos testes do grupo P2.
 * Os localizadores usam os {@code id} já existentes nos formulários.
 */
final class PetClinicPages {

    private PetClinicPages() {
    }

    abstract static class Page {

        protected final WebDriver driver;

        protected final WebDriverWait wait;

        Page(WebDriver driver) {
            this.driver = driver;
            this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        }

        protected WebElement visible(By locator) {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        }

        protected void type(By locator, String text) {
            WebElement field = visible(locator);
            field.clear();
            field.sendKeys(text);
        }

        /**
         * Rola o elemento até o centro da tela antes de clicar (botões abaixo da área visível). Se o layout
         * mudar entre a rolagem e o clique (conteúdo carregado depois), tenta de novo.
         */
        protected void click(By locator) {
            wait.ignoring(ElementClickInterceptedException.class).until(d -> {
                WebElement element = ExpectedConditions.elementToBeClickable(locator).apply(d);
                if (element == null) {
                    return false;
                }
                ((JavascriptExecutor) d).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
                element.click();
                return true;
            });
        }

        /**
         * Seleciona uma opção de um {@code <select>}. Os formulários com signal forms do Angular só atualizam o
         * modelo no evento {@code input}; em 25/11/2026 (Selenium 4.43, Chrome 154) a seleção feita pelo
         * ChromeDriver não disparava esse evento, e o botão de salvar ficava desabilitado. Por isso o evento é
         * disparado explicitamente (diagnóstico registrado como OBS-04 no relatório de defeitos).
         */
        protected void selectOption(By locator, String visibleText) {
            WebElement element = visible(locator);
            new Select(element).selectByVisibleText(visibleText);
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));"
                    + "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));", element);
        }

        protected By button(String text) {
            return By.xpath("//button[normalize-space()='" + text + "']");
        }
    }

    static final class OwnersListPage extends Page {

        OwnersListPage(WebDriver driver, String frontUrl) {
            this(open(driver, frontUrl + "/owners"));
            // aguarda a tabela de donos carregar, para o layout não mudar durante as ações
            visible(By.cssSelector("td.ownerFullName"));
        }

        private static WebDriver open(WebDriver driver, String url) {
            driver.get(url);
            return driver;
        }

        /** Lista já aberta pela própria aplicação (ex.: após salvar um novo dono). */
        OwnersListPage(WebDriver driver) {
            super(driver);
            visible(By.id("search-owner-form"));
        }

        /** Abre os detalhes do dono pelo link com o nome completo. */
        OwnerDetailPage openOwner(String fullName) {
            click(By.xpath("//td[contains(@class,'ownerFullName')]/a[normalize-space()='" + fullName + "']"));
            return new OwnerDetailPage(driver);
        }

        OwnersListPage searchByLastName(String lastName) {
            type(By.id("lastName"), lastName);
            click(button("Find Owner"));
            return this;
        }

        List<String> ownerNames() {
            wait.until(d -> !d.findElements(By.cssSelector("td.ownerFullName")).isEmpty()
                || !d.findElements(By.xpath("//*[contains(text(),'No owners with LastName')]")).isEmpty());
            return driver.findElements(By.cssSelector("td.ownerFullName")).stream().map(WebElement::getText).toList();
        }

        OwnerFormPage addOwner() {
            click(button("Add Owner"));
            return new OwnerFormPage(driver);
        }
    }

    static final class OwnerFormPage extends Page {

        OwnerFormPage(WebDriver driver) {
            super(driver);
            visible(By.xpath("//h2[normalize-space()='New Owner']"));
        }

        OwnerFormPage fill(String firstName, String lastName, String address, String city, String telephone) {
            type(By.id("firstName"), firstName);
            type(By.id("lastName"), lastName);
            type(By.id("address"), address);
            type(By.id("city"), city);
            type(By.id("telephone"), telephone);
            return this;
        }

        boolean isSubmitEnabled() {
            return visible(button("Add Owner")).isEnabled();
        }

        boolean isTelephoneMarkedInvalid() {
            return wait.until(d -> d.findElement(By.id("telephone")).getAttribute("class").contains("is-invalid"));
        }

        /** Após salvar, a aplicação volta para a lista de donos (comportamento do projeto original). */
        OwnersListPage submit() {
            click(button("Add Owner"));
            return new OwnersListPage(driver);
        }
    }

    static final class OwnerDetailPage extends Page {

        OwnerDetailPage(WebDriver driver) {
            super(driver);
            visible(By.xpath("//h2[normalize-space()='Owner Information']"));
        }

        String fullName() {
            // o nome é preenchido depois que a API responde
            return wait.until(d -> {
                String text = d.findElement(By.cssSelector("strong.ownerFullName")).getText().trim();
                return text.isEmpty() ? null : text;
            });
        }

        String pageText() {
            return driver.findElement(By.tagName("body")).getText();
        }

        PetFormPage addPet() {
            click(button("Add New Pet"));
            return new PetFormPage(driver);
        }

        VisitFormPage addVisitToPet(String petName) {
            click(By.xpath("//app-pet-list[.//td[normalize-space()='" + petName + "']]//button[normalize-space()='Add Visit']"));
            return new VisitFormPage(driver);
        }

        boolean hasText(String text) {
            return wait.until(d -> d.findElement(By.tagName("body")).getText().contains(text));
        }
    }

    static final class PetFormPage extends Page {

        PetFormPage(WebDriver driver) {
            super(driver);
            visible(By.xpath("//h2[normalize-space()='Add Pet']"));
        }

        /** Data no formato do datepicker da aplicação: AAAA/MM/DD. */
        OwnerDetailPage save(String name, String birthDate, String type) {
            type(By.id("name"), name);
            type(By.id("birthDate"), birthDate + Keys.TAB);
            selectOption(By.id("typeId"), type);
            click(button("Save Pet"));
            return new OwnerDetailPage(driver);
        }
    }

    static final class VisitFormPage extends Page {

        VisitFormPage(WebDriver driver) {
            super(driver);
            visible(By.xpath("//h2[normalize-space()='New Visit']"));
        }

        /** Data no formato do datepicker da aplicação: AAAA/MM/DD. */
        OwnerDetailPage save(String date, String description) {
            type(By.id("date"), date);
            type(By.id("description"), description);
            click(button("Add Visit"));
            return new OwnerDetailPage(driver);
        }
    }
}
