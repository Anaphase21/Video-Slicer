package com.anaphase.videoeditor.ffmpeg;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class FFmpegCommand {

    private final String SPACE = " ";
    private final String ESCAPE_QUOTE = "\"";
    private boolean thumbnailExtraction = false;

    private Map<String, String> options;

    public FFmpegCommand(){
        options = new LinkedHashMap<>(15);
    }

    public void addOption(String option, String value){
        options.put(option, value);
    }

    public void updateOption(String option, String value){
        addOption(option, value);
    }

    public String getOptionValue(String option){
        return options.get(option);
    }

    @Override
    public String toString(){
        StringBuilder command = new StringBuilder();
        Iterator<String> iterator = options.keySet().iterator();
        String nextOption;
        while(iterator.hasNext()){
            nextOption = iterator.next();
            if(command.length() != 0) {
                if (command.charAt(command.length() - 1) != ' ') {
                    command.append(SPACE);
                }
            }
            if(nextOption.equals(Options.INPUT) || nextOption.equals(Options.OUTPUT)) {
                command.append(nextOption).append(SPACE);
                command.append(ESCAPE_QUOTE.concat(options.get(nextOption)).concat(ESCAPE_QUOTE));
            }else{
                if(command.length() != 0) {
                    if (command.charAt(command.length() - 1) != ' ') {
                        command.append(SPACE);
                    }
                }
                command.append(nextOption).append(SPACE).append(options.get(nextOption));
            }
        }
        return command.toString();
    }

    public void setThumbnailExtraction(boolean flag){
        this.thumbnailExtraction = flag;
    }

    public boolean isThumbnailExtraction(){
        return this.thumbnailExtraction;
    }
}
