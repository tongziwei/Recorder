package com.tzw.recorder.recordUtil.audioRecord;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;

import com.tzw.recorder.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 使用AudioRecord进行音频录制，录制的音频PCM再转换为可直接播放的wav文件
 * Created by clara.tong on 2020/3/4
 * 参考：https://www.jianshu.com/p/993b41bd4a2b
 */
public class AudioRecordManager {
    // 音频源：音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    // 采样率
    // 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    // 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 16000;
    // 音频通道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    // 音频格式：PCM编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区大小：缓冲区字节大小
    private int bufferSizeInBytes = 0;
    // 录音对象
    private AudioRecord audioRecord;
    // 录音状态
    private Status status = Status.STATUS_NO_READY;
    // 文件名
    private String fileName;
    // 录音文件集合
    private List<String> filesName = new ArrayList<>();
    //线程池
    private ExecutorService mExecutorService;

    //录音监听
    private RecordStreamListener listener;
    private OnAudioStatusUpdateListener audioStatusUpdateListener;
    private long startTime;

    private static AudioRecordManager mInstance;
    private AudioRecordManager() {
        mExecutorService = Executors.newCachedThreadPool();
    }

    public static AudioRecordManager getInstance(){
        if(mInstance == null){
            synchronized (AudioRecordManager.class){
                if(mInstance == null){
                    mInstance = new AudioRecordManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 创建录音对象
     */
    public void createAudio(String fileName, int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {
        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
                channelConfig, audioFormat);
        audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        this.fileName = fileName;
    }

    /**
     * 创建默认的录音对象
     * @param fileName 文件名
     */
    public void createDefaultAudio(String fileName) {
        /*mContext = ctx;
        mHandler = handler;*/
        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL, AUDIO_ENCODING);
        audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes);
        this.fileName = fileName;
        status = Status.STATUS_READY;
    }

    /**
     * 开始录音
     * 音频流的监听@param listener
     */
    public void startRecord() {       //startRecord(final RecordStreamListener listener)

        if (status == Status.STATUS_NO_READY || TextUtils.isEmpty(fileName)) {
            throw new IllegalStateException("录音尚未初始化,请检查是否禁止了录音权限~");
        }
        if (status == Status.STATUS_START) {
            throw new IllegalStateException("正在录音");
        }
        Log.d("AudioRecorder","===startRecord==="+audioRecord.getState());
        audioRecord.startRecording();
        startTime = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                writeDataTOFile();  //writeDataTOFile(listener)
            }
        }).start();
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        Log.d("AudioRecorder","===stopRecord===");
        if (status == Status.STATUS_NO_READY || status == Status.STATUS_READY) {
            throw new IllegalStateException("录音尚未开始");
        } else {
            audioRecord.stop();
            status = Status.STATUS_STOP;
            release();
            audioStatusUpdateListener.onStop(FileUtils.getWavFileAbsolutePath(fileName));
        }
    }

    /**
     * 暂停录音
     */
    public void pauseRecord() {
        Log.d("AudioRecorder","===pauseRecord===");
        if (status != Status.STATUS_START) {
            throw new IllegalStateException("没有在录音");
        } else {
            audioRecord.stop();
            status = Status.STATUS_PAUSE;
        }
    }

    /**
     * 取消录音
     */
    public void canel() {
        filesName.clear();
        fileName = null;
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        status = Status.STATUS_NO_READY;
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d("AudioRecorder","===release===");
        //假如有暂停录音
        try {
            if (filesName.size() > 0) {
                List<String> filePaths = new ArrayList<>();
                for (String fileName : filesName) {
                    filePaths.add(FileUtils.getPcmFileAbsolutePath(fileName));
                }
                //清除
                filesName.clear();
                //将多个pcm文件转化为wav文件
                mergePCMFilesToWAVFile(filePaths);

            } else {
                //这里由于只要录音过filesName.size都会大于0,没录音时fileName为null
                //会报空指针 NullPointerException
                // 将单个pcm文件转化为wav文件
                //Log.d("AudioRecorder", "=====makePCMFileToWAVFile======");
                //makePCMFileToWAVFile();
            }
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage());
        }

        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        status = Status.STATUS_NO_READY;
    }

    /**
     * 将音频信息写入文件
     * 音频流的监听@param listener
     */
    private void writeDataTOFile() {               //writeDataTOFile(RecordStreamListener listener)
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audiodata = new byte[bufferSizeInBytes];

        FileOutputStream fos = null;
        int readsize = 0;
        try {
            String currentFileName = fileName;
            if (status == Status.STATUS_PAUSE) {
                //假如是暂停录音 将文件名后面加个数字,防止重名文件内容被覆盖
                currentFileName += filesName.size();
            }
            filesName.add(currentFileName);
            File file = new File(FileUtils.getPcmFileAbsolutePath(currentFileName));
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
        } catch (IllegalStateException e) {
            Log.e("AudioRecorder", e.getMessage());
            throw new IllegalStateException(e.getMessage());
        } catch (FileNotFoundException e) {
            Log.e("AudioRecorder", e.getMessage());
        }
        //将录音状态设置成正在录音状态
        status = Status.STATUS_START;
        while (status == Status.STATUS_START) {
            readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
            if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos != null) {
                try {
                    fos.write(audiodata);
                    /*if (listener != null) {
                        //用于拓展业务
                        listener.recordOfByte(audiodata, 0, audiodata.length);
                    }*/
                } catch (IOException e) {
                    Log.e("AudioRecorder", e.getMessage());
                }
            }
            double db = computeVolume(audiodata,readsize);  //音量分贝值
            audioStatusUpdateListener.onUpdate(db,System.currentTimeMillis()-startTime);
           /* try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        }
        try {
            if (fos != null) {
                fos.close();// 关闭写入流
            }
        } catch (IOException e) {
            Log.e("AudioRecorder", e.getMessage());
        }
    }

    /**
     * 将pcm合并成wav
     *
     * @param filePaths
     */
    private void mergePCMFilesToWAVFile(final List<String> filePaths) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (PcmToWav.mergePCMFilesToWAVFile(filePaths, FileUtils.getWavFileAbsolutePath(fileName))) {
                    //操作成功
                } else {
                    //操作失败
                    Log.e("AudioRecorder", "mergePCMFilesToWAVFile fail");
                    throw new IllegalStateException("mergePCMFilesToWAVFile fail");
                }
            }
        });
    }

    /**
     * 将单个pcm文件转化为wav文件
     */
    private void makePCMFileToWAVFile() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (PcmToWav.makePCMFileToWAVFile(FileUtils.getPcmFileAbsolutePath(fileName), FileUtils.getWavFileAbsolutePath(fileName), true)) {
                    //操作成功
                } else {
                    //操作失败
                    Log.e("AudioRecorder", "makePCMFileToWAVFile fail");
                    throw new IllegalStateException("makePCMFileToWAVFile fail");
                }
            }
        });
    }

    /**
     * 获取录音对象的状态
     *
     * @return
     */
    public Status getStatus() {
        return status;
    }

    /**
     * 获取本次录音文件的个数
     *
     * @return
     */
    public int getPcmFilesCount() {
        return filesName.size();
    }


    public RecordStreamListener getListener() {
        return listener;
    }

    public void setListener(RecordStreamListener listener) {
        this.listener = listener;
    }


    public void setOnAudioStatusUpdateListener(OnAudioStatusUpdateListener audioStatusUpdateListener) {
        this.audioStatusUpdateListener = audioStatusUpdateListener;
    }


    /**
     * 计算音量分贝值，参考https://blog.csdn.net/greatpresident/article/details/38402147
     * @param buffer
     * @param readSize
     * @return
     */
    private double computeVolume(byte[] buffer,int readSize){
        long v = 0;
        // 将 buffer 内容取出，进行平方和运算
        for (int i = 0; i < buffer.length; i++) {
            v += buffer[i] * buffer[i];
        }
        double mean = v / (double) readSize; // 平方和除以数据总长度，得到音量大小。
        double volume = 10 * Math.log10(mean);
        return volume;
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
