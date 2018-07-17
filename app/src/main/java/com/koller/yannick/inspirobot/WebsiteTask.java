package com.koller.yannick.inspirobot;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebsiteTask extends AsyncTask<String, String, String>{

    private static String M_TAG = "WebsiteTask";
    public AsyncResponse delegate = null;

    public WebsiteTask(AsyncResponse delegate){
        this.delegate = delegate;
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            String string = downloadDataFromUrl(urls[0]);
            Log.d("WebsiteTask", string);
            return string;
        } catch (IOException e) {
            return "Unable to retrieve data. URL may be invalid.";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        delegate.processFinish(s);
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    public static String downloadDataFromUrl(String myurl) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000); // time in milliseconds
            conn.setConnectTimeout(15000); // time in milliseconds
            conn.setRequestMethod("GET"); // request method GET OR POST
            conn.setDoInput(true);
            conn.connect(); // calling the web address
            int response = conn.getResponseCode();
            Log.d(M_TAG, "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readInputStream(is);
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    public static String readInputStream(InputStream stream) throws IOException {
        int n = 0;
        char[] buffer = new char[1];
        InputStreamReader reader = new InputStreamReader(stream, "UTF8");
        StringWriter writer = new StringWriter();
        while (-1 != (n = reader.read(buffer)))
            writer.write(buffer, 0, n);
        return writer.toString();
    }
}
