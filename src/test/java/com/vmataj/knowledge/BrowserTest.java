package com.vmataj.knowledge;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "browser.tests", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrowserTest {
    @LocalServerPort int port;

    @Test
    void guestCanAddReorderAndCompleteATask() {
        var options = new ChromeOptions().addArguments("--headless=new", "--window-size=1200,850");
        String version = System.getProperty("browser.version");
        if (version != null) options.setBrowserVersion(version);
        var browser = new ChromeDriver(options);
        try {
            var wait = new WebDriverWait(browser, Duration.ofSeconds(10));
            browser.get("http://127.0.0.1:" + port);
            wait.until(ExpectedConditions.elementToBeClickable(By.id("add"))).click();
            browser.findElement(By.name("title")).sendKeys("Review notes");
            browser.findElement(By.cssSelector("#task-form button")).click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("task-dialog")));
            browser.findElement(By.cssSelector("[aria-label='Move Review notes up']")).click();
            assertTrue(browser.findElements(By.cssSelector(".task-row")).get(2).getText().contains("Review notes"));
            browser.findElement(By.cssSelector("[aria-label='Complete Review notes']")).click();
            browser.navigate().refresh();
            wait.until(ExpectedConditions.elementToBeSelected(By.cssSelector("[aria-label='Complete Review notes']")));
            assertEquals("1", browser.findElement(By.id("done-count")).getText());
            browser.findElement(By.id("timer")).click();
            assertEquals("Pause", browser.findElement(By.id("timer")).getText());
        } finally {
            browser.quit();
        }
    }
}
