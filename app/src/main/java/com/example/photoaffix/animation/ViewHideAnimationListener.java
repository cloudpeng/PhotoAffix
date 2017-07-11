package com.example.photoaffix.animation;

/**
 * Created by zengzhi on 2017/7/11.
 */

import android.animation.Animator;
import android.view.View;

/** @author Aidan Follestad (afollestad) */
public class ViewHideAnimationListener implements Animator.AnimatorListener {

    private final View view;

    public ViewHideAnimationListener(View view) {
        this.view = view;
    }

    @Override
    public void onAnimationStart(Animator animation) {}

    @Override
    public void onAnimationEnd(Animator animation) {
        view.setVisibility(View.GONE);
    }

    @Override
    public void onAnimationCancel(Animator animation) {}

    @Override
    public void onAnimationRepeat(Animator animation) {}
}