package com.koller.yannick.inspirobot;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextSwitcher;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.hanks.htextview.typer.TyperTextView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

public class MindfulnessFragment extends Fragment {

    private InterstitialAd m_interstitialAd;
    private AdView m_bottomBanner;

    private ImageView m_imageView;
    private TyperTextView m_typeTextView;

    private String m_sessionUid;
    private JSONObject m_data;
    private MediaPlayer m_mediaPlayer = new MediaPlayer();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_mindfulness, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initToolbar();
        initComponent(view);
        initAd(view);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        m_mediaPlayer.release();
    }

    private void initToolbar() {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("InspiroBot Mindfulness Mode");
    }

    private void initComponent(View view) {
        m_imageView = view.findViewById(R.id.mindfulness_image);
        m_typeTextView = view.findViewById(R.id.mindfulness_text);
        initMediaPlayer();
        getNewSessionUid();
    }

    private void initAd(View view) {
        MobileAds.initialize(getContext(), "ca-app-pub-9042314102946099~3303611577");
        AdRequest m_request = new AdRequest.Builder().build();

        m_bottomBanner = view.findViewById(R.id.mindfulness_banner);
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
    }

    private void initMediaPlayer() {
        m_mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                displayAd();
                displayAd();
                m_mediaPlayer.reset();
                getNewData();
            }
        });
    }

    private void getNewSessionUid() {
        String[] urls = new String[]{"http://inspirobot.me/api?getSessionID=1"};

        new WebsiteTask(new AsyncResponse() {
            @Override
            public void processFinish(String output) {
                m_sessionUid = output;
                getNewData();
            }
        }).execute(urls);
    }

    private void getNewData() {
        String[] urls = new String[]{"http://inspirobot.me/api?generateFlow=1&sessionID=" + m_sessionUid};

        new WebsiteTask(new AsyncResponse() {
            @Override
            public void processFinish(String output) {
                try {
                    m_data = new JSONObject(output);
                    startMindfulness();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }).execute(urls);
    }

    private void loadImageFromUnsplash(String image) {
        Picasso.get().load("https://source.unsplash.com/" + image).into(m_imageView);
    }

    private void startMindfulness() {
        try {
            m_mediaPlayer.setDataSource(m_data.getString("mp3"));
            m_mediaPlayer.prepare();
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        m_mediaPlayer.start();
        generateHandlers();
    }

    private void generateHandlers() {
        try {
            JSONArray array = m_data.getJSONArray("data");
            loadImageFromUnsplash(array.getJSONObject(0).getString("image"));
            for (int i = 1; i < array.length(); i++) {
                final JSONObject currentObject = array.getJSONObject(i);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (currentObject.getString("type").equals("quote")) {
                                m_typeTextView.animateText(currentObject.getString("text"));
                            } else if (currentObject.getString("type").equals("transition")) {
                                loadImageFromUnsplash(currentObject.getString("image"));
                            }
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        }
                    }
                }, currentObject.getInt("time") * 1000);

                if (currentObject.getString("type").equals("quote")) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            m_typeTextView.animateText("");
                        }
                    }, currentObject.getInt("time") * 1000 + currentObject.getLong("duration") * 2000);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private void displayAd() {
        if (Tools.showAd()) {
            if (m_interstitialAd.isLoaded()) {
                m_interstitialAd.show();
            } else {
                Log.d("Ad", "The interstitial wasn't loaded yet.");
            }
        }
    }
}

