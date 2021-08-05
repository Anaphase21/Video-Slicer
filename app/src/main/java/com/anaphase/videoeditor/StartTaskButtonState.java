package com.anaphase.videoeditor;

import android.widget.Button;

public enum StartTaskButtonState {
    START("START"), CANCEL("CANCEL"), SELECT("SELECT");

    String label;
    StartTaskButtonState(String label){
        this.label = label;
    }
}
