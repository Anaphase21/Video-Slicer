package com.anaphase.videoeditor;

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
                System.out.println("+++++[Path Added]+++++");
                mediaFileObservers.push(mediaFileObserver);
                mediaFileObservers.peek().startWatching();
                pathAdded = false;
            }else if(pathRemoved){
                System.out.println("-----[Path Removed]-----");
                mediaFileObservers.pop().stopWatching();
                pathRemoved = false;
            }
            try{
                System.out.println("++++++++++Thread is about to Sleep++++++++");
                Thread.sleep(Long.MAX_VALUE);
            }catch(InterruptedException interruptedException){
                System.out.println("--------Thread is interrupted--------");
                if(mediaFileObservers.isEmpty()){
                    mediaFileObservers = null;
                    System.out.println("=======================File Observer Thread is Completed=======================");
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
