package com.anaphase.videoeditor.util;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.anaphase.videoeditor.mediafile.MediaStoreTable;

import java.io.File;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Util {
    private static final String[] videoExtensions = {".mp4", ".webm", ".mkv", ".3gp", ".3gpp"};
    private static final String[] audioExtensions = {".mp3", ".m4a", ".aac", ".wav"};
    private static final String[] imageExtensions = {".png", ".jpg", ".gif"};
    public static final String APP_DIRECTORY_NAME = "Video Slicer";
    public static final String PARENT_DIRECTORY = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM;
    public static final String APP_DIRECTORY_PATH = PARENT_DIRECTORY + File.separator + APP_DIRECTORY_NAME;

    public static String toTimeUnits(int time){
        long hours = TimeUnit.MILLISECONDS.toHours(time);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60;
        String s = "";
        s += hours > 9 ? hours + ":" : "0" + hours + ":";
        s += minutes > 9 ? minutes + ":" : "0" + minutes + ":";
        s += seconds > 9 ? seconds : "0" + seconds;
        return s;
    }

    public static String toTimeUnitsFraction(int time){
        long hours = TimeUnit.MILLISECONDS.toHours(time);
        long minutes =  TimeUnit.MILLISECONDS.toMinutes(time) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60;
        float secondsInFraction = (time - ((hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1000))) / 1000.0f;
        String sf = String.format("%.2f", secondsInFraction);
        String s = "";
        s += hours > 9 ? hours + ":" : "0" + hours + ":";
        s += minutes > 9 ? minutes + ":" : "0" + minutes + ":";
        s += seconds > 9 ? seconds : "0" + seconds;
        s += sf.substring(sf.lastIndexOf('.'));
        return s;
    }

    public static int convertFormattedTimeToMilliseconds(String formattedTime){
        int milliSeconds = 0;
        int minutes = 0;
        int hours = 0;
        String[] time = formattedTime.split(":");
        try{
            milliSeconds = (int)(Float.parseFloat(time[2]) * 1000.0f);
            minutes = Integer.parseInt(time[1]);
            hours = Integer.parseInt(time[0]);
        }catch(NumberFormatException numberFormatException){
            numberFormatException.printStackTrace();
        }
        return milliSeconds + (minutes * 60 * 1000) + (hours * 60 * 60 * 1000);
    }

    public static String renameFileIncremental(String filePath){
        File file = new File(filePath);
        String fileName = file.getName();
        String name = fileName.substring(0, fileName.lastIndexOf("_"));
        String renamedFile = PARENT_DIRECTORY + File.separator + APP_DIRECTORY_NAME + File.separator + name;
        file = new File(renamedFile);
        if(!file.exists()){
            file.mkdir();
            MediaStoreTable.notifyNewDirectoryCreated(file.getPath());
        }
        renamedFile = renamedFile + File.separator + fileName;
        file = new File(renamedFile);
        //MediaStoreTable.notifyNewDirectoryCreated(file.getParent());
        while(file.exists()){
            renamedFile = renameFile(renamedFile);
            file = new File(renamedFile);
        }
        return renamedFile;
    }

    public static String changeFileExtension(String filePath, String extension){
        int lastIndexOfPeriod = filePath.lastIndexOf('.');
        //String fileExtension = filePath.substring(lastIndexOfPeriod);
        return filePath.substring(0, lastIndexOfPeriod).concat(extension);
    }

    private static String renameFile(String filePath){
        int lastIndexOfPeriod = filePath.lastIndexOf('.');
        char ch = filePath.charAt(lastIndexOfPeriod - 1);
        StringBuilder nameIndex = new StringBuilder();
        int charIndex = lastIndexOfPeriod - 1;
        while((ch != '_') && (ch >= '0') && (ch <= '9')){
            nameIndex.append(ch);
            ch = filePath.charAt(--charIndex);
        }
        int nameIdx = nameIndex.length() == 0 ? 0 : Integer.parseInt(nameIndex.reverse().toString());
        String fileExtension = filePath.substring(lastIndexOfPeriod);
        filePath = filePath.substring(0, ++charIndex).concat(String.valueOf(++nameIdx));
        filePath = filePath.concat(fileExtension);
        return filePath;
    }

    public static String appendUnderscoredNumber(String filePath){
        int i = filePath.lastIndexOf('.');
        filePath = filePath.substring(0, i).concat("_1").concat(filePath.substring(i));
        return filePath;
    }

    public static boolean isVideoExtension(String path){
        if(path != null){
            return endsWithExtension(videoExtensions, path);
        }
        return false;
    }

    public static boolean isAudioExtension(String path){
        if(path != null){
            return endsWithExtension(audioExtensions, path);
        }
        return false;
    }

    public static boolean isImageExtention(String path){
        if(path != null){
            return endsWithExtension(imageExtensions, path);
        }
        return false;
    }

    private static boolean endsWithExtension(String[] extensions, String path){
        for (String extension : extensions) {
            if (path.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    public static String getExtension(String path){
        if((path == null) || (path.isEmpty())){
            return "";
        }
        int idx = path.lastIndexOf('.');
        if(idx != -1){
            return (path.substring(idx + 1));
        }
        return "";
    }

    public static String convertBytes(long bytes){
        String unit = "";
        float converted;
        if(bytes < 1024){
            converted = (float)bytes;
            unit = "B";
        }else if((converted = bytes / 1024.0f) < 1024){
            unit = "KB";
        }else if((converted /= 1024.0f) < 1024){
            unit = "MB";
        }else if((converted /= 1024.0f) < 1024){
            unit = "GB";
        }else if((converted /= 1024.0f) < 1024){
            unit = "TB";
        }
        return String.format("%.2f", converted) + unit;
    }

    public static String getFormattedDate(long timeMilliseconds){
        Date date = new Date(timeMilliseconds);
        return DateFormat.getDateInstance(DateFormat.SHORT).format(date);
    }

    public static void createAppDirectory(){
        File file = new File(PARENT_DIRECTORY + File.separator + APP_DIRECTORY_NAME);
        if(!file.exists()){
            file.mkdir();
        }
    }

    public static void sendMessage(int data, String key, Handler handler){
        Bundle bundle = new Bundle();
        bundle.putInt(key, data);
        Message msg = handler.obtainMessage();
        msg.setData(bundle);
        handler.sendMessage(msg);
    }
}