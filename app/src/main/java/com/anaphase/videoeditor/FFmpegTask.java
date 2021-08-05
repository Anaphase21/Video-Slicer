package com.anaphase.videoeditor;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;

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
        //durationPattern = Pattern.compile(durationRegex);
        previousCommandTime = new Stack<>();
        previousCommandTime.push(0);
    }

    @Override
    public void run(){
        if(command.isThumbnailExtraction()){
            extractThumbnails();
            return;
        }
        //ss = Util.toTimeUnits(times.get(0).first);
        Bundle bundle = new Bundle();
        endTime = totalDurationOfTask();
        Config.enableLogCallback((message)-> {
            //System.out.println(message.getText());
            String log = message.getText();
            String sTimeSoFar = parseTime(log);
            int progress = 0;
            if (!sTimeSoFar.isEmpty()) {
                timeSoFar = previousCommandTime.peek() + Util.convertFormattedTimeToMilliseconds(sTimeSoFar);
                //previousCommandTime.offer(Util.convertFormattedTimeToMilliseconds(sTimeSoFar));
                //System.out.println(sTimeSoFar+", "+previousCommandTime+", "+timeSoFar);
                progress = Math.round((timeSoFar * 100.0f) / endTime);
                bundle.putString("taskProgress", progress + "%");
                System.out.println(progress+"%");
                Message msg = handler.obtainMessage();
                msg.setData(bundle);
                handler.sendMessage(msg);
                //System.out.println("So far Second: "+timeSoFar);
            }
            //ss = parseTime(message.getText());
        });
        String lastCommandOutput;
        int numCommands = times.size();
        int commandIndex = 1;
        String lastPosition = "0";
        String outputFileName;
        int duration = 0;
        Pair<Integer, Integer> cutPointsPair;
        //System.out.println(command.toString());
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
        //previousCommandTime.offer(times.get(0).second);
        int ss = 0;
        while(commandIndex < numCommands) {
            cutPointsPair = times.get(commandIndex);
            System.out.println(command.toString());
            lastCommandOutput = Config.getLastCommandOutput();
            //System.out.println(parseTime("========"+lastCommandOutput+"======="));
            outputFileName = Util.renameFileIncremental(command.getOptionValue(Options.OUTPUT));
            command.updateOption(Options.OUTPUT, outputFileName);
            //System.out.println("========"+lastCommandOutput+"========");
            lastPosition = parseTime(lastCommandOutput);
            if(cutPointsPair.first == times.get(commandIndex - 1).second) {
                ss = cutPointsPair.first;
                //ss += Util.convertFormattedTimeToMilliseconds(lastPosition);
                //System.out.println("====ss: "+ss+"=======");
            }else{
                ss = times.get(commandIndex).first;
            }
            command.updateOption(Options.SS, Util.toTimeUnitsFraction(ss));
            duration = Math.abs(cutPointsPair.second - ss);
            //duration = Math.abs(cutPointsPair.second);
            command.updateOption(Options.TO, Util.toTimeUnitsFraction(duration));
            System.out.println(command.toString());
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
            //previousCommandTime.offer(cutPointsPair.second);
            ++commandIndex;
        }
        completeTask();
        //System.out.println("So far: "+timeSoFar+", End: "+endTime);
    }

    private void extractThumbnails(){
        Config.enableLogCallback((logMessage)->{
            System.out.println(logMessage.getText());
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
        System.out.println(command.toString());
        int i = 0;
        int progress;
        for(int timePoint : thumbnailTimePoints){
            if(i++ == 0){
                continue;
            }
            command.updateOption(Options.OUTPUT, Util.renameFileIncremental(command.getOptionValue(Options.OUTPUT)));
            command.updateOption(Options.SS, Util.toTimeUnitsFraction(timePoint));
            System.out.println(command.toString());
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
            System.out.println(Config.getLastCommandOutput());
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