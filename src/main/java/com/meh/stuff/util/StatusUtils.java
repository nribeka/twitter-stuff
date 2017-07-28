package com.meh.stuff.util;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Place;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.SymbolEntity;
import twitter4j.TwitterResponse;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

public class StatusUtils {
    public static final String TWITTER_BASE_URL = "https://twitter.com/";
    private static final String PERMALINK_TWEET_CONTAINER = ".permalink-tweet-container";

    public static void saveStatus(Status status, MongoDatabase mongoDatabase) {

        Document tweetDocument = new Document()
                .append("_id", status.getId())
                .append("created_at", status.getCreatedAt())
                .append("text", status.getText())
                .append("source", status.getSource())
                .append("is_truncated", status.isTruncated())
                .append("in_reply_to_status_id", status.getInReplyToStatusId())
                .append("in_reply_to_user_id", status.getInReplyToUserId())
                .append("in_reply_to_screen_name", status.getInReplyToScreenName())
                .append("is_favorited", status.isFavorited())
                .append("is_retweeted", status.isRetweeted())
                .append("favorite_count", status.getFavoriteCount())
                .append("is_retweet", status.isRetweet())
                .append("retweet_count", status.getRetweetCount())
                .append("is_retweeted_by_me", status.isRetweetedByMe())
                .append("current_user_retweet_id", status.getCurrentUserRetweetId())
                .append("is_possibly_sensitive", status.isPossiblySensitive())
                .append("lang", status.getLang())
                .append("quoted_status_id", status.getQuotedStatusId());

        GeoLocation geoLocation = status.getGeoLocation();
        if (geoLocation != null) {
            tweetDocument.append("geo_location", new Document()
                    .append("latitude", geoLocation.getLatitude())
                    .append("longitude", geoLocation.getLongitude())
            );
        }

        Place place = status.getPlace();
        if (place != null) {
            tweetDocument.append("place", place.getId());
            Document placeDocument = new Document()
                    .append("_id", place.getId())
                    .append("name", place.getName())
                    .append("full_name", place.getFullName())
                    .append("place_type", place.getPlaceType())
                    .append("street_address", place.getStreetAddress())
                    .append("country_code", place.getCountryCode())
                    .append("country", place.getCountry())
                    .append("url", place.getURL());
            MongoCollection<Document> placeCollection = mongoDatabase.getCollection("places");
            placeCollection.replaceOne(
                    Filters.eq("_id", place.getId()),
                    placeDocument,
                    new UpdateOptions().upsert(true)
            );
        }

        if (status.getUser() != null) {
            tweetDocument.append("user", status.getUser().getId());
        }

        if (status.getRetweetedStatus() != null) {
            tweetDocument.append("retweeted_status", status.getRetweetedStatus().getId());
        }

        if (status.getScopes() != null) {
            tweetDocument.append("scopes", Arrays.asList(status.getScopes().getPlaceIds()));
        }

        if (status.getWithheldInCountries() != null) {
            tweetDocument.append("withheld_in_countries", Arrays.asList(status.getWithheldInCountries()));
        }

        if (status.getContributors() != null && status.getContributors().length > 0) {
            tweetDocument.append("contributors", Arrays.asList(status.getContributors()));
        }

        MongoCollection<Document> tweetCollection = mongoDatabase.getCollection("tweets");
        tweetCollection.replaceOne(
                Filters.eq("_id", status.getId()),
                tweetDocument,
                new UpdateOptions().upsert(true)
        );

        UserMentionEntity[] userMentionEntities = status.getUserMentionEntities();
        MongoCollection<Document> mentionCollection = mongoDatabase.getCollection("mentions");
        for (UserMentionEntity userMentionEntity : userMentionEntities) {
            Document userMentionDocument = new Document()
                    .append("tweet_id", status.getId())
                    .append("id", userMentionEntity.getId())
                    .append("name", userMentionEntity.getName())
                    .append("screen_name", userMentionEntity.getScreenName())
                    .append("text", userMentionEntity.getText());
            mentionCollection.replaceOne(
                    Filters.and(
                            Filters.eq("tweet_id", status.getId()),
                            Filters.eq("id", userMentionEntity.getId())),
                    userMentionDocument,
                    new UpdateOptions().upsert(true)
            );
        }

        URLEntity[] urlEntities = status.getURLEntities();
        MongoCollection<Document> urlCollection = mongoDatabase.getCollection("urls");
        for (URLEntity urlEntity : urlEntities) {
            Document urlEntityDocument = new Document()
                    .append("tweet_id", status.getId())
                    .append("url", urlEntity.getURL())
                    .append("display_url", urlEntity.getDisplayURL())
                    .append("expanded_url", urlEntity.getExpandedURL())
                    .append("text", urlEntity.getText());
            urlCollection.replaceOne(
                    Filters.and(
                            Filters.eq("tweet_id", status.getId()),
                            Filters.eq("url", urlEntity.getURL())),
                    urlEntityDocument,
                    new UpdateOptions().upsert(true)
            );
        }

        HashtagEntity[] hashtagEntities = status.getHashtagEntities();
        MongoCollection<Document> hashtagCollection = mongoDatabase.getCollection("hashtags");
        for (HashtagEntity hashtagEntity : hashtagEntities) {
            Document hashtagEntityDocument = new Document()
                    .append("tweet_id", status.getId())
                    .append("text", hashtagEntity.getText());
            hashtagCollection.replaceOne(
                    Filters.and(
                            Filters.eq("tweet_id", status.getId()),
                            Filters.eq("text", status.getText())),
                    hashtagEntityDocument,
                    new UpdateOptions().upsert(true)
            );
        }

        MediaEntity[] mediaEntities = status.getMediaEntities();
        MongoCollection<Document> mediaCollection = mongoDatabase.getCollection("documents");
        for (MediaEntity mediaEntity : mediaEntities) {
            Document mediaEntityDocument = new Document()
                    .append("tweet_id", status.getId())
                    .append("id", mediaEntity.getId())
                    .append("url", mediaEntity.getURL())
                    .append("display_url", mediaEntity.getDisplayURL())
                    .append("expanded_url", mediaEntity.getExpandedURL())
                    .append("media_url", mediaEntity.getMediaURL())
                    .append("media_url_https", mediaEntity.getMediaURLHttps())
                    .append("type", mediaEntity.getType())
                    .append("text", mediaEntity.getText());
            mediaCollection.replaceOne(
                    Filters.and(
                            Filters.eq("tweet_id", status.getId()),
                            Filters.eq("url", mediaEntity.getURL()),
                            Filters.eq("id", mediaEntity.getId())),
                    mediaEntityDocument,
                    new UpdateOptions().upsert(true)
            );
        }

        SymbolEntity[] symbolEntities = status.getSymbolEntities();
        MongoCollection<Document> symbolCollection = mongoDatabase.getCollection("symbols");
        for (SymbolEntity symbolEntity : symbolEntities) {
            Document symbolEntityDocument = new Document()
                    .append("tweet_id", status.getId())
                    .append("text", symbolEntity.getText());
            symbolCollection.replaceOne(
                    Filters.and(
                            Filters.eq("tweet_id", status.getId()),
                            Filters.eq("text", symbolEntity.getText())),
                    symbolEntityDocument,
                    new UpdateOptions().upsert(true)
            );
        }
    }

