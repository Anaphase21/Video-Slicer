package com.anaphase.videoeditor.ui.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.VideoView;

import com.anaphase.videoeditor.util.Util;

import java.util.ArrayList;

import java.util.Stack;

public class Timeline extends View {
    private Paint paint = new Paint();
    private int blurColor = (255 << 24) | (21 << 16) | (21 << 8) | 21;
    private int height;
    private int width;
    private volatile float slider_x = 0.0f;
    private float slider_y = 0.0f;
    private final float PADDING_DP = 20.0f;
    private final float KNOB_HEIGHT_DP = 20.0f;
    private float TEXT_SIZE_SMALL_SP = 6.0f;
    private float TEXT_SIZE_MEDIUM_SP = 10.0f;
    private float TIME_AREA_HEIGHT_DP = 13.0f;
    private float scale = getResources().getDisplayMetrics().density;
    private float offset = PADDING_DP * scale;
    private float textSizeSmall = scale * TEXT_SIZE_SMALL_SP;
    private float textSizeMedium = scale * TEXT_SIZE_MEDIUM_SP;
    private float knobHeight = KNOB_HEIGHT_DP * scale;
    private float timeAreaHeight = TIME_AREA_HEIGHT_DP * scale;
    protected float timeBarWidth;
    private float timeBarHeight;
    private float constantIntervalStartPoint;
    private float constantIntervalEndPoint;
    private final int TEXT_COLOR_WHITE = (255 << 24) | (255 << 16) | (255 << 8) | 255;
    private String videoDuration;
    protected Stack<Float> cutPoints = new Stack<>();
    private VideoView videoView;
    private Object[] sortedCutPoints;

    private Bitmap offScreenBitmap;
    private Canvas canvas;

    public Timeline(Context context) {
        super(context);
    }

