package com.lance.pulltorefresh;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by lindan on 16-12-6.
 * 扩展PullToRefresh开源框架，支持横向的RecyclerView
 */

public class PullToRefreshHorizontalRecyclerView extends PullToRefreshBase<RecyclerView> {
    public PullToRefreshHorizontalRecyclerView(Context context) {
        super(context);
    }

    public PullToRefreshHorizontalRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullToRefreshHorizontalRecyclerView(Context context, Mode mode) {
        super(context, mode);
    }

    public PullToRefreshHorizontalRecyclerView(Context context, Mode mode, AnimationStyle animStyle) {
        super(context, mode, animStyle);
    }

    @Override
    public Orientation getPullToRefreshScrollDirection() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected RecyclerView createRefreshableView(Context context, AttributeSet attrs) {
        return new RecyclerView(context, attrs);
    }

    @Override
    protected boolean isReadyForPullEnd() {
        return isLastItemVisible();
    }

    @Override
    protected boolean isReadyForPullStart() {
        return isFirstItemVisible();
    }

    /**
     * 判断第一个Item是否完全可见
     *
     * @return
     */
    private boolean isFirstItemVisible() {
        RecyclerView.Adapter<?> adapter = mRefreshableView.getAdapter();
        //如果未设置Adapter或者没有数据，仍允许下拉刷新
        if (adapter == null || adapter.getItemCount() == 0) {
            return true;
        }
        //第一个item完全可见，可以刷新
        if (getFirstVisiblePosition() == 0) {
            return mRefreshableView.getChildAt(0).getTop() >= mRefreshableView.getTop();
        }
        return false;
    }

    private boolean isLastItemVisible() {
        RecyclerView.Adapter<?> adapter = mRefreshableView.getAdapter();
        //如果未设置Adapter或者没有数据，仍允许上拉加载
        if (adapter == null || adapter.getItemCount() == 0) {
            return true;
        }
        //最后一个Item完全可见，可以刷新
        int lastVisiblePosition = getLastVisiblePosition();
        if (lastVisiblePosition >= mRefreshableView.getAdapter().getItemCount() - 1) {
            return mRefreshableView.getChildAt(mRefreshableView.getChildCount() - 1).getBottom() <= mRefreshableView.getBottom();
        }
        return false;
    }

    /**
     * 获取第一个可见Item的位置
     *
     * @return
     */
    private int getFirstVisiblePosition() {
        View firstVisibleChild = mRefreshableView.getChildAt(0);
        return firstVisibleChild != null ? mRefreshableView.getChildAdapterPosition(firstVisibleChild) : -1;
    }

    /**
     * 获取最后一个可见item的位置
     *
     * @return
     */
    private int getLastVisiblePosition() {
        View lastVisibleChild = mRefreshableView.getChildAt(mRefreshableView.getChildCount() - 1);
        return lastVisibleChild != null ? mRefreshableView.getChildAdapterPosition(lastVisibleChild) : -1;
    }
}