    public static void takeStatusScreenshot(Status status, MongoDatabase mongoDatabase, WebDriver driver) {
        long id = status.getId();
        User user = status.getUser();
        try {
            // visit the twitter url
            driver.get(TWITTER_BASE_URL + user.getScreenName() + "/status/" + id);

            WebDriverWait webDriverWait = new WebDriverWait(driver, 4);
            webDriverWait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector(PERMALINK_TWEET_CONTAINER)));

            // Find the tweet container
            WebElement element = driver.findElement(By.cssSelector(PERMALINK_TWEET_CONTAINER));

            // take a full screenshot
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // create in memory image data
            BufferedImage screenshotImage = ImageIO.read(screenshotFile);

            // calculate the position of the tweet element
            Point point = element.getLocation();
            int elementWidth = element.getSize().getWidth();
            int elementHeight = element.getSize().getHeight();

            // crop to get only the container of the tweet itself (sometimes this doesn't work on mac)
            BufferedImage elementScreenshotImage =
                    screenshotImage.getSubimage(
                            point.getX() + 4,
                            point.getY() + 4,
                            elementWidth - 4,
                            screenshotImage.getHeight() < point.getY() + elementHeight
                                    ? screenshotImage.getHeight() - 4
                                    : elementHeight - 4);
            ImageIO.write(elementScreenshotImage, "png", screenshotFile);
            FileUtils.copyFile(screenshotFile, new File("screenshot/" + id + ".png"));
        } catch (Exception e) {
            saveFailureStatusProcesssed(mongoDatabase, id, false);
            System.out.println(
                    String.format(
                            "Unable to create screenshot for %s with id: %d",
                            user.getScreenName(),
                            id));
            e.printStackTrace();
        }
    }

    public static void saveFailureStatusProcesssed(MongoDatabase mongoDatabase, long id, boolean processed) {
        MongoCollection<Document> failureCollection = mongoDatabase.getCollection("failures");
        failureCollection.replaceOne(
                Filters.eq("tweet_id", id),
                new Document().append("tweet_id", id).append("processed", processed),
                new UpdateOptions().upsert(true));
    }

    public static void sleepWhenRateLimited(TwitterResponse response) {
        RateLimitStatus rateLimitStatus = response.getRateLimitStatus();
        if (rateLimitStatus != null && rateLimitStatus.getRemaining() <= 10) {
            System.out.println("RateLimitStatus remaining: " + rateLimitStatus.getRemaining());
            try {
                Thread.sleep(rateLimitStatus.getSecondsUntilReset());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
