package browser;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.Cookie;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class HTTPClient {

    private HttpURLConnection connection;
    private List<String> cookies;
    private List<Cookie> loginCookies;
    private final String USER_AGENT = "Mozilla/5.0";
    private final String HOST = "ssl.meetme.com";

    /**
     * Send a HTTPs request
     * @param reqMethod POST or GET accepted
     * @param url_str url of request
     * @param params request parameters
     * @param requestProperties a list of request headers
     * @return response string
     * @throws IOException
     */
    public String sendRequest(String reqMethod, String url_str, String params, ArrayList<PropertyEntry> requestProperties) throws IOException {
        // Disabling certificate checking and adding a proxy allows requests to be monitored by the HTTP Toolkit
        disableSSLCertificateChecking();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8000));

        // Setup connection
        URL url = new URL(url_str);
        connection = (HttpsURLConnection) url.openConnection(proxy);
        connection.setUseCaches(false);
        connection.setRequestMethod(reqMethod);

        // Set default properties headers
        connection.setRequestProperty("Host",HOST);
        connection.setRequestProperty("User-Agent",USER_AGENT);

        // Add passed properties headers
        for(Map.Entry<String, String> entry : requestProperties)
            connection.setRequestProperty(entry.getKey(),entry.getValue());

        // Add cookie headers, if cookies are stored
        if (cookies != null) {
            for (int i = 0; i < cookies.size(); i++)
                connection.addRequestProperty("Cookie", cookies.get(i));
        }

        // Send parameters if they are passed
        if(params != null) {
            connection.setDoOutput(true);
            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.writeBytes(params);
            output.flush();
            output.close();
        }

        // Print out response code
        System.out.println("\n" + connection.getRequestMethod() + " request sent to " + url_str);
        System.out.print("Response: " + connection.getResponseCode());
        System.out.println(" " + connection.getResponseMessage());

        // Define input accordingly
        InputStream inputStream;
        if(connection.getErrorStream() != null)
            inputStream = connection.getErrorStream();
        else inputStream = connection.getInputStream();

        // Retrieve response body
        BufferedReader input = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer response = new StringBuffer();
        String line;
        while ((line = input.readLine()) != null) {
            response.append(line);
        }
        input.close();

        // Check for error response and print if detected
        try {
            JSONObject result = (JSONObject) new JSONParser().parse(response.toString());
            if(connection.getErrorStream() != null) {
                System.out.println(result.toString());
            }
            else if(result.get("error") != null) {
                System.out.println(result.get("error") + ": " + result.get("errorType"));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        // If cookies are retrieved, save them
        if(connection.getHeaderFields().get("set-cookie") != null)
            setCookies(connection.getHeaderFields().get("set-cookie"));

        return response.toString();
    }

    /**
     * Sets the current list of cookies
     * @param cookies
     */
    public void setCookies(List<String> cookies) {
        this.cookies = cookies;

        System.out.println("\nCookies set:");
        for(String cookie : cookies) {
            System.out.println("\t"+cookie);
        }
    }

    /**
     * Returns a list of current cookies
     * @return cookies stored
     */
    public List<String> getCookies() {
        return cookies;
    }

    public void setLoginCookies(List<Cookie> loginCookies) {
        this.loginCookies = loginCookies;
    }

    public List<Cookie> getLoginCookes() {
        return loginCookies;
    }

    /**
     * Disables SSL certificate checking for any new instances of HttpsUrlConnection
     *
     * credit: https://gist.github.com/aembleton/889392
     */
    public void disableSSLCertificateChecking() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Not implemented
            }
        } };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}