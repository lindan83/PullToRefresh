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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lance.pulltorefresh.LoadingLayoutBase;
import com.lance.pulltorefresh.PullToRefreshBase;
import com.lance.pulltorefresh.R;


@SuppressLint("ViewConstructor")
abstract class LoadingLayout extends LoadingLayoutBase {

    static final String LOG_TAG = "PullToRefresh-LoadingLayout";

    static final Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

    private FrameLayout innerLayout;

    protected final ImageView headerImage;
    protected final ProgressBar headerProgress;

    private boolean useIntrinsicAnimation;

    private final TextView headerText;
    private final TextView subHeaderText;

    protected final PullToRefreshBase.Mode mode;
    protected final PullToRefreshBase.Orientation scrollDirection;

    private CharSequence pullLabel;
    private CharSequence refreshingLabel;
    private CharSequence releaseLabel;

    public LoadingLayout(Context context, final PullToRefreshBase.Mode mode, final PullToRefreshBase.Orientation scrollDirection, TypedArray attrs) {
        super(context);
        this.mode = mode;
        this.scrollDirection = scrollDirection;

        switch (scrollDirection) {
            case HORIZONTAL:
                LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_header_horizontal, this);
                break;
            case VERTICAL:
            default:
                LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_header_vertical, this);
                break;
        }

        innerLayout = (FrameLayout) findViewById(R.id.fl_inner);
        headerText = (TextView) innerLayout.findViewById(R.id.pull_to_refresh_text);
        headerProgress = (ProgressBar) innerLayout.findViewById(R.id.pull_to_refresh_progress);
        subHeaderText = (TextView) innerLayout.findViewById(R.id.pull_to_refresh_sub_text);
        headerImage = (ImageView) innerLayout.findViewById(R.id.pull_to_refresh_image);

        LayoutParams lp = (LayoutParams) innerLayout.getLayoutParams();

        switch (mode) {
            case PULL_FROM_END:
                lp.gravity = scrollDirection == PullToRefreshBase.Orientation.VERTICAL ? Gravity.TOP : Gravity.START;

                // Load in labels
                pullLabel = context.getString(R.string.pull_to_refresh_from_bottom_pull_label);
                refreshingLabel = context.getString(R.string.pull_to_refresh_from_bottom_refreshing_label);
                releaseLabel = context.getString(R.string.pull_to_refresh_from_bottom_release_label);
                break;

            case PULL_FROM_START:
            default:
                lp.gravity = scrollDirection == PullToRefreshBase.Orientation.VERTICAL ? Gravity.BOTTOM : Gravity.END;

                // Load in labels
                pullLabel = context.getString(R.string.pull_to_refresh_pull_label);
                refreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
                releaseLabel = context.getString(R.string.pull_to_refresh_release_label);
                break;
        }

        if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderBackground)) {
            Drawable background = attrs.getDrawable(R.styleable.PullToRefresh_ptrHeaderBackground);
            if (null != background) {
                ViewCompat.setBackground(this, background);
            }
        }

        if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderTextAppearance)) {
            TypedValue styleID = new TypedValue();
            attrs.getValue(R.styleable.PullToRefresh_ptrHeaderTextAppearance, styleID);
            setTextAppearance(styleID.data);
        }
        if (attrs.hasValue(R.styleable.PullToRefresh_ptrSubHeaderTextAppearance)) {
            TypedValue styleID = new TypedValue();
            attrs.getValue(R.styleable.PullToRefresh_ptrSubHeaderTextAppearance, styleID);
            setSubTextAppearance(styleID.data);
        }

        // Text Color attrs need to be set after TextAppearance attrs
        if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderTextColor)) {
            ColorStateList colors = attrs.getColorStateList(R.styleable.PullToRefresh_ptrHeaderTextColor);
            if (null != colors) {
                setTextColor(colors);
            }
        }
        if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderSubTextColor)) {
            ColorStateList colors = attrs.getColorStateList(R.styleable.PullToRefresh_ptrHeaderSubTextColor);
            if (null != colors) {
                setSubTextColor(colors);
            }
        }

        // Try and get defined drawable from Attrs
        Drawable imageDrawable = null;
        if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawable)) {
            imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawable);
        }

        // Check Specific Drawable from Attrs, these overrite the generic
        // drawable attr above
        switch (mode) {
            case PULL_FROM_START:
            default:
                if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawableStart)) {
                    imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawableStart);
                } else if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawableTop)) {
                    Utils.warnDeprecation("ptrDrawableTop", "ptrDrawableStart");
                    imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawableTop);
                }
                break;

            case PULL_FROM_END:
                if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawableEnd)) {
                    imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawableEnd);
                } else if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawableBottom)) {
                    Utils.warnDeprecation("ptrDrawableBottom", "ptrDrawableEnd");
                    imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawableBottom);
                }
                break;
        }

        // If we don't have a user defined drawable, load the default
        if (null == imageDrawable) {
            imageDrawable = context.getResources().getDrawable(getDefaultDrawableResId());
        }

        // Set Drawable, and save width/height
        setLoadingDrawable(imageDrawable);

        reset();
    }


    @Override
    public final int getContentSize() {
        switch (scrollDirection) {
            case HORIZONTAL:
                return innerLayout.getWidth();
            case VERTICAL:
            default:
                return innerLayout.getHeight();
        }
    }

    @Override
    public final void onPull(float scaleOfLayout) {
        if (!useIntrinsicAnimation) {
            onPullImpl(scaleOfLayout);
        }
    }

    @Override
    public final void pullToRefresh() {
        if (null != headerText) {
            headerText.setText(pullLabel);
        }

        // Now call the callback
        pullToRefreshImpl();
    }

    @Override
    public final void refreshing() {
        if (null != headerText) {
            headerText.setText(refreshingLabel);
        }

        if (useIntrinsicAnimation) {
            ((AnimationDrawable) headerImage.getDrawable()).start();
        } else {
            // Now call the callback
            refreshingImpl();
        }

        if (null != subHeaderText) {
            subHeaderText.setVisibility(View.GONE);
        }
    }

    @Override
    public final void releaseToRefresh() {
        if (null != headerText) {
            headerText.setText(releaseLabel);
        }

        // Now call the callback
        releaseToRefreshImpl();
    }

    @Override
    public final void reset() {
        if (null != headerText) {
            headerText.setText(pullLabel);
        }
        headerImage.setVisibility(View.VISIBLE);

        if (useIntrinsicAnimation) {
            ((AnimationDrawable) headerImage.getDrawable()).stop();
        } else {
            // Now call the callback
            resetImpl();
        }

        if (null != subHeaderText) {
            if (TextUtils.isEmpty(subHeaderText.getText())) {
                subHeaderText.setVisibility(View.GONE);
            } else {
                subHeaderText.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void setLastUpdatedLabel(CharSequence label) {
        setSubHeaderText(label);
    }

    public final void setLoadingDrawable(Drawable imageDrawable) {
        // Set Drawable
        headerImage.setImageDrawable(imageDrawable);
        useIntrinsicAnimation = (imageDrawable instanceof AnimationDrawable);

        // Now call the callback
        onLoadingDrawableSet(imageDrawable);
    }

    public void setPullLabel(CharSequence pullLabel) {
        this.pullLabel = pullLabel;
    }

    public void setRefreshingLabel(CharSequence refreshingLabel) {
        this.refreshingLabel = refreshingLabel;
    }

    public void setReleaseLabel(CharSequence releaseLabel) {
        this.releaseLabel = releaseLabel;
    }

    @Override
    public void setTextTypeface(Typeface tf) {
        headerText.setTypeface(tf);
    }

    protected abstract int getDefaultDrawableResId();

    protected abstract void onLoadingDrawableSet(Drawable imageDrawable);

    protected abstract void onPullImpl(float scaleOfLayout);

    protected abstract void pullToRefreshImpl();

    protected abstract void refreshingImpl();

    protected abstract void releaseToRefreshImpl();

    protected abstract void resetImpl();

    private void setSubHeaderText(CharSequence label) {
        if (null != subHeaderText) {
            if (TextUtils.isEmpty(label)) {
                subHeaderText.setVisibility(View.GONE);
            } else {
                subHeaderText.setText(label);

                // Only set it to Visible if we're GONE, otherwise VISIBLE will
                // be set soon
                if (View.GONE == subHeaderText.getVisibility()) {
                    subHeaderText.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void setSubTextAppearance(int value) {
        if (null != subHeaderText) {
            subHeaderText.setTextAppearance(getContext(), value);
        }
    }

    private void setSubTextColor(ColorStateList color) {
        if (null != subHeaderText) {
            subHeaderText.setTextColor(color);
        }
    }

    private void setTextAppearance(int value) {
        if (null != headerText) {
            headerText.setTextAppearance(getContext(), value);
        }
        if (null != subHeaderText) {
            subHeaderText.setTextAppearance(getContext(), value);
        }
    }

    private void setTextColor(ColorStateList color) {
        if (null != headerText) {
            headerText.setTextColor(color);
        }
        if (null != subHeaderText) {
            subHeaderText.setTextColor(color);
        }
    }
}
