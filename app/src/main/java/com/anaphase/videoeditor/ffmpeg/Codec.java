package com.anaphase.videoeditor.ffmpeg;

public enum Codec {
    COPY("copy"),
    MP3("libmp3lame"),
    AAC("aac"),
    MJPEG("mjpeg"),
    H264("libx264"),
    FFV1("ffv1"),
    FLAC("flac"),
    VP8("libvpx"),
    VP9("libpx-vp9"),
    OPUS("libopus"),
    VORBIS("vorbis");


    String codecName;
    Codec(String codecName){
        this.codecName = codecName;
    }
}
