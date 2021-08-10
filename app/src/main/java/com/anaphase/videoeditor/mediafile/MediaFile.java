package com.anaphase.videoeditor.mediafile;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.anaphase.videoeditor.ui.browser.BaseFileBrowserActivity;
import com.anaphase.videoeditor.ui.browser.FileBrowserActivity;
import com.anaphase.videoeditor.ui.browser.MediaStoreFileBrowser;
import com.anaphase.videoeditor.util.Util;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileFilter;

public class MediaFile{

    private String fileName;
    private String path;
    private int fileDuration = -1;
    private long lastModified = -1;
    private long fileSize = -1;
    private Bitmap thumbnail;
    private Icon fileIcon;
    private Uri uri;
    private Context context;
    private long position;
    private int fileCount;
    private boolean checked;
    private MaterialCardView cardView;

    public MediaFile(){
    }

    public MediaFile(String path){
        setPath(path);
    }

    public void setFileName(String fileName){
        this.fileName = fileName;
    }

    public void setPath(String path){
        this.path = path;
    }

    public void setFileDuration(int fileDuration){
        this.fileDuration = fileDuration;
    }

    public void setLastModified(long lastModified){
        this.lastModified = lastModified;
        if((fileSize > -1) && (fileDuration > -1)){
            sendMessage((new File(getPath())).getParent(), "FILE_LOADED");
        }
    }

    public void setFileSize(long fileSize){
        this.fileSize = fileSize;
    }

    public void setThumbnail(Bitmap thumbnail){
        this.thumbnail = thumbnail;
    }

    public void setFileIcon(Icon fileIcon){
        this.fileIcon = fileIcon;
    }

    public void setChecked(boolean checked){
        this.checked = checked;
        if(cardView != null){
            cardView.setChecked(checked);
        }
    }

    public boolean isChecked(){
        return this.checked;
    }

    public void setCardView(MaterialCardView cardView){
        this.cardView = cardView;
    }

    public MaterialCardView getCardView(){
        return this.cardView;
    }

    public void setContext(Context context){
        this.context = context;
    }

    public Context getContext(){
        return this.context;
    }

    public void setUri(Uri uri){
        this.uri = uri;
    }

    public Uri getUri(){
        return uri;
    }

    public Icon getFileIcon(){
        return this.fileIcon;
    }

    public void setPosition(long position){
        this.position = position;
    }

    public long getPosition(){
        return position;
    }

    public String getFileName(){
        return this.fileName;
    }

    public String getPath(){
        return this.path;
    }

    public void setFileCount(int fileCount){
        this.fileCount = fileCount;
    }

    public int getFileCount(){
        if(context instanceof FileBrowserActivity){
            File file = new File(getPath());
            if(file.getPath().equals("/storage/emulated")){
                setPath(getPath().concat(File.separator).concat("0"));
                setFileName("Phone Storage");
                file = new File(getPath());
            }
            FileFilter filter = (fileElement)->{
                return (Util.isVideoExtension(fileElement.getPath()) || Util.isAudioExtension(fileElement.getPath()));
            };

            File[] files = file.listFiles(filter);
            return files == null ? 0 : files.length;
        }
        return ((MediaStoreFileBrowser)context).getDirectoryFileCount(getPath());
    }

    public int getFileDuration(){
        return this.fileDuration;
    }

    public long getLastModified() {
        if(lastModified == 0){
            try{
                 setLastModified((new File(getPath())).lastModified());
            }catch(SecurityException securityException){
                securityException.printStackTrace();
            }
        }
        return lastModified;
    }

    public long getFileSize(){
        if(fileSize < 0){
            try{
                setFileSize((new File(getPath())).length());
            }catch(SecurityException securityException){
                securityException.printStackTrace();
            }
        }
        return fileSize;
    }

    public Bitmap getThumbnail(){
        return this.thumbnail;
    }

    public boolean toggle(){
        setChecked(!checked);
        if(cardView != null){
            cardView.setChecked(checked);
        }
        return checked;
    }

    public boolean isDirectory(){
        return ((new File(getPath())).isDirectory());
    }

    public boolean isAudio(){
        return (isTrue(new String[]{".mp3", ".wav", ".aac", ".m4a"}));
    }

    public boolean isVideo(){
        return (isTrue(new String[]{".mp4", ".mkv", ".webm", ".3gp", ".3gpp"}));
    }

    public boolean isImage(){return (isTrue(new String[]{".jpeg", ".png"}));}

    private boolean isTrue(String[] extensions){
        if(path != null) {
            for (String extension : extensions) {
                if (path.toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendMessage(String parentFile, String key){
        Handler handler;
        if(context instanceof BaseFileBrowserActivity) {
            handler = ((BaseFileBrowserActivity) context).handler;
            Bundle bundle = new Bundle();
            bundle.putString(key, parentFile);
            Message msg = handler.obtainMessage();
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }
}