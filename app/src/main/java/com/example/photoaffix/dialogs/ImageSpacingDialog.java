package com.example.photoaffix.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Service;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDTintHelper;
import com.example.photoaffix.R;
import com.example.photoaffix.ui.MainActivity;
import com.example.photoaffix.utils.Prefs;
import com.example.photoaffix.views.LineView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by zengzhi on 2017/7/11.
 */

public class ImageSpacingDialog extends DialogFragment {

    @BindView(R.id.spacingHorizontalSeek)
    SeekBar horizontalSeek;
    @BindView(R.id.spacingHorizontalLabel)
    TextView horizontalLabel;
    @BindView(R.id.spacingVerticalSeek)
    SeekBar verticalSeek;
    @BindView(R.id.spacingVerticalLabel)
    TextView verticalLabel;
    @BindView(R.id.verticalLine)
    LineView verticalLine;
    @BindView(R.id.horizontalLine)
    LineView horizontalLine;

    private Unbinder unbinder;

    private SpacingCallback context;

    public ImageSpacingDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        int fillColor = Prefs.bgFillColor(getActivity());
        if (fillColor == Color.TRANSPARENT)

            fillColor = ContextCompat.getColor(getActivity(), R.color.colorAccent);
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.image_spacing)
                .customView(R.layout.dialog_imagespacing, true)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .positiveColor(fillColor)
                .negativeColor(fillColor)
                .widgetColor(fillColor)
                .onPositive((materialDialog, dialogAction) -> notifyActivity())
                .onNegative((materialDialog, dialogAction) -> MainActivity.dismissDialog(materialDialog))
                .build();

        final View v = dialog.getCustomView();
        assert v != null;
        unbinder = ButterKnife.bind(this, v);


        SeekBar.OnSeekBarChangeListener seekListener =
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if (seekBar.getId() == R.id.spacingHorizontalSeek) {

                            horizontalLabel.setText(String.format("%d", progress));
                            horizontalLine.setWidth(progress);
                        } else {

                            verticalLabel.setText(String.format("%d", progress));
                            verticalLine.setWidth(progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                };

        horizontalSeek.setOnSeekBarChangeListener(seekListener);
        verticalSeek.setOnSeekBarChangeListener(seekListener);

        MDTintHelper.setTint(horizontalSeek, fillColor);
        MDTintHelper.setTint(verticalSeek, fillColor);

        horizontalLine.setColor(fillColor);
        verticalLine.setColor(fillColor);

        final int[] spacing = Prefs.imageSpacing(getActivity());

        horizontalSeek.setProgress(spacing[0]);
        verticalSeek.setProgress(spacing[1]);

        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        unbinder = null;
    }



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = (SpacingCallback) activity;
    }

    private void notifyActivity() {

        if (context == null) return;

        context.onSpacingChanged(horizontalSeek.getProgress(), verticalSeek.getProgress());
    }


    public interface SpacingCallback {

        void onSpacingChanged(int horizontal, int vertical);
    }


}
