package com.koller.yannick.inspirobot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final static int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 1000;
    private final static int CLICKS_TILL_AD = 9;

    private int m_numberOfButtonClicks = 0;
    private String m_currentImageUrl;

    private AdView m_bottomBanner;
    private InterstitialAd m_interstitialAd;

    private ImageView m_imageView;
    private FloatingActionButton m_downloadButton;
    private FloatingActionButton m_reloadButton;
    private FloatingActionButton m_shareButton;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();
        //Picasso.setSingletonInstance(new Picasso.Builder(getBaseContext()).build());

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        MobileAds.initialize(this, "ca-app-pub-9042314102946099~3303611577");
        AdRequest m_request = new AdRequest.Builder().build();

        m_bottomBanner = findViewById(R.id.bottom_banner);
        m_bottomBanner.loadAd(m_request);
        m_bottomBanner.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next banner.
                m_bottomBanner.loadAd(new AdRequest.Builder().build());
            }

        });

        m_interstitialAd = new InterstitialAd(this);
        m_interstitialAd.setAdUnitId("ca-app-pub-9042314102946099/2812656982");
        m_interstitialAd.loadAd(m_request);
        m_interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                m_interstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });

        m_imageView = (ImageView) findViewById(R.id.imageView);

        m_downloadButton = (FloatingActionButton) findViewById(R.id.downloadActionButton);
        m_downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleButtonState(m_downloadButton);

                displayAd();

                if(m_imageView.getDrawable() != null) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(MainActivity.this, "Permission needed!", Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                        return;
                    } else {
                        String fileName = UUID.randomUUID() + ".jpg";
                        Picasso.get().load(m_currentImageUrl).into(new SaveImageHelper(getBaseContext(), getApplicationContext().getContentResolver(), fileName, "image_description"));
                        Toast.makeText(MainActivity.this, "Image downloaded", Toast.LENGTH_SHORT).show();
                    }
                }

                cycleButtonState(m_downloadButton);
            }
        });

        m_reloadButton = (FloatingActionButton) findViewById(R.id.reloadActionButton);
        m_reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleButtonState(m_reloadButton);

                displayAd();

                Picasso.get().load(new WebsiteTask().doInBackground("http://inspirobot.me/api?generate=true")).into(m_imageView);

                cycleButtonState(m_reloadButton);
            }
        });

        m_shareButton = (FloatingActionButton) findViewById(R.id.shareActionButton);
        m_shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleButtonState(m_shareButton);

                displayAd();

                if(m_imageView.getDrawable() != null){
                    clearTempImages();
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("image/*");
                    try {
                        sharingIntent.putExtra(Intent.EXTRA_STREAM, getLocalBitmapUri(m_imageView));
                        startActivity(Intent.createChooser(sharingIntent, "Share via..."));
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to share image", Toast.LENGTH_SHORT).show();
                    }
                }

                cycleButtonState(m_shareButton);
            }
        });
    }

    private class WebsiteTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... urls) {
            try {
                String string = Utility.downloadDataFromUrl(urls[0]);
                Log.d("WebsiteTask", string);
                return m_currentImageUrl = string;
            } catch (IOException e) {
                return "Unable to retrieve data. URL may be invalid.";
            }
        }

        // onPostExecute displays the results of the doInBackgroud and also we
        // can hide progress dialog.
        @Override
        protected void onPostExecute(String result) {
        }
    }

    public void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
        }
    }

    // Returns the URI path to the Bitmap displayed in specified ImageView
    public Uri getLocalBitmapUri(ImageView imageView) {
        // Extract Bitmap from ImageView drawable
        Drawable drawable = imageView.getDrawable();
        Bitmap bmp = null;
        if (drawable instanceof BitmapDrawable){
            bmp = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        } else {
            return null;
        }
        // Store image to default external storage directory
        Uri bmpUri = null;
        try {
            // Use methods on Context to access package-specific directories on external storage.
            // This way, you don't need to request external read/write permission.
            // See https://youtu.be/5xVh-7ywKpE?t=25m25s
            File file =  new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + "_temp.png");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            // **Warning:** This will fail for API >= 24, use a FileProvider as shown below instead.
            bmpUri = Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bmpUri;
    }

    private void clearTempImages(){
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toURI());
        if (file.isDirectory())
        {
            String[] children = file.list();
            for (int i = 0; i < children.length; i++)
            {
                new File(file, children[i]).delete();
            }
        }
    }

    private void displayAd() {
        m_numberOfButtonClicks++;
        if(m_numberOfButtonClicks == CLICKS_TILL_AD){
            if (m_interstitialAd.isLoaded()) {
                m_interstitialAd.show();
            } else {
                Log.d("Ad", "The interstitial wasn't loaded yet.");
            }
            m_numberOfButtonClicks = 0;
        }
    }

    private void cycleButtonState(FloatingActionButton button){
        if(button.isEnabled()){
            button.setEnabled(false);
            button.setClickable(false);
        }else {
            button.setEnabled(true);
            button.setClickable(true);
        }
    }
}
