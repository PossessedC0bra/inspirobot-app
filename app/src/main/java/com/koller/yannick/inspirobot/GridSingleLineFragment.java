package com.koller.yannick.inspirobot;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.util.ArrayUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class GridSingleLineFragment extends Fragment {

    private View parent_view;
    private InterstitialAd m_interstitialAd;
    private AdView m_bottomBanner;

    private RecyclerView recyclerView;
    private AdapterGridSingleLine mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_grid_single_line, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        parent_view = view;

        initToolbar();
        initComponent(view);
        initAd(view);
    }

    private void initToolbar(){
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("History");
    }

    private void initComponent(View view) {
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(), 2));
        //recyclerView.addItemDecoration(new SpacingItemDecoration(2, Tools.dpToPx(view.getContext(), 3), true));
        recyclerView.setHasFixedSize(true);

        List<File> files = loadImageHistory();

        mAdapter = new AdapterGridSingleLine(view.getContext(), files);
        recyclerView.setAdapter(mAdapter);

        // on item list clicked
        mAdapter.setOnItemClickListener(new AdapterGridSingleLine.OnItemClickListener() {
            @Override
            public void onItemClick(View view, File[] files, int position) {
                displayAd();
                Intent intent = new Intent(getActivity(), ImageDetailActivity.class);
                intent.putExtra("Files", files);
                intent.putExtra("Pos", position);
                getActivity().startActivity(intent);
            }
        });
    }

    private void initAd(View view){
        MobileAds.initialize(getContext(), "ca-app-pub-9042314102946099~3303611577");
        AdRequest m_request = new AdRequest.Builder().build();

        m_bottomBanner = view.findViewById(R.id.history_banner);
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

    private void displayAd() {
        if(Tools.showAd()){
            if (m_interstitialAd.isLoaded()) {
                m_interstitialAd.show();
            } else {
                Log.d("Ad", "The interstitial wasn't loaded yet.");
            }
        }
    }

    private List<File> loadImageHistory(){
        File file = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES).toURI());

        File[] files = file.listFiles();
        if (files != null && files.length > 1) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File object1, File object2) {
                    return (int) ((object1.lastModified() > object2.lastModified()) ? -1 : 1);
                }
            });
        }

        return new ArrayList<File>(Arrays.asList(files));
    }
}
