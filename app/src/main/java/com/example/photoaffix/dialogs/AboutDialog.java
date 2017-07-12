package com.example.photoaffix.dialogs;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.photoaffix.R;

/**
 * Created by zengzhi on 2017/7/11.
 */

public class AboutDialog extends DialogFragment {

    public static void shwo(AppCompatActivity context) {

        AboutDialog dialog = new AboutDialog();

        dialog.show(context.getSupportFragmentManager(), "[ABOUT_DIALOG]");
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Spanned content;

        if (Build.VERSION.SDK_INT >= 24) {

            content = Html.fromHtml(getString(R.string.about_body), Html.FROM_HTML_MODE_LEGACY);
        } else {


            content = Html.fromHtml(getString(R.string.about_body));
        }
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.about)
                .positiveText(R.string.dismiss)
                .content(content)
                .contentLineSpacing(1.6f)
                .build();
    }

}
