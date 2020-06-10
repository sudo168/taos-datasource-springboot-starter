package net.ewant.taos.http;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class HttpClient {

    private static final int DEFAULT_CONNECT_TIMEOUT = 20000;
    private static final int DEFAULT_READ_TIMEOUT = 30000;
    private static final int DEFAULT_IDEL_TIMEOUT = 30000;

    Map<String, String> headers = new HashMap<>();
    int lastStatus = 200;
    long lastResponseTime = System.currentTimeMillis();
    String url;

    public HttpClient(String httpUrl) {
        this.url = httpUrl;
    }

    public boolean isAvailable(){
        return lastStatus < 500 && (System.currentTimeMillis() - lastResponseTime < DEFAULT_IDEL_TIMEOUT);
    }

    public void close(){
        lastStatus = 0;
        lastResponseTime = 0;
    }

    public HttpClient addHeader(String name, String value){
        headers.put(name, value);
        return this;
    }

    public String doGet() {
        try {
            HttpURLConnection connection = getConnection(url);
            connection.setRequestMethod("GET");
            headers.keySet().forEach(name->{
                connection.setRequestProperty(name, headers.get(name));
            });
            headers.clear();
            connection.connect();

            return getResponse(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String doPost(String body){
        try {
            HttpURLConnection connection = getConnection(url);
            connection.setRequestMethod("POST");
            // 发送POST请求必须设置为true
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            headers.keySet().forEach(name->{
                connection.setRequestProperty(name, headers.get(name));
            });
            headers.clear();
            OutputStream out = connection.getOutputStream();
            out.write(body.getBytes("UTF-8"));
            out.flush();
            out.close();

            return getResponse(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getResponse(HttpURLConnection connection) throws IOException {
        this.lastStatus = connection.getResponseCode();
        this.lastResponseTime = System.currentTimeMillis();
        if(connection.getResponseCode() != 200){
            TaosHttpConnection.logger.error("Server returned HTTP response code: "+connection.getResponseCode()+" for URL: " + url);
            return null;
        }
        BufferedReader resp = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        StringBuffer sbf = new StringBuffer();
        String temp;
        while ((temp = resp.readLine()) != null) {
            sbf.append(temp);
        }
        resp.close();
        return sbf.toString();
    }

    private static HttpURLConnection getConnection(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        boolean useHttps = url.startsWith("https");
        if(useHttps){
            HttpsURLConnection https = (HttpsURLConnection) connection;
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
            SSLSocketFactory newFactory = sc.getSocketFactory();
            https.setHostnameVerifier(IGNORE_HOSTNAME_VERIFIER);
            https.setSSLSocketFactory(newFactory);
        }
        connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
        return connection;
    }

    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }
    }};

    private static final HostnameVerifier IGNORE_HOSTNAME_VERIFIER = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };
}
