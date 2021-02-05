package com.pkq.util;

import com.alibaba.fastjson.JSONObject;
import com.pkq.model.TestHttpClientResp;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class HttpClientUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    private static volatile HttpClientUtil client;

    private HttpClientUtil(){}

    public static HttpClientUtil getClient(){
        if(Objects.isNull(client)){
            synchronized (HttpClientUtil.class){
                if(Objects.isNull(client)){
                    client = new HttpClientUtil();
                }
            }
        }
        return client;
    }

    public <T> T get(String url, Map<String,Object> params, Class<T> clazz) {
        if(Objects.isNull(url) || url.length() == 0){
            return null;
        }
        BasicHeader header = new BasicHeader("Content-Type","application/json; charset=UTF-8");
        // 获得Http客户端(可以理解为:你得先有一个浏览器;注意:实际上HttpClient与浏览器是不一样的)
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultHeaders(Collections.singleton(header))
                .build();
        // 创建Get请求
        HttpGet httpGet = new HttpGet(url);
        // 响应模型
        CloseableHttpResponse response = null;
        try {
            // 由客户端执行(发送)Get请求
            response = httpClient.execute(httpGet);
            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity();
            logger.info("http code:{}", response.getStatusLine().getStatusCode());
            if (responseEntity != null) {
                String result = EntityUtils.toString(responseEntity);
                logger.info("响应内容为:{}" , result);
                return JSONObject.parseObject(result,clazz);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // 释放资源
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public <T> T post(String url, Map<String,Object> params, Class<T> clazz) {
        if(Objects.isNull(url) || url.length() == 0){
            return null;
        }
        return (T) "";
    }
}
