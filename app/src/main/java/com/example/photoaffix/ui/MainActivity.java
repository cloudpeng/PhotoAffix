package com.example.photoaffix.ui;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerView;
import com.afollestad.inquiry.Inquiry;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.example.photoaffix.R;
import com.example.photoaffix.adapters.PhotoGridAdapter;
import com.example.photoaffix.animation.HeightEvaluator;
import com.example.photoaffix.animation.ViewHideAnimationListener;
import com.example.photoaffix.data.Photo;
import com.example.photoaffix.dialogs.ImageSizingDialog;
import com.example.photoaffix.dialogs.ImageSpacingDialog;
import com.example.photoaffix.utils.Prefs;
import com.example.photoaffix.views.ColorCircleView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;

public class MainActivity extends AppCompatActivity implements
        ColorChooserDialog.ColorCallback,
        ImageSpacingDialog.SpacingCallback{

    private static final int PERMISSION_RC = 69;
    private static final int BROWSE_RC = 21;
    private static final String TAG = "MainActivity";

    @BindView(R.id.list)
    public DragSelectRecyclerView list;

    @BindView(R.id.toolbar)
    public Toolbar toolbar;

    @BindView(R.id.affixButton)
    public Button affixButton;

    @BindView(R.id.empty)
    public TextView empty;

    @BindView(R.id.stackHorizonllySwitch)
    public CheckBox stackHorizontallyCheck;

    @BindView(R.id.stackHorizontallyLabel)
    TextView stackHorizontallyLabel;

    @BindView(R.id.bgFillColorCircle)
    public ColorCircleView bgFillColorCircle;


    @BindView(R.id.bgFillColorLabel)
    TextView bgFillColorLabel;

    @BindView(R.id.removeBgButton)
    public Button removeBgFillBtn;

    @BindView(R.id.imagePaddingLabel)
    public TextView imagePaddingLabel;

    @BindView(R.id.scalePrioritySwitch)
    public CheckBox scalePrioritySwitch;

    @BindView(R.id.scalePriorityLabel)
    public TextView scalePriorityLabel;

    @BindView(R.id.settingsFrame)
    ViewGroup settingsFrame;


    private PhotoGridAdapter adapter;

    private Photo[] selectedPhotos;

    private int traverseIndex;

    private boolean autoSelectFirst;

    private int originalSettingsFrameHeight = -1;
    private ValueAnimator settingsFrameAnimator;

    private Unbinder unbinder;


    // Avoids a rare crash
    public static void dismissDialog(@Nullable Dialog dialog) {
        if (dialog == null) return;
        try {
            dialog.dismiss();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        unbinder = ButterKnife.bind(this);

        //获取Inquiry单例
        Inquiry.newInstance(this, null).build();

        list.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.grid_width)));

        adapter = new PhotoGridAdapter(this);
        list.setAdapter(adapter);
        list.setItemAnimator(new DefaultItemAnimator());

        /**
         *
         * 设置视图
         */

        //设置横竖屏
        final boolean stackHorizontally = Prefs.stackHorizontally(this);
        stackHorizontallyCheck.setChecked(stackHorizontally);
        stackHorizontallyLabel.setText(stackHorizontally ? R.string.stack_horizontally : R.string.stack_vertically);
        //设置背景填充颜色
        final int bgFillColor = Prefs.bgFillColor(this);
        bgFillColorCircle.setColor(bgFillColor);

        //设置spacing
        final int[] padding = Prefs.imageSpacing(this);
        imagePaddingLabel.setText(getString(R.string.image_spacing_x, padding[0], padding[1]));


        if (bgFillColor != Color.TRANSPARENT) {

            removeBgFillBtn.setVisibility(View.GONE);
            bgFillColorLabel.setText(R.string.background_fill_color);
        } else {
            bgFillColorLabel.setText(R.string.background_fill_color_transparen);
        }
        //设置scale



    }
    //获取图库的照片，给adapter显示
    //noinspection VisibleForTests

    private void refresh() {
        int permission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_RC);
            return;
        }

        //noinspection VisibleForTests
        Inquiry.get(this)
                .selectFrom(Uri.parse("content://media/external/images/media"), Photo.class)
                .sort("datetaken DESC")
                .where("_data IS NOT NULL")
                .all(
                        photos -> {
                            if (isFinishing()) {
                                return;
                            }
                            if (empty != null) {
                                adapter.setPhotos(photos);
                                empty.setVisibility(
                                        photos == null || photos.length == 0 ? View.VISIBLE : View.GONE);
                                if (photos != null && photos.length > 0 && autoSelectFirst) {
                                    adapter.shiftSelections();
                                    adapter.setSelected(1, true);
                                    autoSelectFirst = false;
                                }
                            }
                        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_RC) {
            //加载数据
            refresh();
        }
    }


    /**
     * 打开recyclerview的item 0时，启动活动，选择更多照片
     */
    public void browseExternalPhotos() {
        Intent intent =
                new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("image/*");
        startActivityForResult(intent, BROWSE_RC);
    }


    @Override
    protected void onResume() {
        super.onResume();

        //重新加载数据
        refresh();
    }

    @OnClick(R.id.removeBgButton)
    public void onClickRemoveBgFill() {
        removeBgFillBtn.setVisibility(View.GONE);
        //noinspection ConstantConditions
        onColorSelection(null, Color.TRANSPARENT);
    }

    @OnClick(R.id.expandButton)
    public void onClickExpandButton(ImageView button) {

        if (originalSettingsFrameHeight == -1) {

            //获取每一个设置的高度
            final int settingControlHeight = (int) getResources().getDimension(R.dimen.settings_control_height);
            //高度*4 = 整个view的高度
            originalSettingsFrameHeight = settingControlHeight * settingsFrame.getChildCount();
        }

        if (settingsFrameAnimator != null) settingsFrameAnimator.cancel();

        //如果view不可见
        if (settingsFrame.getVisibility() == View.GONE) {
            //设为可见
            settingsFrame.setVisibility(View.VISIBLE);
            button.setImageResource(R.drawable.ic_collapse);

            //动画
            settingsFrameAnimator =
                    ValueAnimator.ofObject(
                            new HeightEvaluator(settingsFrame), 0, originalSettingsFrameHeight);
        } else {
            Log.i(TAG, "nihao");
            button.setImageResource(R.drawable.ic_expand);
            settingsFrameAnimator =
                    ValueAnimator.ofObject(
                            new HeightEvaluator(settingsFrame), originalSettingsFrameHeight, 0);

            //监听，动画结束的时候，settingsFrame设置为GONE
            settingsFrameAnimator.addListener(new ViewHideAnimationListener(settingsFrame));
        }
        settingsFrameAnimator.setInterpolator(new FastOutSlowInInterpolator());
        settingsFrameAnimator.setDuration(200);
        settingsFrameAnimator.start();
    }

    @OnClick({
            R.id.settingStackHorizontally,
            R.id.settingBgFillColor,
            R.id.settingImagePadding,
            R.id.settingScalePriority
    })

    public void onClickSetting(View view) {
        switch (view.getId()) {
            case R.id.settingStackHorizontally:
                stackHorizontallyCheck.setChecked(!stackHorizontallyCheck.isChecked());
                stackHorizontallyLabel.setText(
                        stackHorizontallyCheck.isChecked()
                                ? R.string.stack_horizontally
                                : R.string.stack_vertically);
                Prefs.stackHorizontally(this, stackHorizontallyCheck.isChecked());
                break;

            case R.id.settingBgFillColor:
                new ColorChooserDialog.Builder(this, R.string.background_fill_color_title)
                        .backButton(R.string.back)
                        .doneButton(R.string.done)
                        .allowUserColorInputAlpha(false)
                        .preselect(Prefs.bgFillColor(this))
                        .show();
                break;

            case R.id.settingImagePadding:

                new ImageSpacingDialog().show(getFragmentManager(), "[IMAGE_PADDING_DIALOG]");
                break;

            case R.id.settingScalePriority:

                break;
        }
    }


    /**
     *
     * 对话框选择颜色，
     * @param dialog
     * @param selectedColor
     */
    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {

        if (selectedColor != Color.TRANSPARENT) {

            removeBgFillBtn.setVisibility(View.VISIBLE);
            bgFillColorLabel.setText(R.string.background_fill_color);
        } else {
            bgFillColorLabel.setText(R.string.background_fill_color_transparen);
        }

        Prefs.bgFillColor(this, selectedColor);
        bgFillColorCircle.setColor(selectedColor);


    }

    @Override
    public void onColorChooserDismissed(@NonNull ColorChooserDialog dialog) {

    }

    @Override
    public void onSpacingChanged(int horizontal, int vertical) {

        Prefs.imageSpacing(this, horizontal, vertical);

        imagePaddingLabel.setText(getString(R.string.image_spacing_x, horizontal, vertical));

    }
}
