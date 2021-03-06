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
 * 默认的加载Footer Layout，可作为Demo模仿
 */

public class DefaultLoadingFooterLayout extends LoadingLayoutBase {
    private FrameLayout innerLayout;

    private TextView tvRefreshing;
    private ProgressBar pbRefreshing;

    private CharSequence pullLabel;
    private CharSequence refreshingLabel;
    private CharSequence releaseLabel;

    public DefaultLoadingFooterLayout(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.default_loading_footer_layout, this);
        innerLayout = (FrameLayout) findViewById(R.id.fl_inner);
        tvRefreshing = (TextView) innerLayout.findViewById(R.id.tv_refreshing_text);
        pbRefreshing = (ProgressBar) innerLayout.findViewById(R.id.pb_refreshing_progress);
        LayoutParams lp = (LayoutParams) innerLayout.getLayoutParams();
        lp.gravity = Gravity.TOP;

        pullLabel = context.getString(R.string.pull_to_refresh_pull_up_label);
        refreshingLabel = context.getString(R.string.pull_to_refresh_loading);
        releaseLabel = context.getString(R.string.pull_to_refresh_release_load_label);
    }

    /**
     * 获取加载底部的高度
     *
     * @return size
     */
    @Override
    public int getContentSize() {
        return innerLayout.getHeight();
    }

    /**
     * 开始下拉时的回调
     */
    @Override
    public void pullToRefresh() {
        if (!TextUtils.equals(tvRefreshing.getText(), pullLabel)) {
            tvRefreshing.setText(pullLabel);
        }
    }

    /**
     * 加载底部完全显示时的回调
     */
    @Override
    public void releaseToRefresh() {
        if (TextUtils.equals(tvRefreshing.getText(), releaseLabel)) {
            return;
        }
        tvRefreshing.setText(releaseLabel);
    }

    /**
     * 用户拖动时的回调
     *
     * @param scaleOfLayout scaleOfLayout 拖动距离与加载底部高度的比例
     */
    @Override
    public void onPull(float scaleOfLayout) {

    }

    /**
     * 加载底部完全显示并已释放时回调，用于提示用户正在加载数据
     */
    @Override
    public void refreshing() {
        if (pbRefreshing.getVisibility() != VISIBLE) {
            pbRefreshing.setVisibility(VISIBLE);
        }
        if (!TextUtils.equals(tvRefreshing.getText(), refreshingLabel)) {
            tvRefreshing.setText(refreshingLabel);
        }
    }

    /**
     * 数据加载完毕的回调
     */
    @Override
    public void reset() {
        pbRefreshing.setVisibility(GONE);
    }

    /**
     * 设置下拉时标题
     * 例如：提示用户下拉可以刷新
     *
     * @param pullLabel - CharSequence to display
     */
    @Override
    public void setPullLabel(CharSequence pullLabel) {
        this.pullLabel = pullLabel;
    }

    /**
     * 设置释放时的标题
     * 例如：提示用户正在加载数据
     *
     * @param refreshingLabel - CharSequence to display
     */
    @Override
    public void setRefreshingLabel(CharSequence refreshingLabel) {
        this.refreshingLabel = refreshingLabel;
    }

    /**
     * 设置底部完全显示的标题
     * 例如：提示用户可以释放了
     *
     * @param releaseLabel - CharSequence to display
     */
    @Override
    public void setReleaseLabel(CharSequence releaseLabel) {
        this.releaseLabel = releaseLabel;
    }
}
