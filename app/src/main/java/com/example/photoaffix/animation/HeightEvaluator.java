package com.example.photoaffix.animation;

import android.animation.IntEvaluator;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by zengzhi on 2017/7/11.
 */

public class HeightEvaluator extends IntEvaluator {

    private final View v;

    public HeightEvaluator(View v) {
        this.v = v;
    }


    @Override
    public Integer evaluate(float fraction, Integer startValue, Integer endValue) {

        int num = super.evaluate(fraction, startValue, endValue);

        ViewGroup.LayoutParams params = v.getLayoutParams();

        params.height = num;
        v.setLayoutParams(params);

        return num;
    }
}
