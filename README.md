# Recorder
An Android audio recorder App .
一、Android提供了两个API用于录音的实现：MediaRecorder 和AudioRecord。

MediaRecorder：录制的音频文件是经过压缩后的，需要设置编码器。并且录制的音频文件可以用系统自带的Music播放器播放。MediaRecorder已经集成了录音、编码、压缩等，并支持少量的录音音频格式，但是这也是他的缺点，支持的格式过少并且无法实时处理音频数据。

AudioRecord：主要实现对音频实时处理以及边录边播功能，相对MediaRecorder比较专业，输出是PCM语音数据，如果保存成音频文件，是不能够被播放器播放的，所以必须先写代码实现数据编码以及压缩。

在本App 中分别通过这两个API实现了音频的录制
1、MediaRecorder 实现音频录制 参考：https://blog.csdn.net/weixin_41402015/article/details/80716952
代码：com.tzw.recorder.recordUtil.mediaRecord.MediaRecorderManager
2、AudioRecord 实现音频录制 参考：https://www.jianshu.com/p/993b41bd4a2b
代码：com.tzw.recorder.recordUtil.audioRecord.AudioRecordManager
录制的PCM文件合并成WAV 代码 com.tzw.recorder.recordUtil.audioRecord.PcmToWav
AudioRecord录制音频也可以实现边录边播，播放通过AudioTrack实现，这部分功能暂时没有在当前加入App内运行，参考https://blog.csdn.net/lantingshuxu/article/details/53520316
代码：com.tzw.recorder.recordUtil.audioRecord内AudioRecorderHandler和AudioPlayerHandler

二、音频播放
当前APP内的音频播放主要是通过MediaPlayer实现，代码：com.tzw.recorder.activity.AudioListeningActivity
播放过程实现已经插入耳机或连接了蓝牙耳机，需要使用扬声器播放音频，主要参考https://blog.csdn.net/u012545728/article/details/99462873


三、录音界面随音量跳动动画
  1、实时获取音量实现 https://blog.csdn.net/greatpresident/article/details/38402147
  2、根据音量大小实现简单动画效果：https://blog.csdn.net/q15037911903/article/details/82781631