    public Timeline(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Timeline(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if((width == 0) || (height == 0)){
            width = getWidth();
            height = getHeight();
            timeBarHeight = height - (timeAreaHeight + knobHeight);
            timeBarWidth = width - offset;
        }
        offScreenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.canvas = new Canvas(offScreenBitmap);
        doubleBufferCanvas();
        canvas.drawBitmap(offScreenBitmap,0, 0, new Paint());
    }

    private void doubleBufferCanvas(){
        paint.setColor(blurColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, width, knobHeight, paint);
        canvas.drawRect(0, knobHeight, offset - (offset / 2.0f), height, paint);
        canvas.drawRect(width - (offset / 2.0f), knobHeight, width, height, paint);
        canvas.drawRect(0, knobHeight + timeBarHeight, width, height, paint);
        paint.setColor((255 << 24) | (200 << 16));
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawRect(slider_x, slider_y, slider_x + offset, knobHeight, paint);
        canvas.drawLine(slider_x + (offset / 2.0f), slider_y, slider_x + (offset / 2.0f), height - timeAreaHeight, paint);
        int numCuts = cutPoints.size();
        sortedCutPoints = cutPoints.stream().sorted().toArray();
        paint.setTextSize(textSizeSmall);
        for(int i = numCuts - 1; i > -1; --i){
            paint.setColor(TEXT_COLOR_WHITE);
            if((float)sortedCutPoints[0] == 0.0f){
                if((float)sortedCutPoints[i] < timeBarWidth) {
                    canvas.drawText(i + 1 + "", (float) sortedCutPoints[i] + (offset / 2.0f) + 20.0f, 20, paint);
                }
            }else {
                canvas.drawText(i + 1 + "", (float) sortedCutPoints[i] + (offset / 2.0f) - 20.0f, 20, paint);
            }
            paint.setColor((255 << 24) | (200 << 16));
            canvas.drawLine(cutPoints.elementAt(i) + offset / 2.0f, slider_y, cutPoints.elementAt(i) + offset / 2.0f, height - timeAreaHeight, paint);
        }
        if(numCuts > 0) {
            paint.setColor(TEXT_COLOR_WHITE);
            if((float)sortedCutPoints[0] > 0.0f) {
                if((float)sortedCutPoints[numCuts - 1] < timeBarWidth) {
                    canvas.drawText(numCuts + 1 + "", (float) sortedCutPoints[numCuts - 1] + (offset / 2.0f) + 20.0f, 20, paint);
                }
            }
        }
        if(videoView != null) {
            paint.setColor(TEXT_COLOR_WHITE);
            paint.setTextSize(textSizeMedium);
            canvas.drawText(Util.toTimeUnits(getSeekPosition(slider_x)), 0, height, paint);
            canvas.drawText(videoDuration, width - paint.measureText(videoDuration), height, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(event.getActionMasked() == MotionEvent.ACTION_DOWN){
            slider_x = getXNormalizedToBarWidth(event.getX());
            videoView.seekTo(getSeekPosition(slider_x));
            ((EditFileActivity)getContext()).setCurrentPosition(videoView.getCurrentPosition());
            invalidate();
            return true;
        }
        if(event.getActionMasked() == MotionEvent.ACTION_MOVE){
            slider_x = getXNormalizedToBarWidth(event.getX());
            videoView.seekTo(getSeekPosition(slider_x));
            ((EditFileActivity)getContext()).setCurrentPosition(videoView.getCurrentPosition());
            invalidate();
            return true;
        }
        return false;
    }

    public float getSlider_x(){
        return slider_x;
    }

    public void setSlider_x(float x){
        this.slider_x = ((x * timeBarWidth) / 100.0f);
        invalidate();
    }

    public void setVideoView(VideoView videoView){
        this.videoView = videoView;
        videoDuration = Util.toTimeUnits(videoView.getDuration());
        invalidate();
    }

    //Convert the slider position which is in pixels into
    //time position to be seeked.
    private int getSeekPosition(float sliderPosition){
        float proportion = sliderPosition / timeBarWidth;
        float time = proportion * videoView.getDuration();
        return (int)time;
    }

    private float getXNormalizedToBarWidth(float x){
        if(x < offset / 2.0f){
            return 0.0f;
        }
        if(x > width - offset / 2.0f){
            return timeBarWidth;
        }
        return x - offset / 2.0f;
    }

    public ArrayList<Integer> getCutPoints(){
        ArrayList<Integer> sortedCuts = new ArrayList<>();
        for(Float cutPoint : cutPoints){
            sortedCuts.add(getSeekPosition(cutPoint));
        }
        sortedCuts.sort((i1, i2)->{
            if(i1 < i2)
                return -1;
            else if(i1 > i2){
                return 1;
            }
            return 0;
        });
        return sortedCuts;
    }

    public ArrayList<Integer> getPaddedCutPoints(){
        ArrayList<Integer> sortedCuts = getCutPoints();
        if((sortedCuts.size() == 0) || (sortedCuts.size() > 0 && sortedCuts.get(0) > 0)){
            sortedCuts.add(0, 0);
        }
        int size = sortedCuts.size();
        int duration = videoView.getDuration();
        if(sortedCuts.get(size - 1) < duration){
            sortedCuts.add(duration);
        }
        return sortedCuts;
    }

    public void setRegularCutPoints(int interval){
        int duration = videoView.getDuration();
        clearCutPoints();
        float percentageTimeIncrement = (interval * 100.0f) / duration;
        float pixelIncrement = (percentageTimeIncrement * timeBarWidth) / 100.0f;
        float cutPoint = constantIntervalStartPoint;
        if(constantIntervalEndPoint == 0) {
            setConstantIntervalEndPoint(timeBarWidth);
        }
        while((cutPoint += pixelIncrement) < constantIntervalEndPoint){
            cutPoints.push(cutPoint);
        }
        cutPoints.push(constantIntervalStartPoint);
        cutPoints.push(constantIntervalEndPoint);
        invalidate();
    }

    public void setConstantIntervalStartPoint(float startPoint){
        constantIntervalStartPoint = startPoint;
    }

    public void setConstantIntervalEndPoint(float endPoint){
        constantIntervalEndPoint = endPoint;
    }

    public void clearCutPoints(){
        if(cutPoints != null){
            cutPoints.clear();
        }
    }

    public float getTimeBarWidth(){
        return timeBarWidth;
    }
}