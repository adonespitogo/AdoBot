package com.android.adobot.http;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by adones on 2/17/17.
 */

public class HttpRequest implements Runnable {

    private static final String TAG = "HttpRequest";
    public static final String USER_AGENT = "Mozilla/5.0";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";

    protected HttpCallback callback;
    protected Map<String, String> params;
    protected String method;
    protected String url;

    public void setUrl(String url) {
        this.url = url.replace("?", "");
    }

    public void setMethod(String method) {
        this.method = method.toUpperCase();
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public void setCallback(HttpCallback cb) {
        this.callback = cb;
    }

    @Override
    public void run() {
        StringBuilder paramBuilder = new StringBuilder("");
        HashMap hashMapResponse = new HashMap();
        String result = "";
        int statusCode = 0;
        BufferedReader in;

        try {
            if (params != null) {
                for (String s : this.params.keySet()) {
                    if (params.get(s) != null) {
                        paramBuilder.append("&" + s + "=");
                        paramBuilder.append(URLEncoder.encode(String.valueOf(params.get(s)), "UTF-8"));
                    }
                }

                if (this.method == METHOD_GET) {
                    this.url = this.url + "?" + paramBuilder.substring(1);
                }
            }

            String url = this.url;
            URL obj = new URL(this.url);
            if (obj.getProtocol().toLowerCase().equals("https")) {

                HttpsURLConnection httpsConnection = (HttpsURLConnection) obj.openConnection();

                httpsConnection.setRequestMethod(this.method);
                httpsConnection.setRequestProperty("User-Agent", USER_AGENT);
                httpsConnection.setRequestProperty("Accept-Language", "UTF-8");

                httpsConnection.setDoOutput(true);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpsConnection.getOutputStream());
                outputStreamWriter.write(paramBuilder.toString());
                outputStreamWriter.flush();

                statusCode = httpsConnection.getResponseCode();
                Log.i(TAG, "\nSending '" + this.method + "' request to URL : " + url);
                Log.i(TAG, this.method + " parameters : " + paramBuilder);
                Log.i(TAG, "Response Code : " + statusCode);

                in = new BufferedReader(new InputStreamReader(httpsConnection.getInputStream()));
            } else {
                HttpURLConnection httpConnection = (HttpURLConnection) obj.openConnection();

                httpConnection.setRequestMethod(this.method);
                httpConnection.setRequestProperty("User-Agent", USER_AGENT);
                httpConnection.setRequestProperty("Accept-Language", "UTF-8");

                httpConnection.setDoOutput(true);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpConnection.getOutputStream());
                outputStreamWriter.write(paramBuilder.toString());
                outputStreamWriter.flush();

                statusCode = httpConnection.getResponseCode();
                Log.i(TAG, "\nSending '" + this.method + "' request to URL : " + url);
                Log.i(TAG, this.method + " parameters : " + paramBuilder);
                Log.i(TAG, "Response Code : " + statusCode);

                in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close();

            result = response.toString();
            Log.i(TAG, "Response Body : " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            hashMapResponse.put("status", statusCode);
            hashMapResponse.put("body", result);
            if (callback != null)
                callback.onResponse(hashMapResponse);
        }
    }

}