package com.example.photoaffix.adapters;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerViewAdapter;
import com.example.photoaffix.R;
import com.example.photoaffix.data.Photo;
import com.example.photoaffix.data.PhotoHolder;

/**
 * Created by zengzhi on 2017/7/7.
 */

public class PhotoGridAdapter extends DragSelectRecyclerViewAdapter<PhotoGridAdapter.PhotoViewHolder>{

    private Context mContext;

    private Photo[] photos;

    public PhotoGridAdapter(Context context) {

        this.mContext = context;
    }

    @Override
    public void saveInstanceState(Bundle out) {
        super.saveInstanceState(out);
        if (photos != null) {

            out.putSerializable("photos", new PhotoHolder(photos));
        }
    }

    @Override
    public void restoreInstanceState(Bundle in) {
        super.restoreInstanceState(in);
        if (in != null && in.containsKey("photos")) {

            PhotoHolder ph = (PhotoHolder) in.getSerializable("photos");

            if (ph != null) {

                setPhotos(ph.photos);
            }
        }
    }


    public void setPhotos(Photo[] photos) {
        this.photos = photos;
        notifyDataSetChanged();
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType == 0 ? R.layout.griditem_browse : R.layout.griditem_photo, parent, false);

        return new PhotoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class PhotoViewHolder extends RecyclerView.ViewHolder {


        public PhotoViewHolder(View itemView) {
            super(itemView);
        }
    }
}
