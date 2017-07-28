package com.meh.stuff;

import com.meh.stuff.util.Constants;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.InputStream;
import java.util.Properties;

public class SeleniumPlayground {
    public static void main(String[] args) {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(Constants.APPLICATION_CONFIGURATION);

        Properties properties = new Properties();
        try {
            properties.load(inputStream);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set the chrome executable location
        System.setProperty(Constants.WEBDRIVER_CHROME_DRIVER, Constants.WEBDRIVER_CHROME_DRIVER_PATH);
        WebDriver driver = new ChromeDriver();

        driver.get("https://twitter.com");

        WebElement loginButton = driver.findElement(By.linkText("Log in"));
        loginButton.click();

        WebDriverWait wait = new WebDriverWait(driver, 1000);
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("login-dialog")));

        WebElement loginDialog = driver.findElement(By.id("login-dialog"));

        WebElement usernameElement = loginDialog.findElement(By.cssSelector("input[type='text']"));
        usernameElement.sendKeys(properties.getProperty("twitter.username", ""));

        WebElement passwordElement = loginDialog.findElement(By.cssSelector("input[type='password']"));
        passwordElement.sendKeys(properties.getProperty("twitter.password", ""));

        WebElement submitElement = loginDialog.findElement(By.cssSelector("input[type='submit']"));
        submitElement.click();

        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        By.id("login-dialog")));

        driver.get("https://twitter.com/realDonaldTrump/status/738233323344920576");

        // make the page zoom level 80%
        // driver.findElement(By.tagName("html")).sendKeys(Keys.chord(Keys.COMMAND, Keys.SUBTRACT));
        // driver.findElement(By.tagName("html")).sendKeys(Keys.chord(Keys.COMMAND, Keys.SUBTRACT));

        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) driver;
        javascriptExecutor.executeScript("document.body.style.zoom = 80%");
    }
}
