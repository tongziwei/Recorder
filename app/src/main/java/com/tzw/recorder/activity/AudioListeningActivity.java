package com.tzw.recorder.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tzw.recorder.R;
import com.tzw.recorder.constant.Constants;
import com.tzw.recorder.constant.MsgIdConstants;
import com.tzw.recorder.utils.TimeTransferUtil;

import java.io.File;

public class AudioListeningActivity extends AppCompatActivity {
    private TextView mTvAudioFileName;
    private LinearLayout mLlProgressbar;
    private TextView mTvAudioLiveTime;
    private TextView mTvAudioTotalTime;
    private SeekBar mSbProgress;
    private ImageButton mIbtnControlAudio;

    private MediaPlayer mMediaPlayer;
    private File mAudioFile;
    private AudioManager mAudioManager;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MsgIdConstants.MediaPreviewMsgId.BASE_MSG_UPDATE_TIME:
                    updateTime();
                    mHandler.sendEmptyMessageDelayed(MsgIdConstants.MediaPreviewMsgId.BASE_MSG_UPDATE_TIME, 500);
                    break;
            }
        }
    };

    private void updateTime() {
        if (mMediaPlayer != null) {
            mTvAudioLiveTime.setText(TimeTransferUtil.convertDuration(mMediaPlayer.getCurrentPosition()));
            mSbProgress.setProgress(mMediaPlayer.getCurrentPosition());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_listening);
        mAudioFile = (File) getIntent().getSerializableExtra(Constants.KEY_AUDIO_FILE);
        initView();
        initListener();
        //下面的设置使得插入耳机后仍用扬声器外放
     /*   mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setMicrophoneMute(false);
        mAudioManager.setSpeakerphoneOn(true);//使用扬声器外放，即使已经插入耳机
        setVolumeControlStream(AudioManager.STREAM_MUSIC);//控制声音的大小
        mAudioManager.setMode(AudioManager.STREAM_MUSIC);*/
        registerReceiver(scoUpdateReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
        speaker();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
        umAudioListener();
        offSpeaker();
        unregisterReceiver(scoUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void initView(){
        mTvAudioFileName = (TextView)findViewById(R.id.tv_audio_file_name);
        mLlProgressbar = (LinearLayout)findViewById(R.id.ll_audio_progress_bar);
        mTvAudioLiveTime =(TextView)findViewById(R.id.tv_audio_live_time);
        mTvAudioTotalTime = (TextView)findViewById(R.id.tv_audio_total_time);
        mSbProgress = (SeekBar)findViewById(R.id.control_audio_seekbar);
        mIbtnControlAudio = (ImageButton)findViewById(R.id.ibtn_controlAudio);

        mTvAudioFileName.setText(mAudioFile.getName());
    }

    private void initListener(){
        mIbtnControlAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(mMediaPlayer == null){
                   doPlay(mAudioFile);
                   mIbtnControlAudio.setImageResource(R.mipmap.icon_pause);
               }else{
                   if(mMediaPlayer.isPlaying()){
                       mMediaPlayer.pause();
                       mIbtnControlAudio.setImageResource(R.mipmap.icon_play);
                   }else{
                       mMediaPlayer.start();
                       initAudioListener();
                       mIbtnControlAudio.setImageResource(R.mipmap.icon_pause);
                   }
               }
            }
        });
    }


    private void doPlay(File audioFile) {
        try {
            //配置播放器 MediaPlayer
            mMediaPlayer = new MediaPlayer();
            //设置声音文件
            mMediaPlayer.setDataSource(audioFile.getAbsolutePath());
            //配置音量,中等音量
            mMediaPlayer.setVolume(1,1);
            //播放是否循环
            mMediaPlayer.setLooping(false);

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mTvAudioLiveTime.setText(TimeTransferUtil.convertDuration(mMediaPlayer
                            .getCurrentPosition()));
                    mTvAudioTotalTime.setText(TimeTransferUtil.convertDuration(mMediaPlayer
                            .getDuration()));
                    mSbProgress.setMax(mMediaPlayer.getDuration());
                    mSbProgress.setProgress(mMediaPlayer.getCurrentPosition());
                    mMediaPlayer.start();
                    initAudioListener();
                    mHandler.sendEmptyMessageDelayed(MsgIdConstants.MediaPreviewMsgId.BASE_MSG_UPDATE_TIME, 500);
                }
            });

            //设置监听回调 播放完毕
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    releasePlayer();
                    umAudioListener();
                    mIbtnControlAudio.setImageResource(R.mipmap.icon_play);
                }
            });


            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    releasePlayer();
                    return true;
                }
            });

            //设置播放
            mMediaPlayer.prepareAsync();


            //异常处理，防止闪退

        } catch (Exception e) {
            e.printStackTrace();
            releasePlayer();
        }

    }



    private void releasePlayer(){
        if(mMediaPlayer!=null){
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer =null;
        }

    }

    /*--------------------------------------------------------------------------------------------*/
    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    //长时间丢失焦点，这个时候需要停止播放，并释放资源。根据不同的逻辑，有时候还会释放焦点
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                        mIbtnControlAudio.setImageResource(R.mipmap.icon_play);

                    }
                    mAudioManager.abandonAudioFocus(afChangeListener);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    //短暂失去焦点，这时可以暂停播放，但是不必要释放资源，因为很快又会获取到焦点
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    //短暂失去焦点，但是可以跟新的焦点拥有者同时播放，并做降噪处理
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:

                    //获得了音频焦点，可以播放声音
                    break;
            }
        }
    };

    /**
     * Init the audiochangelistener
     */
    private void initAudioListener() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }

        mAudioManager.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC, // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
    }

    /**
     * Clear the audiochangelistener
     */
    private void umAudioListener() {
        if (afChangeListener != null && mAudioManager != null) {
            //  Log.d("tzw", "umAudioListener: ");
            mAudioManager.abandonAudioFocus(afChangeListener);
        }
    }

    /**
     * 关闭扬声器
     */
    private void offSpeaker() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        Log.i("tzw", "isBluetoothSco 2:" + mAudioManager.isBluetoothScoOn());
        if(mAudioManager.isWiredHeadsetOn() || isBluetoothHeadsetConnected()){
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION); //AudioManager.MODE_IN_COMMUNICATION
            //如果有蓝牙耳机设备连接，打开Sco通道使用蓝牙耳机播放音频
            if (isBluetoothHeadsetConnected()) {
                Log.i("tzw", "need start BluetoothSco");
                mAudioManager.startBluetoothSco();
                mAudioManager.setBluetoothScoOn(true);
            }
            //关闭扬声器

            mAudioManager.setSpeakerphoneOn(false);
            Log.d("tzw", "offSpeaker: ");
        }

    }

    /**
     * 打开扬声器
     */
    private void speaker() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        if(mAudioManager.isWiredHeadsetOn() || isBluetoothHeadsetConnected()){
            mAudioManager.setMicrophoneMute(false);
            //关闭Sco
            if (isBluetoothHeadsetConnected()) {
                mAudioManager.setBluetoothScoOn(false);
                mAudioManager.stopBluetoothSco();
            }
            //打开扬声器
            mAudioManager.setSpeakerphoneOn(true);
            mAudioManager.setMode(AudioManager.STREAM_MUSIC);
            Log.i("tzw", "isBluetoothSco 1:" + mAudioManager.isBluetoothScoOn());
            Log.d("tzw", "open speaker: ");
        }

    }

    /**
     * 判断蓝牙耳机是否连接
     * @return
     */
    private boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (BluetoothProfile.STATE_CONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
            return true;
        }
        return false;
    }

    /**
     * 监听Sco变化广播
     */
    BroadcastReceiver scoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -100);
            if(AudioManager.SCO_AUDIO_STATE_CONNECTED == state ){
                Log.d("tzw", "onReceive: sco connected");
            }else if(AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state){
                Log.d("tzw", "onReceive: sco disconnected");

            }
        }
    };

}
