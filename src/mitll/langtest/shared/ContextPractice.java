/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by je24276 on 9/20/15.
 */
public class ContextPractice implements CommonContextPractice{

    public ContextPractice(){}; //required for serialization

    public ContextPractice toContextPractice(){return this;}

    private Map<String, String[]> dialogToPartsMap;

    private Map<String, String> sentToSlowAudioPath;

    private Map<String, String> sentToAudioPath;

    private Map<String, HashMap<String, Integer>> dialogToSpeakerToLast;

    private Map<String, HashMap<Integer, String>> dialogToSentIndexToSpeaker;

    private Map<String, HashMap<Integer, String>> dialogToSentIndexToSent;

    public ContextPractice(Map<String, String[]> dialogToPartsMap, Map<String, String> sentToSlowAudioPath, Map<String, String> sentToAudioPath, Map<String, HashMap<String, Integer>> dialogToSpeakerToLast, Map<String, HashMap<Integer, String>> dialogToSentIndexToSpeaker, Map<String, HashMap<Integer, String>> dialogToSentIndexToSent){
        this.dialogToPartsMap = dialogToPartsMap;
        this.sentToSlowAudioPath = sentToSlowAudioPath;
        this.sentToAudioPath = sentToAudioPath;
        this.dialogToSpeakerToLast = dialogToSpeakerToLast;
        this.dialogToSentIndexToSpeaker = dialogToSentIndexToSpeaker;
        this.dialogToSentIndexToSent = dialogToSentIndexToSent;
    }

    public Map<String, String[]> getDialogToPartsMap() {return this.dialogToPartsMap;}
    public Map<String, String> getSentToSlowAudioPath() {return this.sentToSlowAudioPath;}
    public Map<String, String> getSentToAudioPath() {return this.sentToAudioPath;}
    public Map<String, HashMap<String, Integer>> getDialogToSpeakerToLast() {return this.dialogToSpeakerToLast;}
    public Map<String, HashMap<Integer, String>> getDialogToSentIndexToSpeaker() {return this.dialogToSentIndexToSpeaker;}
    public Map<String, HashMap<Integer, String>> getDialogToSentIndexToSent() {return this.dialogToSentIndexToSent;}
}
