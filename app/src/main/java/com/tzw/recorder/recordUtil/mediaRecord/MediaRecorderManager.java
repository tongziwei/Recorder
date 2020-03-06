package com.tzw.recorder.recordUtil.mediaRecord;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.tzw.recorder.utils.TimeTransferUtil;

import java.io.File;
import java.io.IOException;

/**
 * 使用MediaRecord方法进行音频录制
 *
 * Created by clara.tong on 2020/2/28
 * 参考：https://blog.csdn.net/weixin_41402015/article/details/80716952
 */
public class MediaRecorderManager {
    private static final String TAG = "MediaRecorderManager";
    public static final int MAX_LENGTH = 1000 * 60 * 10;// 最大录音时长1000*60*10;
    private static final int UPDATE_RECORD_FILE = 10010;

    private long mMaxSingRecordTime = 60*1000; //默认单个录音文件最大时长为60s
    private long intervalTime;
    private long currentTime;
    //文件路径
    private String filePath;
    //文件夹路径
    private String folderPath;

    private MediaRecorder mMediaRecorder;

    private long startTime;
    private long endTime;

    private OnAudioStatusUpdateListener audioStatusUpdateListener;

    private RecordState mCurrentRecordState = RecordState.IDLE;

    public static MediaRecorderManager mInstance;

    public static MediaRecorderManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (MediaRecorderManager.class) {
                if (mInstance == null) {
                    mInstance = new MediaRecorderManager();
                }
            }
        }
        return mInstance;
    }


    public MediaRecorderManager() {
        this(Environment.getExternalStorageDirectory()+"/Record/");
    }

    public MediaRecorderManager(String folderPath) {
        this.folderPath = folderPath;
        File path = new File(folderPath);
        if(!path.exists())
            path.mkdirs();
    }

    public void setSaveFolderPath(String folderPath){
        if (TextUtils.isEmpty(folderPath)) {
            this.folderPath =Environment.getExternalStorageDirectory()+"/Record/";
        } else {
            this.folderPath = folderPath;
        }

        File path = new File(folderPath);
        if(!path.exists())
            path.mkdirs();

    }

    public void setMaxSingRecordTime(long maxSingRecordTimetime){
        this.mMaxSingRecordTime = maxSingRecordTimetime;
    }

    /**
     * 开始录音 使用amr格式
     *      录音文件
     * @return
     */
    public void startRecord() {
        // 开始录音
        /* ①Initial：实例化MediaRecorder对象 */
        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();
        try {
            /* ②setAudioSource/setVedioSource */
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);   //AAC 对应文件名.m4a

            String filename = TimeTransferUtil.timeStamp2Date(System.currentTimeMillis(),"yyyy_MM_dd_HH_mm_ss");

            filePath = folderPath + filename + ".amr" ;
            /* ③准备 */
            mMediaRecorder.setOutputFile(filePath);
            mMediaRecorder.setMaxDuration(MAX_LENGTH);
            mMediaRecorder.prepare();
            /* ④开始 */
            mMediaRecorder.start();
            // AudioRecord audioRecord.
            /* 获取开始时间* */
            startTime = System.currentTimeMillis();
            //intervalTime = startTime;
            mCurrentRecordState = RecordState.RECORDING;
            mHandler.sendEmptyMessageDelayed(UPDATE_RECORD_FILE,mMaxSingRecordTime);
            updateMicStatus();
            Log.e(TAG, "startTime" + startTime);
        } catch (IllegalStateException e) {
            Log.i(TAG, "illegal state ,call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        } catch (IOException e) {
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        }
    }

    /**
     * 停止录音
     */
    public long stopRecord() {
        if (mMediaRecorder == null)
            return 0L;
        endTime = System.currentTimeMillis();
        mHandler.removeMessages(UPDATE_RECORD_FILE);
        //有一些网友反应在5.0以上在调用stop的时候会报错，翻阅了一下谷歌文档发现上面确实写的有可能会报错的情况，捕获异常清理一下就行了，感谢大家反馈！
        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            audioStatusUpdateListener.onStop(filePath);
            mCurrentRecordState = RecordState.IDLE;
            filePath = "";

        }catch (RuntimeException e){
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            File file = new File(filePath);
            if (file.exists())
                file.delete();

            filePath = "";

        }
        return endTime - startTime;
    }

    /**
     * 取消录音
     */
    public void cancelRecord(){

        try {

            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

        }catch (RuntimeException e){
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        File file = new File(filePath);
        if (file.exists())
            file.delete();

        filePath = "";

    }

    public RecordState getCurrentRecordState(){
        return mCurrentRecordState;
    }

    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case UPDATE_RECORD_FILE:
                    Log.d(TAG, "update file");
                    stopRecord();
                   startRecord();
                    break;
            }
        }
    };

    private Runnable mUpdateMicStatusTimer = new Runnable() {
        public void run() {
            updateMicStatus();
        }
    };




    private int BASE = 1;
    private int SPACE = 100;// 间隔取样时间

    public void setOnAudioStatusUpdateListener(OnAudioStatusUpdateListener audioStatusUpdateListener) {
        this.audioStatusUpdateListener = audioStatusUpdateListener;
    }

    /**
     * 更新麦克状态
     */
    private void updateMicStatus() {

        if (mMediaRecorder != null) {
            double ratio = (double)mMediaRecorder.getMaxAmplitude() / BASE;
            double db = 0;// 分贝
            if (ratio > 1) {
                db = 20 * Math.log10(ratio);
                if(null != audioStatusUpdateListener) {
                    audioStatusUpdateListener.onUpdate(db,System.currentTimeMillis()-startTime);
                }
            }
            mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
        }
    }

    public interface OnAudioStatusUpdateListener {
        /**
         * 录音中...
         * @param db 当前声音分贝
         * @param time 录音时长
         */
        public void onUpdate(double db,long time);

        /**
         * 停止录音
         * @param filePath 保存路径
         */
        public void onStop(String filePath);
    }




}
