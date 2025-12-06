package org.zcdada.shortlink_zc.project.service.impl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.project.common.convention.exception.ClientException;
import org.zcdada.shortlink_zc.project.service.UrlService;

import java.io.IOException;
import java.util.Objects;

@Service
public class UrlServiceImpl implements UrlService {

    @Override
    public String getTitle(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            return doc.title(); // 直接返回<title>里的内容
        } catch (IOException e) {

            System.err.println("获取标题失败: " + e.getMessage());
            throw new ClientException("获取标题失败");
        }
    }

    @Override
    public String getFavicon(String url) {
        try {
            Document doc = Jsoup.connect(url).get();

            // 优先从HTML中找图标（常见于<link rel="icon">）
            Elements iconLinks = doc.select("link[rel=icon], link[rel=shortcut icon]");
            if (!iconLinks.isEmpty()) {
                return Objects.requireNonNull(iconLinks.first()).absUrl("href"); // 自动转成绝对URL
            }

            // 如果HTML没找到，尝试默认路径 /favicon.ico
            String baseUrl = url.endsWith("/") ? url : url + "/";
            return baseUrl + "favicon.ico";
        } catch (IOException e) {
            System.err.println("获取图标失败: " + e.getMessage());
            throw new ClientException("获取图标失败");
        }
    }

}
