/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bigbluebutton.api.libs;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

//import org.red5.logging.Red5LoggerFactory;
//import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;

/**
 *
 * @author iadd
 */
public class Curl {
    //private static Logger log = LoggerFactory.getLogger(Curl.class, "bigbluebutton");

    public static interface Listener {
        void onResponse(String response);
    }

    public static void GET(final String urlstr, final String params,
            final Listener listener) {
        Thread send_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlstr);
                    URLConnection conn = url.openConnection();
                    conn.setDoOutput(true);
                    OutputStreamWriter writer = new OutputStreamWriter(
                            conn.getOutputStream());
                    writer.write(params);
                    writer.flush();
                    String line;
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    if (listener != null)
                        listener.onResponse(response.toString());
                    writer.close();
                    reader.close();
                } catch (MalformedURLException ex) {
                    //log.debug(ex.toString());
                } catch (IOException ex) {
                    //log.debug(ex.toString());
                }
            }
        });
        send_thread.start();
    }

    public static void POST(final String urlstr, final String params,
            final Listener listener) {
        try {
            URL url = new URL(urlstr);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length",
                    "" + Integer.toString(params.getBytes().length));
            connection.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(params);
            wr.flush();
            wr.close();
            
            String line;
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            if (listener != null)
                listener.onResponse(response.toString());
            
            connection.disconnect();
        } catch (IOException ex) {
            //log.debug(ex.toString());
        }
    }
}