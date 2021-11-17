package com.anaphase.videoeditor.ffmpeg;

import android.os.Handler;
import android.util.Pair;

import com.anaphase.videoeditor.util.Settings;
import com.anaphase.videoeditor.util.Util;

import java.util.ArrayList;

public class EditActions {

    Handler handler;
    FFmpegTask taskThread;

    public EditActions(Handler handler){
        this.handler = handler;
    }

    public void cut(ArrayList<Pair<Integer, Integer>> times, Codec videoCodec, Codec audioCodec, String input, boolean audioOnly){
        FFmpegCommand command = new FFmpegCommand();
        Util.createAppDirectory();
        String outputFile = Util.renameFileIncremental(Util.appendUnderscoredNumber(input));
        Pair<Integer, Integer> cutPointsPair = times.get(0);
        command.addOption(Options.SS, Util.toTimeUnitsFraction(cutPointsPair.first));
        command.addOption(Options.INPUT, input);
        command.addOption(Options.TO, Util.toTimeUnitsFraction(cutPointsPair.second - cutPointsPair.first));
        if(audioOnly){
            outputFile = Util.appendUnderscoredNumber(input);
            outputFile = Util.renameFileIncremental(Util.changeFileExtension(outputFile, ".mp3"));
            command.addOption(Options.MAP, "0:a");
            command.addOption(Options.C_A, audioCodec.codecName);
        }else {
            command.addOption(Options.C_V, videoCodec.codecName);
            command.addOption(Options.C_A, audioCodec.codecName);
            command.addOption(Options.C_S, Codec.COPY.codecName);//Copy subtitles
            command.addOption(Options.PRESET, "ultrafast");
        }
        command.addOption(Options.OUTPUT, outputFile);
        startTask(command, times, null);
    }

    public void grabThumbnails(ArrayList<Integer> thumbnailTimePoints, Codec imageCodec, String input, String frameRate){
        FFmpegCommand command = new FFmpegCommand();
        String outputFile = Util.appendUnderscoredNumber(input);
        outputFile = Util.renameFileIncremental(Util.changeFileExtension(outputFile, ".jpeg"));
        command.addOption(Options.SS, Util.toTimeUnitsFraction(thumbnailTimePoints.get(0)));
        command.addOption(Options.INPUT, input);
        command.addOption(Options.SS, Util.toTimeUnitsFraction(thumbnailTimePoints.get(0)));
        if(Settings.extractionType == Settings.ExtractionType.POINT_EXTRACTION){
            command.addOption(Options.FRAMES, "1");
        }else {//In case range extraction is supported in the future.
            command.addOption(Options.R, frameRate);
            command.addOption(Options.TO, Util.toTimeUnitsFraction(1000));
        }
        command.addOption(Options.CODEC, imageCodec.codecName);
        command.addOption(Options.OUTPUT, outputFile);
        command.setThumbnailExtraction(true);
        startTask(command, null, thumbnailTimePoints);
    }

    private void startTask(FFmpegCommand command, ArrayList<Pair<Integer, Integer>> times, ArrayList<Integer> thumbnailTimePoints){
        taskThread = new FFmpegTask(command, handler, times);
        if(thumbnailTimePoints != null){
            taskThread.setThumbnailTimePoints(thumbnailTimePoints);
        }
        taskThread.start();
    }
}
