package com.koller.yannick.inspirobot;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;

public class SaveImageHelper implements Target {
    private Context m_context;
    private WeakReference<ContentResolver> m_contentResolverWeakReference;
    private String m_name;
    private String m_description;

    public SaveImageHelper(Context m_context, ContentResolver m_contentResolver, String m_name, String m_description) {
        this.m_context = m_context;
        this.m_contentResolverWeakReference = new WeakReference<ContentResolver>(m_contentResolver);
        this.m_name = m_name;
        this.m_description = m_description;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from){
        ContentResolver resolver = m_contentResolverWeakReference.get();

        if(resolver != null){
            String imagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/InspiroBot";
            FileOutputStream fOut = null;
            File file = new File(imagePath,m_name+".png");

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
            values.put(MediaStore.Images.Media.TITLE, m_name);
            values.put(MediaStore.Images.Media.DESCRIPTION, m_description);
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis ());
            values.put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().toLowerCase(Locale.US).hashCode());
            values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.getName().toLowerCase(Locale.US));
            values.put("_data", file.getAbsolutePath());

            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }
    }

    @Override
    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
        Log.d("SaveImageHelper", "Failed to load Bitmap");
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }

}
