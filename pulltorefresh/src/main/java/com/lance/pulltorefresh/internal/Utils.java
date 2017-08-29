package com.lance.pulltorefresh.internal;

import android.util.Log;

public class Utils {
    private static final String LOG_TAG = "PullToRefresh";

    public static void warnDeprecation(String deprecated, String replacement) {
        Log.w(LOG_TAG, "You're using the deprecated " + deprecated + " attr, please switch over to " + replacement);
    }
}
