package com.tzw.recorder.constant;

/**
 * Created by clara.tong on 2019/9/10
 */
public interface MsgIdConstants {
    interface CommonMsgId {

        int BASE_MSG_ID = 10001000;
        int MSG_AUDIO_RECORD_UPDATE_TIME = BASE_MSG_ID + 1;
        int MSG_AUDIO_RECORD_STOP = BASE_MSG_ID + 2;

    }

    interface MediaPreviewMsgId{
        int BASE_MSG_ID = 20001000;
        int BASE_MSG_UPDATE_TIME = BASE_MSG_ID + 1;
        int BASE_MSG_START_PLAY = BASE_MSG_ID + 2;

    }


}
