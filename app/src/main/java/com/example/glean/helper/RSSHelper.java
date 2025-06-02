package com.example.glean.helper;

import android.util.Log;
import android.util.Xml;

import com.example.glean.model.NewsItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RSSHelper {
    
    private static final String TAG = "RSSHelper";
    
    /**
     * Parse RSS feed from input stream
     */
    public static List<NewsItem> parse(InputStream inputStream) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            inputStream.close();
        }
    }
    
    /**
     * Read RSS feed and extract items
     */
    private static List<NewsItem> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<NewsItem> items = new ArrayList<>();
        
        parser.require(XmlPullParser.START_TAG, null, "rss");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            
            String name = parser.getName();
            if (name.equals("channel")) {
                readChannel(parser, items);
            } else {
                skip(parser);
            }
        }
        
        return items;
    }
    
    /**
     * Process channel element to find items
     */
    private static void readChannel(XmlPullParser parser, List<NewsItem> items) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "channel");
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            
            String name = parser.getName();
            if (name.equals("item")) {
                items.add(readItem(parser));
            } else {
                skip(parser);
            }
        }
    }
    
    /**
     * Parse a single item from RSS feed
     */
    private static NewsItem readItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "item");
        
        int id = 0;
        String title = null;
        String content = null;
        String imageUrl = null;
        String date = null;
        String category = null;
        
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            
            String name = parser.getName();
            if (name.equals("title")) {
                title = readText(parser);
            } else if (name.equals("description")) {
                content = readText(parser);
            } else if (name.equals("enclosure")) {
                // Get image URL from enclosure
                imageUrl = parser.getAttributeValue(null, "url");
                skip(parser);
            } else if (name.equals("pubDate")) {
                String rawDate = readText(parser);
                date = formatDate(rawDate);
            } else if (name.equals("category")) {
                category = readText(parser);
            } else {
                skip(parser);
            }
        }
        
        // Use the available 3-parameter constructor (assuming it's id, title, content)
        NewsItem newsItem = new NewsItem(id, title != null ? title : "", content != null ? content : "");
        
        // Set other properties using setters (if available)
        if (imageUrl != null) {
            newsItem.setImageUrl(imageUrl);
        }
        if (date != null) {
            newsItem.setDate(date);
        }
        if (category != null) {
            newsItem.setCategory(category);
        } else {
            newsItem.setCategory("News");
        }
        
        return newsItem;
    }
    
    /**
     * Format RSS date string to simpler format
     */
    private static String formatDate(String rawDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = inputFormat.parse(rawDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage());
            return rawDate;
        }
    }
    
    /**
     * Read text content of element
     */
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }
    
    /**
     * Skip unwanted elements
     */
    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}