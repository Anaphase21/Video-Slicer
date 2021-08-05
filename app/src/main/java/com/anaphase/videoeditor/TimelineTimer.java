package com.anaphase.videoeditor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.VideoView;

public class TimelineTimer implements Runnable{

    private int currentPosition = 0;
    private Handler handler;
    private VideoView videoView;
    private volatile boolean completed = false;
    private boolean sleep = false;
    private int duration;
    private volatile Bundle bundle;

    public TimelineTimer(Handler handler, VideoView videoView){
        this.handler = handler;
        this.videoView = videoView;
        bundle = new Bundle();
    }

    @Override
    public void run(){
        Bundle bundle = new Bundle();
        float percentage = 0.0f;
        while(!completed){
            try{
                if(sleep){
                    Thread.sleep(1000L);
                    continue;
                }else {
                    Thread.sleep(5L);
                }
            }catch (InterruptedException ine){
                System.out.println("INTERRUPTED EXCEPTION");
                //Thread.currentThread().interrupt();
            }
            if(completed){
                sendMessageForCurrentPosition(0.0f);
                return;
            }
            currentPosition = videoView.getCurrentPosition();
            System.out.println("Position: "+Util.toTimeUnits(currentPosition)+" Duration: "+Util.toTimeUnits(duration));
            sendMessageForCurrentPosition(currentPosition);
        }
    }

    private void sendMessageForCurrentPosition(float currentPosition){
        float percentage = (currentPosition * 100.0f) / duration;
        //bundle.clear();
        bundle.putFloat("currentPosition", percentage);
        Message message = handler.obtainMessage();
        message.setData(bundle);
        handler.sendMessage(message);
    }
    public void setCompleted(boolean flag){
        this.completed = flag;
    }

    public void setDuration(int duration){
        this.duration = duration;
    }


    public void setThreadToSleep(boolean sleep){
        this.sleep = sleep;
    }
}
