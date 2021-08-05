package com.anaphase.videoeditor.ui.editor;

public enum StartTaskButtonState {
    START("START"), CANCEL("CANCEL"), SELECT("SELECT");

    String label;
    StartTaskButtonState(String label){
        this.label = label;
    }
}
