package com.lance.pulltorefresh;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import java.util.HashSet;

public class LoadingLayoutProxy implements ILoadingLayout {

    private final HashSet<LoadingLayoutBase> loadingLayouts;

    public LoadingLayoutProxy() {
        loadingLayouts = new HashSet<>();
    }

    /**
     * This allows you to add extra LoadingLayout instances to this proxy. This
     * is only necessary if you keep your own instances, and want to have them
     * included in any
     * {@link PullToRefreshBase#createLoadingLayoutProxy(boolean, boolean)
     * createLoadingLayoutProxy(...)} calls.
     *
     * @param layout - LoadingLayout to have included.
     */
    public void addLayout(LoadingLayoutBase layout) {
        if (null != layout) {
            loadingLayouts.add(layout);
        }
    }

    @Override
    public void setLastUpdatedLabel(CharSequence label) {
        for (LoadingLayoutBase layout : loadingLayouts) {
            layout.setLastUpdatedLabel(label);
        }
    }

    @Override
    public void setLoadingDrawable(Drawable drawable) {
        for (LoadingLayoutBase layout : loadingLayouts) {
            layout.setLoadingDrawable(drawable);
        }
    }

    @Override
    public void setRefreshingLabel(CharSequence refreshingLabel) {
        for (LoadingLayoutBase layout : loadingLayouts) {
            layout.setRefreshingLabel(refreshingLabel);
        }
    }

    @Override
    public void setPullLabel(CharSequence label) {
        for (LoadingLayoutBase layout : loadingLayouts) {
            layout.setPullLabel(label);
        }
    }

    @Override
    public void setReleaseLabel(CharSequence label) {
        for (LoadingLayoutBase layout : loadingLayouts) {
            layout.setReleaseLabel(label);
        }
    }

    public void setTextTypeface(Typeface tf) {
        for (LoadingLayoutBase layout : loadingLayouts) {
            layout.setTextTypeface(tf);
        }
    }
}
