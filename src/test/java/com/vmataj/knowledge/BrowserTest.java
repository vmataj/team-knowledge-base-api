package com.vmataj.knowledge;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "browser.tests", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:knowledge-browser;DB_CLOSE_DELAY=-1")
class BrowserTest {
    @LocalServerPort int port;
    private WebDriver browser;
    private WebDriverWait wait;

    @BeforeEach
    void openBrowser() {
        var options = new ChromeOptions().addArguments("--headless=new", "--window-size=1280,900");
        String version = System.getProperty("browser.version");
        if (version != null && !version.isBlank()) options.setBrowserVersion(version);
        browser = new ChromeDriver(options);
        wait = new WebDriverWait(browser, Duration.ofSeconds(10));
    }

    @AfterEach
    void closeBrowser() {
        if (browser != null) browser.quit();
    }

    @Test
    void createsFindsEditsAndDeletesAnArticle() {
        browser.get(home());
        click(By.id("new"));
        browser.findElement(By.name("title")).sendKeys("Browser checks");
        browser.findElement(By.name("body")).sendKeys("Run the smoke suite before tagging a release.");
        click(By.cssSelector("#runbook-form button[type='submit'], #runbook-form button:not([type])"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("title"), "Browser checks"));

        browser.findElement(By.id("search")).sendKeys("smoke suite");
        click(By.cssSelector("#article-search button"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("result-count"), "1 article"));

        click(By.id("edit"));
        browser.findElement(By.name("title")).clear();
        browser.findElement(By.name("title")).sendKeys("Release checks");
        click(By.cssSelector("#runbook-form button:not([type])"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("title"), "Release checks"));

        click(By.id("edit"));
        click(By.id("delete"));
        click(By.cssSelector("#delete-dialog button[value='delete']"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("status"), "Article deleted"));
        assertFalse(browser.findElement(By.id("list")).getText().contains("Release checks"));
    }

    private String home() {
        return "http://127.0.0.1:" + port + "/";
    }

    private void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }
}
