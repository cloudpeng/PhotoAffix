package com.example.photoaffix.utils;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Looper;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.photoaffix.R;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by zengzhi on 2017/7/12.
 */

public class Util {

    public static void showMemoryError(Activity context) {
        Util.showError(
                context,
                new Exception(
                        "You've run out of RAM for processing images; I'm working to improve memory usage! Sit tight while this app is in beta."));
    }


    public static void showError(final Activity context, final Exception e) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            context.runOnUiThread(() -> showError(context, e));
            return;
        }
        new MaterialDialog.Builder(context)
                .title(R.string.error)
                .content(e.getMessage())
                .positiveText(android.R.string.ok)
                .show();
    }


    public static InputStream openStream(Context context, Uri uri) throws FileNotFoundException {
        if (uri == null) return null;
        if (uri.getScheme() == null || uri.getScheme().equalsIgnoreCase("file")) {
            return new FileInputStream(uri.getPath());
        } else {
            return context.getContentResolver().openInputStream(uri);
        }
    }


    public static void closeQuietely(Closeable c) {
        try {
            c.close();
        } catch (Throwable ignored) {
        }
    }
}
