/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.contextPractice;

import mitll.langtest.shared.ContextPractice;

import java.io.*;
import java.util.ArrayList;
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
  Pattern partEx = Pattern.compile("^\\s*(\\d+)\\s*:\\s*(.*)\\s*\\((.*),\\s*(.*)\\)\\s*$");
  private Map<String, String> sentToAudioPath = new HashMap<String, String>();
  private Map<String, String[]> dialogToPartsMap = new HashMap<String, String[]>();
  private Map<String, String> sentToSlowAudioPath = new HashMap<String, String>();
  private Map<String, HashMap<String, Integer>> dialogToSpeakerToLast = new HashMap<String, HashMap<String, Integer>>();
  private Map<String, HashMap<Integer, String>> dialogToSentIndexToSpeaker = new HashMap<String, HashMap<Integer, String>>();
  private Map<String, HashMap<Integer, String>> dialogToSentIndexToSent = new HashMap<String, HashMap<Integer, String>>();
  private ContextPractice contextPractice;

  /**
   * @param file
   * @see mitll.langtest.server.database.DatabaseImpl#makeContextPractice(String, String)
   */
  public ContextPracticeImport(String file) {
    if (file == null) {
      return;
    }
    try {
      Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
      BufferedReader fin = new BufferedReader(reader);
      String s;

      String currTitle = "";
      HashMap<String, String> speakerToIdent = new HashMap<String, String>();
      HashMap<String, String> identToSpeaker = new HashMap<String, String>();
      HashMap<String, Integer> speakerToLast = new HashMap<String, Integer>();
      HashMap<Integer, String> sentIndexToSpeaker = new HashMap<Integer, String>();
      HashMap<Integer, String> sentIndexToSent = new HashMap<Integer, String>();
      ArrayList<String> parts = new ArrayList<String>();
      int sentIndex = 0;
      while ((s = fin.readLine()) != null) {
        Matcher dm = dialogEx.matcher(s);
        Matcher sm = speakerEx.matcher(s);
        Matcher pm = partEx.matcher(s);
        if (dm.matches()) {
          if (!currTitle.equals("")) {
            dialogToPartsMap.put(currTitle, parts.toArray(new String[parts.size()]));
            dialogToSpeakerToLast.put(currTitle, speakerToLast);
            dialogToSentIndexToSent.put(currTitle, sentIndexToSent);
            dialogToSentIndexToSpeaker.put(currTitle, sentIndexToSpeaker);
          }
          currTitle = dm.group(1);
          speakerToIdent = new HashMap<String, String>();
          identToSpeaker = new HashMap<String, String>();
          speakerToLast = new HashMap<String, Integer>();
          sentIndexToSpeaker = new HashMap<Integer, String>();
          sentIndexToSent = new HashMap<Integer, String>();
          parts = new ArrayList<String>();
          sentIndex = 0;
        } else if (sm.matches()) {
          speakerToIdent.put(sm.group(2), sm.group(1));
          identToSpeaker.put(sm.group(1), sm.group(2));
          parts.add(sm.group(2));
        } else if (pm.matches()) {
          sentToAudioPath.put(pm.group(2), pm.group(3));
          sentToSlowAudioPath.put(pm.group(2), pm.group(4));
          String speaker = identToSpeaker.get(pm.group(1));
          speakerToLast.put(speaker, sentIndex);
          sentIndexToSpeaker.put(sentIndex, speaker);
          sentIndexToSent.put(sentIndex, pm.group(2));
          sentIndex += 1;
        }
      }
      dialogToPartsMap.put(currTitle, parts.toArray(new String[parts.size()]));
      dialogToSpeakerToLast.put(currTitle, speakerToLast);
      dialogToSentIndexToSent.put(currTitle, sentIndexToSent);
      dialogToSentIndexToSpeaker.put(currTitle, sentIndexToSpeaker);
      this.contextPractice = new ContextPractice(dialogToPartsMap, sentToSlowAudioPath,
          sentToAudioPath, dialogToSpeakerToLast, dialogToSentIndexToSpeaker,
          dialogToSentIndexToSent);

    } catch (IOException e) {
      e.printStackTrace();
      this.contextPractice = new ContextPractice();
    }
  }

  public ContextPractice getContextPractice() {
    return this.contextPractice;
  }
}
