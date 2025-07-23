package com.sectrend.buildscan.utils;

import com.alibaba.fastjson.JSON;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OkHttpUtil<T> {

    private Map<String, T> paramMap;
    private Map<String, String> headerMap;

    private OkHttpClient client;

    private final Logger logger = LoggerFactory.getLogger(OkHttpUtil.class);

    /**
     * 添加参数
     * @return
     */
    public OkHttpUtil addParam(String key, T value){
        if(paramMap == null){
            paramMap = new HashMap<>();
        }
        paramMap.put(key, value);
        return this;
    }

    /**
     * 添加 header 参数
     * @param key
     * @param value
     * @return
     */
    public OkHttpUtil addHeader(String key, String value){
        if(headerMap == null){
            headerMap = new HashMap<>();
        }
        headerMap.put(key, value);
        return this;
    }

    /**
     * 设置 Header 参数
     * @param request
     */
    private Request setHeader(Request request){
        Request.Builder requestBuilder = request.newBuilder();
        if(headerMap != null){
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
            return requestBuilder.build();
        }
        return request;
    }

    /**
     * 设置参数值
     */
    private void setFormDataPart(MultipartBody.Builder builder){

        if (this.paramMap != null) {
            for (Map.Entry<String, T> entry : paramMap.entrySet()) {
                if(entry.getValue() instanceof File){
                    File file = (File) entry.getValue();
                    this.addFormFile(builder, entry.getKey(), file);
                    continue;
                }
                if(entry.getValue() instanceof List){
                    List<T> object = (List<T>) entry.getValue();
                    object.forEach(e -> {
                        if(e instanceof File)
                            this.addFormFile(builder, entry.getKey(), (File) e);
                    });
                    continue;
                }
                if(entry.getValue() instanceof String){
                    builder.addFormDataPart(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        }
    }

    /**
     * 添加 File 参数
     * @param builder
     * @param key 键
     * @param file file值
     */
    private void addFormFile(MultipartBody.Builder builder, String key, File file){
        builder.addFormDataPart(key, file.getAbsolutePath(),
                RequestBody.create(MediaType.parse("application/octet-stream"),
                        file));
    }

    /**
     * 执行http-post，参数请求格式JSON
     * @param url
     * @return
     * @throws IOException
     */
    public Response sentPostByJson(String url) throws IOException {

        try {
            OkHttpClient client = getOkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, JSON.toJSONString(paramMap));
            Request request = new Request.Builder()
                    .url(url)
                    .method("POST", body)
                    .build();
            request = setHeader(request);
            Response response = client.newCall(request).execute();
            return response;
        } finally {
            clearMap();
        }
    }

    /**
     * 执行http-post，参数请求格式JSON
     * @param url
     * @return
     * @throws IOException
     */
    public Response sentGetByJson(String url) throws IOException {

        try {
            OkHttpClient client = getOkHttpClient();
            Request request = new Request.Builder().url(url).get().build();
            request = setHeader(request);
            Response response = client.newCall(request).execute();
            return response;
        } finally {
            clearMap();
        }
    }

    private OkHttpClient getOkHttpClient() {

        if(this.client == null){
            this.client = new OkHttpClient().newBuilder()
                    .build();
        }
        return this.client;
    }

    public void  buildOkHttpClient(OkHttpClient client){
        this.client = client;
    }

    /**
     * 发送post请求通过Form表单形式  返回response.body().string();
     */
    public Response sendPostByForm(String url) throws IOException {

        try {
            OkHttpClient client = getOkHttpClient();
            MultipartBody.Builder builder = new MultipartBody.Builder();
            builder.setType(MultipartBody.FORM);
            setFormDataPart(builder);

            RequestBody body = builder.build();
            Request request = new Request.Builder()
                    .url(url)
                    .method("POST", body)
                    .build();

            request = setHeader(request);
            Response response = client.newCall(request).execute();
            if(paramMap.get("scanResult") != null){
                String scanResult = (String) paramMap.get("scanResult");
                paramMap.put("scanResult", (T) String.valueOf(scanResult.length()));
            }
            logger.info("Upload parameters：{}", JSON.toJSONString(paramMap));
            return response;

        } finally {
            clearMap();
        }
    }







    /**
     * 清除map数据
     */
    private void clearMap(){
        if(this.paramMap != null){
            this.paramMap.clear();
        }

        if(this.headerMap != null){
            this.headerMap.clear();
        }
    }


}
