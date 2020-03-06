package com.tzw.recorder.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tzw.recorder.constant.MsgIdConstants;
import com.tzw.recorder.recordUtil.audioRecord.AudioRecordManager;
import com.tzw.recorder.R;
import com.tzw.recorder.recordUtil.audioRecord.Status;
import com.tzw.recorder.utils.TimeTransferUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE = 1000;
    private TextView mTvRecordTime;
    private ImageView mIvVoice;
    private Drawable mVoiceDrawable;
    private ImageButton mIBtnRecord;
    private ImageButton mIbtnCheckFile;
    private TextView mTvRecordState;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MsgIdConstants.CommonMsgId.MSG_AUDIO_RECORD_UPDATE_TIME:
                    mIBtnRecord.setImageResource(R.mipmap.btn_stop_record);
                    mTvRecordState.setText("点击完成");
                    mTvRecordTime.setVisibility(View.VISIBLE);
                    long time = msg.getData().getLong("time");
                    double db = msg.getData().getDouble("volume");
                    mTvRecordTime.setText(TimeTransferUtil.convertDuration(time));
                    mVoiceDrawable.setLevel((int)(3000 + 6000 * db / 100));
                    break;
                case MsgIdConstants.CommonMsgId.MSG_AUDIO_RECORD_STOP:
                    mIBtnRecord.setImageResource(R.mipmap.btn_start_record);
                    mTvRecordState.setText("点击录音");
                    mTvRecordTime.setVisibility(View.INVISIBLE);
                    mVoiceDrawable.setLevel(0);
                    break;

            }
        }
    };
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        requestPermission(PERMISSIONS,REQUEST_CODE);
        initVoice();
        initListener();
    }

    private void initViews(){
        mTvRecordTime = (TextView)findViewById(R.id.tv_record_time);
        mIBtnRecord = (ImageButton)findViewById(R.id.ibtn_record);
        mIbtnCheckFile = (ImageButton) findViewById(R.id.ibtn_check_record_file);
        mTvRecordState = (TextView)findViewById(R.id.tv_record_state);
        mIvVoice = (ImageView)findViewById(R.id.iv_voice);
        mVoiceDrawable = mIvVoice.getDrawable();


       /* if(MediaRecorderManager.getInstance(this).getCurrentRecordState() == RecordState.RECORDING){
            mIBtnRecord.setImageResource(R.mipmap.btn_stop_record);
            mTvRecordState.setText("点击完成");
        }else{
            mIBtnRecord.setImageResource(R.mipmap.btn_start_record);
            mTvRecordState.setText("点击录音");
        }*/

       //AudioRecord录音
       if(AudioRecordManager.getInstance().getStatus() == Status.STATUS_START){
           mIBtnRecord.setImageResource(R.mipmap.btn_stop_record);
           mTvRecordState.setText("点击完成");
       }else{
           mIBtnRecord.setImageResource(R.mipmap.btn_start_record);
           mTvRecordState.setText("点击录音");
       }

    }

    private void initListener(){
        mIBtnRecord.setOnClickListener(this);
        mIbtnCheckFile.setOnClickListener(this);
       /* MediaRecorderManager.getInstance(this).setOnAudioStatusUpdateListener(new MediaRecorderManager.OnAudioStatusUpdateListener() {
            @Override
            public void onUpdate(double db, long time) {
                mIBtnRecord.setImageResource(R.mipmap.btn_stop_record);
                mTvRecordState.setText("点击完成");
                mTvRecordTime.setVisibility(View.VISIBLE);
                mTvRecordTime.setText(TimeTransferUtil.convertDuration(time));
                mVoiceDrawable.setLevel((int)(3000 + 6000 * db / 100));
            }

            @Override
            public void onStop(String filePath) {
                mIBtnRecord.setImageResource(R.mipmap.btn_start_record);
                mTvRecordState.setText("点击录音");
                mTvRecordTime.setVisibility(View.INVISIBLE);
                mVoiceDrawable.setLevel(0);
            }
        });*/
       AudioRecordManager.getInstance().setOnAudioStatusUpdateListener(new AudioRecordManager.OnAudioStatusUpdateListener() {
           @Override
           public void onUpdate(double db, long time) {
               Message msg = new Message();
               msg.what = MsgIdConstants.CommonMsgId.MSG_AUDIO_RECORD_UPDATE_TIME;
               Bundle bundle = new Bundle();
               bundle.putDouble("volume",db);
               bundle.putLong("time",time);
               msg.setData(bundle);
               mHandler.sendMessage(msg);

           }

           @Override
           public void onStop(String filePath) {
               mHandler.sendEmptyMessage(MsgIdConstants.CommonMsgId.MSG_AUDIO_RECORD_STOP);
           }
       });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ibtn_record:
               /* if(MediaRecorderManager.getInstance(this).getCurrentRecordState()== RecordState.RECORDING){
                    MediaRecorderManager.getInstance(this).stopRecord();
                    mIBtnRecord.setImageResource(R.mipmap.btn_start_record);
                    mTvRecordState.setText("点击录音");
                    mTvRecordTime.setVisibility(View.INVISIBLE);
                }else{
                    MediaRecorderManager.getInstance(this).startRecord();
                    mIBtnRecord.setImageResource(R.mipmap.btn_stop_record);
                    mTvRecordState.setText("点击完成");
                    mTvRecordTime.setVisibility(View.VISIBLE);
                }*/
                if(AudioRecordManager.getInstance().getStatus() == Status.STATUS_START){
                    AudioRecordManager.getInstance().stopRecord();
                    mIBtnRecord.setImageResource(R.mipmap.btn_start_record);
                    mTvRecordState.setText("点击录音");
                    mTvRecordTime.setVisibility(View.INVISIBLE);
                }else{
                    String filename = TimeTransferUtil.timeStamp2Date(System.currentTimeMillis(),"yyyy_MM_dd_HH_mm_ss");
                    AudioRecordManager.getInstance().createDefaultAudio(filename);
                    AudioRecordManager.getInstance().startRecord();
                    mIBtnRecord.setImageResource(R.mipmap.btn_stop_record);
                    mTvRecordState.setText("点击完成");
                    mTvRecordTime.setVisibility(View.VISIBLE);
                }


                break;
            case R.id.ibtn_check_record_file:
                Intent checkFiles = new Intent(MainActivity.this,RecordFilesActivity.class);
                startActivity(checkFiles);
                break;
            default:
                break;
        }
    }




    /**
     * Request permission.
     */
    protected void requestPermission(String[] permissions, int code) {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> deniedPermissions = getDeniedPermissions(this, permissions);
            if (deniedPermissions.isEmpty()) {
                onPermissionGranted(code);
            } else {
                permissions = new String[deniedPermissions.size()];
                deniedPermissions.toArray(permissions);
                ActivityCompat.requestPermissions(this, permissions, code);
            }
        } else {
            onPermissionGranted(code);
        }
    }

    private void initVoice(){
        // MediaRecorderManager.getInstance(this).setSaveFolderPath();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (isGrantedResult(grantResults)) onPermissionGranted(requestCode);
        else onPermissionDenied(requestCode);
    }

    private static List<String> getDeniedPermissions(Context context, String... permissions) {
        List<String> deniedList = new ArrayList<>(2);
        for (String permission : permissions) {
            if (PermissionChecker.checkSelfPermission(context, permission) != PermissionChecker.PERMISSION_GRANTED) {
                deniedList.add(permission);
            }
        }
        return deniedList;
    }

    private static boolean isGrantedResult(int... grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) return false;
        }
        return true;
    }

    private void onPermissionGranted(int code){

    }

    private void onPermissionDenied(int code) {
        Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
    }

}
