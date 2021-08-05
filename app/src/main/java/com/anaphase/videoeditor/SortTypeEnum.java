package com.anaphase.videoeditor;

public enum SortTypeEnum {
    BY_NAME(0),
    BY_DATE(1),
    BY_SIZE(2),
    BY_DURATION(3);

    int sortType;
    SortTypeEnum(int sortType){
        this.sortType = sortType;
    }
}
