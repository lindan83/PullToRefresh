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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.lance.pulltorefresh.internal.FlipLoadingLayout;
import com.lance.pulltorefresh.internal.RotateLoadingLayout;
import com.lance.pulltorefresh.internal.Utils;
import com.lance.pulltorefresh.internal.ViewCompat;

public abstract class PullToRefreshBase<T extends View> extends LinearLayout implements IPullToRefresh<T> {

    // ===========================================================
    // Constants
    // ===========================================================

    public static final boolean DEBUG = false;

    static final boolean USE_HW_LAYERS = false;

    public static final String LOG_TAG = "PullToRefresh";

    static final float FRICTION = 2.0f;

    public static final int SMOOTH_SCROLL_DURATION_MS = 200;
    public static final int SMOOTH_SCROLL_LONG_DURATION_MS = 325;
    static final int DEMO_SCROLL_INTERVAL = 225;

    static final String STATE_STATE = "ptr_state";
    static final String STATE_MODE = "ptr_mode";
    static final String STATE_CURRENT_MODE = "ptr_current_mode";
    static final String STATE_SCROLLING_REFRESHING_ENABLED = "ptr_disable_scrolling";
    static final String STATE_SHOW_REFRESHING_VIEW = "ptr_show_refreshing_view";
    static final String STATE_SUPER = "ptr_super";

    // ===========================================================
    // Fields
    // ===========================================================

    private int touchSlop;
    private float lastMotionX, lastMotionY;
    private float initialMotionX, initialMotionY;

    private boolean isBeingDragged;
    private State state = State.RESET;
    private Mode mode = Mode.getDefault();

    private Mode currentMode;
    protected T refreshableView;
    private FrameLayout refreshableViewWrapper;

    private boolean showViewWhileRefreshing = true;
    private boolean scrollingWhileRefreshingEnabled = false;
    private boolean filterTouchEvents = true;
    private boolean overScrollEnabled = true;
    private boolean layoutVisibilityChangesEnabled = true;
    private boolean hasPullDownFriction = true;
    private boolean hasPullUpFriction = true;

    private Interpolator scrollAnimationInterpolator;
    private AnimationStyle loadingAnimationStyle = AnimationStyle.getDefault();

    protected LoadingLayoutBase headerLayout;
    protected LoadingLayoutBase footerLayout;

    private OnRefreshListener<T> onRefreshListener;
    private OnRefreshListener2<T> onRefreshListener2;
    private OnPullEventListener<T> onPullEventListener;

    private SmoothScrollRunnable currentSmoothScrollRunnable;

    // ===========================================================
    // Constructors
    // ===========================================================

    public PullToRefreshBase(Context context) {
        super(context);
        init(context, null);
    }

    public PullToRefreshBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PullToRefreshBase(Context context, Mode mode) {
        super(context);
        this.mode = mode;
        init(context, null);
    }

    public PullToRefreshBase(Context context, Mode mode, AnimationStyle animStyle) {
        super(context);
        this.mode = mode;
        loadingAnimationStyle = animStyle;
        init(context, null);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (DEBUG) {
            Log.d(LOG_TAG, "addView: " + child.getClass().getSimpleName());
        }

        final T refreshableView = getRefreshableView();

        if (refreshableView instanceof ViewGroup) {
            ((ViewGroup) refreshableView).addView(child, index, params);
        } else {
            throw new UnsupportedOperationException("Refreshable View is not a ViewGroup so can't addView");
        }
    }

    @Override
    public final boolean demo() {
        if (mode.showHeaderLoadingLayout() && isReadyForPullStart()) {
            smoothScrollToAndBack(-getHeaderSize() * 2);
            return true;
        } else if (mode.showFooterLoadingLayout() && isReadyForPullEnd()) {
            smoothScrollToAndBack(getFooterSize() * 2);
            return true;
        }
        return false;
    }

    @Override
    public final Mode getCurrentMode() {
        return currentMode;
    }

    @Override
    public final boolean getFilterTouchEvents() {
        return filterTouchEvents;
    }

    @Override
    public final ILoadingLayout getLoadingLayoutProxy() {
        return getLoadingLayoutProxy(true, true);
    }

    @Override
    public final ILoadingLayout getLoadingLayoutProxy(boolean includeStart, boolean includeEnd) {
        return createLoadingLayoutProxy(includeStart, includeEnd);
    }

    @Override
    public final Mode getMode() {
        return mode;
    }

    @Override
    public final T getRefreshableView() {
        return refreshableView;
    }

    @Override
    public final boolean getShowViewWhileRefreshing() {
        return showViewWhileRefreshing;
    }

    @Override
    public final State getState() {
        return state;
    }

    @Override
    public final boolean isPullToRefreshEnabled() {
        return mode.permitsPullToRefresh();
    }

    @Override
    public final boolean isPullToRefreshOverScrollEnabled() {
        return overScrollEnabled && OverScrollHelper.isAndroidOverScrollEnabled(refreshableView);
    }

    @Override
    public final boolean isRefreshing() {
        return state == State.REFRESHING || state == State.MANUAL_REFRESHING;
    }

    @Override
    public final boolean isScrollingWhileRefreshingEnabled() {
        return scrollingWhileRefreshingEnabled;
    }

    @Override
    public final boolean onInterceptTouchEvent(MotionEvent event) {
        if (!isPullToRefreshEnabled()) {
            return false;
        }

        final int action = event.getAction();

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            isBeingDragged = false;
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN && isBeingDragged) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                // If we're refreshing, and the flag is set. Eat all MOVE events
                if (!scrollingWhileRefreshingEnabled && isRefreshing()) {
                    return true;
                }

