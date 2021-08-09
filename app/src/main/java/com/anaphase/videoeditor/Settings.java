package com.anaphase.videoeditor;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {

    public static ExtractionType extractionType = ExtractionType.POINT_EXTRACTION;

    public static void save(Context context, String key, int value){
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.settings), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static int retrieve(Context context, String key){
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.settings), Context.MODE_PRIVATE);
        int value = sharedPreferences.getInt(key, -1);
        return value;
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