/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared;

import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/20/15.
 */
public class ContextPractice implements CommonContextPractice {
  //required for serialization
  public ContextPractice() {
  }

  public ContextPractice toContextPractice() {
    return this;
  }

  private Map<String, String[]> dialogToPartsMap;

  private Map<String, String> sentToSlowAudioPath;

  private Map<String, String> sentToAudioPath;

  private Map<String, Map<String, Integer>> dialogToSpeakerToLast;

  private Map<String, Map<Integer, String>> dialogToSentIndexToSpeaker;

  private Map<String, Map<Integer, String>> dialogToSentIndexToSent;

  public ContextPractice(Map<String, String[]> dialogToPartsMap,
                         Map<String, String> sentToSlowAudioPath,
                         Map<String, String> sentToAudioPath,
                         Map<String, Map<String, Integer>> dialogToSpeakerToLast,
                         Map<String, Map<Integer, String>> dialogToSentIndexToSpeaker,
                         Map<String, Map<Integer, String>> dialogToSentIndexToSent) {
    this.dialogToPartsMap = dialogToPartsMap;
    this.sentToSlowAudioPath = sentToSlowAudioPath;
    this.sentToAudioPath = sentToAudioPath;
    this.dialogToSpeakerToLast = dialogToSpeakerToLast;
    this.dialogToSentIndexToSpeaker = dialogToSentIndexToSpeaker;
    this.dialogToSentIndexToSent = dialogToSentIndexToSent;
  }

  public Map<String, String[]> getDialogToPartsMap() {
    return this.dialogToPartsMap;
  }

  public Map<String, String> getSentToSlowAudioPath() {
    return this.sentToSlowAudioPath;
  }

  public Map<String, String> getSentToAudioPath() {
    return this.sentToAudioPath;
  }

  public Map<String, Map<String, Integer>> getDialogToSpeakerToLast() {
    return this.dialogToSpeakerToLast;
  }

  public Map<String, Map<Integer, String>> getDialogToSentIndexToSpeaker() {
    return this.dialogToSentIndexToSpeaker;
  }

  public Map<String, Map<Integer, String>> getDialogToSentIndexToSent() {
    return this.dialogToSentIndexToSent;
  }
}
