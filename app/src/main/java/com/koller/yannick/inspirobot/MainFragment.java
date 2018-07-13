package com.koller.yannick.inspirobot;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

import static com.koller.yannick.inspirobot.FontAwesomeManager.FONTAWESOME_SOLID;

public class MainFragment extends Fragment {

    private final static int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 1000;
    private final static int CLICKS_TILL_AD = 18;
    private final static int MAX_HISTORY_SIZE = 20;

    private int m_numberOfButtonClicks = 0;
    private static String m_currentImageUrl;
    private UUID m_currentImageUUID;

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
                    Toast.makeText(getActivity(), "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        checkPermissions();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        MobileAds.initialize(getContext(), "ca-app-pub-9042314102946099~3303611577");
        AdRequest m_request = new AdRequest.Builder().build();

        m_bottomBanner = view.findViewById(R.id.bottom_banner);
        m_bottomBanner.loadAd(m_request);
        m_bottomBanner.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next banner.
                m_bottomBanner.loadAd(new AdRequest.Builder().build());
            }

        });

        m_interstitialAd = new InterstitialAd(getContext());
        m_interstitialAd.setAdUnitId("ca-app-pub-9042314102946099/2812656982");
        m_interstitialAd.loadAd(m_request);
        m_interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                m_interstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });

        m_imageView = view.findViewById(R.id.imageView);

        m_downloadButton = view.findViewById(R.id.downloadActionButton);
        m_downloadButton.setImageDrawable(getFontAwesomeDrawable(R.string.fa_download));
        m_downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAd();

                if(m_imageView.getDrawable() != null) {
                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getContext(), "Permission needed!", Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                    } else {
                        storeImageToGallery();
                        Toast.makeText(getContext(), "Image downloaded", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        m_reloadButton = view.findViewById(R.id.reloadActionButton);
        m_reloadButton.setImageDrawable(getFontAwesomeDrawable(R.string.fa_sync_alt));
        m_reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAd();
                clearTempImages();
                loadNewImage();
            }
        });

        m_shareButton = view.findViewById(R.id.shareActionButton);
        m_shareButton.setImageDrawable(getFontAwesomeDrawable(R.string.fa_share_alt));
        m_shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAd();

                if(m_imageView.getDrawable() != null){
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("image/*");
                    try {
                        sharingIntent.putExtra(Intent.EXTRA_STREAM, getCurrentImageUri());
                        startActivity(Intent.createChooser(sharingIntent, "Share via..."));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("ShareImage", e.getLocalizedMessage());
                        Toast.makeText(getContext(), "Failed to share image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        loadLastImage();
    }

    private void loadLastImage(){
        File[] files = getTmpImages();

        if(files.length > 1){
            Picasso.get().load(files[files.length-1]).into(m_imageView);
        }
    }

    private void loadNewImage() {
        m_currentImageUUID = UUID.randomUUID();
        downloadImage("http://inspirobot.me/api?generate=true");
    }

    private void downloadImage(String url) {
        Picasso.get().load(new WebsiteTask().doInBackground("http://inspirobot.me/api?generate=true")).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                m_imageView.setImageBitmap(bitmap);
                storeImage(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(), bitmap);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Toast.makeText(getContext(), "Failed to load new image", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable){}
        });
    }

    private void storeImage(String path, Bitmap bitmap) {
        FileOutputStream fOut = null;
        File file = new File(path ,m_currentImageUUID + ".png");

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
        values.put(MediaStore.Images.Media.TITLE, m_currentImageUUID.toString());
        values.put(MediaStore.Images.Media.DESCRIPTION, "Quote: " + m_currentImageUUID);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis ());
        values.put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().toLowerCase(Locale.US).hashCode());
        values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.getName().toLowerCase(Locale.US));
        values.put("_data", file.getAbsolutePath());

        getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void storeImageToGallery() {
        File file = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString(), getFileName());
        Picasso.get().load(file).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                storeImage(getExternalImageDirectory(), bitmap);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Toast.makeText(getContext(), "Failed to store image", Toast.LENGTH_SHORT);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        });
    }

    private Uri getCurrentImageUri(){
        File file =  new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), m_currentImageUUID + ".png");
        return FileProvider.getUriForFile(getContext(), "com.koller.yannick.inspirobot", file);
    }

    private TextDrawable getFontAwesomeDrawable(int id) {
        TextDrawable drawable = new TextDrawable(getContext());
        drawable.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        drawable.setTextAlign(Layout.Alignment.ALIGN_CENTER);
        drawable.setTypeface(FontAwesomeManager.getTypeface(getContext(), FONTAWESOME_SOLID));
        drawable.setTextColor(Color.WHITE);
        drawable.setText(getResources().getText(id));
        return drawable;
    }

    public void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
        }
    }

    // Returns the URI path to the Bitmap displayed in specified ImageView
    public Uri getLocalBitmapUri(ImageView imageView) {
        // Extract Bitmap from ImageView drawable
        Drawable drawable = imageView.getDrawable();
        Bitmap bmp;
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
            File file =  new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + m_currentImageUUID + "_temp.png");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            bmpUri = FileProvider.getUriForFile(getContext(), "com.koller.yannick.inspirobot", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bmpUri;
    }

    private void clearTempImages(){
        File[] files = getTmpImages();

        if(files.length > MAX_HISTORY_SIZE-1){
            files[0].delete();
        }
    }

    private File[] getTmpImages(){
        File file = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toURI());
        File[] files = file.listFiles();

        if (files != null && files.length > 1) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File object1, File object2) {
                    return (int) ((object1.lastModified() > object2.lastModified()) ? 1 : -1);
                }
            });
        }
        return files;
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

    private String getFileName(){
        return m_currentImageUUID + ".png";
    }

    private String getInternalImageDirectory(){
        return getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
    }

    private String getExternalImageDirectory(){
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/InspiroBot";
    }

    private static class WebsiteTask extends AsyncTask<String, Void, String> {

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
    }
}