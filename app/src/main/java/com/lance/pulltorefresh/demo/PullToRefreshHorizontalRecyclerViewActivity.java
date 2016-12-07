package com.lance.pulltorefresh.demo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Toast;

import com.lance.common.recyclerview.adapter.AbstractRecyclerViewAdapter;
import com.lance.common.recyclerview.adapter.CommonRecyclerViewAdapter;
import com.lance.common.recyclerview.adapter.base.CommonRecyclerViewHolder;
import com.lance.pulltorefresh.PullToRefreshBase;
import com.lance.pulltorefresh.PullToRefreshHorizontalRecyclerView;
import com.lance.pulltorefresh.demo.view.HorizontalLoadingFooterLayout;
import com.lance.pulltorefresh.demo.view.HorizontalRefreshingHeaderLayout;
import com.lance.pulltorefresh.extras.SoundPullEventListener;

import java.util.Arrays;
import java.util.LinkedList;

public class PullToRefreshHorizontalRecyclerViewActivity extends AppCompatActivity {
    private static final String TAG = "RecyclerView";
    private PullToRefreshHorizontalRecyclerView mPullRefreshRecycler;
    private CommonRecyclerViewAdapter<String> mAdapter;
    private LinkedList<String> mData;
    private String[] mStrings = {"Abbaye de Belloc", "Abbaye du Mont des Cats", "Abertam", "Abondance", "Ackawi",
            "Acorn", "Adelost", "Affidelice au Chablis", "Afuega'l Pitu", "Airag", "Airedale", "Aisy Cendre",
            "Allgauer Emmentaler", "Abbaye de Belloc", "Abbaye du Mont des Cats", "Abertam", "Abondance", "Ackawi",
            "Acorn", "Adelost", "Affidelice au Chablis", "Afuega'l Pitu", "Airag", "Airedale", "Aisy Cendre",
            "Allgauer Emmentaler"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pull_to_refresh_horizontal_recycler_view);

        mPullRefreshRecycler = (PullToRefreshHorizontalRecyclerView) findViewById(R.id.pull_refresh_recycler);
        mPullRefreshRecycler.setMode(PullToRefreshBase.Mode.BOTH);
        //设置自定义刷新头部和加载底部
        mPullRefreshRecycler.setHeaderLayout(new HorizontalRefreshingHeaderLayout(this));
        mPullRefreshRecycler.setFooterLayout(new HorizontalLoadingFooterLayout(this));
        // Set a listener to be invoked when the list should be refreshed.
        mPullRefreshRecycler.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<RecyclerView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<RecyclerView> refreshView) {
                String label = DateUtils.formatDateTime(getApplicationContext(), System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

                // Update the LastUpdatedLabel
                refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
                new PullToRefreshHorizontalRecyclerViewActivity.GetDataTask(true).execute();
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<RecyclerView> refreshView) {
                new PullToRefreshHorizontalRecyclerViewActivity.GetDataTask(false).execute();
            }
        });

        RecyclerView recyclerView = mPullRefreshRecycler.getRefreshableView();
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));
        // Need to use the Actual RecyclerView when registering for Context Menu
        registerForContextMenu(recyclerView);

        mData = new LinkedList<>();
        mData.addAll(Arrays.asList(mStrings));

        mAdapter = new CommonRecyclerViewAdapter<String>(this, R.layout.item_horizontal_recycler_view, mData) {
            @Override
            protected void convert(CommonRecyclerViewHolder holder, String item, int position) {
                holder.setText(R.id.tv_item, item);
            }
        };

        /**
         * Add Sound Event Listener
         */
        SoundPullEventListener<RecyclerView> soundListener = new SoundPullEventListener<>(this);
        soundListener.addSoundEvent(PullToRefreshBase.State.PULL_TO_REFRESH, R.raw.pull_event);
        soundListener.addSoundEvent(PullToRefreshBase.State.RESET, R.raw.reset_sound);
        soundListener.addSoundEvent(PullToRefreshBase.State.REFRESHING, R.raw.refreshing_sound);
        mPullRefreshRecycler.setOnPullEventListener(soundListener);

        // You can also just use setListAdapter(mAdapter) or
        // mPullRefreshListView.setAdapter(mAdapter)
        recyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(new AbstractRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, RecyclerView.ViewHolder holder, int position) {
                Toast.makeText(PullToRefreshHorizontalRecyclerViewActivity.this, mAdapter.getData().get(position), Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean onItemLongClick(View view, RecyclerView.ViewHolder holder, int position) {
                return false;
            }
        });
    }

    private class GetDataTask extends AsyncTask<Void, Void, String[]> {
        boolean refresh;

        GetDataTask(boolean refresh) {
            this.refresh = refresh;
        }

        @Override
        protected String[] doInBackground(Void... params) {
            // Simulates a background job.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return mStrings;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (refresh) {
                mData.addFirst("Added after refresh...");
            } else {
                mData.addLast("Added after load more...");
            }
            mAdapter.notifyDataSetChanged();

            // Call onRefreshComplete when the list has been refreshed.
            mPullRefreshRecycler.onRefreshComplete();
            super.onPostExecute(result);
        }
    }
}
