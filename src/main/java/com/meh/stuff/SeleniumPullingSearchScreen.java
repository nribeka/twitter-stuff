package com.meh.stuff;

import com.meh.stuff.util.Constants;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class to pull twitter data for group of twitter handle. The output is in the items folder with the twitter
 * handle filename. The file will contains (almost) all of the handle tweets.
 */
public class SeleniumPullingSearchScreen {

    private static long currentTimeMillis = System.currentTimeMillis();

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

        String[] twitterHandles = properties
                .getProperty("twitter.handles", "")
                .split(",");
        for (String twitterHandle : twitterHandles) {

            twitterHandle = twitterHandle.trim();
            Set<String> itemIds = new TreeSet<>();
            for (int i = 0; i < 15; i++) {
                Calendar calendar = Calendar.getInstance();

                calendar.set(Calendar.YEAR, 2005 + i);
                calendar.set(Calendar.MONTH, Calendar.DECEMBER);
                calendar.set(Calendar.DATE, 31);
                Date startDate = calendar.getTime();

                calendar.set(Calendar.YEAR, 2005 + i + 1);
                calendar.set(Calendar.MONTH, Calendar.MARCH);
                calendar.set(Calendar.DATE, 31);
                Date endDate = calendar.getTime();

                parseTweets(driver, twitterHandle, startDate, endDate, itemIds);

                calendar.set(Calendar.MONTH, Calendar.MARCH);
                calendar.set(Calendar.DATE, 31);
                startDate = calendar.getTime();

                calendar.set(Calendar.MONTH, Calendar.JUNE);
                calendar.set(Calendar.DATE, 30);
                endDate = calendar.getTime();

                parseTweets(driver, twitterHandle, startDate, endDate, itemIds);

                calendar.set(Calendar.MONTH, Calendar.JUNE);
                calendar.set(Calendar.DATE, 30);
                startDate = calendar.getTime();

                calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
                calendar.set(Calendar.DATE, 30);
                endDate = calendar.getTime();

                parseTweets(driver, twitterHandle, startDate, endDate, itemIds);

                calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
                calendar.set(Calendar.DATE, 30);
                startDate = calendar.getTime();

                calendar.set(Calendar.MONTH, Calendar.DECEMBER);
                calendar.set(Calendar.DATE, 31);
                endDate = calendar.getTime();

                parseTweets(driver, twitterHandle, startDate, endDate, itemIds);
            }
            persistTweets(itemIds, twitterHandle);
            System.out.println("Number of tweets found: " + itemIds.size());
        }
        driver.quit();
    }

    private static void parseTweets(WebDriver driver, String handle,
                                    Date startDate, Date endDate,
                                    Set<String> itemIds) {

        String twitterBase = "https://twitter.com/";
        String twitterSearchStaticTemplate = "search?f=tweets&vertical=default&src=typd";
        String twitterSearchDynamicTemplate = "&q=from:%s since:%s until:%sinclude:retweets";

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String url = twitterBase + twitterSearchStaticTemplate +
                String.format(
                        twitterSearchDynamicTemplate,
                        handle,
                        dateFormat.format(startDate),
                        dateFormat.format(endDate));

        System.out.println("Search url: " + url);
        parseTweets(driver, itemIds, url);
    }

    private static void parseTweets(WebDriver driver, Set<String> itemIds, String url) {
        driver.get(url);
        boolean canScroll;
        do {
            JavascriptExecutor js = ((JavascriptExecutor) driver);
            js.executeScript("window.scrollTo(0, document.body.scrollHeight)");

            List<WebElement> webElements = driver.findElements(By.cssSelector(".has-more-items"));
            canScroll = (webElements.size() > 0);
        } while (canScroll);

        List<WebElement> tweetElements =
                driver.findElements(By.cssSelector(".js-stream-item.stream-item.stream-item"));
        for (WebElement tweetElement : tweetElements) {
            String itemId = tweetElement.getAttribute("data-item-id");
            itemIds.add(itemId);
        }
    }

    private static void persistTweets(Set<String> itemIds, String handle) {
        try {
            File file = new File("items/" + currentTimeMillis + "-" + handle + ".tweets");

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            for (String itemId : itemIds) {
                bufferedWriter.write(itemId);
                bufferedWriter.write(System.lineSeparator());
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
