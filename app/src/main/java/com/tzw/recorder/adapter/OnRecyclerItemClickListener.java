package com.tzw.recorder.adapter;

import android.view.View;

/**
 * Created by clara.tong on 2020/3/2
 */
public interface OnRecyclerItemClickListener {
    void onItemClicked(View view,int position);
    void onItemLongClicked(View view,int position);
}
