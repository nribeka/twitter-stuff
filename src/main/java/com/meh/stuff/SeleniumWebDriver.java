package com.meh.stuff;

import com.meh.stuff.util.Constants;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.meh.stuff.util.StatusUtils;
import org.bson.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class that will keep on updating your local tweets record. This class have dependency with mongodb to store all
 * of your previous tweets.
 * <p>
 * This class will pull the tweet data and take screenshot of each tweet.
 * <p>
 * Run this class to update your record after you run the SeleniumInitialize class.
 */
public class SeleniumWebDriver {

    private static final String TWITTER_HANDLE = "WinNyoman";

    private static void retrieve() {
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
        MongoDatabase mongoDatabase = mongoClient.getDatabase(TWITTER_HANDLE);

        MongoCollection<Document> tweetCollection = mongoDatabase.getCollection("tweets");
        Document document = tweetCollection.find().sort(Sorts.descending("_id")).first();

        try {
            if (document != null) {

                long sinceId = document.getLong("_id");

                boolean hasMore = true;
                Paging paging = new Paging();
                ResponseList<Status> statuses;
                while (hasMore) {
                    paging.setSinceId(sinceId);
                    statuses = twitter.getUserTimeline(TWITTER_HANDLE, paging);
                    if (statuses.size() <= 0) {
                        hasMore = false;
                    } else {
                        for (Status status : statuses) {
                            if (sinceId < status.getId()) {
                                sinceId = status.getId() + 1;
                            }
                            StatusUtils.saveStatus(status, mongoDatabase);
                            StatusUtils.takeStatusScreenshot(status, mongoDatabase, driver);
                        }
                    }
                    StatusUtils.sleepWhenRateLimited(statuses);
                }
            }
            driver.quit();
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(SeleniumWebDriver::retrieve, 0, 300, TimeUnit.MINUTES);
    }
}