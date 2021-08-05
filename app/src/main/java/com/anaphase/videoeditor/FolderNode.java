package com.anaphase.videoeditor;

import java.util.ArrayList;

public class FolderNode {
    //private FolderNode parentFolder;
    private ArrayList<MediaFile> children;
    private String path;
    private int depthPosition;

    public FolderNode(int depthPosition, String path){
        this.depthPosition = depthPosition;
        this.path = path;
    }

    public void setDepthPosition(int depthPosition){
        this.depthPosition = depthPosition;
    }

    public int getDepthPosition(){
        return this.depthPosition;
    }

    public void setChildren(ArrayList<MediaFile> children){
        this.children = children;
    }

    public void addChild(MediaFile mediaFile){
        this.children.add(mediaFile);
    }

    public ArrayList<MediaFile> getChildren(){
        return this.children;
    }

    public void setPath(String path){
        this.path = path;
    }

    public String getPath(){
        return this.path;
    }
}
