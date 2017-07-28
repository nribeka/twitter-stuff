package com.meh.stuff;

import com.meh.stuff.util.Constants;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.meh.stuff.util.StatusUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which will read the record in items and go through each file and pull the tweet data and take screenshot of
 * the tweet.
 */
public class SeleniumInitialize {

    public static void main(String[] args) {
        // Set the chrome executable location
        System.setProperty(Constants.WEBDRIVER_CHROME_DRIVER, Constants.WEBDRIVER_CHROME_DRIVER_PATH);
        WebDriver driver = new ChromeDriver();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(Constants.APPLICATION_CONFIGURATION);

        Properties properties = new Properties();
        try {
            properties.load(inputStream);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setDebugEnabled(true)
                .setOAuthConsumerKey(properties.getProperty("twitter.oauth.consumerKey"))
                .setOAuthConsumerSecret(properties.getProperty("twitter.oauth.consumerSecret"))
                .setOAuthAccessToken(properties.getProperty("twitter.oauth.accessToken"))
                .setOAuthAccessTokenSecret(properties.getProperty("twitter.oauth.accessTokenSecret"));

        TwitterFactory twitterFactory = new TwitterFactory(configurationBuilder.build());
        Twitter twitter = twitterFactory.getInstance();

        MongoClient mongoClient = new MongoClient();

        String template = "(\\w+)-(\\w+).tweets";

        File itemFolder = new File("item");
        FileFilter fileFilter = new WildcardFileFilter("*.tweets");
        File[] itemFiles = itemFolder.listFiles(fileFilter);
        if (itemFiles != null) {
            for (File itemFile : itemFiles) {
                String filename = itemFile.getName();

                Pattern pattern = Pattern.compile(template);
                Matcher matcher = pattern.matcher(filename);

                if (matcher.matches()) {
                    String handle = matcher.group(2);

                    MongoDatabase mongoDatabase = mongoClient.getDatabase(handle);

                    try {
                        BufferedReader bufferedReader = new BufferedReader(new FileReader(itemFile));

                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            System.out.println("Processing status id: " + line);
                            Status status = twitter.showStatus(Long.parseLong(line));
                            StatusUtils.saveStatus(status, mongoDatabase);
                            StatusUtils.takeStatusScreenshot(status, mongoDatabase, driver);
                            StatusUtils.sleepWhenRateLimited(status);
                        }
                        bufferedReader.close();

                        FileUtils.moveFile(itemFile, new File("archive", itemFile.getName()));
                    } catch (IOException | TwitterException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
