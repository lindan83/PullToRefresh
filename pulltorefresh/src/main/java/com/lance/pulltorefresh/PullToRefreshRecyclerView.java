package com.lance.pulltorefresh;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by lindan on 16-12-6.
 * 扩展PullToRefresh开源框架，支持RecyclerView
 */

public class PullToRefreshRecyclerView extends PullToRefreshBase<RecyclerView> {
    public PullToRefreshRecyclerView(Context context) {
        super(context);
    }

    public PullToRefreshRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullToRefreshRecyclerView(Context context, Mode mode) {
        super(context, mode);
    }

    public PullToRefreshRecyclerView(Context context, Mode mode, AnimationStyle animStyle) {
        super(context, mode, animStyle);
    }

    @Override
    public Orientation getPullToRefreshScrollDirection() {
        return Orientation.VERTICAL;
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
        RecyclerView.Adapter<?> adapter = refreshableView.getAdapter();
        //如果未设置Adapter或者没有数据，仍允许下拉刷新
        if (adapter == null || adapter.getItemCount() == 0) {
            return true;
        }
        //第一个item完全可见，可以刷新
        if (getFirstVisiblePosition() == 0) {
            return refreshableView.getChildAt(0).getTop() >= refreshableView.getTop();
        }
        return false;
    }

    private boolean isLastItemVisible() {
        RecyclerView.Adapter<?> adapter = refreshableView.getAdapter();
        //如果未设置Adapter或者没有数据，仍允许上拉加载
        if (adapter == null || adapter.getItemCount() == 0) {
            return true;
        }
        //最后一个Item完全可见，可以刷新
        int lastVisiblePosition = getLastVisiblePosition();
        if (lastVisiblePosition >= refreshableView.getAdapter().getItemCount() - 1) {
            return refreshableView.getChildAt(refreshableView.getChildCount() - 1).getBottom() <= refreshableView.getBottom();
        }
        return false;
    }

    /**
     * 获取第一个可见Item的位置
     *
     * @return
     */
    private int getFirstVisiblePosition() {
        View firstVisibleChild = refreshableView.getChildAt(0);
        return firstVisibleChild != null ? refreshableView.getChildAdapterPosition(firstVisibleChild) : -1;
    }

    /**
     * 获取最后一个可见item的位置
     *
     * @return
     */
    private int getLastVisiblePosition() {
        View lastVisibleChild = refreshableView.getChildAt(refreshableView.getChildCount() - 1);
        return lastVisibleChild != null ? refreshableView.getChildAdapterPosition(lastVisibleChild) : -1;
    }
}
