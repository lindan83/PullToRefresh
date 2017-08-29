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
    private FrameLayout innerLayout;
    private TextView tvRefreshing;
    private ProgressBar pbRefreshing;

    private CharSequence pullLabel;
    private CharSequence refreshingLabel;
    private CharSequence releaseLabel;

    public DefaultRefreshingHeaderLayout(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.default_refreshing_header_layout, this);

        innerLayout = (FrameLayout) findViewById(R.id.fl_inner);
        tvRefreshing = (TextView) innerLayout.findViewById(R.id.tv_refreshing_text);
        pbRefreshing = (ProgressBar) innerLayout.findViewById(R.id.pb_refreshing_progress);
        LayoutParams lp = (LayoutParams) innerLayout.getLayoutParams();
        lp.gravity = Gravity.BOTTOM;

        pullLabel = context.getString(R.string.pull_to_refresh_pull_label);
        refreshingLabel = context.getString(R.string.pull_to_refresh_refreshing);
        releaseLabel = context.getString(R.string.pull_to_refresh_release_label);

        reset();
    }

    @Override
    public final int getContentSize() {
        return innerLayout.getHeight();
    }

    @Override
    public final void pullToRefresh() {
        if (!TextUtils.equals(tvRefreshing.getText(), pullLabel)) {
            tvRefreshing.setText(pullLabel);
        }
    }

    @Override
    public final void onPull(float scaleOfLayout) {
    }

    @Override
    public final void refreshing() {
        if (pbRefreshing.getVisibility() != VISIBLE) {
            pbRefreshing.setVisibility(VISIBLE);
        }
        if (!TextUtils.equals(refreshingLabel, tvRefreshing.getText())) {
            tvRefreshing.setText(refreshingLabel);
        }
    }

    @Override
    public final void releaseToRefresh() {
        if (!TextUtils.equals(releaseLabel, tvRefreshing.getText())) {
            tvRefreshing.setText(releaseLabel);
        }
    }

    @Override
    public final void reset() {
        pbRefreshing.setVisibility(GONE);
    }

    @Override
    public void setPullLabel(CharSequence pullLabel) {
        this.pullLabel = pullLabel;
    }

    @Override
    public void setRefreshingLabel(CharSequence refreshingLabel) {
        this.refreshingLabel = refreshingLabel;
    }

    @Override
    public void setReleaseLabel(CharSequence releaseLabel) {
        this.releaseLabel = releaseLabel;
    }
}
