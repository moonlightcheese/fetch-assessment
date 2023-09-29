package com.fetch;

import android.os.Looper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class HttpRequestThread extends Thread {
    private final URL url;
    private final HttpEventListener listener;
    private HttpURLConnection urlConnection;
    private String responseString = new String();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public HttpRequestThread(
            URL url,
            HttpEventListener listener) {
        this.url = url;
        this.listener = listener;
    }

    public void run() {
        Looper.prepare();
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            if(urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                logger.info("Response 200 OK");
                //read character data
//                Reader in = new InputStreamReader(urlConnection.getInputStream());
//                char[] buffer = new char[1024];
//                while(in.read(buffer) >= -1) {
//                    responseString.append(buffer);
//                }

                if (this.listener != null) {
                    responseString = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))
                            .lines().collect(Collectors.joining("\n")).toString();
                    logger.debug("Received response string:\n" + responseString);
                    this.listener.onResponseReceived(responseString.toString());
                }
            } else {
                if (this.listener != null) {
                    this.listener.onError("Response code invalid: " + urlConnection.getResponseCode(), null);
                }
            }
        } catch(IOException ioe) {
            logger.warn("Unable to read HTTP stream!", ioe);
            if (this.listener != null) {
                this.listener.onError(null, ioe);
            }
        } finally {
            urlConnection.disconnect();
        }
    }
}

