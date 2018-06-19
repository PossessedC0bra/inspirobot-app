package com.koller.yannick.inspirobot;

import android.content.Context;
import android.graphics.Typeface;

public class FontAwesomeManager {

    public static final String ROOT = "fonts/";
    public static final String FONTAWESOME_REGULAR = ROOT + "fa-regular.ttf";
    public static final String FONTAWESOME_SOLID = ROOT + "solid.ttf";
    public static final String FONTAWESOME_BRANDS = ROOT + "fa-brands.ttf";

    public static Typeface getTypeface(Context context, String font) {
        return Typeface.createFromAsset(context.getAssets(), font);
    }

}