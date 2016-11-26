package com.dellteam.careapp.fragment;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.dellteam.careapp.Config;
import com.dellteam.careapp.GetFileInfo;
import com.dellteam.careapp.R;
import com.dellteam.careapp.activity.MainActivity;
import com.dellteam.careapp.widget.AdvancedWebView;
import com.dellteam.careapp.widget.scrollable.ToolbarWebViewScrollListener;

import java.util.concurrent.ExecutionException;

public class WebFragment extends Fragment implements AdvancedWebView.Listener, SwipeRefreshLayout.OnRefreshListener{

    private Activity mAct;
    public FrameLayout rl;
    public AdvancedWebView browser;
    public View mCustomView;
    public WebChromeClient.CustomViewCallback mCustomViewCallback;
    public SwipeRefreshLayout swipeLayout;
    public ProgressBar progressBar;

    private int mOriginalSystemUiVisibility;
    private int mOriginalOrientation;

    public String MAINURL = null;
    static String URL = "url";
    public int firstLoad = 0;

    public WebFragment() {
        // Required empty public constructor
    }

    public static WebFragment newInstance(String url) {
        WebFragment fragment = new WebFragment();
        Bundle args = new Bundle();
        args.putString(URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && MAINURL == null) {
            MAINURL = getArguments().getString(URL);
            firstLoad = 0;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rl = (FrameLayout) inflater.inflate(R.layout.fragment_observable_web_view, container,
                false);

        progressBar = (ProgressBar) rl.findViewById(R.id.progressbar);
        browser = (AdvancedWebView) rl.findViewById(R.id.scrollable);
        swipeLayout = (SwipeRefreshLayout) rl.findViewById(R.id.swipe_container);

        return rl;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAct = getActivity();

        if (Config.PULL_TO_REFRESH)
            swipeLayout.setOnRefreshListener(this);
        else
            swipeLayout.setEnabled(false);

        browser.setListener(this, this);

        if (MainActivity.getCollapsingActionBar()) {
			//Setting the scroll related listeners
            ((MainActivity) getActivity()).showToolbar(this);

            browser.setOnScrollChangeListener(browser, new ToolbarWebViewScrollListener() {
                @Override
                public void onHide() {
                    ((MainActivity) getActivity()).hideToolbar();
                }

                @Override
                public void onShow() {
                    ((MainActivity) getActivity()).showToolbar(WebFragment.this);
                }
            });

        }

        // set javascript and zoom and some other settings
        browser.requestFocus();
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setBuiltInZoomControls(false);
        browser.getSettings().setAppCacheEnabled(true);
        browser.getSettings().setDatabaseEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        // Below required for geolocation
        browser.setGeolocationEnabled(true);
        // 3RD party plugins (on older devices)
        browser.getSettings().setPluginState(PluginState.ON);

        browser.setWebViewClient(new WebViewClient() {
            // Make sure any url clicked is opened in webview
            @TargetApi(Build.VERSION_CODES.GINGERBREAD)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if ((url.contains("market://")
                        || url.contains("play.google.com")
                        || url.contains("plus.google.com")
                        || url.contains("mailto:") || url.contains("tel:")
                        || url.contains("vid:") || url.contains("geo:")
                        || url.contains("sms:") || url.contains("intent://")) == true) {
                    // Load new URL Don't override URL Link
                    try {
                        view.getContext().startActivity(
                                new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch(ActivityNotFoundException e) {
                        if (url.startsWith("intent://")) {
                            view.getContext().startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("intent://", "http://"))));
                        } else {
                            Toast.makeText(getActivity(), getResources().getString(R.string.no_app_message), Toast.LENGTH_LONG).show();
                        }
                    }

                    return true;
                } else if (url.endsWith(".mp4") || url.endsWith(".avi")
                        || url.endsWith(".flv")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(url), "video/mp4");
                        view.getContext().startActivity(intent);
                    } catch (Exception e) {
                        // error
                    }

                    return true;
                } else if (url.endsWith(".mp3") || url.endsWith(".wav")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(url), "audio/mp3");
                        view.getContext().startActivity(intent);
                    } catch (Exception e) {
                        // error
                    }

