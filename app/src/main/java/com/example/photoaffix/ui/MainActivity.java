package com.example.photoaffix.ui;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerView;
import com.afollestad.inquiry.Inquiry;
import com.example.photoaffix.R;
import com.example.photoaffix.adapters.PhotoGridAdapter;
import com.example.photoaffix.data.Photo;
import com.example.photoaffix.views.ColorCircleView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_RC = 69;
    private static final int BROWSE_RC = 21;

    @BindView(R.id.list)
    public DragSelectRecyclerView list;

    @BindView(R.id.toolbar)
    public Toolbar toolbar;

    @BindView(R.id.affixButton)
    public Button affixButton;

    @BindView(R.id.expandButton)
    public ImageView expandButton;

    @BindView(R.id.empty)
    public TextView empty;

    @BindView(R.id.stackHorizonllySwitch)
    public CheckBox stackHorizonllySwitch;

    @BindView(R.id.bgFillColorCircle)
    public ColorCircleView bgFillColorCircle;

    @BindView(R.id.bgFillColorLabel)
    public TextView bgFillColorLabel;

    @BindView(R.id.removeBgButton)
    public Button removeBgButton;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        unbinder = ButterKnife.bind(this);

        Inquiry.newInstance(this, null).build();

        list.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.grid_width)));

        adapter = new PhotoGridAdapter(this);
        list.setAdapter(adapter);
        list.setItemAnimator(new DefaultItemAnimator());








    }

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

            refresh();
        }
    }

    public void browseExternalPhotos() {
        Intent intent =
                new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("image/*");
        startActivityForResult(intent, BROWSE_RC);
    }


    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}
