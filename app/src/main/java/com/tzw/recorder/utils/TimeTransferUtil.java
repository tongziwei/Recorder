package com.tzw.recorder.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by clara.tong on 2019/4/23
 */
public class TimeTransferUtil {
    /**
     * 根据秒数,获得格式为00:00的时间
     *
     * @param timems 单位ms
     * @return
     */
    public static String getTimeStrByMin(int timems) {
        int time = timems/1000;
        if (time <= 0) {
            return "00:00";
        }
        String strMin = "";
        String strSec = "";
        int min = (time / 60);
        int sec = (time % 60);
        if (min < 10) {
            strMin = "0" + min;
        } else {
            strMin = "" + min;
        }
        if (sec < 10) {
            strSec = "0" + sec;
        } else {
            strSec = "" + sec;
        }
        return strMin + ":" + strSec;
    }

    public static String getTimeStrByMin(long timems) {
        long time = timems/1000;
        if (time <= 0) {
            return "00:00";
        }
        String strMin = "";
        String strSec = "";
        long min = (time / 60);
        long sec = (time % 60);
        if (min < 10) {
            strMin = "0" + min;
        } else {
            strMin = "" + min;
        }
        if (sec < 10) {
            strSec = "0" + sec;
        } else {
            strSec = "" + sec;
        }
        return strMin + ":" + strSec;
    }

    public static String formatLongToTimeStr(int l) {
        int hour = 0;
        int minute = 0;
        int second = 0;

        second = l / 1000;

        if (second > 60) {
            minute = second / 60;
            second = second % 60;
        }
        if (minute >= 60) {
            hour = minute / 60;
            minute = minute % 60;
            return getTwoLength(hour) + ":" + getTwoLength(minute) + ":" + getTwoLength(second);
        }else{
            return getTwoLength(minute) + ":" + getTwoLength(second);
        }

    }

    public static String getTwoLength(final int data) {
        if(data < 10) {
            return "0" + data;
        } else {
            return "" + data;
        }
    }

    public static String convertDuration(long duration) {
        duration /= 1000;
        int hour = (int) (duration / 3600);
        int minute = (int) ((duration - hour * 3600) / 60);
        int second = (int) (duration - hour * 3600 - minute * 60);

        String hourValue = "";
        String minuteValue;
        String secondValue;
        if (hour > 0) {
            if (hour >= 10) {
                hourValue = Integer.toString(hour);
            } else {
                hourValue = "0" + hour;
            }
            hourValue += ":";
        }
        if (minute > 0) {
            if (minute >= 10) {
                minuteValue = Integer.toString(minute);
            } else {
                minuteValue = "0" + minute;
            }
        } else {
            minuteValue = "00";
        }
        minuteValue += ":";
        if (second > 0) {
            if (second >= 10) {
                secondValue = Integer.toString(second);
            } else {
                secondValue = "0" + second;
            }
        } else {
            secondValue = "00";
        }
        return hourValue + minuteValue + secondValue;
    }

    public static String timeStamp2Date(long time, String format) {
        if (format == null || format.isEmpty()) {
            format = "yyyy_MM_dd_HH_mm_ss";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(time));
    }


}
