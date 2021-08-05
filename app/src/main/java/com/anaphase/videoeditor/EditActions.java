package com.anaphase.videoeditor;

import android.os.Handler;
import android.util.Pair;

import java.util.ArrayList;

public class EditActions {

    Handler handler;
    FFmpegTask taskThread;

    public EditActions(Handler handler){
        this.handler = handler;
    }

    public void cut(ArrayList<Pair<Integer, Integer>> times, Codec videoCodec, Codec audioCodec, String input, boolean audioOnly){
        FFmpegCommand command = new FFmpegCommand();
        for(Pair<Integer, Integer> pair : times){
            if(pair != null) {
                System.out.println(Util.toTimeUnits(pair.first) + "   " + Util.toTimeUnits(pair.second));
            }
        }
        System.out.println("Times Size: "+times.size());
        String outputFile = Util.renameFileIncremental(Util.appendUnderscoredNumber(input));
        //int from = times.get(0);
        Pair<Integer, Integer> cutPointsPair = times.get(0);
        command.addOption(Options.SS, Util.toTimeUnitsFraction(cutPointsPair.first));
        command.addOption(Options.INPUT, input);
        //command.addOption(Options.SS, Util.toTimeUnitsFraction(cutPointsPair.first));
        //command.addOption(Options.TO, Util.toTimeUnitsFraction(cutPointsPair.second));
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
            //command.addOption(Options.C_V, videoCodec.codecName);
        }

        //
        command.addOption(Options.OUTPUT, outputFile);
        startTask(command, times, null);
    }

    public void grabThumbnails(ArrayList<Integer> thumbnailTimePoints, Codec imageCodec, String input, String framerate){
        FFmpegCommand command = new FFmpegCommand();
        String outputFile = Util.appendUnderscoredNumber(input);
        outputFile = Util.renameFileIncremental(Util.changeFileExtension(outputFile, ".jpeg"));
        command.addOption(Options.INPUT, input);
        command.addOption(Options.SS, Util.toTimeUnitsFraction(thumbnailTimePoints.get(0)));
        if(Settings.extractionType == Settings.ExtractionType.POINT_EXTRACTION){
            command.addOption(Options.FRAMES, "1");
        }else {
            command.addOption(Options.R, framerate);
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
