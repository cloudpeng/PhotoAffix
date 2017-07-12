package com.example.photoaffix.ui;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Looper;
import android.os.PersistableBundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerView;
import com.afollestad.dragselectrecyclerview.DragSelectRecyclerViewAdapter;
import com.afollestad.inquiry.Inquiry;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.example.photoaffix.R;
import com.example.photoaffix.adapters.PhotoGridAdapter;
import com.example.photoaffix.animation.HeightEvaluator;
import com.example.photoaffix.animation.ViewHideAnimationListener;
import com.example.photoaffix.data.Photo;
import com.example.photoaffix.dialogs.AboutDialog;
import com.example.photoaffix.dialogs.ImageSizingDialog;
import com.example.photoaffix.dialogs.ImageSpacingDialog;
import com.example.photoaffix.utils.Prefs;
import com.example.photoaffix.utils.Util;
import com.example.photoaffix.views.ColorCircleView;

import java.io.InputStream;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.R.attr.process;
import static android.R.attr.slideEdge;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;

public class MainActivity extends AppCompatActivity implements
        ColorChooserDialog.ColorCallback,
        ImageSpacingDialog.SpacingCallback,
        DragSelectRecyclerViewAdapter.SelectionListener {

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

        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.clear:

                        clearSelection();
                        return true;

                    case R.id.about:
                        AboutDialog.shwo(MainActivity.this);

                        return true;

                    default:
                        return false;
                }

            }
        });

        list.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.grid_width)));

        adapter = new PhotoGridAdapter(this);
        adapter.restoreInstanceState(savedInstanceState);

        //设置监听
        adapter.setSelectionListener(this);
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

        //设置scale
        final boolean scalePriority = Prefs.scalePriority(this);
        scalePrioritySwitch.setChecked(scalePriority);
        scalePriorityLabel.setText(scalePriority ? R.string.scale_priority_on : R.string.scale_priority_off);
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


        processIntent(getIntent());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent != null && Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {

            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null && uris.size() > 0) {

                selectedPhotos = new Photo[uris.size()];
                for (int i = 0; i < uris.size(); i++) selectedPhotos[i] = new Photo(uris.get(i));

                beginProcessing();

            } else {

                Toast.makeText(this, R.string.need_two_or_more, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbinder.unbind();
        unbinder = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        if (adapter != null) adapter.saveInstanceState(outState);
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

    @Override
    protected void onPause() {
        super.onPause();

        //判断活动是否被销毁
        if (isFinishing()) Inquiry.destroy(this);
    }


    /**
     * 清楚已选择的照片
     */
    public void clearSelection() {

        if (Looper.myLooper() != Looper.getMainLooper()) {

            runOnUiThread(this::clearSelection);
            return;
        }
        selectedPhotos = null;
        adapter.clearSelected();
        toolbar.getMenu().findItem(R.id.clear).setVisible(false);
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


    private void beginProcessing() {

        affixButton.setEnabled(false);
        try {
            startProcessing();
        } catch (OutOfMemoryError e) {
            Util.showMemoryError(MainActivity.this);
        }

        affixButton.setEnabled(true);
    }

    private void startProcessing() {

        Util.lockOrientation(this);
        //获取要设置的宽高
        final int[] imageSpacing = Prefs.imageSpacing(MainActivity.this);
        final int SPACING_HORIZONTAL = imageSpacing[0];
        final int SPACING_VERTICAL = imageSpacing[1];
        //水平还是垂直
        final boolean horizontal = stackHorizontallyCheck.isChecked();

        int resultWidth;
        int resultHeight;

        if (horizontal) {

            int maxHeight = -1;
            int minHeight = -1;
            traverseIndex = -1;

            int[] size;
            //还有下一张图片
            while ((size = getNextBitmapSize()) != null) {
                if (size[0] == 0 && size[1] == 0) return;

                if (maxHeight == -1 ) maxHeight = size[1];
                else if (size[1] > maxHeight) maxHeight = size[1];
                if (minHeight == -1) minHeight = size[1];
                else if (size[1] > minHeight) minHeight = size[1];
            }
        }


        Toast.makeText(this, "hahahaha", Toast.LENGTH_SHORT).show();
    }

    //点击处理选择的图片
    @OnClick(R.id.affixButton)
    public void onClickAffixButton(View v) {

        //获取选择的图片的数组
        selectedPhotos = adapter.getSelectedPhotos();

        Toast.makeText(this, "" + selectedPhotos.length, Toast.LENGTH_SHORT).show();

        beginProcessing();


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

                scalePrioritySwitch.setChecked(!scalePrioritySwitch.isChecked());

                scalePriorityLabel.setText(scalePrioritySwitch.isChecked() ? R.string.scale_priority_on : R.string.scale_priority_off);

                Prefs.scalePriority(this, scalePrioritySwitch.isChecked());

                break;
        }
    }


    /**
     * 对话框选择颜色，
     *
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

    /**
     * @return 返回照片的宽高
     */
    @Size(2)
    private int[] getNextBitmapSize() {

        if (selectedPhotos == null || selectedPhotos.length == 0) {

            selectedPhotos = adapter.getSelectedPhotos();

            if (selectedPhotos == null || selectedPhotos.length == 0)

                return new int[]{10, 10};
        }
        traverseIndex++;

        if (traverseIndex > selectedPhotos.length - 1) return null;

        Photo nextPhoto = selectedPhotos[traverseIndex];
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        InputStream is = null;

        try {
            is = Util.openStream(this, nextPhoto.getUri());

            BitmapFactory.decodeStream(is, null, options);

        } catch (Exception e) {
            Util.showError(this, e);
            return new int[]{0, 0};
        } finally {
            Util.closeQuietely(is);
        }

        return new int[]{options.outWidth, options.outHeight};
    }


    @Override
    public void onBackPressed() {
        if (adapter.getSelectedCount() > 0) {
            clearSelection();
        } else super.onBackPressed();
    }

    @Override
    public void onDragSelectionChanged(int count) {

        Log.i(TAG, String.valueOf(count));
        Toast.makeText(this, "count=" + count, Toast.LENGTH_SHORT).show();
        affixButton.setText(getString(R.string.affix_x, count));
        affixButton.setEnabled(count > 0);
        toolbar
                .getMenu()
                .findItem(R.id.clear)
                .setVisible(adapter != null && adapter.getSelectedCount() > 0);
    }
}
