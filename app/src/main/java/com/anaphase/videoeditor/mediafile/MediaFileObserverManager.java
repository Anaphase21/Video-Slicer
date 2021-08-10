package com.anaphase.videoeditor.mediafile;

import java.util.Stack;

public class MediaFileObserverManager implements Runnable{
    private volatile Stack<MediaFileObserver> mediaFileObservers;
    private volatile MediaFileObserver mediaFileObserver;
    private volatile boolean pathAdded;
    private volatile boolean pathRemoved;

    public MediaFileObserverManager(MediaFileObserver mediaFileObserver){
        mediaFileObservers = new Stack();
        mediaFileObservers.push(mediaFileObserver);
    }

    @Override
    public void run(){
        while(true) {
            if(pathAdded) {
                mediaFileObservers.push(mediaFileObserver);
                mediaFileObservers.peek().startWatching();
                pathAdded = false;
            }else if(pathRemoved){
                mediaFileObservers.pop().stopWatching();
                pathRemoved = false;
            }
            try{
                Thread.sleep(Long.MAX_VALUE);
            }catch(InterruptedException interruptedException){
                if(mediaFileObservers.isEmpty()){
                    mediaFileObservers = null;
                    return;
                }
                continue;
            }
        }
    }

    public void setPathAdded(boolean flag){
        this.pathAdded = flag;
    }

    public void setPathRemoved(boolean flag){
        this.pathRemoved = flag;
    }

    public void setMediaFileObserver(MediaFileObserver mediaFileObserver){
        this.mediaFileObserver = mediaFileObserver;
    }

    public void clearObservers(){
        for(MediaFileObserver observer : mediaFileObservers){
            observer.stopWatching();
        }
        mediaFileObservers.clear();
    }
}
