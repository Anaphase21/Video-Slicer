package com.anaphase.videoeditor.ffmpeg;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;

import com.anaphase.videoeditor.util.Util;
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.util.ArrayList;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegTask extends Thread{

    private FFmpegCommand command;
    private Handler handler;
    private ArrayList<Pair<Integer, Integer>> times;
    private ArrayList<Integer> thumbnailTimePoints;
    private Pattern timePattern;
    private Pattern durationPattern;
    private String timeRegex = "(time=)(\\d\\d:){2}(\\d\\d\\.\\d+)";
    private String durationRegex = "(\\d\\d:){2}(\\d\\d.\\d{2})";
    private int timeSoFar;
    private Stack<Integer> previousCommandTime;
    private int endTime;
    private ArrayList<String> paths;

    public FFmpegTask(FFmpegCommand command, Handler handler, ArrayList<Pair<Integer, Integer>> times){
        this.command = command;
        this.handler = handler;
        this.times = times;
        timePattern = Pattern.compile(timeRegex);
        previousCommandTime = new Stack<>();
        previousCommandTime.push(0);
    }

    @Override
    public void run(){
        if(command.isThumbnailExtraction()){
            extractThumbnails();
            return;
        }
        Bundle bundle = new Bundle();
        endTime = totalDurationOfTask();
        Config.enableLogCallback((message)-> {
            String log = message.getText();
            String sTimeSoFar = parseTime(log);
            int progress = 0;
            if (!sTimeSoFar.isEmpty()) {
                timeSoFar = previousCommandTime.peek() + Util.convertFormattedTimeToMilliseconds(sTimeSoFar);
                progress = Math.round((timeSoFar * 100.0f) / endTime);
                bundle.putString("taskProgress", progress + "%");
                Message msg = handler.obtainMessage();
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        });
        String lastCommandOutput;
        int numCommands = times.size();
        int commandIndex = 1;
        String lastPosition = "0";
        String outputFileName;
        int duration = 0;
        Pair<Integer, Integer> cutPointsPair;
        paths = new ArrayList<>(times.size());
        int responseCode = FFmpeg.execute(command.toString());
        if((responseCode != Config.RETURN_CODE_SUCCESS) && (responseCode != Config.RETURN_CODE_CANCEL)){
            sendErrorMessage();
            return;
        }
        if(responseCode == Config.RETURN_CODE_CANCEL){
            sendCancelMessage();
            return;
        }
        notifyFileAdded(command.getOptionValue(Options.OUTPUT));
        paths.add(command.getOptionValue(Options.OUTPUT));
        previousCommandTime.push(timeSoFar);
        int ss = 0;
        while(commandIndex < numCommands) {
            cutPointsPair = times.get(commandIndex);
            lastCommandOutput = Config.getLastCommandOutput();
            outputFileName = Util.renameFileIncremental(command.getOptionValue(Options.OUTPUT));
            command.updateOption(Options.OUTPUT, outputFileName);
            lastPosition = parseTime(lastCommandOutput);
            if(cutPointsPair.first == times.get(commandIndex - 1).second) {
                ss = cutPointsPair.first;
            }else{
                ss = times.get(commandIndex).first;
            }
            command.updateOption(Options.SS, Util.toTimeUnitsFraction(ss));
            duration = Math.abs(cutPointsPair.second - ss);
            command.updateOption(Options.TO, Util.toTimeUnitsFraction(duration));
            responseCode = FFmpeg.execute(command.toString());
            if((responseCode != Config.RETURN_CODE_SUCCESS) && (responseCode != Config.RETURN_CODE_CANCEL)){
                sendErrorMessage();
                return;
            }
            if(responseCode == Config.RETURN_CODE_CANCEL){
                sendCancelMessage();
                return;
            }
            notifyFileAdded(outputFileName);
            paths.add(outputFileName);
            previousCommandTime.push(timeSoFar);
            ++commandIndex;
        }
        completeTask();
    }

    private void extractThumbnails(){
        Config.enableLogCallback((logMessage)->{
        });
        int size = thumbnailTimePoints.size();
        paths = new ArrayList(size);
        int responseCode = FFmpeg.execute(command.toString());
        Bundle bundle = new Bundle();
        if((responseCode != Config.RETURN_CODE_SUCCESS) && (responseCode != Config.RETURN_CODE_CANCEL)) {
            sendErrorMessage();
            return;
        }
        if(responseCode == Config.RETURN_CODE_CANCEL){
            sendCancelMessage();
            return;
        }
        paths.add(command.getOptionValue(Options.OUTPUT));
        int i = 0;
        int progress;
        for(int timePoint : thumbnailTimePoints){
            if(i++ == 0){
                continue;
            }
            command.updateOption(Options.OUTPUT, Util.renameFileIncremental(command.getOptionValue(Options.OUTPUT)));
            command.updateOption(Options.SS, Util.toTimeUnitsFraction(timePoint));
            responseCode = FFmpeg.execute(command.toString());
            progress = Math.round((i * 100.0f) / size);
            sendThumbnailProgress(progress);
            if((responseCode != Config.RETURN_CODE_SUCCESS) && (responseCode != Config.RETURN_CODE_CANCEL)){
                sendErrorMessage();
                return;
            }
            if(responseCode == Config.RETURN_CODE_CANCEL){
                sendCancelMessage();
                return;
            }
            notifyFileAdded(command.getOptionValue(Options.OUTPUT));
            paths.add(command.getOptionValue(Options.OUTPUT));
        }
        completeTask();
    }

    public void setThumbnailTimePoints(ArrayList<Integer> thumbnailTimePoints){
        this.thumbnailTimePoints = thumbnailTimePoints;
    }

    private String parseTime(String message){
        Matcher matcher = timePattern.matcher(message);
        String str = "";
        while(matcher.find()){
            str = matcher.group();
        }
        str = str.isEmpty() ? str : str.split("=")[1];
        return str;
    }

    private int totalDurationOfTask(){
        int totalTime = 0;
        for(Pair<Integer, Integer> pair : times){
            totalTime += pair.second - pair.first;
        }
        return totalTime;
    }

    private void sendErrorMessage(){
        Bundle bundle = new Bundle();
        bundle.putString("errorMessage", "Process couldn't complete due to an error.");
        Message message = handler.obtainMessage();
        message.setData(bundle);
        handler.sendMessage(message);
    }

    private void completeTask(){
        Bundle bundle = new Bundle();
        bundle.putInt("complete", 0);
        bundle.putStringArrayList("paths", paths);
        Message message = handler.obtainMessage();
        message.setData(bundle);
        handler.sendMessage(message);
    }

    private void sendCancelMessage(){
        Bundle bundle = new Bundle();
        bundle.putString("cancelled", "Process cancelled.");
        Message message = handler.obtainMessage();
        message.setData(bundle);
        handler.sendMessage(message);
    }

    private void notifyFileAdded(String path){
        Bundle bundle = new Bundle();
        bundle.putString("fileAdded", path);
        Message message = handler.obtainMessage();
        message.setData(bundle);
        handler.sendMessage(message);
    }

    private void sendThumbnailProgress(int progress){
        Bundle bundle = new Bundle();
        bundle.putString("taskProgress", progress+"%");
        Message message = handler.obtainMessage();
        message.setData(bundle);
        handler.sendMessage(message);
    }
}