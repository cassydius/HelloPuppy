package com.beyowi.hellopuppy;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

/**
 * Created by User on 30/03/2015.
 */
public class GlobalApp extends Application {

    //Trackers
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);

            Tracker t = analytics.newTracker(R.xml.analytics);
            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }
}
