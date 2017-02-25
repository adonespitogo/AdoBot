package com.android.adobot.http;


import java.util.HashMap;

public class Http extends HttpRequest implements HttpCallback {

    private Thread thread;
    private HttpCallback callback;

    public void execute(){
        super.setCallback(this);
        this.thread = new Thread(this);
        this.thread.start();
    }

    public void setCallback (HttpCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onResponse(HashMap response) {
        if (this.callback != null) this.callback.onResponse(response);
        try {
            this.thread.join();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}