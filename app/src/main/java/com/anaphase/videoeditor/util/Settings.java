package com.anaphase.videoeditor.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.anaphase.videoeditor.R;

public class Settings {

    public static ExtractionType extractionType = ExtractionType.POINT_EXTRACTION;
    public static int savedVideoCurrentPosition;

    public static void save(Context context, String key, int value){
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.settings), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static int retrieve(Context context, String key){
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.settings), Context.MODE_PRIVATE);
        return sharedPreferences.getInt(key, -1);
    }

    public enum ExtractionType{
        POINT_EXTRACTION(0),
        RANGE_EXTRACTION(1);

        int extractionType;
        ExtractionType(int extractionType){
            this.extractionType = extractionType;
        }
    }
}
