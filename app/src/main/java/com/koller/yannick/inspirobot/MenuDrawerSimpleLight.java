package com.koller.yannick.inspirobot;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MenuDrawerSimpleLight extends AppCompatActivity {

    private ActionBar m_actionBar;
    private Toolbar m_toolbar;

    private int m_selectedNavigationItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_drawer_simple_light);

        initToolbar();
        initNavigationMenu();
    }

    private void initToolbar() {
        m_toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(m_toolbar);
        m_actionBar = getSupportActionBar();
        m_actionBar.setDisplayHomeAsUpEnabled(true);
        m_actionBar.setTitle(R.string.app_name);
    }

    private void initNavigationMenu() {
        final NavigationView nav_view = (NavigationView) findViewById(R.id.nav_view);
        if(nav_view.getFitsSystemWindows()){
            nav_view.setFitsSystemWindows(false);
            nav_view.requestApplyInsets();
        }
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, m_toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        nav_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(final MenuItem item) {
                Fragment fragment = null;
                int id = item.getItemId();

                if(m_selectedNavigationItem != id) {
                    switch (id) {
                        case R.id.nav_home:
                            fragment = new MainFragment();
                            break;
                        case R.id.nav_mindfulness:
                            fragment = new MindfulnessFragment();
                            break;
                         case R.id.nav_history:
                             fragment = new GridSingleLineFragment();
                             break;
                    }

                    if (fragment != null) {
                        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.frameLayout, fragment);
                        fragmentTransaction.commit();
                    }

                    m_selectedNavigationItem = id;
                }

                drawer.closeDrawers();
                return true;
            }
        });

        nav_view.setCheckedItem(R.id.nav_home);
        m_selectedNavigationItem = R.id.nav_home;
        initFrameLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    private void initFrameLayout(){
        Fragment fragment = new MainFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commit();
    }
}
