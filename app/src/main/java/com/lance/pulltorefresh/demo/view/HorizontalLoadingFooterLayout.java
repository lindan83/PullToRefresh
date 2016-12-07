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
 * 默认的加载Footer Layout，可作为Demo模仿
 */

public class HorizontalLoadingFooterLayout extends LoadingLayoutBase {
    private FrameLayout mInnerLayout;

    private TextView mTvRefreshing;
    private ProgressBar mPbRefreshing;

    private CharSequence mPullLabel;
    private CharSequence mRefreshingLabel;
    private CharSequence mReleaseLabel;

    public HorizontalLoadingFooterLayout(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.horizontal_loading_footer_layout, this);
        mInnerLayout = (FrameLayout) findViewById(com.lance.pulltorefresh.R.id.fl_inner);
        mTvRefreshing = (TextView) mInnerLayout.findViewById(com.lance.pulltorefresh.R.id.tv_refreshing_text);
        mPbRefreshing = (ProgressBar) mInnerLayout.findViewById(com.lance.pulltorefresh.R.id.pb_refreshing_progress);
        LayoutParams lp = (LayoutParams) mInnerLayout.getLayoutParams();
        lp.gravity = Gravity.START;

        mPullLabel = context.getString(com.lance.pulltorefresh.R.string.pull_to_refresh_pull_up_label);
        mRefreshingLabel = context.getString(com.lance.pulltorefresh.R.string.pull_to_refresh_loading);
        mReleaseLabel = context.getString(com.lance.pulltorefresh.R.string.pull_to_refresh_release_load_label);
    }

    /**
     * 获取加载底部的高度
     *
     * @return size
     */
    @Override
    public int getContentSize() {
        return mInnerLayout.getWidth();
    }

    /**
     * 开始下拉时的回调
     */
    @Override
    public void pullToRefresh() {
        if (!TextUtils.equals(mTvRefreshing.getText(), mPullLabel)) {
            mTvRefreshing.setText(mPullLabel);
        }
    }

    /**
     * 加载底部完全显示时的回调
     */
    @Override
    public void releaseToRefresh() {
        if (TextUtils.equals(mTvRefreshing.getText(), mReleaseLabel)) {
            return;
        }
        mTvRefreshing.setText(mReleaseLabel);
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
        if (mPbRefreshing.getVisibility() != VISIBLE) {
            mPbRefreshing.setVisibility(VISIBLE);
        }
        if (!TextUtils.equals(mTvRefreshing.getText(), mRefreshingLabel)) {
            mTvRefreshing.setText(mRefreshingLabel);
        }
    }

    /**
     * 数据加载完毕的回调
     */
    @Override
    public void reset() {
        mPbRefreshing.setVisibility(GONE);
    }

    /**
     * 设置下拉时标题
     * 例如：提示用户下拉可以刷新
     *
     * @param pullLabel - CharSequence to display
     */
    @Override
    public void setPullLabel(CharSequence pullLabel) {
        mPullLabel = pullLabel;
    }

    /**
     * 设置释放时的标题
     * 例如：提示用户正在加载数据
     *
     * @param refreshingLabel - CharSequence to display
     */
    @Override
    public void setRefreshingLabel(CharSequence refreshingLabel) {
        mRefreshingLabel = refreshingLabel;
    }

    /**
     * 设置底部完全显示的标题
     * 例如：提示用户可以释放了
     *
     * @param releaseLabel - CharSequence to display
     */
    @Override
    public void setReleaseLabel(CharSequence releaseLabel) {
        mReleaseLabel = releaseLabel;
    }
}
