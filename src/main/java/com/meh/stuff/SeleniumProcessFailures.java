package com.meh.stuff;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.meh.stuff.util.Constants;
import com.meh.stuff.util.StatusUtils;
import org.bson.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.InputStream;
import java.util.Properties;

public class SeleniumProcessFailures {
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
        for (String databaseName : mongoClient.listDatabaseNames()) {
            MongoDatabase mongoDatabase = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> failureCollection = mongoDatabase.getCollection("failures");
            for (Document document : failureCollection.find(Filters.eq("processed", false))) {
                long tweetId = document.getLong("tweet_id");
                StatusUtils.saveFailureStatusProcesssed(mongoDatabase, tweetId, true);
                try {
                    Status status = twitter.showStatus(tweetId);
                    StatusUtils.saveStatus(status, mongoDatabase);
                    StatusUtils.takeStatusScreenshot(status, mongoDatabase, driver);
                    StatusUtils.sleepWhenRateLimited(status);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
            }
            failureCollection.deleteMany(Filters.eq("processed", true));
        }
        driver.quit();
    }
}
