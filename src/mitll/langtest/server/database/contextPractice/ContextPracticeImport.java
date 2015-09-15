package mitll.langtest.server.database.contextPractice;
import mitll.langtest.client.sound.PlayAudioPanel;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by je24276 on 9/15/15.
 */
public class ContextPracticeImport {

    Pattern dialogEx = Pattern.compile("^\\s*Title\\s*:\\s*(.*)$");
    Pattern speakerEx = Pattern.compile("^Speaker\\s*(\\d+)\\s*:\\s*(.*)$");
    Pattern partEx = Pattern.compile("^\\s*(\\d+)\\s*:\\s*(.*)$");
    private Map<String, String> sentToAudioPath = new HashMap<String, String>();
    private Map<String, Iterable<String>> dialogToPartsMap = new HashMap<String, String[]>();
    private Map<String, String> sentToSlowAudioPath = new HashMap<String, String>();
    private Map<String, Map<String, Integer>> dialogToSpeakerToLast = new HashMap<String, HashMap<String, Integer>>();
    private

    public ContextPracticeImport(String file){
        try{
            Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            BufferedReader fin = new BufferedReader(reader);
            String s;

            while((s = fin.readLine()) != null){
                Matcher dm = dialogEx.matcher(s);
                Matcher sm = speakerEx.matcher(s);
                Matcher pm = partEx.matcher(s);
                if(dm.matches()){

                }else if(sm.matches()){

                }else if(pm.matches()){

                }
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public Map<String, String> getSentToAudioPath(){

    }

    public Map<String, Iterable<String>> getDialogToPartsMap() {

    }

    public Map<String, String> getSentToSlowAudioPath() {

    }

    public Map<String, Map<String, Integer>> getDialogToSpeakerToLast(){

    }

    public Map<String, Map<Integer, String>> getDialogToSentIndexToSpeaker(){

    }

    public Map<String, Map<Integer, String>> getDialogToSentIndexToSent(){

    }



}