                    return true;
                }

                // Return true to override url loading (In this case do
                // nothing).
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                try {
                    ((MainActivity) ((Activity) getActivity())).hideSplash();
                } catch (Exception e){
                    e.printStackTrace();
                }


            }

            // handeling errors
            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {

                if (hasConnectivity("", false)) {
                    Builder builder = new Builder(
                            getActivity());
                    builder.setMessage(description)
                            .setPositiveButton(getText(R.string.ok), null)
                            .setTitle("Whoops");
                    builder.show();
                } else {
                    if (!failingUrl.startsWith("file:///android_asset")) {
                        browser.loadUrl("");
                        hasConnectivity("", true);
                    }
                }
            }

        });

        // load url (if connection available
        if (hasConnectivity(MAINURL, true)) {
            String pushurl = ((MainActivity) getActivity()).push_url;
            if (pushurl != null){
                browser.loadUrl(pushurl);
                ((MainActivity) getActivity()).push_url = null;
            } else {
                browser.loadUrl(MAINURL);
            }

        }

        // has all to do with progress bar
        browser.setWebChromeClient(webChromeClient);
    }

    WebChromeClient webChromeClient = new WebChromeClient() {

        @Override
        public void onProgressChanged(WebView view, int progress) {
            if (Config.LOAD_AS_PULL && swipeLayout != null){
                swipeLayout.setRefreshing(true);
                if (progress == 100)
                    swipeLayout.setRefreshing(false);
            } else {
                progressBar.setProgress(0);

                progressBar.setVisibility(View.VISIBLE);

                progressBar.setProgress(progress);

                progressBar.incrementProgressBy(progress);

                if (progress > 99) {
                    progressBar.setVisibility(View.GONE);

                    if (swipeLayout != null && swipeLayout.isRefreshing()) {
                        swipeLayout.setRefreshing(false);
                    }
                }
            }
        }


        // Setting the title
        @Override
        public void onReceivedTitle(WebView view, String title) {

            ((MainActivity) ((Activity) getActivity())).setTitle(browser.getTitle());
        }

        @SuppressWarnings("unused")
        @Override
        public Bitmap getDefaultVideoPoster() {
            if (getActivity() == null) {
                return null;
            }

            return BitmapFactory.decodeResource(getActivity()
                            .getApplicationContext().getResources(),
                    R.drawable.vert_loading);
        }

        @SuppressLint("InlinedApi")
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onShowCustomView(View view,
                                     WebChromeClient.CustomViewCallback callback) {
            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                onHideCustomView();
                return;
            }

            // 1. Stash the current state
            mCustomView = view;
            mCustomView.setBackgroundColor(Color.BLACK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mOriginalSystemUiVisibility = getActivity().getWindow()
                        .getDecorView().getSystemUiVisibility();
            }
            mOriginalOrientation = getActivity().getRequestedOrientation();

            // 2. Stash the custom view callback
            mCustomViewCallback = callback;

            // 3. Add the custom view to the view hierarchy
            FrameLayout decor = (FrameLayout) getActivity().getWindow()
                    .getDecorView();
            decor.addView(mCustomView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            // 4. Change the state of the window
            getActivity()
                    .getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE);
            getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onHideCustomView() {
            // 1. Remove the custom view
            FrameLayout decor = (FrameLayout) getActivity().getWindow()
                    .getDecorView();
            decor.removeView(mCustomView);
            mCustomView = null;

            // 2. Restore the state to it's original form
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                getActivity().getWindow().getDecorView()
                        .setSystemUiVisibility(mOriginalSystemUiVisibility);
            }
            getActivity().setRequestedOrientation(mOriginalOrientation);

            // 3. Call the custom view callback
            mCustomViewCallback.onCustomViewHidden();
            mCustomViewCallback = null;

        }

    };

    @Override public void onRefresh() {
        browser.reload();
    }

    @SuppressLint("NewApi")
    @Override
    public void onPause() {
        super.onPause();
        browser.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        browser.onDestroy();
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        super.onResume();
        browser.onResume();
    }

    @SuppressLint("NewApi")
    @Override
    public void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        //todo Auto generated method stub

        String filename = null;
        try {
            filename = new GetFileInfo().execute(url).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (filename == null) {
            String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(url);
            filename = URLUtil.guessFileName(url, null, fileExtenstion);
        }

        if (AdvancedWebView.handleDownload(getActivity(), url, filename)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.download_done), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getActivity(), getResources().getString(R.string.download_fail), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        if (firstLoad == 0 && MainActivity.getCollapsingActionBar()){
            ((MainActivity) getActivity()).showToolbar(this);
            firstLoad = 1;
        } else if (firstLoad == 0){
            firstLoad = 1;
        }
    }

    @Override
    public void onPageFinished(String url) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onExternalPageRequest(String url) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        browser.onActivityResult(requestCode, resultCode, data);
    }

    // Checking for an internet connection
    private boolean hasConnectivity(String loadUrl, boolean showDialog) {
        boolean enabled = true;

        if (loadUrl.startsWith("file:///android_asset")){
            return true;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        if ((info == null || !info.isConnected() || !info.isAvailable())) {

            enabled = false;

            if (showDialog){
                Builder builder = new Builder(getActivity());
                builder.setMessage(getString(R.string.noconnection));
                builder.setCancelable(false);
                builder.setNeutralButton(R.string.ok, null);
                builder.setTitle(getString(R.string.error));
                builder.create().show();
            }
        }
        return enabled;
    }

    // sharing
    public void shareURL() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String appname = getString(R.string.app_name);
        // This will put the share text:
        // "I came across "BrowserTitle" using "appname"
        shareIntent
                .putExtra(
                        Intent.EXTRA_TEXT,
                        (getText(R.string.share1)
                                + " "
                                + browser.getTitle()
                                + " "
                                + getText(R.string.share2)
                                + " "
                                + appname
                                + " https://play.google.com/store/apps/details?id=" + getActivity()
                                .getPackageName()));
        startActivity(Intent.createChooser(shareIntent,
                getText(R.string.sharetitle)));
    }
}
