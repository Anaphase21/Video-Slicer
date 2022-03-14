package com.anaphase.videoeditor.mediafile;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;

import com.anaphase.videoeditor.util.Util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MediaFileObserver extends FileObserver {

    private Handler handler;
    private Bundle bundle;
    private Message message;
    private final String dir;
    private final Map<String, String> writeClosedFiles;

    public MediaFileObserver(String path, int mask){
        super(path, mask);
        writeClosedFiles = new HashMap<>(16);
        dir = path;
    }

    public MediaFileObserver(String path){
        super(path);
        writeClosedFiles = new HashMap<>(16);
        dir = path;
    }

    @TargetApi(Build.VERSION_CODES.Q)
    public MediaFileObserver(File file){
        super(file);
        writeClosedFiles = new HashMap<>(16);
        dir = file.getPath();
    }

    public void setHandler(Handler handler){
        this.handler = handler;
    }

    @Override
    public void onEvent(int event, String path){
        String fullPath = dir + File.separator + path;
        if(path == null){
            return;
        }
        if(!((Util.isAudioExtension(path) || (Util.isVideoExtension(path))) || Util.isImageExtension(path))){
            return;
        }
        bundle = new Bundle();
        message = handler.obtainMessage();
        /*if(((event & CREATE) == CREATE) || ((event & MOVED_TO) == MOVED_TO)){
        }else*/
        if(((event & DELETE) == DELETE) || (event & MOVED_FROM) == (MOVED_FROM)){
            writeClosedFiles.remove(fullPath);
            sendMessage("-fileChange", dir + File.separator + path);
        }else if(((event & CLOSE_WRITE) == CLOSE_WRITE)){// && (writeClosedFiles.get(fullPath) == null)){
            writeClosedFiles.put(fullPath, fullPath);
            sendMessage("+fileChange", fullPath);
        }
    }

    private void sendMessage(String key, String messageContent){
        bundle.putString(key, messageContent);
        message.setData(bundle);
        handler.sendMessage(message);
    }
}
