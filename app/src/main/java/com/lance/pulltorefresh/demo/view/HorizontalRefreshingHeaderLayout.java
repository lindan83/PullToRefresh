package com.lance.pulltorefresh.demo.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lance.pulltorefresh.LoadingLayoutBase;
import com.lance.pulltorefresh.demo.R;

/**
 * Created by lindan on 16-12-6.
 */

public class HorizontalRefreshingHeaderLayout extends LoadingLayoutBase {
    private FrameLayout mInnerLayout;
    private TextView mTvRefreshing;
    private ProgressBar mPbRefreshing;

    private CharSequence mPullLabel;
    private CharSequence mRefreshingLabel;
    private CharSequence mReleaseLabel;

    public HorizontalRefreshingHeaderLayout(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.horizontal_refreshing_header_layout, this);

        mInnerLayout = (FrameLayout) findViewById(com.lance.pulltorefresh.R.id.fl_inner);
        mTvRefreshing = (TextView) mInnerLayout.findViewById(com.lance.pulltorefresh.R.id.tv_refreshing_text);
        mPbRefreshing = (ProgressBar) mInnerLayout.findViewById(com.lance.pulltorefresh.R.id.pb_refreshing_progress);
        LayoutParams lp = (LayoutParams) mInnerLayout.getLayoutParams();
        lp.gravity = Gravity.END;

        mPullLabel = context.getString(com.lance.pulltorefresh.R.string.pull_to_refresh_pull_label);
        mRefreshingLabel = context.getString(com.lance.pulltorefresh.R.string.pull_to_refresh_refreshing);
        mReleaseLabel = context.getString(com.lance.pulltorefresh.R.string.pull_to_refresh_release_label);

        reset();
    }

    @Override
    public final int getContentSize() {
        return mInnerLayout.getWidth();
    }

    @Override
    public final void pullToRefresh() {
        if (!TextUtils.equals(mTvRefreshing.getText(), mPullLabel)) {
            mTvRefreshing.setText(mPullLabel);
        }
    }

    @Override
    public final void onPull(float scaleOfLayout) {
    }

    @Override
    public final void refreshing() {
        if (mPbRefreshing.getVisibility() != VISIBLE) {
            mPbRefreshing.setVisibility(VISIBLE);
        }
        if (!TextUtils.equals(mRefreshingLabel, mTvRefreshing.getText())) {
            mTvRefreshing.setText(mRefreshingLabel);
        }
    }

    @Override
    public final void releaseToRefresh() {
        if (!TextUtils.equals(mReleaseLabel, mTvRefreshing.getText())) {
            mTvRefreshing.setText(mReleaseLabel);
        }
    }

    @Override
    public final void reset() {
        mPbRefreshing.setVisibility(GONE);
    }

    @Override
    public void setPullLabel(CharSequence pullLabel) {
        mPullLabel = pullLabel;
    }

    @Override
    public void setRefreshingLabel(CharSequence refreshingLabel) {
        mRefreshingLabel = refreshingLabel;
    }

    @Override
    public void setReleaseLabel(CharSequence releaseLabel) {
        mReleaseLabel = releaseLabel;
    }
}