                if (isReadyForPull()) {
                    final float y = event.getY(), x = event.getX();
                    final float diff, oppositeDiff, absDiff;

                    // We need to use the correct values, based on scroll
                    // direction
                    switch (getPullToRefreshScrollDirection()) {
                        case HORIZONTAL:
                            diff = x - lastMotionX;
                            oppositeDiff = y - lastMotionY;
                            break;
                        case VERTICAL:
                        default:
                            diff = y - lastMotionY;
                            oppositeDiff = x - lastMotionX;
                            break;
                    }
                    absDiff = Math.abs(diff);

                    if (absDiff > touchSlop && (!filterTouchEvents || absDiff > Math.abs(oppositeDiff))) {
                        if (mode.showHeaderLoadingLayout() && diff >= 1f && isReadyForPullStart()) {
                            lastMotionY = y;
                            lastMotionX = x;
                            isBeingDragged = true;
                            if (mode == Mode.BOTH) {
                                currentMode = Mode.PULL_FROM_START;
                            }
                        } else if (mode.showFooterLoadingLayout() && diff <= -1f && isReadyForPullEnd()) {
                            lastMotionY = y;
                            lastMotionX = x;
                            isBeingDragged = true;
                            if (mode == Mode.BOTH) {
                                currentMode = Mode.PULL_FROM_END;
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                if (isReadyForPull()) {
                    lastMotionY = initialMotionY = event.getY();
                    lastMotionX = initialMotionX = event.getX();
                    isBeingDragged = false;
                }
                break;
        }
        return isBeingDragged;
    }

    @Override
    public final void onRefreshComplete() {
        if (isRefreshing()) {
            setState(State.RESET);
        }
    }

    @Override
    public final boolean onTouchEvent(MotionEvent event) {

        if (!isPullToRefreshEnabled()) {
            return false;
        }

        // If we're refreshing, and the flag is set. Eat the event
        if (!scrollingWhileRefreshingEnabled && isRefreshing()) {
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (isBeingDragged) {
                    lastMotionY = event.getY();
                    lastMotionX = event.getX();
                    pullEvent();
                    return true;
                }
                break;

            case MotionEvent.ACTION_DOWN:
                if (isReadyForPull()) {
                    lastMotionY = initialMotionY = event.getY();
                    lastMotionX = initialMotionX = event.getX();
                    return true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isBeingDragged) {
                    isBeingDragged = false;

                    if (state == State.RELEASE_TO_REFRESH
                            && (null != onRefreshListener || null != onRefreshListener2)) {
                        setState(State.REFRESHING, true);
                        return true;
                    }

                    // If we're already refreshing, just scroll back to the top
                    if (isRefreshing()) {
                        smoothScrollTo(0);
                        return true;
                    }

                    // If we haven't returned by here, then we're not in a state
                    // to pull, so just reset
                    setState(State.RESET);

                    return true;
                }
                break;
        }
        return false;
    }

    public final void setScrollingWhileRefreshingEnabled(boolean allowScrollingWhileRefreshing) {
        scrollingWhileRefreshingEnabled = allowScrollingWhileRefreshing;
    }

    @Override
    public final void setFilterTouchEvents(boolean filterEvents) {
        filterTouchEvents = filterEvents;
    }

    /**
     * @param label label
     */
    public void setLastUpdatedLabel(CharSequence label) {
        getLoadingLayoutProxy().setLastUpdatedLabel(label);
    }

    /**
     * @param drawable drawable
     */
    public void setLoadingDrawable(Drawable drawable) {
        getLoadingLayoutProxy().setLoadingDrawable(drawable);
    }

    /**
     * @param drawable drawable
     * @param mode     mode
     */
    public void setLoadingDrawable(Drawable drawable, Mode mode) {
        getLoadingLayoutProxy(mode.showHeaderLoadingLayout(), mode.showFooterLoadingLayout()).setLoadingDrawable(
                drawable);
    }

    @Override
    public void setLongClickable(boolean longClickable) {
        getRefreshableView().setLongClickable(longClickable);
    }

    @Override
    public final void setMode(Mode mode) {
        if (mode != this.mode) {
            if (DEBUG) {
                Log.d(LOG_TAG, "Setting mode to: " + mode);
            }
            this.mode = mode;
            updateUIForMode();
        }
    }

    public void setOnPullEventListener(OnPullEventListener<T> listener) {
        onPullEventListener = listener;
    }

    @Override
    public final void setOnRefreshListener(OnRefreshListener<T> listener) {
        onRefreshListener = listener;
        onRefreshListener2 = null;
    }

    @Override
    public final void setOnRefreshListener(OnRefreshListener2<T> listener) {
        onRefreshListener2 = listener;
        onRefreshListener = null;
    }

    @Override
    public void setHeaderLayout(LoadingLayoutBase headerLayout) {
        this.headerLayout = headerLayout;
        updateUIForMode();
    }

    @Override
    public void setFooterLayout(LoadingLayoutBase footerLayout) {
        this.footerLayout = footerLayout;
        updateUIForMode();
    }

    @Override
    public void setSecondFooterLayout(View secondFooterLayout) {
    }

    /**
     * @param pullLabel pullLabel
     */
    public void setPullLabel(CharSequence pullLabel) {
        getLoadingLayoutProxy().setPullLabel(pullLabel);
    }

    /**
     * @param mode      mode
     * @param pullLabel pullLabel
     */
    public void setPullLabel(CharSequence pullLabel, Mode mode) {
        getLoadingLayoutProxy(mode.showHeaderLoadingLayout(), mode.showFooterLoadingLayout()).setPullLabel(pullLabel);
    }

    /**
     * @param enable Whether Pull-To-Refresh should be used
     */
    public final void setPullToRefreshEnabled(boolean enable) {
        setMode(enable ? Mode.getDefault() : Mode.DISABLED);
    }

    @Override
    public final void setPullToRefreshOverScrollEnabled(boolean enabled) {
        overScrollEnabled = enabled;
    }

    @Override
    public final void setRefreshing() {
        setRefreshing(true);
    }

    @Override
    public final void setRefreshing(boolean doScroll) {
        if (!isRefreshing()) {
            setState(State.MANUAL_REFRESHING, doScroll);
        }
    }

    @Override
    public void setHasPullDownFriction(boolean hasPullDownFriction) {
        this.hasPullDownFriction = hasPullDownFriction;
    }

    @Override
    public void setHasPullUpFriction(boolean hasPullUpFriction) {
        this.hasPullUpFriction = hasPullUpFriction;
    }

    /**
     * @param refreshingLabel refreshingLabel
     */
    public void setRefreshingLabel(CharSequence refreshingLabel) {
        getLoadingLayoutProxy().setRefreshingLabel(refreshingLabel);
    }

    /**
     * @param mode            mode
     * @param refreshingLabel refreshingLabel
     */
    public void setRefreshingLabel(CharSequence refreshingLabel, Mode mode) {
        getLoadingLayoutProxy(mode.showHeaderLoadingLayout(), mode.showFooterLoadingLayout()).setRefreshingLabel(
                refreshingLabel);
    }

    /**
     * @param releaseLabel releaseLabel
     */
    public void setReleaseLabel(CharSequence releaseLabel) {
        setReleaseLabel(releaseLabel, Mode.BOTH);
    }

    /**
     * @param mode         mode
     * @param releaseLabel releaseLabel
     */
    public void setReleaseLabel(CharSequence releaseLabel, Mode mode) {
        getLoadingLayoutProxy(mode.showHeaderLoadingLayout(), mode.showFooterLoadingLayout()).setReleaseLabel(
                releaseLabel);
    }

    public void setScrollAnimationInterpolator(Interpolator interpolator) {
        scrollAnimationInterpolator = interpolator;
    }

    @Override
    public final void setShowViewWhileRefreshing(boolean showView) {
        showViewWhileRefreshing = showView;
    }

    /**
     * @return Either {@link Orientation#VERTICAL} or
     * {@link Orientation#HORIZONTAL} depending on the scroll direction.
     */
    public abstract Orientation getPullToRefreshScrollDirection();

    final void setState(State state, final boolean... params) {
        this.state = state;
        if (DEBUG) {
            Log.d(LOG_TAG, "State: " + this.state.name());
        }

        switch (this.state) {
            case RESET:
                onReset();
                break;
            case PULL_TO_REFRESH:
                onPullToRefresh();
                break;
            case RELEASE_TO_REFRESH:
                onReleaseToRefresh();
                break;
            case REFRESHING:
            case MANUAL_REFRESHING:
                onRefreshing(params[0]);
                break;
            case OVER_SCROLLING:
                // NO-OP
                break;
        }

        // Call OnPullEventListener
        if (null != onPullEventListener) {
            onPullEventListener.onPullEvent(this, this.state, currentMode);
        }
    }

    /**
     * Used internally for adding view. Need because we override addView to
     * pass-through to the Refreshable View
     *
     * @param child  child
     * @param index  index
     * @param params params
     */
    protected final void addViewInternal(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
    }

    /**
     * Used internally for adding view. Need because we override addView to
     * pass-through to the Refreshable View
     *
     * @param child  child
     * @param params params
     */
    protected final void addViewInternal(View child, ViewGroup.LayoutParams params) {
        super.addView(child, -1, params);
    }

    protected LoadingLayoutBase createLoadingLayout(Context context, Mode mode, TypedArray attrs) {
        LoadingLayoutBase layout = loadingAnimationStyle.createLoadingLayout(context, mode,
                getPullToRefreshScrollDirection(), attrs);
        layout.setVisibility(View.INVISIBLE);
        return layout;
    }

    /**
     * Used internally for {@link #getLoadingLayoutProxy(boolean, boolean)}.
     * Allows derivative classes to include any extra LoadingLayouts.
     *
     * @param includeEnd   includeEnd
     * @param includeStart includeStart
     */
    protected LoadingLayoutProxy createLoadingLayoutProxy(final boolean includeStart, final boolean includeEnd) {
        LoadingLayoutProxy proxy = new LoadingLayoutProxy();

        if (includeStart && mode.showHeaderLoadingLayout()) {
            proxy.addLayout(headerLayout);
        }
        if (includeEnd && mode.showFooterLoadingLayout()) {
            proxy.addLayout(footerLayout);
        }

        return proxy;
    }

    /**
     * This is implemented by derived classes to return the created View. If you
     * need to use a custom View (such as a custom ListView), override this
     * method and return an instance of your custom class.
     * Be sure to set the ID of the view in this method, especially if you're
     * using a ListActivity or ListFragment.
     *
     * @param context Context to create view with
     * @param attrs   AttributeSet from wrapped class. Means that anything you
     *                include in the XML layout declaration will be routed to the
     *                created View
     * @return New instance of the Refreshable View
     */
    protected abstract T createRefreshableView(Context context, AttributeSet attrs);

    protected final void disableLoadingLayoutVisibilityChanges() {
        layoutVisibilityChangesEnabled = false;
    }

    protected final LoadingLayoutBase getFooterLayout() {
        return footerLayout;
    }

    protected final int getFooterSize() {
        return footerLayout.getContentSize();
    }

    protected final LoadingLayoutBase getHeaderLayout() {
        return headerLayout;
    }

    protected final int getHeaderSize() {
        return headerLayout.getContentSize();
    }

    protected int getPullToRefreshScrollDuration() {
        return SMOOTH_SCROLL_DURATION_MS;
    }

    protected int getPullToRefreshScrollDurationLonger() {
        return SMOOTH_SCROLL_LONG_DURATION_MS;
    }

    protected FrameLayout getRefreshableViewWrapper() {
        return refreshableViewWrapper;
    }

    /**
     * Allows Derivative classes to handle the XML Attrs without creating a
     * TypedArray themsevles
     *
     * @param a - TypedArray of PullToRefresh Attributes
     */
    protected void handleStyledAttributes(TypedArray a) {
    }

    /**
     * Implemented by derived class to return whether the View is in a state
     * where the user can Pull to Refresh by scrolling from the end.
     *
     * @return true if the View is currently in the correct state (for example,
     * bottom of a ListView)
     */
    protected abstract boolean isReadyForPullEnd();

    /**
     * Implemented by derived class to return whether the View is in a state
     * where the user can Pull to Refresh by scrolling from the start.
     *
     * @return true if the View is currently the correct state (for example, top
     * of a ListView)
     */
    protected abstract boolean isReadyForPullStart();

    /**
     * Called by {@link #onRestoreInstanceState(Parcelable)} so that derivative
     * classes can handle their saved instance state.
     *
     * @param savedInstanceState - Bundle which contains saved instance state.
     */
    protected void onPtrRestoreInstanceState(Bundle savedInstanceState) {
    }

    /**
     * Called by {@link #onSaveInstanceState()} so that derivative classes can
     * save their instance state.
     *
     * @param saveState - Bundle to be updated with saved state.
     */
    protected void onPtrSaveInstanceState(Bundle saveState) {
    }

    /**
     * Called when the UI has been to be updated to be in the
     * {@link State#PULL_TO_REFRESH} state.
     */
    protected void onPullToRefresh() {
        switch (currentMode) {
            case PULL_FROM_END:
                footerLayout.pullToRefresh();
                break;
            case PULL_FROM_START:
                headerLayout.pullToRefresh();
                break;
            default:
                // NO-OP
                break;
        }
    }

    /**
     * Called when the UI has been to be updated to be in the
     * {@link State#REFRESHING} or {@link State#MANUAL_REFRESHING} state.
     *
     * @param doScroll - Whether the UI should scroll for this event.
     */
    protected void onRefreshing(final boolean doScroll) {
        if (mode.showHeaderLoadingLayout()) {
            headerLayout.refreshing();
        }
        if (mode.showFooterLoadingLayout()) {
            footerLayout.refreshing();
        }

        if (doScroll) {
            if (showViewWhileRefreshing) {

                // Call Refresh Listener when the Scroll has finished
                OnSmoothScrollFinishedListener listener = new OnSmoothScrollFinishedListener() {
                    @Override
                    public void onSmoothScrollFinished() {
                        callRefreshListener();
                    }
                };

                switch (currentMode) {
                    case MANUAL_REFRESH_ONLY:
                    case PULL_FROM_END:
                        smoothScrollTo(getFooterSize(), listener);
                        break;
                    default:
                    case PULL_FROM_START:
                        smoothScrollTo(-getHeaderSize(), listener);
                        break;
                }
            } else {
                smoothScrollTo(0);
            }
        } else {
            // We're not scrolling, so just call Refresh Listener now
            callRefreshListener();
        }
    }

    /**
     * Called when the UI has been to be updated to be in the
     * {@link State#RELEASE_TO_REFRESH} state.
     */
    protected void onReleaseToRefresh() {
        switch (currentMode) {
            case PULL_FROM_END:
                footerLayout.releaseToRefresh();
                break;
            case PULL_FROM_START:
                headerLayout.releaseToRefresh();
                break;
            default:
                // NO-OP
                break;
        }
    }

    /**
     * Called when the UI has been to be updated to be in the
     * {@link State#RESET} state.
     */
    protected void onReset() {
        isBeingDragged = false;
        layoutVisibilityChangesEnabled = true;

        // Always reset both layouts, just in case...
        headerLayout.reset();
        footerLayout.reset();

        smoothScrollTo(0);
    }

    @Override
    protected final void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            setMode(Mode.mapIntToValue(bundle.getInt(STATE_MODE, 0)));
            currentMode = Mode.mapIntToValue(bundle.getInt(STATE_CURRENT_MODE, 0));

            scrollingWhileRefreshingEnabled = bundle.getBoolean(STATE_SCROLLING_REFRESHING_ENABLED, false);
            showViewWhileRefreshing = bundle.getBoolean(STATE_SHOW_REFRESHING_VIEW, true);

            // Let super Restore Itself
            super.onRestoreInstanceState(bundle.getParcelable(STATE_SUPER));

            State viewState = State.mapIntToValue(bundle.getInt(STATE_STATE, 0));
            if (viewState == State.REFRESHING || viewState == State.MANUAL_REFRESHING) {
                setState(viewState, true);
            }

            // Now let derivative classes restore their state
            onPtrRestoreInstanceState(bundle);
            return;
        }

        super.onRestoreInstanceState(state);
    }

