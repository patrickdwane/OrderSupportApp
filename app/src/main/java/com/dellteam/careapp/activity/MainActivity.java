package com.dellteam.careapp.activity;

import com.dellteam.careapp.Config;
import com.dellteam.careapp.R;
import com.dellteam.careapp.widget.SwipeableViewPager;
import com.tjeannin.apprate.AppRate;

import com.dellteam.careapp.adapter.NavigationAdapter;
import com.dellteam.careapp.fragment.WebFragment;
import com.dellteam.careapp.widget.SlidingTabLayout;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.MenuInflater;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity{

    public Toolbar mToolbar;

    public View mHeaderView;

    public SlidingTabLayout mSlidingTabLayout;

    public SwipeableViewPager mViewPager;

    private NavigationAdapter mAdapter;

    public String push_url = null;

    WebFragment CurrentAnimatingFragment = null;
    int CurrentAnimation = 0;
    static int NO = 0;
    static int HIDING = 1;
    static int SHOWING = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mHeaderView = (View) findViewById(R.id.header_container);
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.tabs);
        mViewPager = (SwipeableViewPager) findViewById(R.id.pager);

        setSupportActionBar(mToolbar);

        // Checking if push provided extra data
        Intent intent = getIntent();
        try {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String jsonData = extras.getString("com.parse.Data");
                if (null != jsonData) {
                    String pushStore;

                    JSONObject json = new JSONObject(jsonData);
                    pushStore = json.getString("data");

                    Log.v("INFO", pushStore);
                    push_url = pushStore;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mAdapter = new NavigationAdapter(getSupportFragmentManager());

        if (Config.HIDE_ACTIONBAR)
            getSupportActionBar().hide();

        if (getHideTabs())
            mSlidingTabLayout.setVisibility(View.GONE);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mViewPager.getLayoutParams();
        if ((Config.HIDE_ACTIONBAR && getHideTabs()) || ((Config.HIDE_ACTIONBAR || getHideTabs()) && getCollapsingActionBar())){
            lp.topMargin = 0;
        } else if ((Config.HIDE_ACTIONBAR || getHideTabs()) || (!Config.HIDE_ACTIONBAR && !getHideTabs() && getCollapsingActionBar())){
            lp.topMargin = getActionBarHeight(this);
        } else if (!Config.HIDE_ACTIONBAR && !getHideTabs()){
            lp.topMargin = getActionBarHeight(this) * 2;
        }

        mViewPager.setLayoutParams(lp);

        mViewPager.setAdapter(mAdapter);
        mViewPager.setOffscreenPageLimit(2);

        mSlidingTabLayout.setCustomTabView(R.layout.layout_tab_indicator, android.R.id.text1);
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setSelectedIndicatorColors(getResources().getColor(R.color.accent));
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (getCollapsingActionBar()) {
                    showToolbar(getFragment());
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        // admob
        if (!(getResources().getString(R.string.publisher_id).equals(""))) {
            // Look up the AdView as a resource and load a request.
            AdView adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            AdView adView = (AdView) findViewById(R.id.adView);
            adView.setVisibility(View.GONE);
        }

        // application rating
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.rate_title))
                .setMessage(String.format(getString(R.string.rate_message), getString(R.string.app_name)))
                .setPositiveButton(getString(R.string.rate_yes), null)
                .setNegativeButton(getString(R.string.rate_never), null)
                .setNeutralButton(getString(R.string.rate_later), null);

        new AppRate(this)
                .setShowIfAppHasCrashed(false)
                .setMinDaysUntilPrompt(2)
                .setMinLaunchesUntilPrompt(2)
                .setCustomDialog(builder)
                .init();

        // showing the splash screen
        if (Config.SPLASH) {
            findViewById(R.id.imageLoading1).setVisibility(View.VISIBLE);
            //getFragment().browser.setVisibility(View.GONE);
        }
    }

    // using the back button of the device
    @Override
    public void onBackPressed() {
        if ((getFragment().mCustomView == null)
                && getFragment().browser.canGoBack()) {
            getFragment().browser.goBack();
        } else if (getFragment().mCustomView != null
                && getFragment().mCustomViewCallback != null) {
            getFragment().mCustomViewCallback.onCustomViewHidden();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        WebView browser = getFragment().browser;
        if (item.getItemId() == (R.id.next)) {
            browser.goForward();
            return true;
        } else if (item.getItemId() == R.id.previous) {
            browser.goBack();
            return true;
        } else if (item.getItemId() == R.id.share) {
            getFragment().shareURL();
            return true;
        } else if (item.getItemId() == R.id.about) {
            AboutDialog();
            return true;
        } else if (item.getItemId() == R.id.home) {
            browser.loadUrl(getFragment().MAINURL);
            return true;
        } else if (item.getItemId() == R.id.close) {
            finish();
            Toast.makeText(getApplicationContext(),
                    getText(R.string.exit_message), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // showing about dialog
    private void AboutDialog() {
        // setting the dialogs text, and making the links clickable
        final TextView message = new TextView(this);
        // i.e.: R.string.dialog_message =>
        final SpannableString s = new SpannableString(
                this.getText(R.string.dialog_about));
        Linkify.addLinks(s, Linkify.WEB_URLS);
        message.setTextSize(15f);
        message.setPadding(20, 15, 15, 15);
        message.setText(Html.fromHtml(getString(R.string.dialog_about)));
        message.setMovementMethod(LinkMovementMethod.getInstance());

        // creating the actual dialog

        AlertDialog.Builder AlertDialog = new AlertDialog.Builder(this);
        AlertDialog.setTitle(Html.fromHtml(getString(R.string.about)))
                // .setTitle(R.string.about)
                .setCancelable(true)
                        // .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("ok", null).setView(message).create().show();
    }

    public void setTitle(String title) {
        if (mAdapter != null && mAdapter.getCount() == 1)
            getSupportActionBar().setTitle(title);
    }

    public WebFragment getFragment(){
        return (WebFragment) mAdapter.getCurrentFragment();
    }

    public void hideSplash() {
        if (Config.SPLASH) {
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    // hide splash image
                    if (findViewById(R.id.imageLoading1).getVisibility() == View.VISIBLE) {
                        findViewById(R.id.imageLoading1).setVisibility(
                                View.GONE);
                        // show webview
                        //findViewById(R.id.webkit).setVisibility(View.VISIBLE);
                    }
                }
                // set a delay before splashscreen is hidden
            }, 0);
        }
    }

    public void hideToolbar() {
        if (CurrentAnimation != HIDING) {
            CurrentAnimation = HIDING;
            AnimatorSet animSetXY = new AnimatorSet();

            ObjectAnimator animY = ObjectAnimator.ofFloat(getFragment().rl, "y", 0);
            ObjectAnimator animY1 = ObjectAnimator.ofFloat(mHeaderView, "y", -getActionBarHeight(this));
            animSetXY.playTogether(animY, animY1);

            animSetXY.start();
            animSetXY.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    CurrentAnimation = NO;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });

        }
    }

    public void showToolbar(WebFragment fragment) {
        if (CurrentAnimation != SHOWING || fragment != CurrentAnimatingFragment) {
            CurrentAnimation = SHOWING;
            CurrentAnimatingFragment = fragment;
            AnimatorSet animSetXY = new AnimatorSet();
            ObjectAnimator animY = ObjectAnimator.ofFloat(fragment.rl, "y", getActionBarHeight(this));
            ObjectAnimator animY1 = ObjectAnimator.ofFloat(mHeaderView, "y", 0);
            animSetXY.playTogether(animY, animY1);

            animSetXY.start();
            animSetXY.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    CurrentAnimation = NO;
                    CurrentAnimatingFragment = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });

        }
    }

    public static int getActionBarHeight(Context context) {
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[]{R.attr.actionBarSize};

        int indexOfAttrTextSize = 0;

        TypedArray a = context.obtainStyledAttributes(typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();

        return actionBarSize;
    }

    boolean getHideTabs(){
        if (mAdapter.getCount() == 1){
            return true;
        } else {
            return Config.HIDE_TABS;
        }
    }

    public static boolean getCollapsingActionBar(){
        if (Config.COLLAPSING_ACTIONBAR && !Config.HIDE_ACTIONBAR){
            return true;
        } else {
            return false;
        }
    }

}