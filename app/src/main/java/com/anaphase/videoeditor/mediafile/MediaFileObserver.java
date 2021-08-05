package com.anaphase.videoeditor.mediafile;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;

import com.anaphase.videoeditor.util.Util;

import java.io.File;

public class MediaFileObserver extends FileObserver {

    private Handler handler;
    private Bundle bundle;
    private Message message;
    private String dir;

    public MediaFileObserver(String path, int mask){
        super(path, mask);
        dir = path;
    }

    public MediaFileObserver(String path){
        super(path);
        dir = path;
    }

    @TargetApi(Build.VERSION_CODES.Q)
    public MediaFileObserver(File file){
        super(file);
        dir = file.getPath();
    }

    public void setHandler(Handler handler){
        this.handler = handler;
    }

    @Override
    public void onEvent(int event, String path){
        File fullPath = new File(path + dir + File.separator + path);
        if((!Util.isAudioExtension(path) && (!Util.isVideoExtension(path)) && (!(fullPath.isDirectory())))){
            return;
        }
        
        bundle = new Bundle();
        message = handler.obtainMessage();
        if(((event & CREATE) == CREATE) || ((event & MOVED_TO) == MOVED_TO)){
            sendMessage("+fileChange", dir + File.separator + path);
        }else if(((event & DELETE) == DELETE) || (event & MOVED_FROM) == (MOVED_FROM)){
            sendMessage("-fileChange", dir + File.separator + path);
        }else if((event & CLOSE_WRITE) == CLOSE_WRITE){
        }
    }

    private void sendMessage(String key, String messageContent){
        bundle.putString(key, messageContent);
        message.setData(bundle);
        handler.sendMessage(message);
    }
}
