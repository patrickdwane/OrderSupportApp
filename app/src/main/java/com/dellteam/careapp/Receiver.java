package com.dellteam.careapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

import com.dellteam.careapp.activity.MainActivity;

public class Receiver extends ParsePushBroadcastReceiver {

    @Override
    public void onPushOpen(Context context, Intent intent) {
        Log.e("Push", "Openend");
        Intent i = new Intent(context, MainActivity.class);
        i.putExtras(intent.getExtras());
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}