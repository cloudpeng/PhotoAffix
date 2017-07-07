package com.example.photoaffix.data;

import android.net.Uri;

import com.afollestad.inquiry.annotations.Column;

import java.io.Serializable;

/**
 * Created by zengzhi on 2017/7/7.
 */

public class Photo implements Serializable{

    @Column public long _id;
    @Column public String _data;
    @Column public long datetaken;

    public Photo() {
    }

    public Photo(Uri uri) {

        _data = uri.toString();
    }


    public Uri getUri() {

        Uri uri = Uri.parse(_data);

        if (!uri.toString().startsWith("file://") && !uri.toString().startsWith("content://")) {

            uri = Uri.parse(String.format("file://%s", uri.toString()));

        }
        return uri;
    }
}
