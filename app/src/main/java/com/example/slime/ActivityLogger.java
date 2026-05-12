package com.example.slime;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ActivityLogger {

    private ActivityLogger() {}

    /**
     * Logs a screen/function access to the local SQLite database.
     * Call this in onCreate() of each Activity.
     *
     * @param context    any Context (Activity, Application, etc.)
     * @param screenName human-readable name of the screen or function being opened
     */
    public static void log(Context context, String screenName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String username = (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? user.getDisplayName()
                : "Guest";

        // managerName: reserved for admin/manager roles; currently unused in this game
        String managerName = null;

        ActivityLogDbHelper.getInstance(context).insert(screenName, username, managerName);
    }
}
