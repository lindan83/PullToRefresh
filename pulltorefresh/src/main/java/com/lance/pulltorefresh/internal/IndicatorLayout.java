/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.lance.pulltorefresh.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.lance.pulltorefresh.PullToRefreshBase;
import com.lance.pulltorefresh.R;

@SuppressLint("ViewConstructor")
public class IndicatorLayout extends FrameLayout implements AnimationListener {

    static final int DEFAULT_ROTATION_ANIMATION_DURATION = 150;

    private Animation inAnim, outAnim;
    private ImageView arrowImageView;

    private final Animation rotateAnimation, resetRotateAnimation;

    public IndicatorLayout(Context context, PullToRefreshBase.Mode mode) {
        super(context);
        arrowImageView = new ImageView(context);

        Drawable arrowD = getResources().getDrawable(R.mipmap.indicator_arrow);
        arrowImageView.setImageDrawable(arrowD);

        final int padding = getResources().getDimensionPixelSize(R.dimen.indicator_internal_padding);
        arrowImageView.setPadding(padding, padding, padding, padding);
        addView(arrowImageView);

        int inAnimResId, outAnimResId;
        switch (mode) {
            case PULL_FROM_END:
                inAnimResId = R.anim.slide_in_from_bottom;
                outAnimResId = R.anim.slide_out_to_bottom;
                setBackgroundResource(R.drawable.indicator_bg_bottom);

                // Rotate Arrow so it's pointing the correct way
                arrowImageView.setScaleType(ScaleType.MATRIX);
                Matrix matrix = new Matrix();
                matrix.setRotate(180f, arrowD.getIntrinsicWidth() / 2f, arrowD.getIntrinsicHeight() / 2f);
                arrowImageView.setImageMatrix(matrix);
                break;
            default:
            case PULL_FROM_START:
                inAnimResId = R.anim.slide_in_from_top;
                outAnimResId = R.anim.slide_out_to_top;
                setBackgroundResource(R.drawable.indicator_bg_top);
                break;
        }

        inAnim = AnimationUtils.loadAnimation(context, inAnimResId);
        inAnim.setAnimationListener(this);

        outAnim = AnimationUtils.loadAnimation(context, outAnimResId);
        outAnim.setAnimationListener(this);

        final Interpolator interpolator = new LinearInterpolator();
        rotateAnimation = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        rotateAnimation.setInterpolator(interpolator);
        rotateAnimation.setDuration(DEFAULT_ROTATION_ANIMATION_DURATION);
        rotateAnimation.setFillAfter(true);

        resetRotateAnimation = new RotateAnimation(-180, 0, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        resetRotateAnimation.setInterpolator(interpolator);
        resetRotateAnimation.setDuration(DEFAULT_ROTATION_ANIMATION_DURATION);
        resetRotateAnimation.setFillAfter(true);

    }

    public final boolean isVisible() {
        Animation currentAnim = getAnimation();
        if (null != currentAnim) {
            return inAnim == currentAnim;
        }

        return getVisibility() == View.VISIBLE;
    }

    public void hide() {
        startAnimation(outAnim);
    }

    public void show() {
        arrowImageView.clearAnimation();
        startAnimation(inAnim);
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (animation == outAnim) {
            arrowImageView.clearAnimation();
            setVisibility(View.GONE);
        } else if (animation == inAnim) {
            setVisibility(View.VISIBLE);
        }

        clearAnimation();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // NO-OP
    }

    @Override
    public void onAnimationStart(Animation animation) {
        setVisibility(View.VISIBLE);
    }

    public void releaseToRefresh() {
        arrowImageView.startAnimation(rotateAnimation);
    }

    public void pullToRefresh() {
        arrowImageView.startAnimation(resetRotateAnimation);
    }

}
