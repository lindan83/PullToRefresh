package com.lance.pulltorefresh;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by lindan on 16-12-6.
 * 默认的刷新Header Layout，可作为Demo模仿
 */

public class DefaultRefreshingHeaderLayout extends LoadingLayoutBase {
    private FrameLayout mInnerLayout;
    private TextView mTvRefreshing;
    private ProgressBar mPbRefreshing;

    private CharSequence mPullLabel;
    private CharSequence mRefreshingLabel;
    private CharSequence mReleaseLabel;

    public DefaultRefreshingHeaderLayout(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.default_refreshing_header_layout, this);

        mInnerLayout = (FrameLayout) findViewById(R.id.fl_inner);
        mTvRefreshing = (TextView) mInnerLayout.findViewById(R.id.tv_refreshing_text);
        mPbRefreshing = (ProgressBar) mInnerLayout.findViewById(R.id.pb_refreshing_progress);
        LayoutParams lp = (LayoutParams) mInnerLayout.getLayoutParams();
        lp.gravity = Gravity.BOTTOM;

        mPullLabel = context.getString(R.string.pull_to_refresh_pull_label);
        mRefreshingLabel = context.getString(R.string.pull_to_refresh_refreshing);
        mReleaseLabel = context.getString(R.string.pull_to_refresh_release_label);

        reset();
    }

    @Override
    public final int getContentSize() {
        return mInnerLayout.getHeight();
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
