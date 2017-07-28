package com.meh.stuff;

import com.meh.stuff.util.Constants;
import com.meh.stuff.util.StatusUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Class to try to open a twitter handle's home screen and scroll through the status. The drawback is twitter doesn't
 * list out of your tweets in your home page. You probably gonna get a few thousands out of all of your tweets.O
 */
public class SeleniumPullingScreen {

    private static String DEFAULT_TWITTER_HANDLE = "WinNyoman";

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

        driver.get(StatusUtils.TWITTER_BASE_URL + properties.getProperty("twitter.handle", DEFAULT_TWITTER_HANDLE));

        List<WebElement> tweetElements;
        Set<String> itemIds = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            JavascriptExecutor js = ((JavascriptExecutor) driver);
            js.executeScript("window.scrollTo(0, document.body.scrollHeight)");

            tweetElements = driver.findElements(By.cssSelector(".js-stream-item.stream-item.stream-item"));
            for (WebElement tweetElement : tweetElements) {
                String itemId = tweetElement.getAttribute("data-item-id");
                itemIds.add(itemId);
            }

            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(itemIds.size());

        driver.quit();
    }
}
