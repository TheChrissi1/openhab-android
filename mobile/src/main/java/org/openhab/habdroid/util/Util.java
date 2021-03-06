/*
 * Copyright (c) 2010-2016, openHAB.org and others.
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.habdroid.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.util.Log;
import android.util.TypedValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openhab.habdroid.R;
import org.openhab.habdroid.model.OpenHABItem;
import org.openhab.habdroid.model.OpenHABSitemap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Headers;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

public class Util {

    private final static String TAG = Util.class.getSimpleName();

    public static void overridePendingTransition(Activity activity, boolean reverse) {
        if (!PreferenceManager.getDefaultSharedPreferences(activity).getString(Constants.PREFERENCE_ANIMATION, "android").equals("android")) {
            if (PreferenceManager.getDefaultSharedPreferences(activity).getString(Constants.PREFERENCE_ANIMATION, "android").equals("ios")) {
                if (reverse) {
                    activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                } else {
                    activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            } else {
                activity.overridePendingTransition(0, 0);
            }
        }
    }

    public static String normalizeUrl(String sourceUrl) {
        String normalizedUrl = "";
        try {
            URL url = new URL(sourceUrl);
            normalizedUrl = url.toString();
            normalizedUrl = normalizedUrl.replace("\n", "");
            normalizedUrl = normalizedUrl.replace(" ", "");
            if (!normalizedUrl.endsWith("/"))
                normalizedUrl = normalizedUrl + "/";
        } catch (MalformedURLException e) {
            Log.e(TAG, "normalizeUrl: invalid URL");
        }
        return normalizedUrl;
    }

    public static String removeProtocolFromUrl(String url) {
        Uri uri = Uri.parse(url);
        return uri.getHost();
    }

    public static List<OpenHABSitemap> parseSitemapList(Document document) {
        List<OpenHABSitemap> sitemapList = new ArrayList<OpenHABSitemap>();
        NodeList sitemapNodes = document.getElementsByTagName("sitemap");
        if (sitemapNodes.getLength() > 0) {
            for (int i = 0; i < sitemapNodes.getLength(); i++) {
                sitemapList.add(OpenHABSitemap.fromXml(sitemapNodes.item(i)));
            }
        }
        return sitemapList;
    }

    public static List<OpenHABSitemap> parseSitemapList(JSONArray jsonArray) {
        List<OpenHABSitemap> sitemapList = new ArrayList<OpenHABSitemap>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject sitemapJson = jsonArray.getJSONObject(i);
                OpenHABSitemap openHABSitemap = OpenHABSitemap.fromJson(sitemapJson);
                if (!(openHABSitemap.name().equals("_default") && jsonArray.length() != 1)) {
                    sitemapList.add(openHABSitemap);
                }
            } catch (JSONException e) {
                Log.d(TAG, "Error while parsing sitemap", e);
            }
        }
        return sitemapList;
    }

    public static List<OpenHABSitemap> sortSitemapList(List<OpenHABSitemap> sitemapList, String defaultSitemapName) {
        // Sort by sitename label, the default sitemap should be the first one
        Collections.sort(sitemapList, new Comparator<OpenHABSitemap>() {
            @Override
            public int compare(OpenHABSitemap sitemap1, OpenHABSitemap sitemap2) {
                if (sitemap1.name().equals(defaultSitemapName)) {
                    return -1;
                }
                if (sitemap2.name().equals(defaultSitemapName)) {
                    return 1;
                }
                return sitemap1.label().compareToIgnoreCase(sitemap2.label());
            }
        });

        return sitemapList;
    }

    public static boolean sitemapExists(List<OpenHABSitemap> sitemapList, String sitemapName) {
        for (int i = 0; i < sitemapList.size(); i++) {
            if (sitemapList.get(i).name().equals(sitemapName))
                return true;
        }
        return false;
    }

    public static OpenHABSitemap getSitemapByName(List<OpenHABSitemap> sitemapList, String sitemapName) {
        for (int i = 0; i < sitemapList.size(); i++) {
            if (sitemapList.get(i).name().equals(sitemapName))
                return sitemapList.get(i);
        }
        return null;
    }

    public static void setActivityTheme(@NonNull final Activity activity) {
        setActivityTheme(activity, null);
    }

    public static void setActivityTheme(@NonNull final Activity activity, String theme) {
        activity.setTheme(getActivityThemeID(activity, theme));

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            TypedValue typedValue = new TypedValue();
            activity.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
            activity.setTaskDescription(new ActivityManager.TaskDescription(
                    activity.getString(R.string.app_name),
                    BitmapFactory.decodeResource(activity.getResources(), R.mipmap.icon_round),
                    typedValue.data));
        }
    }

    public static @StyleRes int getActivityThemeID(@NonNull final Activity activity) {
        return getActivityThemeID(activity, null);
    }

    public static @StyleRes int getActivityThemeID(@NonNull final Activity activity, String theme) {
        if (theme == null) {
            theme = PreferenceManager.getDefaultSharedPreferences(activity).getString(
                    Constants.PREFERENCE_THEME, activity.getString(R.string.theme_value_light));
        }

        if (theme.equals(activity.getString(R.string.theme_value_dark))) {
            return R.style.HABDroid_Dark;
        }
        if (theme.equals(activity.getString(R.string.theme_value_black))) {
            return R.style.HABDroid_Black;
        }
        if (theme.equals(activity.getString(R.string.theme_value_basic_ui))) {
            return R.style.HABDroid_Basic_ui;
        }
        if (theme.equals(activity.getString(R.string.theme_value_basic_ui_dark))) {
            return R.style.HABDroid_Basic_ui_dark;
        }

        return R.style.HABDroid_Light;
    }

    public static boolean exceptionHasCause(Throwable error, Class<? extends Throwable> cause) {
        while (error != null) {
            if (error.getClass().equals(cause)) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }

    public static void sendItemCommand(MyAsyncHttpClient client, OpenHABItem item, String command) {
        if (item == null) {
            return;
        }
        sendItemCommand(client, item.link(), command);
    }

    public static void sendItemCommand(MyAsyncHttpClient client, String itemUrl, String command) {
        if (itemUrl == null || command == null) {
            return;
        }
        client.post(itemUrl, command, "text/plain;charset=UTF-8", new MyHttpClient.TextResponseHandler() {
            @Override
            public void onFailure(Call call, int statusCode, Headers headers, String responseString, Throwable error) {
                Log.e(TAG, "Got command error " + error.getMessage());
                if (responseString != null)
                    Log.e(TAG, "Error response = " + responseString);
            }

            @Override
            public void onSuccess(Call call, int statusCode, Headers headers, String responseString) {
                Log.d(TAG, "Command was sent successfully");
            }
        });
    }

    /**
     * Replaces everything after the first 3 chars with asterisks
     * @param string to obfuscate
     * @return obfuscated string
     */
    public static String obfuscateString(String string) {
        return obfuscateString(string, 3);
    }

    /**
     * Replaces everything after the first clearTextCharCount chars with asterisks
     * @param string to obfuscate
     * @param clearTextCharCount leave the first clearTextCharCount in clear text
     * @return obfuscated string
     */
    public static String obfuscateString(String string, int clearTextCharCount) {
        clearTextCharCount = Math.min(string.length(), clearTextCharCount);
        return string.substring(0, clearTextCharCount) +
                string.substring(clearTextCharCount).replaceAll(".", "*");
    }
}
