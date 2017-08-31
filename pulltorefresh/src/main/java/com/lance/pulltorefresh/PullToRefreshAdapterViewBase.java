/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.lance.pulltorefresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import com.lance.pulltorefresh.internal.EmptyViewMethodAccessor;
import com.lance.pulltorefresh.internal.IndicatorLayout;

public abstract class PullToRefreshAdapterViewBase<T extends AbsListView>
        extends PullToRefreshBase<T>
        implements OnScrollListener {

    private boolean lastItemVisible;
    private OnScrollListener onScrollListener;
    private OnLastItemVisibleListener onLastItemVisibleListener;
    private View emptyView;

    private IndicatorLayout indicatorIvTop;
    private IndicatorLayout indicatorIvBottom;

    private boolean showIndicator;
    private boolean scrollEmptyView = true;

    public PullToRefreshAdapterViewBase(Context context) {
        super(context);
        refreshableView.setOnScrollListener(this);
    }

    public PullToRefreshAdapterViewBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        refreshableView.setOnScrollListener(this);
    }

    public PullToRefreshAdapterViewBase(Context context, Mode mode) {
        super(context, mode);
        refreshableView.setOnScrollListener(this);
    }

    public PullToRefreshAdapterViewBase(Context context, Mode mode, AnimationStyle animStyle) {
        super(context, mode, animStyle);
        refreshableView.setOnScrollListener(this);
    }

    private static FrameLayout.LayoutParams convertEmptyViewLayoutParams(ViewGroup.LayoutParams lp) {
        FrameLayout.LayoutParams newLp = null;
        if (null != lp) {
            newLp = new FrameLayout.LayoutParams(lp);
            if (lp instanceof LayoutParams) {
                newLp.gravity = ((LayoutParams) lp).gravity;
            } else {
                newLp.gravity = Gravity.CENTER;
            }
        }
        return newLp;
    }

    /**
     * Gets whether an indicator graphic should be displayed when the View is in
     * a state where a Pull-to-Refresh can happen. An example of this state is
     * when the Adapter View is scrolled to the top and the mode is set to
     * {@link Mode#PULL_FROM_START}. The default value is <var>true</var> if
     * {@link PullToRefreshBase#isPullToRefreshOverScrollEnabled()
     * isPullToRefreshOverScrollEnabled()} returns false.
     *
     * @return true if the indicators will be shown
     */
    public boolean getShowIndicator() {
        return showIndicator;
    }

    public final void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
                               final int totalItemCount) {

        if (DEBUG) {
            Log.d(LOG_TAG, "First Visible: " + firstVisibleItem + ". Visible Count: " + visibleItemCount
                    + ". Total Items:" + totalItemCount);
        }

        /*
         * Set whether the Last Item is Visible. lastVisibleItemIndex is a
         * zero-based index, so we minus one totalItemCount to check
         */
        if (null != onLastItemVisibleListener) {
            lastItemVisible = (totalItemCount > 0) && (firstVisibleItem + visibleItemCount >= totalItemCount - 1);
        }

        // If we're showing the indicator, check positions...
        if (getShowIndicatorInternal()) {
            updateIndicatorViewsVisibility();
        }

        // Finally call OnScrollListener if we have one
        if (null != onScrollListener) {
            onScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    public final void onScrollStateChanged(final AbsListView view, final int state) {
        /*
         * Check that the scrolling has stopped, and that the last item is visible.
         */
        if (state == OnScrollListener.SCROLL_STATE_IDLE && null != onLastItemVisibleListener && lastItemVisible) {
            onLastItemVisibleListener.onLastItemVisible();
        }

        if (null != onScrollListener) {
            onScrollListener.onScrollStateChanged(view, state);
        }
    }

    /**
     * Pass-through method for {@link PullToRefreshBase#getRefreshableView()
     * getRefreshableView()}.
     * {@link AdapterView#setAdapter(Adapter)}
     * setAdapter(adapter)}. This is just for convenience!
     *
     * @param adapter - Adapter to set
     */
    public void setAdapter(ListAdapter adapter) {
        refreshableView.setAdapter(adapter);
    }

    /**
     * Sets the Empty View to be used by the Adapter View.
     * We need it handle it ourselves so that we can Pull-to-Refresh when the
     * Empty View is shown.
     * Please note, you do <strong>not</strong> usually need to call this method
     * yourself. Calling setEmptyView on the AdapterView will automatically call
     * this method and set everything up. This includes when the Android
     * Framework automatically sets the Empty View based on it's ID.
     *
     * @param newEmptyView - Empty View to be used
     */
    public final void setEmptyView(View newEmptyView) {
        FrameLayout refreshableViewWrapper = getRefreshableViewWrapper();

        if (null != newEmptyView) {
            // New view needs to be clickable so that Android recognizes it as a
            // target for Touch Events
            newEmptyView.setClickable(true);

            ViewParent newEmptyViewParent = newEmptyView.getParent();
            if (null != newEmptyViewParent && newEmptyViewParent instanceof ViewGroup) {
                ((ViewGroup) newEmptyViewParent).removeView(newEmptyView);
            }

            // We need to convert any LayoutParams so that it works in our
            // FrameLayout
            FrameLayout.LayoutParams lp = convertEmptyViewLayoutParams(newEmptyView.getLayoutParams());
            if (null != lp) {
                refreshableViewWrapper.addView(newEmptyView, lp);
            } else {
                refreshableViewWrapper.addView(newEmptyView);
            }
        }

        if (refreshableView instanceof EmptyViewMethodAccessor) {
            ((EmptyViewMethodAccessor) refreshableView).setEmptyViewInternal(newEmptyView);
        } else {
            refreshableView.setEmptyView(newEmptyView);
        }
        emptyView = newEmptyView;
    }

    /**
     * Pass-through method for {@link PullToRefreshBase#getRefreshableView()
     * getRefreshableView()}.
     * {@link AdapterView#setOnItemClickListener(OnItemClickListener)
     * setOnItemClickListener(listener)}. This is just for convenience!
     *
     * @param listener - OnItemClickListener to use
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        refreshableView.setOnItemClickListener(listener);
    }

    public final void setOnLastItemVisibleListener(OnLastItemVisibleListener listener) {
        onLastItemVisibleListener = listener;
    }

    public final void setOnScrollListener(OnScrollListener listener) {
        onScrollListener = listener;
    }

    public final void setScrollEmptyView(boolean doScroll) {
        scrollEmptyView = doScroll;
    }

    /**
     * Sets whether an indicator graphic should be displayed when the View is in
     * a state where a Pull-to-Refresh can happen. An example of this state is
     * when the Adapter View is scrolled to the top and the mode is set to
     * {@link Mode#PULL_FROM_START}
     *
     * @param showIndicator - true if the indicators should be shown.
     */
    public void setShowIndicator(boolean showIndicator) {
        this.showIndicator = showIndicator;

        if (getShowIndicatorInternal()) {
            // If we're set to Show Indicator, add/update them
            addIndicatorViews();
        } else {
            // If not, then remove then
            removeIndicatorViews();
        }
    }

    @Override
    protected void onPullToRefresh() {
        super.onPullToRefresh();

        if (getShowIndicatorInternal()) {
            switch (getCurrentMode()) {
                case PULL_FROM_END:
                    indicatorIvBottom.pullToRefresh();
                    break;
                case PULL_FROM_START:
                    indicatorIvTop.pullToRefresh();
                    break;
                default:
                    // NO-OP
                    break;
            }
        }
    }

    protected void onRefreshing(boolean doScroll) {
        super.onRefreshing(doScroll);

        if (getShowIndicatorInternal()) {
            updateIndicatorViewsVisibility();
        }
    }

    @Override
    protected void onReleaseToRefresh() {
        super.onReleaseToRefresh();

        if (getShowIndicatorInternal()) {
            switch (getCurrentMode()) {
                case PULL_FROM_END:
                    indicatorIvBottom.releaseToRefresh();
                    break;
                case PULL_FROM_START:
                    indicatorIvTop.releaseToRefresh();
                    break;
                default:
                    // NO-OP
                    break;
            }
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        if (getShowIndicatorInternal()) {
            updateIndicatorViewsVisibility();
        }
    }

    @Override
    protected void handleStyledAttributes(TypedArray a) {
        // Set Show Indicator to the XML value, or default value
        showIndicator = a.getBoolean(R.styleable.PullToRefresh_ptrShowIndicator, !isPullToRefreshOverScrollEnabled());
    }

    protected boolean isReadyForPullStart() {
        return isFirstItemVisible();
    }

    protected boolean isReadyForPullEnd() {
        return isLastItemVisible();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (null != emptyView && !scrollEmptyView) {
            emptyView.scrollTo(-l, -t);
        }
    }

    @Override
    protected void updateUIForMode() {
        super.updateUIForMode();

        // Check Indicator Views consistent with new Mode
        if (getShowIndicatorInternal()) {
            addIndicatorViews();
        } else {
            removeIndicatorViews();
        }
    }

    private void addIndicatorViews() {
        Mode mode = getMode();
        FrameLayout refreshableViewWrapper = getRefreshableViewWrapper();

        if (mode.showHeaderLoadingLayout() && null == indicatorIvTop) {
            // If the mode can pull down, and we don't have one set already
            indicatorIvTop = new IndicatorLayout(getContext(), Mode.PULL_FROM_START);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = getResources().getDimensionPixelSize(R.dimen.indicator_right_padding);
            params.gravity = Gravity.TOP | Gravity.END;
            refreshableViewWrapper.addView(indicatorIvTop, params);

        } else if (!mode.showHeaderLoadingLayout() && null != indicatorIvTop) {
            // If we can't pull down, but have a View then remove it
            refreshableViewWrapper.removeView(indicatorIvTop);
            indicatorIvTop = null;
        }

        if (mode.showFooterLoadingLayout() && null == indicatorIvBottom) {
            // If the mode can pull down, and we don't have one set already
            indicatorIvBottom = new IndicatorLayout(getContext(), Mode.PULL_FROM_END);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = getResources().getDimensionPixelSize(R.dimen.indicator_right_padding);
            params.gravity = Gravity.BOTTOM | Gravity.END;
            refreshableViewWrapper.addView(indicatorIvBottom, params);

        } else if (!mode.showFooterLoadingLayout() && null != indicatorIvBottom) {
            // If we can't pull down, but have a View then remove it
            refreshableViewWrapper.removeView(indicatorIvBottom);
            indicatorIvBottom = null;
        }
    }

    private boolean getShowIndicatorInternal() {
        return showIndicator && isPullToRefreshEnabled();
    }

    private boolean isFirstItemVisible() {
        final Adapter adapter = refreshableView.getAdapter();

        if (null == adapter || adapter.isEmpty()) {
            if (DEBUG) {
                Log.d(LOG_TAG, "isFirstItemVisible. Empty View.");
            }
            return true;

        } else {

            /*
             * This check should really just be:
             * refreshableView.getFirstVisiblePosition() == 0, but PtRListView
             * internally use a HeaderView which messes the positions up. For
             * now we'll just add one to account for it and rely on the inner
             * condition which checks getTop().
             */
            if (refreshableView.getFirstVisiblePosition() <= 1) {
                final View firstVisibleChild = refreshableView.getChildAt(0);
                if (firstVisibleChild != null) {
                    return firstVisibleChild.getTop() >= refreshableView.getTop();
                }
            }
        }

        return false;
    }

    private boolean isLastItemVisible() {
        final Adapter adapter = refreshableView.getAdapter();

        if (null == adapter || adapter.isEmpty()) {
            if (DEBUG) {
                Log.d(LOG_TAG, "isLastItemVisible. Empty View.");
            }
            return true;
        } else {
            final int lastItemPosition = refreshableView.getCount() - 1;
            final int lastVisiblePosition = refreshableView.getLastVisiblePosition();

            if (DEBUG) {
                Log.d(LOG_TAG, "isLastItemVisible. Last Item Position: " + lastItemPosition + " Last Visible Pos: "
                        + lastVisiblePosition);
            }

            /*
             * This check should really just be: lastVisiblePosition ==
             * lastItemPosition, but PtRListView internally uses a FooterView
             * which messes the positions up. For me we'll just subtract one to
             * account for it and rely on the inner condition which checks
             * getBottom().
             */
            if (lastVisiblePosition >= lastItemPosition - 1) {
                final int childIndex = lastVisiblePosition - refreshableView.getFirstVisiblePosition();
                final View lastVisibleChild = refreshableView.getChildAt(childIndex);
                if (lastVisibleChild != null) {
                    return lastVisibleChild.getBottom() <= refreshableView.getBottom();
                }
            }
        }

        return false;
    }

    private void removeIndicatorViews() {
        if (null != indicatorIvTop) {
            getRefreshableViewWrapper().removeView(indicatorIvTop);
            indicatorIvTop = null;
        }

        if (null != indicatorIvBottom) {
            getRefreshableViewWrapper().removeView(indicatorIvBottom);
            indicatorIvBottom = null;
        }
    }

    private void updateIndicatorViewsVisibility() {
        if (null != indicatorIvTop) {
            if (!isRefreshing() && isReadyForPullStart()) {
                if (!indicatorIvTop.isVisible()) {
                    indicatorIvTop.show();
                }
            } else {
                if (indicatorIvTop.isVisible()) {
                    indicatorIvTop.hide();
                }
            }
        }

        if (null != indicatorIvBottom) {
            if (!isRefreshing() && isReadyForPullEnd()) {
                if (!indicatorIvBottom.isVisible()) {
                    indicatorIvBottom.show();
                }
            } else {
                if (indicatorIvBottom.isVisible()) {
                    indicatorIvBottom.hide();
                }
            }
        }
    }
}
