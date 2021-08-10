package com.anaphase.videoeditor.ui.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.CompoundButton;

public class SelectPartButton extends CompoundButton {

    private Pair<Integer, Integer> cutPoints;

    public SelectPartButton(Context context){
        super(context);
    }

    public SelectPartButton(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public SelectPartButton(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
    }

    public SelectPartButton (Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setCutPoints(Pair<Integer, Integer> cutPoints){
        this.cutPoints = cutPoints;
    }

    public Pair<Integer, Integer> getCutPoints(){
        return this.cutPoints;
    }
}
