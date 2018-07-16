package com.koller.yannick.inspirobot;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;

public class CustomSwipeAdapter extends PagerAdapter {

    private File[] m_files;
    private Context m_ctx;
    private LayoutInflater m_layoutInflater;

    public CustomSwipeAdapter(File[] files, Context ctx) {
        m_files = files;
        m_ctx = ctx;
    }

    @Override
    public int getCount() {
        return m_files.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (ConstraintLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        m_layoutInflater = (LayoutInflater) m_ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = m_layoutInflater.inflate(R.layout.slider_image_layout, container, false);
        ImageView imageView = view.findViewById(R.id.image_detail_image);

        Picasso.get().load(m_files[position]).into(imageView);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ConstraintLayout) object);
    }

}
