package com.example.slime;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS = "slime_prefs";
    private static final String KEY_LANGUAGE = "app_language";
    public static final String LANG_EN = "en";
    public static final String LANG_VI = "vi";

    public static Context applyLocale(Context context) {
        String lang = getSavedLanguage(context);
        return updateLocale(context, lang);
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANG_EN);
    }

    public static void saveLanguage(Context context, String language) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, language)
                .apply();
    }

    public static String toggleLanguage(Context context) {
        String current = getSavedLanguage(context);
        String next = current.equals(LANG_VI) ? LANG_EN : LANG_VI;
        saveLanguage(context, next);
        return next;
    }

    private static Context updateLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }
}
