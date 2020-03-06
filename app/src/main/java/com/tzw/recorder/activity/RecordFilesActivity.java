package com.tzw.recorder.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.tzw.recorder.R;
import com.tzw.recorder.adapter.OnRecyclerItemClickListener;
import com.tzw.recorder.adapter.RecorderFileAdapter;
import com.tzw.recorder.constant.Constants;
import com.tzw.recorder.utils.FileUtils;
import com.tzw.recorder.view.YesNoDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordFilesActivity extends AppCompatActivity {
    private RecyclerView mRvFiles;
    private RecorderFileAdapter mRecorderFileAdapter;
    private List<File> mRecordFiles = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_files);
        initViews();
        initData();
        initListener();
    }

    private void initViews(){
        mRvFiles = (RecyclerView)findViewById(R.id.rv_files);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRvFiles.setLayoutManager(linearLayoutManager);
        mRecorderFileAdapter = new RecorderFileAdapter(this,mRecordFiles);
        mRvFiles.setAdapter(mRecorderFileAdapter);

    }

    private void initData(){
        mRecordFiles.clear();
      /*   File fileFolder = new File(Environment.getExternalStorageDirectory()+"/Record/");
         File[] files = fileFolder.listFiles();
        mRecordFiles.addAll(Arrays.asList(files));*/      //对应使用MediaRecord录音的文件
        mRecordFiles.addAll(FileUtils.getWavFiles());   //对应使用AudioRecord录音的文件
        mRecorderFileAdapter.notifyDataSetChanged();
    }

    private void initListener(){
        mRecorderFileAdapter.setOnRecyclerItemClickListener(new OnRecyclerItemClickListener() {
            @Override
            public void onItemClicked(View view, int position) {
               if(mRecordFiles.size()>0){
                   File audioFile = mRecordFiles.get(position);
                   Intent intent = new Intent(RecordFilesActivity.this,AudioListeningActivity.class);
                   intent.putExtra(Constants.KEY_AUDIO_FILE,audioFile);
                   startActivity(intent);
               }
            }

            @Override
            public void onItemLongClicked(View view, int position) {
                if(mRecordFiles.size()>0){
                    File audioFile = mRecordFiles.get(position);
                    showDeleteConfirmDialog(audioFile);
                }

            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


    public void showDeleteConfirmDialog(final File audioFile){
        final YesNoDialog deleteConfirmDialog = new YesNoDialog(this,YesNoDialog.DIALOG_DELETE_ITEMS);
        deleteConfirmDialog.setOnYesNoDialogBtnClickListener(new YesNoDialog.OnYesNoDialogBtnClickListener() {
            @Override
            public void onDialogBtnCancel(View view) {
                deleteConfirmDialog.cancel();
            }

            @Override
            public void onDialogBtnConfirm(View view) {
                if(audioFile.exists()){
                    audioFile.delete();
                }
                initData();
                deleteConfirmDialog.cancel();
            }
        });
        deleteConfirmDialog.show();
    }


}
