package com.sectrend.buildscan.utils;

import java.security.SecureRandom;
import okhttp3.*;
import com.alibaba.fastjson.JSON;
import java.io.File;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import com.sectrend.buildscan.model.FileParam;
import java.net.URLEncoder;
import javax.net.ssl.SSLSocketFactory;
import java.util.Map;
import java.util.LinkedHashMap;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
public class OkHttpUtils {
    private static volatile OkHttpClient okHttpUtilClient = null;
    private Map<String, String> requestHeaderMap;
    private Request.Builder request;
    private String requestUrl;
    private Map<String, Object> requestParamMap;

    /**
     * 初始化okHttpClient，兼容 https 访问
     */
    private OkHttpUtils() {
        if (okHttpUtilClient == null) {
            synchronized (OkHttpUtils.class) {
                if (okHttpUtilClient == null) {
                    TrustManager[] managers = buildTrustManagers();
                    okHttpUtilClient = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.MINUTES)
                            .writeTimeout(20, TimeUnit.MINUTES)
                            .readTimeout(20, TimeUnit.MINUTES)
                            .callTimeout(20, TimeUnit.MINUTES)
                            .sslSocketFactory(createSSLSocketFactory(managers), (X509TrustManager) managers[0])
                            .hostnameVerifier((hostName, session) -> true)
                            .retryOnConnectionFailure(true)
                            .build();
                    addRequestHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
                }
            }
        }
    }

    /**
     * OkHttpUtils 创建
     *
     * @return OkHttpUtils
     */
    public static OkHttpUtils builder() {
        return new OkHttpUtils().addRequestHeader("sourceType", "1");
    }

    /**
     * 添加url
     *
     * @param requestUrl 请求 url
     * @return OkHttpUtils
     */
    public OkHttpUtils url(String requestUrl) {
        this.requestUrl = requestUrl;
        return this;
    }

    /**
     * 添加请求参数
     *
     * @param key   参数名称
     * @param value 参数值
     * @return OkHttpUtils
     */
    public OkHttpUtils addRequestParam(String key, Object value) {
        if (requestParamMap == null) {
            requestParamMap = new LinkedHashMap<>(16);
        }
        requestParamMap.put(key, value);
        return this;
    }

    /**
     * 添加请求头参数
     *
     * @param key   参数名称
     * @param value 参数值
     * @return OkHttpUtils
     */
    public OkHttpUtils addRequestHeader(String key, String value) {
        if (requestHeaderMap == null) {
            requestHeaderMap = new LinkedHashMap<>(16);
        }
        requestHeaderMap.put(key, value);
        return this;
    }

    /**
     * get方法初始化
     *
     * @return OkHttpUtils
     */
    public OkHttpUtils get() {
        request = new Request.Builder().get();
        StringBuilder requestUrlBuilder = new StringBuilder(requestUrl);
        if (requestParamMap != null) {
            requestUrlBuilder.append("?");
            try {
                for (Map.Entry<String, Object> entry : requestParamMap.entrySet()) {
                    requestUrlBuilder.append(URLEncoder.encode(entry.getKey(), "utf-8")).
                            append("=").
                            append(URLEncoder.encode(String.valueOf(entry.getValue()), "utf-8")).
                            append("&");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            requestUrlBuilder.deleteCharAt(requestUrlBuilder.length() - 1);
        }
        request.url(requestUrlBuilder.toString());
        return this;
    }

    /**
     * post方法初始化
     *
     * @param isJsonRawPost true等于json的方式提交数据，类似postman里post方法的raw
     *                   false等于普通的表单提交
     * @return OkHttpUtils
     */
    public OkHttpUtils post(boolean isJsonRawPost) {
        RequestBody requestBody;
        if (isJsonRawPost) {
            String json = "";
            if (requestParamMap != null) {
                json = JSON.toJSONString(requestParamMap);
            }
            requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);
        } else {
            MultipartBody.Builder multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM);
            if (requestParamMap != null) {
                requestParamMap.forEach((key, val) -> addMultipartBody(key, val, multipartBody));
            }
            requestBody = multipartBody.build();

        }
        if(requestParamMap.get("scanResult") != null){
            String scanResult = (String) requestParamMap.get("scanResult");
            requestParamMap.put("scanResult",  String.valueOf(scanResult.length()));
        }
        log.debug("Upload parameters：{}", JSON.toJSONString(requestParamMap));
        request = new Request.Builder().post(requestBody).url(requestUrl);
        return this;
    }

    private void addMultipartBody(String key, Object val, MultipartBody.Builder multipartBody){
        if(val instanceof File){
            File file = (File) val;
            this.addFormFile(multipartBody, key, file);
            return;
        }
        if (val instanceof FileParam){
            FileParam fileParam = (FileParam) val;
            this.addFormFile(multipartBody, key, fileParam);
            return;
        }

        if(val instanceof String){
            multipartBody.addFormDataPart(key, String.valueOf(val));
        }
    }


    /**
     * 添加 File 参数
     * @param builder
     * @param key 键
     * @param file file值
     */
    private void addFormFile(MultipartBody.Builder builder, String key, File file){
        builder.addFormDataPart(key, file.getName(),
                RequestBody.create(MediaType.parse("application/octet-stream"),
                        file));
    }

    /**
     * 添加 File 参数
     * @param builder
     * @param key 键
     * @param fileParam 文件参数
     */
    private void addFormFile(MultipartBody.Builder builder, String key, FileParam fileParam){
        builder.addFormDataPart(key, fileParam.getFileName(),
                RequestBody.create(MediaType.parse("application/octet-stream"),
                        fileParam.getFileText()));
    }


    /**
     * 同步请求
     *
     * @return
     */
    public Response sync() {
        setHeader(request);
        try {
            Response response = okHttpUtilClient.newCall(request.build()).execute();
            if (response.body() == null) {
                return null;
            }

            return response;
        } catch (IOException e) {
            log.error("request was aborted", e);
            return null;
        }
    }

    /**
     * request 添加请求头
     *
     * @param request 请求
     */
    private void setHeader(Request.Builder request) {
        if (requestHeaderMap != null) {
            try {
                for (Map.Entry<String, String> entry : requestHeaderMap.entrySet()) {
                    request.addHeader(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 生成 SSLSocket 工厂，用于 https 请求的证书跳过
     *
     * @return SSLSocketFactory
     */
    private static SSLSocketFactory createSSLSocketFactory(TrustManager[] trustManagers) {
        SSLSocketFactory sslSocketFactory = null;
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustManagers, new SecureRandom());
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sslSocketFactory;
    }

    private static TrustManager[] buildTrustManagers() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };
    }
}

