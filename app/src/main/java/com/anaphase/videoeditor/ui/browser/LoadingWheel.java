package com.anaphase.videoeditor.ui.browser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class LoadingWheel extends View {
    private final Paint paint = new Paint();
    private RectF majorOval;
    private RectF minorOval;
    private float height;
    private float width;
    private float left;
    private float right;
    private float top;
    private float bottom;
    private final float[] center = new float[2];
    private final float majorRadius = 195.0f;
    private final float minorRadius = 95.0f;
    private int sweepAngle = 0;
    private int percent = 0;
    private final float textSize = 35.0f;
    private final int majorOvalColor = 0xff836fa9;
    private final int minorOvalColor = 0x99000000;
    private final String loadingText = "Scanning Media Files...";

    public LoadingWheel(Context context){
        super(context);
    }

    public LoadingWheel(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public LoadingWheel(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(width == 0){
            width = getWidth();
            height = getHeight();
            center[0] = width / 2.0f;
            center[1] = height / 2.0f;
            left = center[0] - majorRadius;
            right = center[0] + majorRadius;
            top = center[1] - majorRadius;
            bottom = center[1] + majorRadius;
            majorOval = new RectF(left, top, right, bottom);
            minorOval = new RectF(center[0] - minorRadius, center[1] - minorRadius, center[0] + minorRadius, center[1] + minorRadius);
        }
        paint.setColor(majorOvalColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawArc(majorOval, 270, sweepAngle, true, paint);
        paint.setColor(minorOvalColor);
        canvas.drawArc(minorOval, 0, 360, true, paint);
        paint.setColor(0xffffffff);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.SANS_SERIF);
        canvas.drawText(percent + "%", center[0] - paint.measureText(percent + "%") / 2.0f, center[1] + textSize / 2.0f, paint);
        canvas.drawText(loadingText, (width / 2.0f) - paint.measureText(loadingText) / 2.0f, height - 10, paint);
    }

    public void setSweepAngleAndPercentage(int sweepAngle){
        this.sweepAngle = sweepAngle;
        this.percent = (100 * sweepAngle) / 360;
        invalidate();
    }
}
