package com.koller.yannick.inspirobot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

public class ImageDetailActivity extends AppCompatActivity {

    private final static int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 1000;

    private File[] m_files;
    private String m_fileName;
    private int m_pos;

    private InterstitialAd m_interstitialAd;

    private Toolbar m_toolbar;
    private ActionBar m_actionBar;
    private TextView m_name;
    private ImageButton m_download;
    private ImageButton m_share;

    private ViewPager m_viewPager;
    private CustomSwipeAdapter m_cSAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail_view);

        m_files = (File[]) getIntent().getSerializableExtra("Files");
        m_pos = getIntent().getIntExtra("Pos", 0);

        m_name = findViewById(R.id.image_detail_name);

        m_viewPager = findViewById(R.id.image_detail_view_pager);
        m_cSAdapter = new CustomSwipeAdapter(m_files, this);
        m_viewPager.setAdapter(m_cSAdapter);
        m_viewPager.setCurrentItem(m_pos);
        m_viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {}
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                displayAd();
                updateData(position);
            }
        });

        m_download = findViewById(R.id.image_detail_download);
        m_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAd();
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission needed!", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(ImageDetailActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                } else {
                    storeImageToGallery(m_files[m_pos]);
                    Toast.makeText(getApplicationContext(), "Image downloaded", Toast.LENGTH_SHORT).show();
                }
            }
        });

        m_share = findViewById(R.id.image_detail_share);
        m_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAd();
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("image/*");
                try {
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, getCurrentImageUri());
                    startActivity(Intent.createChooser(sharingIntent, "Share via..."));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("ShareImage", e.getLocalizedMessage());
                    Toast.makeText(getApplicationContext(), "Failed to share image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        initToolbar();
        initAd();
        updateData(m_pos);
    }

    private void initToolbar() {
        m_toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(m_toolbar);
        m_actionBar = getSupportActionBar();
        m_actionBar.setDisplayHomeAsUpEnabled(true);
        updateTitleBarText(m_pos);
    }

    private void initAd(){
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-9042314102946099~3303611577");
        AdRequest m_request = new AdRequest.Builder().build();

        m_interstitialAd = new InterstitialAd(getApplicationContext());
        m_interstitialAd.setAdUnitId("ca-app-pub-9042314102946099/2812656982");
        m_interstitialAd.loadAd(m_request);
        m_interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                m_interstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });
    }

    private void displayAd() {
        if(Tools.showAd()){
            if (m_interstitialAd.isLoaded()) {
                m_interstitialAd.show();
            } else {
                Log.d("Ad", "The interstitial wasn't loaded yet.");
            }
        }
    }

    private void updateData(int pos) {
        m_pos = pos;
        m_fileName = m_files[m_pos].getName().substring(0, m_files[m_pos].getName().indexOf("."));
        m_name.setText("Quote " + (m_pos + 1));
        updateTitleBarText(m_pos);
    }

    private void updateTitleBarText(int pos){
        m_actionBar.setTitle((pos + 1) + "/" + m_files.length);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    public void storeImageToGallery(File file) {
        Picasso.get().load(file).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                storeImage(getExternalImageDirectory(), bitmap);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Toast.makeText(getApplicationContext(), "Failed to store image", Toast.LENGTH_SHORT);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        });
    }

    private void storeImage(String path, Bitmap bitmap) {
        FileOutputStream fOut = null;
        File file = new File(path, m_fileName + ".png");

        try {
            fOut = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        try {
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, m_fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, "Quote: " + m_fileName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().toLowerCase(Locale.US).hashCode());
        values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.getName().toLowerCase(Locale.US));
        values.put("_data", file.getAbsolutePath());

        getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private Uri getCurrentImageUri() {
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), m_fileName + ".png");
        return FileProvider.getUriForFile(getApplicationContext(), "com.koller.yannick.inspirobot", file);
    }

    private String getExternalImageDirectory() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/InspiroBot";
    }
}
