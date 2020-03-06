package com.tzw.recorder.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tzw.recorder.R;

import java.io.File;
import java.util.List;

/**
 * Created by clara.tong on 2020/3/2
 */
public class RecorderFileAdapter extends RecyclerView.Adapter<RecorderFileAdapter.FileViewHolder> {
    private Context mContext;
    private List<File> mRecorderFiles;
    private OnRecyclerItemClickListener mOnRecyclerItemClickListener;

    public RecorderFileAdapter(Context mContext, List<File> mRecorderFiles) {
        this.mContext = mContext;
        this.mRecorderFiles = mRecorderFiles;
    }

    public void setOnRecyclerItemClickListener(OnRecyclerItemClickListener onRecyclerItemClickListener){
        this.mOnRecyclerItemClickListener = onRecyclerItemClickListener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_record_file,viewGroup,false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder fileViewHolder, final int i) {
        File recordFile = mRecorderFiles.get(i);
        fileViewHolder.mTvFileName.setText(recordFile.getName());
        fileViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               mOnRecyclerItemClickListener.onItemClicked(v,i);
            }
        });
        fileViewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mOnRecyclerItemClickListener.onItemLongClicked(v,i);
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRecorderFiles.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    static class FileViewHolder extends RecyclerView.ViewHolder{
        TextView mTvFileName;
        FileViewHolder( View itemView) {
            super(itemView);
           this.mTvFileName = itemView.findViewById(R.id.tv_record_file_name);
        }

    }

}
