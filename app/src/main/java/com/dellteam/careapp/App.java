package com.dellteam.careapp;

import android.app.Application; 

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.parse.Parse; 
import com.parse.ParsePush;

public class App extends Application { 
	
	  public static GoogleAnalytics analytics;
	  public static Tracker tracker;

    @Override public void onCreate() {
        super.onCreate();
        
        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker(Config.ANALYTICS_ID); // Replace with actual tracker/property Id
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);

		Parse.initialize(this, Config.PARSE_APP_ID , Config.PARSE_CLIENT_ID);
        ParsePush.subscribeInBackground("");
    }
} 