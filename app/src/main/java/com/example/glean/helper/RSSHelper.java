package com.example.glean.helper;

import android.util.Log;

import com.example.glean.model.NewsItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RSSHelper {
    private static final String TAG = "RSSHelper";
    private static final int CONNECTION_TIMEOUT = 15000; // Increased to 15 seconds
    private static final int READ_TIMEOUT = 20000; // Increased to 20 seconds
    private static final int MAX_REDIRECTS = 5;
    
    public List<NewsItem> fetchNewsFromRSS(String rssUrl) {
        List<NewsItem> newsList = new ArrayList<>();
        
        try {
            Log.d(TAG, "Fetching RSS from: " + rssUrl);
            
            // Try with different approaches if the first one fails
            newsList = attemptFetchWithRetry(rssUrl);
            
            if (newsList.isEmpty()) {
                Log.w(TAG, "No articles fetched from: " + rssUrl);
            } else {
                Log.d(TAG, "Successfully fetched " + newsList.size() + " articles from: " + rssUrl);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching RSS from " + rssUrl, e);
        }
        
        return newsList;
    }
    
    private List<NewsItem> attemptFetchWithRetry(String rssUrl) {
        List<NewsItem> newsList = new ArrayList<>();
        
        // Try standard approach first
        newsList = fetchWithUserAgent(rssUrl, "GleanGo/1.0 (Environmental News Reader)");
        
        // If failed, try with different user agents
        if (newsList.isEmpty()) {
            Log.d(TAG, "Retrying with browser user agent for: " + rssUrl);
            newsList = fetchWithUserAgent(rssUrl, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        }
        
        // If still failed, try with mobile user agent
        if (newsList.isEmpty()) {
            Log.d(TAG, "Retrying with mobile user agent for: " + rssUrl);
            newsList = fetchWithUserAgent(rssUrl, "Mozilla/5.0 (Android 12; Mobile; rv:68.0) Gecko/68.0 Firefox/88.0");
        }
        
        return newsList;
    }
    
    private List<NewsItem> fetchWithUserAgent(String rssUrl, String userAgent) {
        List<NewsItem> newsList = new ArrayList<>();
        
        try {
            URL url = new URL(rssUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set connection properties
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", userAgent);
            connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, application/atom+xml, */*");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Cache-Control", "no-cache");
            
            // Handle redirects manually for better control
            connection.setInstanceFollowRedirects(true);
            
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code for " + rssUrl + ": " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                newsList = parseRSSFeed(inputStream, rssUrl);
                inputStream.close();
            } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                      responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                      responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                // Handle redirects
                String newUrl = connection.getHeaderField("Location");
                if (newUrl != null && !newUrl.equals(rssUrl)) {
                    Log.d(TAG, "Following redirect from " + rssUrl + " to " + newUrl);
                    connection.disconnect();
                    return fetchWithUserAgent(newUrl, userAgent);
                }
            } else {
                Log.w(TAG, "HTTP error code: " + responseCode + " for URL: " + rssUrl);
                // Try to get error response
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    errorStream.close();
                }
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching RSS with user agent " + userAgent + " from " + rssUrl, e);
        }
        
        return newsList;
    }
    
    private List<NewsItem> parseRSSFeed(InputStream inputStream, String sourceUrl) {
        List<NewsItem> newsList = new ArrayList<>();
        
        try {
            // First, read the stream and clean the XML
            String xmlContent = readStreamToString(inputStream);
            String cleanedXml = sanitizeXml(xmlContent);
            
            // Parse the cleaned XML
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(cleanedXml));
            
            NewsItem currentItem = null;
            String currentTag = null;
            StringBuilder textContent = new StringBuilder();
            boolean isInEntry = false; // For Atom feeds
            
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                
                try {
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            currentTag = parser.getName().toLowerCase();
                            textContent.setLength(0);
                            
                            // Handle both RSS (item) and Atom (entry) formats
                            if ("item".equals(currentTag) || "entry".equals(currentTag)) {
                                currentItem = new NewsItem();
                                currentItem.setSource(extractSourceFromUrl(sourceUrl));
                                isInEntry = "entry".equals(currentTag);
                            } else if ("enclosure".equals(currentTag) && currentItem != null) {
                                // Handle RSS enclosure for images
                                String type = parser.getAttributeValue(null, "type");
                                String url = parser.getAttributeValue(null, "url");
                                if (type != null && type.startsWith("image") && url != null) {
                                    currentItem.setImageUrl(url);
                                }
                            } else if ("link".equals(currentTag) && currentItem != null && isInEntry) {
                                // Handle Atom link tag with href attribute
                                String href = parser.getAttributeValue(null, "href");
                                if (href != null) {
                                    currentItem.setUrl(href);
                                }
                            }
                            break;
                            
                        case XmlPullParser.TEXT:
                            if (currentTag != null) {
                                String text = parser.getText();
                                if (text != null) {
                                    textContent.append(text);
                                }
                            }
                            break;
                            
                        case XmlPullParser.END_TAG:
                            String endTag = parser.getName().toLowerCase();
                            
                            if (("item".equals(endTag) || "entry".equals(endTag)) && currentItem != null) {
                                // Finalize current item
                                enhanceNewsItem(currentItem);
                                
                                // Only add if we have essential fields
                                if (currentItem.getTitle() != null && !currentItem.getTitle().trim().isEmpty()) {
                                    newsList.add(currentItem);
                                }
                                
                                currentItem = null;
                                isInEntry = false;
                                
                            } else if (currentItem != null && textContent.length() > 0) {
                                String content = textContent.toString().trim();
                                
                                switch (endTag) {
                                    case "title":
                                        currentItem.setTitle(cleanHtml(content));
                                        break;
                                    case "description":
                                    case "summary":
                                    case "content":
                                        if (currentItem.getPreview() == null || currentItem.getPreview().isEmpty()) {
                                            currentItem.setPreview(cleanHtml(content));
                                        }
                                        break;
                                    case "content:encoded":
                                        currentItem.setFullContent(content);
                                        break;
                                    case "link":
                                        if (!isInEntry) { // RSS link is text content
                                            currentItem.setUrl(content);
                                        }
                                        break;
                                    case "guid":
                                        // Use GUID as URL if no link is provided
                                        if (currentItem.getUrl() == null || currentItem.getUrl().isEmpty()) {
                                            if (content.startsWith("http")) {
                                                currentItem.setUrl(content);
                                            }
                                        }
                                        break;
                                    case "pubdate":
                                    case "published":
                                    case "updated":
                                    case "dc:date":
                                        currentItem.setTimestamp(parseDate(content));
                                        break;
                                    case "category":
                                    case "dc:subject":
                                        currentItem.setCategory(content);
                                        break;
                                    case "dc:creator":
                                    case "author":
                                        // Could be used for source attribution
                                        break;
                                    case "media:thumbnail":
                                    case "media:content":
                                        if (currentItem.getImageUrl() == null) {
                                            String imageUrl = extractImageUrl(content);
                                            if (imageUrl != null) {
                                                currentItem.setImageUrl(imageUrl);
                                            }
                                        }
                                        break;
                                }
                            }
                            
                            textContent.setLength(0);
                            currentTag = null;
                            break;
                    }
                    
                    eventType = parser.next();
                    
                } catch (Exception parseException) {
                    // Skip malformed parts and continue parsing
                    Log.w(TAG, "Skipping malformed XML element in " + sourceUrl + ": " + parseException.getMessage());
                    try {
                        eventType = parser.next();
                    } catch (Exception nextException) {
                        // If we can't even get the next event, break the loop
                        Log.w(TAG, "Cannot continue parsing, breaking loop");
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing RSS feed from " + sourceUrl, e);
        }
        
        Log.d(TAG, "Parsed " + newsList.size() + " items from " + sourceUrl);
        return newsList;
    }
    
    // ADDED: Method to read InputStream to String
    private String readStreamToString(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        
        reader.close();
        return stringBuilder.toString();
    }
    
    // ADDED: Method to sanitize XML and fix common issues
    private String sanitizeXml(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return xml;
        }
        
        // Fix common XML issues
        String sanitized = xml;
        
        // Fix unterminated entity references
        sanitized = fixUnterminatedEntities(sanitized);
        
        // Remove invalid characters that might cause XML parsing issues
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // Fix common encoding issues
        sanitized = sanitized.replace("&amp;amp;", "&amp;")
                             .replace("&amp;lt;", "&lt;")
                             .replace("&amp;gt;", "&gt;")
                             .replace("&amp;quot;", "&quot;");
        
        // Ensure proper XML declaration
        if (!sanitized.trim().startsWith("<?xml")) {
            sanitized = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + sanitized;
        }
        
        return sanitized;
    }
    
    // ADDED: Method to fix unterminated entity references
    private String fixUnterminatedEntities(String xml) {
        // Pattern to find unterminated entities (& followed by text but no semicolon)
        Pattern pattern = Pattern.compile("&([a-zA-Z0-9]+)(?![a-zA-Z0-9]*;)");
        Matcher matcher = pattern.matcher(xml);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String entity = matcher.group(1);
            
            // Check if it's a known HTML entity
            String replacement;
            switch (entity.toLowerCase()) {
                case "amp":
                    replacement = "&amp;";
                    break;
                case "lt":
                    replacement = "&lt;";
                    break;
                case "gt":
                    replacement = "&gt;";
                    break;
                case "quot":
                    replacement = "&quot;";
                    break;
                case "apos":
                    replacement = "&apos;";
                    break;
                case "nbsp":
                    replacement = "&nbsp;";
                    break;
                default:
                    // For unknown entities, escape the ampersand
                    replacement = "&amp;" + entity;
                    break;
            }
            
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private void enhanceNewsItem(NewsItem newsItem) {
        // Extract image from content if not already set
        if (newsItem.getImageUrl() == null && newsItem.getFullContent() != null) {
            String imageUrl = extractImageFromContent(newsItem.getFullContent());
            if (imageUrl != null) {
                newsItem.setImageUrl(imageUrl);
            }
        }
        
        // Try to extract image from preview if still no image
        if (newsItem.getImageUrl() == null && newsItem.getPreview() != null) {
            String imageUrl = extractImageFromContent(newsItem.getPreview());
            if (imageUrl != null) {
                newsItem.setImageUrl(imageUrl);
            }
        }
        
        // Set category based on content analysis
        if (newsItem.getCategory() == null || newsItem.getCategory().isEmpty()) {
            newsItem.setCategory(detectCategory(newsItem));
        }
        
        // Clean and validate URLs
        if (newsItem.getImageUrl() != null) {
            newsItem.setImageUrl(validateImageUrl(newsItem.getImageUrl()));
        }
        
        // Set current timestamp if not already set
        if (newsItem.getTimestamp() == 0) {
            newsItem.setTimestamp(System.currentTimeMillis());
        }
        
        // Set formatted date for display
        if (newsItem.getDate() == null || newsItem.getDate().isEmpty()) {
            newsItem.setDate(formatTimestamp(newsItem.getTimestamp()));
        }
        
        // Ensure we have some preview content
        if ((newsItem.getPreview() == null || newsItem.getPreview().isEmpty()) && 
            newsItem.getFullContent() != null) {
            String cleanContent = cleanHtml(newsItem.getFullContent());
            if (cleanContent.length() > 200) {
                newsItem.setPreview(cleanContent.substring(0, 200) + "...");
            } else {
                newsItem.setPreview(cleanContent);
            }
        }
        
        // Calculate and set reading time
        newsItem.setReadingTimeMinutes(calculateReadingTime(newsItem));
    }
    
    // Enhanced reading time calculation
    private int calculateReadingTime(NewsItem newsItem) {
        String content = "";
        
        // Combine all available text content
        if (newsItem.getFullContent() != null && !newsItem.getFullContent().isEmpty()) {
            content = newsItem.getFullContent();
        } else if (newsItem.getPreview() != null && !newsItem.getPreview().isEmpty()) {
            content = newsItem.getPreview();
        } else if (newsItem.getTitle() != null) {
            content = newsItem.getTitle();
        }
        
        if (content.isEmpty()) {
            return 3; // Default 3 minutes
        }
        
        // Clean HTML tags and calculate word count
        String cleanContent = cleanHtml(content);
        String[] words = cleanContent.split("\\s+");
        int wordCount = words.length;
        
        // Average reading speed: 200-250 words per minute
        // Using 200 words per minute for conservative estimate
        int readingTime = Math.max(1, wordCount / 200);
        
        // Cap at reasonable maximum
        return Math.min(readingTime, 30);
    }
    
    // ADDED: Helper method to format timestamp for display
    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    private String extractSourceFromUrl(String url) {
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            
            // Remove www. prefix
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            
            // Extract main domain name
            String[] parts = host.split("\\.");
            if (parts.length >= 2) {
                return capitalizeFirst(parts[0]);
            }
            
            return host;
        } catch (Exception e) {
            return "Environmental News";
        }
    }
    
    private String detectCategory(NewsItem newsItem) {
        String title = newsItem.getTitle() != null ? newsItem.getTitle().toLowerCase() : "";
        String content = (newsItem.getPreview() != null ? newsItem.getPreview() : "").toLowerCase();
        String fullText = title + " " + content;
        
        // Environmental keywords mapping
        if (containsKeywords(fullText, "climate", "global warming", "greenhouse", "carbon", "emission")) {
            return "Climate Change";
        } else if (containsKeywords(fullText, "renewable", "solar", "wind", "energy", "battery")) {
            return "Renewable Energy";
        } else if (containsKeywords(fullText, "wildlife", "species", "conservation", "biodiversity", "ecosystem")) {
            return "Conservation";
        } else if (containsKeywords(fullText, "pollution", "waste", "plastic", "recycling", "toxic")) {
            return "Pollution";
        } else if (containsKeywords(fullText, "sustainable", "green", "eco-friendly", "sustainability")) {
            return "Sustainability";
        } else if (containsKeywords(fullText, "technology", "innovation", "green tech", "clean tech")) {
            return "Green Technology";
        } else if (containsKeywords(fullText, "policy", "government", "regulation", "law", "legislation")) {
            return "Environmental Policy";
        }
        
        return "Environmental";
    }
    
    private boolean containsKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private String extractImageFromContent(String content) {
        // Enhanced regex patterns for image URLs
        String[] patterns = {
            "<img[^>]+src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            "<media:thumbnail[^>]+url\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            "<media:content[^>]+url\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            "https?://[^\\s\"'<>]+\\.(jpg|jpeg|png|gif|webp|svg)(?:\\?[^\\s\"'<>]*)?",
            "<meta[^>]+property\\s*=\\s*[\"']og:image[\"'][^>]+content\\s*=\\s*[\"']([^\"']+)[\"']",
            "<meta[^>]+content\\s*=\\s*[\"']([^\"']+)[\"'][^>]+property\\s*=\\s*[\"']og:image[\"']"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                String imageUrl = matcher.group(1);
                if (imageUrl != null && isValidImageUrl(imageUrl)) {
                    return imageUrl;
                }
            }
        }
        
        return null;
    }
    
    private String extractImageUrl(String content) {
        // Simple URL extraction
        if (content.startsWith("http") && isValidImageUrl(content)) {
            return content;
        }
        return null;
    }
    
    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") || 
               lowerUrl.contains(".png") || lowerUrl.contains(".gif") ||
               lowerUrl.contains(".webp") || lowerUrl.contains(".svg");
    }
    
    private String validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        
        // Ensure URL is complete
        if (imageUrl.startsWith("//")) {
            imageUrl = "https:" + imageUrl;
        } else if (imageUrl.startsWith("/")) {
            // Relative URL - would need base URL context
            return null;
        }
        
        return imageUrl.trim();
    }
    
    private long parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return System.currentTimeMillis();
        }
        
        // Enhanced RSS/Atom date formats
        String[] dateFormats = {
            "EEE, dd MMM yyyy HH:mm:ss Z",        // RFC 822
            "EEE, dd MMM yyyy HH:mm:ss zzz",      // RFC 822 with timezone
            "yyyy-MM-dd'T'HH:mm:ss'Z'",           // ISO 8601
            "yyyy-MM-dd'T'HH:mm:ssZ",             // ISO 8601 with timezone
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",       // ISO 8601 with milliseconds
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",         // ISO 8601 with milliseconds and timezone
            "yyyy-MM-dd'T'HH:mm:ss",              // ISO 8601 without timezone
            "yyyy-MM-dd HH:mm:ss",                // Simple format
            "MMM dd, yyyy",                       // Human readable
            "dd MMM yyyy HH:mm:ss",               // Alternative format
            "yyyy-MM-dd",                         // Date only
            "EEE MMM dd HH:mm:ss yyyy"            // Another common format
        };
        
        for (String format : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
                Date date = sdf.parse(dateStr.trim());
                return date.getTime();
            } catch (ParseException e) {
                // Try next format
            }
        }
        
        // If all parsing fails, return current time
        Log.w(TAG, "Could not parse date: " + dateStr);
        return System.currentTimeMillis();
    }
    
    private String cleanHtml(String html) {
        if (html == null) {
            return "";
        }
        
        // Remove HTML tags
        String cleaned = html.replaceAll("<[^>]*>", "");
        
        // Decode common HTML entities using Unicode escape sequences
        cleaned = cleaned.replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .replace("&apos;", "'")
                        .replace("&nbsp;", " ")
                        .replace("&hellip;", "\u2026")  // …
                        .replace("&mdash;", "\u2014")   // —
                        .replace("&ndash;", "\u2013")   // –
                        .replace("&rsquo;", "\u2019")   // '
                        .replace("&lsquo;", "\u2018")   // '
                        .replace("&rdquo;", "\u201D")   // "
                        .replace("&ldquo;", "\u201C");  // "
        
        return cleaned.trim();
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}