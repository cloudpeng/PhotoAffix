package com.example.photoaffix.data;

import java.io.Serializable;

/**
 * Created by zengzhi on 2017/7/7.
 */

public class PhotoHolder implements Serializable{

    public Photo[] photos;

    public PhotoHolder() {

    }

    public PhotoHolder(Photo[] photos) {

        this.photos = photos;
    }
}
