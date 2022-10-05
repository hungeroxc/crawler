package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) {

        // 待处理的连接池
        List<String> linkPool = new ArrayList<>();
        Set<String> processedLinks = new HashSet<>();
        linkPool.add("https://news.sina.cn");

        while (true) {
            if(linkPool.isEmpty()) {
                break;
            }

            String link = linkPool.remove(linkPool.size() - 1);

            if(processedLinks.contains(link)) {
                continue;
            }

            if(isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);

                doc.select("a").stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);

                storeIntoDatabaseIfItIsNewsPage(doc);

                processedLinks.add(link);
            }
        }
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Document doc) {
        // 如果这是一个新闻的详情页面，就存入数据库，否则不做
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {

            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String strRes = EntityUtils.toString(entity1);
            String html = new String(strRes
                    .getBytes("ISO-8859-1"), "UTF-8");
            return Jsoup.parse(html);

        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link);
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }


}