    @Override
    protected final Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();

        // Let derivative classes get a chance to save state first, that way we
        // can make sure they don't overrite any of our values
        onPtrSaveInstanceState(bundle);

        bundle.putInt(STATE_STATE, state.getIntValue());
        bundle.putInt(STATE_MODE, mode.getIntValue());
        bundle.putInt(STATE_CURRENT_MODE, currentMode.getIntValue());
        bundle.putBoolean(STATE_SCROLLING_REFRESHING_ENABLED, scrollingWhileRefreshingEnabled);
        bundle.putBoolean(STATE_SHOW_REFRESHING_VIEW, showViewWhileRefreshing);
        bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState());

        return bundle;
    }

    @Override
    protected final void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (DEBUG) {
            Log.d(LOG_TAG, String.format("onSizeChanged. W: %d, H: %d", w, h));
        }

        super.onSizeChanged(w, h, oldw, oldh);

        // We need to update the header/footer when our size changes
        refreshLoadingViewsSize();

        // Update the Refreshable View layout
        refreshRefreshableViewSize(w, h);

        /*
         * As we're currently in a Layout Pass, we need to schedule another one
         * to layout any changes we've made here
         */
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });
    }

    /**
     * Re-measure the Loading Views height, and adjust internal padding as
     * necessary
     */
    protected final void refreshLoadingViewsSize() {
        final int maximumPullScroll = (int) (getMaximumPullScroll() * 1.2f);

        int pLeft = getPaddingLeft();
        int pTop = getPaddingTop();
        int pRight = getPaddingRight();
        int pBottom = getPaddingBottom();

        switch (getPullToRefreshScrollDirection()) {
            case HORIZONTAL:
                if (mode.showHeaderLoadingLayout()) {
                    headerLayout.setWidth(maximumPullScroll);
                    pLeft = -maximumPullScroll;
                } else {
                    pLeft = 0;
                }

                if (mode.showFooterLoadingLayout()) {
                    footerLayout.setWidth(maximumPullScroll);
                    pRight = -maximumPullScroll;
                } else {
                    pRight = 0;
                }
                break;

            case VERTICAL:
                if (mode.showHeaderLoadingLayout()) {
                    headerLayout.setHeight(maximumPullScroll);
                    pTop = -maximumPullScroll;
                } else {
                    pTop = 0;
                }

                if (mode.showFooterLoadingLayout()) {
                    footerLayout.setHeight(maximumPullScroll);
                    pBottom = -maximumPullScroll;
                } else {
                    pBottom = 0;
                }
                break;
        }

        if (DEBUG) {
            Log.d(LOG_TAG, String.format("Setting Padding. L: %d, T: %d, R: %d, B: %d", pLeft, pTop, pRight, pBottom));
        }
        setPadding(pLeft, pTop, pRight, pBottom);
    }

    protected final void refreshRefreshableViewSize(int width, int height) {
        // We need to set the Height of the Refreshable View to the same as
        // this layout
        LayoutParams lp = (LayoutParams) refreshableViewWrapper.getLayoutParams();

        switch (getPullToRefreshScrollDirection()) {
            case HORIZONTAL:
                if (lp.width != width) {
                    lp.width = width;
                    refreshableViewWrapper.requestLayout();
                }
                break;
            case VERTICAL:
                if (lp.height != height) {
                    lp.height = height;
                    refreshableViewWrapper.requestLayout();
                }
                break;
        }
    }

    /**
     * Helper method which just calls scrollTo() in the correct scrolling
     * direction.
     *
     * @param value - New Scroll value
     */
    protected final void setHeaderScroll(int value) {
        if (DEBUG) {
            Log.d(LOG_TAG, "setHeaderScroll: " + value);
        }

        // Clamp value to with pull scroll range
        final int maximumPullScroll = getMaximumPullScroll();
        value = Math.min(maximumPullScroll, Math.max(-maximumPullScroll, value));

        if (layoutVisibilityChangesEnabled) {
            if (value < 0) {
                headerLayout.setVisibility(View.VISIBLE);
            } else if (value > 0) {
                footerLayout.setVisibility(View.VISIBLE);
            } else {
                headerLayout.setVisibility(View.INVISIBLE);
                footerLayout.setVisibility(View.INVISIBLE);
            }
        }

        if (USE_HW_LAYERS) {
            /*
             * Use a Hardware Layer on the Refreshable View if we've scrolled at
             * all. We don't use them on the Header/Footer Views as they change
             * often, which would negate any HW layer performance boost.
             */
            ViewCompat.setLayerType(refreshableViewWrapper, value != 0 ? View.LAYER_TYPE_HARDWARE
                    : View.LAYER_TYPE_NONE);
        }

        switch (getPullToRefreshScrollDirection()) {
            case VERTICAL:
                scrollTo(0, value);
                break;
            case HORIZONTAL:
                scrollTo(value, 0);
                break;
        }
    }

    /**
     * Smooth Scroll to position using the default duration of
     * {@value #SMOOTH_SCROLL_DURATION_MS} ms.
     *
     * @param scrollValue - Position to scroll to
     */
    protected final void smoothScrollTo(int scrollValue) {
        smoothScrollTo(scrollValue, getPullToRefreshScrollDuration());
    }

    /**
     * Smooth Scroll to position using the default duration of
     * {@value #SMOOTH_SCROLL_DURATION_MS} ms.
     *
     * @param scrollValue - Position to scroll to
     * @param listener    - Listener for scroll
     */
    protected final void smoothScrollTo(int scrollValue, OnSmoothScrollFinishedListener listener) {
        smoothScrollTo(scrollValue, getPullToRefreshScrollDuration(), 0, listener);
    }

    /**
     * Smooth Scroll to position using the longer default duration of
     * {@value #SMOOTH_SCROLL_LONG_DURATION_MS} ms.
     *
     * @param scrollValue - Position to scroll to
     */
    protected final void smoothScrollToLonger(int scrollValue) {
        smoothScrollTo(scrollValue, getPullToRefreshScrollDurationLonger());
    }

    /**
     * Updates the View State when the mode has been set. This does not do any
     * checking that the mode is different to current state so always updates.
     */
    protected void updateUIForMode() {
        // We need to use the correct LayoutParam values, based on scroll
        // direction
        final LayoutParams lp = getLoadingLayoutLayoutParams();

        // Remove Header, and then add Header Loading View again if needed
        if (this == headerLayout.getParent()) {
            removeView(headerLayout);
        }
        if (mode.showHeaderLoadingLayout()) {
            addViewInternal(headerLayout, 0, lp);
        }

        // Remove Footer, and then add Footer Loading View again if needed
        if (this == footerLayout.getParent()) {
            removeView(footerLayout);
        }
        if (mode.showFooterLoadingLayout()) {
            addViewInternal(footerLayout, lp);
        }

        // Hide Loading Views
        refreshLoadingViewsSize();

        // If we're not using Mode.BOTH, set currentMode to mode, otherwise
        // set it to pull down
        currentMode = (mode != Mode.BOTH) ? mode : Mode.PULL_FROM_START;
    }

    private void addRefreshableView(Context context, T refreshableView) {
        refreshableViewWrapper = new FrameLayout(context);
        refreshableViewWrapper.addView(refreshableView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        addViewInternal(refreshableViewWrapper, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
    }

    private void callRefreshListener() {
        if (null != onRefreshListener) {
            onRefreshListener.onRefresh(this);
        } else if (null != onRefreshListener2) {
            if (currentMode == Mode.PULL_FROM_START) {
                onRefreshListener2.onPullDownToRefresh(this);
            } else if (currentMode == Mode.PULL_FROM_END) {
                onRefreshListener2.onPullUpToRefresh(this);
            }
        }
    }

    private void init(Context context, AttributeSet attrs) {
        switch (getPullToRefreshScrollDirection()) {
            case HORIZONTAL:
                setOrientation(LinearLayout.HORIZONTAL);
                break;
            case VERTICAL:
            default:
                setOrientation(LinearLayout.VERTICAL);
                break;
        }

        setGravity(Gravity.CENTER);

        ViewConfiguration config = ViewConfiguration.get(context);
        touchSlop = config.getScaledTouchSlop();

        // Styleables from XML
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullToRefresh);

        if (a.hasValue(R.styleable.PullToRefresh_ptrMode)) {
            mode = Mode.mapIntToValue(a.getInteger(R.styleable.PullToRefresh_ptrMode, 0));
        }

        if (a.hasValue(R.styleable.PullToRefresh_ptrAnimationStyle)) {
            loadingAnimationStyle = AnimationStyle.mapIntToValue(a.getInteger(
                    R.styleable.PullToRefresh_ptrAnimationStyle, 0));
        }

        // Refreshable View
        // By passing the attrs, we can add ListView/GridView params via XML
        refreshableView = createRefreshableView(context, attrs);
        addRefreshableView(context, refreshableView);

        // We need to create now layouts now
        headerLayout = createLoadingLayout(context, Mode.PULL_FROM_START, a);
        footerLayout = createLoadingLayout(context, Mode.PULL_FROM_END, a);

        /*
         * Styleables from XML
         */
        if (a.hasValue(R.styleable.PullToRefresh_ptrRefreshableViewBackground)) {
            Drawable background = a.getDrawable(R.styleable.PullToRefresh_ptrRefreshableViewBackground);
            if (null != background) {
                refreshableView.setBackgroundDrawable(background);
            }
        } else if (a.hasValue(R.styleable.PullToRefresh_ptrAdapterViewBackground)) {
            Utils.warnDeprecation("ptrAdapterViewBackground", "ptrRefreshableViewBackground");
            Drawable background = a.getDrawable(R.styleable.PullToRefresh_ptrAdapterViewBackground);
            if (null != background) {
                refreshableView.setBackgroundDrawable(background);
            }
        }

        if (a.hasValue(R.styleable.PullToRefresh_ptrOverScroll)) {
            overScrollEnabled = a.getBoolean(R.styleable.PullToRefresh_ptrOverScroll, true);
        }

        if (a.hasValue(R.styleable.PullToRefresh_ptrScrollingWhileRefreshingEnabled)) {
            scrollingWhileRefreshingEnabled = a.getBoolean(
                    R.styleable.PullToRefresh_ptrScrollingWhileRefreshingEnabled, false);
        }

        // Let the derivative classes have a go at handling attributes, then
        // recycle them...
        handleStyledAttributes(a);
        a.recycle();

        // Finally update the UI for the modes
        updateUIForMode();
    }

    private boolean isReadyForPull() {
        switch (mode) {
            case PULL_FROM_START:
                return isReadyForPullStart();
            case PULL_FROM_END:
                return isReadyForPullEnd();
            case BOTH:
                return isReadyForPullEnd() || isReadyForPullStart();
            default:
                return false;
        }
    }

    /**
     * Actions a Pull Event
     * return true if the Event has been handled, false if there has been no
     * change
     */
    private void pullEvent() {
        final int newScrollValue;
        final int itemDimension;
        final float initialMotionValue, lastMotionValue;

        switch (getPullToRefreshScrollDirection()) {
            case HORIZONTAL:
                initialMotionValue = initialMotionX;
                lastMotionValue = lastMotionX;
                break;
            case VERTICAL:
            default:
                initialMotionValue = initialMotionY;
                lastMotionValue = lastMotionY;
                break;
        }

        switch (currentMode) {
            case PULL_FROM_END:
                if (hasPullUpFriction) {
                    newScrollValue = Math.round(Math.max(initialMotionValue - lastMotionValue, 0) / FRICTION);
                } else {
                    newScrollValue = Math.round(Math.max(initialMotionValue - lastMotionValue, 0));
                }
                itemDimension = getFooterSize();
                break;
            case PULL_FROM_START:
            default:
                if (hasPullDownFriction) {
                    newScrollValue = Math.round(Math.min(initialMotionValue - lastMotionValue, 0) / FRICTION);
                } else {
                    newScrollValue = Math.round(Math.min(initialMotionValue - lastMotionValue, 0));
                }
                itemDimension = getHeaderSize();
                break;
        }

        setHeaderScroll(newScrollValue);

        if (newScrollValue != 0 && !isRefreshing()) {
            float scale = Math.abs(newScrollValue) / (float) itemDimension;
            switch (currentMode) {
                case PULL_FROM_END:
                    footerLayout.onPull(scale);
                    break;
                case PULL_FROM_START:
                default:
                    headerLayout.onPull(scale);
                    break;
            }

            if (state != State.PULL_TO_REFRESH && itemDimension >= Math.abs(newScrollValue)) {
                setState(State.PULL_TO_REFRESH);
            } else if (state == State.PULL_TO_REFRESH && itemDimension < Math.abs(newScrollValue)) {
                setState(State.RELEASE_TO_REFRESH);
            }
        }
    }

    private LayoutParams getLoadingLayoutLayoutParams() {
        switch (getPullToRefreshScrollDirection()) {
            case HORIZONTAL:
                return new LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.MATCH_PARENT);
            case VERTICAL:
            default:
                return new LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT);
        }
    }

    private int getMaximumPullScroll() {
        switch (getPullToRefreshScrollDirection()) {
            case HORIZONTAL:
                return Math.round(getWidth() / FRICTION);
            case VERTICAL:
            default:
                return Math.round(getHeight() / FRICTION);
        }
    }

    /**
     * Smooth Scroll to position using the specific duration
     *
     * @param scrollValue - Position to scroll to
     * @param duration    - Duration of animation in milliseconds
     */
    private void smoothScrollTo(int scrollValue, long duration) {
        smoothScrollTo(scrollValue, duration, 0, null);
    }

    private void smoothScrollTo(int newScrollValue, long duration, long delayMillis,
                                OnSmoothScrollFinishedListener listener) {
        if (null != currentSmoothScrollRunnable) {
            currentSmoothScrollRunnable.stop();
        }

        final int oldScrollValue;
        switch (getPullToRefreshScrollDirection()) {
            case HORIZONTAL:
                oldScrollValue = getScrollX();
                break;
            case VERTICAL:
            default:
                oldScrollValue = getScrollY();
                break;
        }

        if (oldScrollValue != newScrollValue) {
            if (null == scrollAnimationInterpolator) {
                // Default interpolator is a Decelerate Interpolator
                scrollAnimationInterpolator = new DecelerateInterpolator();
            }
            currentSmoothScrollRunnable = new SmoothScrollRunnable(oldScrollValue, newScrollValue, duration, listener);

            if (delayMillis > 0) {
                postDelayed(currentSmoothScrollRunnable, delayMillis);
            } else {
                post(currentSmoothScrollRunnable);
            }
        }
    }

    private void smoothScrollToAndBack(int y) {
        smoothScrollTo(y, SMOOTH_SCROLL_DURATION_MS, 0, new OnSmoothScrollFinishedListener() {

            @Override
            public void onSmoothScrollFinished() {
                smoothScrollTo(0, SMOOTH_SCROLL_DURATION_MS, DEMO_SCROLL_INTERVAL, null);
            }
        });
    }

    public enum AnimationStyle {
        /**
         * This is the default for Android-PullToRefresh. Allows you to use any
         * drawable, which is automatically rotated and used as a Progress Bar.
         */
        ROTATE,

        /**
         * This is the old default, and what is commonly used on iOS. Uses an
         * arrow image which flips depending on where the user has scrolled.
         */
        FLIP;

        static AnimationStyle getDefault() {
            return ROTATE;
        }

        /**
         * Maps an int to a specific mode. This is needed when saving state, or
         * inflating the view from XML where the mode is given through a attr
         * int.
         *
         * @param modeInt - int to map a Mode to
         * @return Mode that modeInt maps to, or ROTATE by default.
         */
        static AnimationStyle mapIntToValue(int modeInt) {
            switch (modeInt) {
                case 0x0:
                default:
                    return ROTATE;
                case 0x1:
                    return FLIP;
            }
        }

        LoadingLayoutBase createLoadingLayout(Context context, Mode mode, Orientation scrollDirection, TypedArray attrs) {
            switch (this) {
                case ROTATE:
                default:
                    return new RotateLoadingLayout(context, mode, scrollDirection, attrs);
                case FLIP:
                    return new FlipLoadingLayout(context, mode, scrollDirection, attrs);
            }
        }
    }

    public enum Mode {
        /**
         * Disable all Pull-to-Refresh gesture and Refreshing handling
         */
        DISABLED(0x0),

        /**
         * Only allow the user to Pull from the start of the Refreshable View to
         * refresh. The start is either the Top or Left, depending on the
         * scrolling direction.
         */
        PULL_FROM_START(0x1),

        /**
         * Only allow the user to Pull from the end of the Refreshable View to
         * refresh. The start is either the Bottom or Right, depending on the
         * scrolling direction.
         */
        PULL_FROM_END(0x2),

        /**
         * Allow the user to both Pull from the start, from the end to refresh.
         */
        BOTH(0x3),

        /**
         * Disables Pull-to-Refresh gesture handling, but allows manually
         * setting the Refresh state via
         * {@link PullToRefreshBase#setRefreshing() setRefreshing()}.
         */
        MANUAL_REFRESH_ONLY(0x4);

        /**
         * Maps an int to a specific mode. This is needed when saving state, or
         * inflating the view from XML where the mode is given through a attr
         * int.
         *
         * @param modeInt - int to map a Mode to
         * @return Mode that modeInt maps to, or PULL_FROM_START by default.
         */
        static Mode mapIntToValue(final int modeInt) {
            for (Mode value : Mode.values()) {
                if (modeInt == value.getIntValue()) {
                    return value;
                }
            }

            // If not, return default
            return getDefault();
        }

        static Mode getDefault() {
            return PULL_FROM_START;
        }

        private int intValue;

        // The modeInt values need to match those from attrs.xml
        Mode(int modeInt) {
            intValue = modeInt;
        }

        /**
         * @return true if the mode permits Pull-to-Refresh
         */
        boolean permitsPullToRefresh() {
            return !(this == DISABLED || this == MANUAL_REFRESH_ONLY);
        }

        /**
         * @return true if this mode wants the Loading Layout Header to be shown
         */
        public boolean showHeaderLoadingLayout() {
            return this == PULL_FROM_START || this == BOTH;
        }

        /**
         * @return true if this mode wants the Loading Layout Footer to be shown
         */
        public boolean showFooterLoadingLayout() {
            return this == PULL_FROM_END || this == BOTH || this == MANUAL_REFRESH_ONLY;
        }

        int getIntValue() {
            return intValue;
        }

    }

    // ===========================================================
    // Inner, Anonymous Classes, and Enumerations
    // ===========================================================

    /**
     * Simple Listener that allows you to be notified when the user has scrolled
     * to the end of the AdapterView. See (
     * {@link PullToRefreshAdapterViewBase#setOnLastItemVisibleListener}.
     *
     * @author Chris Banes
     */
    public interface OnLastItemVisibleListener {

        /**
         * Called when the user has scrolled to the end of the list
         */
        void onLastItemVisible();

    }

    /**
     * Listener that allows you to be notified when the user has started or
     * finished a touch event. Useful when you want to append extra UI events
     * (such as sounds). See (
     * {@link PullToRefreshAdapterViewBase#setOnPullEventListener}.
     *
     * @author Chris Banes
     */
    public interface OnPullEventListener<V extends View> {

        /**
         * Called when the internal state has been changed, usually by the user
         * pulling.
         *
         * @param refreshView - View which has had it's state change.
         * @param state       - The new state of View.
         * @param direction   - One of {@link Mode#PULL_FROM_START} or
         *                    {@link Mode#PULL_FROM_END} depending on which direction
         *                    the user is pulling. Only useful when <var>state</var> is
         *                    {@link State#PULL_TO_REFRESH} or
         *                    {@link State#RELEASE_TO_REFRESH}.
         */
        void onPullEvent(final PullToRefreshBase<V> refreshView, State state, Mode direction);

    }

    /**
     * Simple Listener to listen for any callbacks to Refresh.
     *
     * @author Chris Banes
     */
    public interface OnRefreshListener<V extends View> {

        /**
         * onRefresh will be called for both a Pull from start, and Pull from
         * end
         *
         * @param refreshView refreshView
         */
        void onRefresh(final PullToRefreshBase<V> refreshView);

    }

    /**
     * An advanced version of the Listener to listen for callbacks to Refresh.
     * This listener is different as it allows you to differentiate between Pull
     * Ups, and Pull Downs.
     *
     * @author Chris Banes
     */
    public interface OnRefreshListener2<V extends View> {
        // These methods need renaming to START/END rather than DOWN/UP

        /**
         * onPullDownToRefresh will be called only when the user has Pulled from
         * the start, and released.
         *
         * @param refreshView refreshView
         */
        void onPullDownToRefresh(final PullToRefreshBase<V> refreshView);

        /**
         * onPullUpToRefresh will be called only when the user has Pulled from
         * the end, and released.
         *
         * @param refreshView refreshView
         */
        void onPullUpToRefresh(final PullToRefreshBase<V> refreshView);

    }

    public enum Orientation {
        VERTICAL, HORIZONTAL
    }

    public enum State {

        /**
         * When the UI is in a state which means that user is not interacting
         * with the Pull-to-Refresh function.
         */
        RESET(0x0),

        /**
         * When the UI is being pulled by the user, but has not been pulled far
         * enough so that it refreshes when released.
         */
        PULL_TO_REFRESH(0x1),

        /**
         * When the UI is being pulled by the user, and <strong>has</strong>
         * been pulled far enough so that it will refresh when released.
         */
        RELEASE_TO_REFRESH(0x2),

        /**
         * When the UI is currently refreshing, caused by a pull gesture.
         */
        REFRESHING(0x8),

        /**
         * When the UI is currently refreshing, caused by a call to
         * {@link PullToRefreshBase#setRefreshing() setRefreshing()}.
         */
        MANUAL_REFRESHING(0x9),

        /**
         * When the UI is currently overscrolling, caused by a fling on the
         * Refreshable View.
         */
        OVER_SCROLLING(0x10);

        /**
         * Maps an int to a specific state. This is needed when saving state.
         *
         * @param stateInt - int to map a State to
         * @return State that stateInt maps to
         */
        static State mapIntToValue(final int stateInt) {
            for (State value : State.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }

            // If not, return default
            return RESET;
        }

        private int intValue;

        State(int intValue) {
            this.intValue = intValue;
        }

        int getIntValue() {
            return intValue;
        }
    }

    final class SmoothScrollRunnable implements Runnable {
        private final Interpolator interpolator;
        private final int scrollToY;
        private final int scrollFromY;
        private final long duration;
        private OnSmoothScrollFinishedListener listener;

        private boolean continueRunning = true;
        private long startTime = -1;
        private int currentY = -1;

        SmoothScrollRunnable(int fromY, int toY, long duration, OnSmoothScrollFinishedListener listener) {
            scrollFromY = fromY;
            scrollToY = toY;
            interpolator = scrollAnimationInterpolator;
            this.duration = duration;
            this.listener = listener;
        }

        @Override
        public void run() {

            /*
             * Only set startTime if this is the first time we're starting,
             * else actually calculate the Y delta
             */
            if (startTime == -1) {
                startTime = System.currentTimeMillis();
            } else {

                /*
                 * We do do all calculations in long to reduce software float
                 * calculations. We use 1000 as it gives us good accuracy and
                 * small rounding errors
                 */
                long normalizedTime = (1000 * (System.currentTimeMillis() - startTime)) / duration;
                normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

                final int deltaY = Math.round((scrollFromY - scrollToY)
                        * interpolator.getInterpolation(normalizedTime / 1000f));
                currentY = scrollFromY - deltaY;
                setHeaderScroll(currentY);
            }

            // If we're not at the target Y, keep going...
            if (continueRunning && scrollToY != currentY) {
                ViewCompat.postOnAnimation(PullToRefreshBase.this, this);
            } else {
                if (null != listener) {
                    listener.onSmoothScrollFinished();
                }
            }
        }

        void stop() {
            continueRunning = false;
            removeCallbacks(this);
        }
    }

    interface OnSmoothScrollFinishedListener {
        void onSmoothScrollFinished();
    }
}
