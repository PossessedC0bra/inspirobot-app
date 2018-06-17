package com.koller.yannick.inspirobot;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.lang.ref.WeakReference;

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
            MediaStore.Images.Media.insertImage(resolver, bitmap, m_name, m_description);
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
