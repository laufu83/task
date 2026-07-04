package cn.fred.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.entity.StringEntity;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP工具增强版
 * 支持：GET、POST、PUT、DELETE、PATCH、HEAD、OPTIONS
 * 1. 三层超时控制、自动重定向
 * 2. 请求日志、耗时、状态码打印，大响应自动截断
 * 3. 参数去空格容错、空JSON兼容
 * 4. 最大响应2MB防OOM
 * 5. 默认UA、默认JSON请求头，自定义头可覆盖
 * 6. 非2xx状态码抛异常，业务可捕获记录失败
 * 7. 全方法快捷重载调用
 */
public class HttpClientUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    // 超时配置 毫秒
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 8000;
    private static final int CONNECTION_REQUEST_TIMEOUT = 5000;
    // 最大响应 2MB
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    /**
     * 统一HTTP请求入口，支持 GET POST PUT DELETE PATCH HEAD OPTIONS
     * @param url 请求地址
     * @param method 请求方式（不区分大小写）
     * @param headerJson 请求头JSON
     * @param bodyJson 请求体JSON
     * @return 响应内容
     * @throws IOException 网络、状态码异常
     */
    public static String doRequest(String url, String method, String headerJson, String bodyJson) throws IOException {
        headerJson = trimStr(headerJson);
        bodyJson = trimStr(bodyJson);
        String reqMethod = method.toUpperCase().trim();
        long start = System.currentTimeMillis();

        // 超时、重定向配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
                .setRedirectsEnabled(true)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            Map<String, String> headerMap = null;
            if (headerJson != null && !headerJson.isBlank()) {
                try {
                    headerMap = JSON.parseObject(headerJson, new TypeReference<>() {
                    });
                } catch (Exception e) {
                    log.error("请求头JSON解析失败：{}", headerJson, e);
                    throw new IOException("请求头JSON格式错误，请检查");
                }
            }

            HttpRequestBase request = buildHttpRequest(reqMethod, url, bodyJson);

            // 默认请求头
            request.setHeader("User-Agent", "Task-Scheduler-HttpClient/1.0");
            if (request instanceof HttpEntityEnclosingRequestBase) {
                request.setHeader("Content-Type", "application/json;charset=UTF-8");
            }

            // 自定义请求头覆盖默认
            if (headerMap != null) {
                headerMap.forEach(request::setHeader);
            }

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String respBody = null;

                if (response.getEntity() != null) {
                    long contentLen = response.getEntity().getContentLength();
                    if (contentLen > MAX_RESPONSE_BYTES) {
                        EntityUtils.consume(response.getEntity());
                        throw new IOException("响应数据超出最大限制2MB");
                    }
                    respBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }

                long cost = System.currentTimeMillis() - start;
                String logResp = respBody != null && respBody.length() > 500
                        ? respBody.substring(0, 500) + "..."
                        : respBody;

                log.info("[HTTP] {} {} | status:{} | cost:{}ms | header:{} | body:{} | resp:{}",
                        reqMethod, url, statusCode, cost, headerJson, bodyJson, logResp);

                // 非2xx抛出异常
                if (statusCode < 200 || statusCode >= 300) {
                    throw new IOException("请求异常，状态码：" + statusCode + "，响应：" + respBody);
                }

                return respBody;
            }
        }
    }

    /**
     * 根据请求方式构建对应请求对象，支持带Body的PUT/POST/PATCH
     */
    private static HttpRequestBase buildHttpRequest(String method, String url, String bodyJson) throws IOException {
        HttpEntityEnclosingRequestBase entityRequest = null;
        switch (method) {
            case "POST":
                entityRequest = new HttpPost(url);
                break;
            case "PUT":
                entityRequest = new HttpPut(url);
                break;
            case "PATCH":
                entityRequest = new HttpPatch(url);
                break;
            case "DELETE":
                // DELETE 支持携带请求体
                entityRequest = new HttpDeleteWithBody(url);
                break;
            case "GET":
                return new HttpGet(url);
            case "HEAD":
                return new HttpHead(url);
            case "OPTIONS":
                return new HttpOptions(url);
            default:
                throw new IOException("不支持的请求方式：" + method);
        }

        // PUT POST PATCH DELETE 携带Body
        if (bodyJson != null) {
            StringEntity entity = new StringEntity(bodyJson, StandardCharsets.UTF_8);
            entityRequest.setEntity(entity);
        }
        return entityRequest;
    }

    /**
     * 字符串去首尾空白，空白返回null
     */
    private static String trimStr(String str) {
        if (str == null) return null;
        String trim = str.trim();
        return trim.isBlank() ? null : trim;
    }

    // ===================== 快捷重载方法 =====================
    public static String get(String url) throws IOException {
        return doRequest(url, "GET", null, null);
    }

    public static String get(String url, String headerJson) throws IOException {
        return doRequest(url, "GET", headerJson, null);
    }

    public static String post(String url, String bodyJson) throws IOException {
        return doRequest(url, "POST", null, bodyJson);
    }

    public static String post(String url, String headerJson, String bodyJson) throws IOException {
        return doRequest(url, "POST", headerJson, bodyJson);
    }

    public static String put(String url, String headerJson, String bodyJson) throws IOException {
        return doRequest(url, "PUT", headerJson, bodyJson);
    }

    public static String delete(String url, String headerJson, String bodyJson) throws IOException {
        return doRequest(url, "DELETE", headerJson, bodyJson);
    }

    public static String patch(String url, String headerJson, String bodyJson) throws IOException {
        return doRequest(url, "PATCH", headerJson, bodyJson);
    }

    public static String head(String url, String headerJson) throws IOException {
        return doRequest(url, "HEAD", headerJson, null);
    }

    public static String options(String url, String headerJson) throws IOException {
        return doRequest(url, "OPTIONS", headerJson, null);
    }

    // 自定义支持带Body的DELETE
    static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        public HttpDeleteWithBody(String uri) {
            super();
            setURI(URI.create(uri));
        }
        @Override
        public String getMethod() {
            return "DELETE";
        }
    }
}