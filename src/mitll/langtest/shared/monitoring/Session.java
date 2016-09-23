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

package mitll.langtest.shared.monitoring;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.shared.flashcard.SetScore;

import java.util.*;

/**
* Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/11/13
* Time: 7:07 PM
* To change this template use File | Settings | File Templates.
*/
public class Session implements IsSerializable, SetScore {
  private int numAnswers;
  public long duration;
  private int userid;
  private int correct;
  private float correctPercent;
  private float avgScore;
  private Map<Integer,Boolean> exidToCorrect = new HashMap<>();
  private Map<Integer,Float>   exidToScore = new HashMap<>();
  private long timestamp;

  private Set<Integer> exids = new HashSet<>();
  private boolean latest;
  private int id;

  public Session(){} // required

  /**
   * @see ResultDAO#partitionIntoSessions2
   * @param userid
   * @param timestamp
   */
  public Session(int id, int userid, long timestamp) {
    this.userid = userid;
    this.id = id;
    this.timestamp = timestamp;
  }

  private long getAverageDurMillis() { return duration/ getNumAnswers(); }
  public  long getSecAverage() { return (duration/ getNumAnswers())/1000; }

  /**
   * @see ResultDAO#partitionIntoSessions2
   * @param id
   */
  public void addExerciseID(int id) { exids.add(id);  }

  public int getNumAnswers() {
    return exids == null? numAnswers : exids.size();
  }

  public void setNumAnswers() {
    this.numAnswers = exids.size();
    this.avgScore = calcAvgScore();
    this.correct = calcCorrect();
    correctPercent = 100f*((float)correct/(float)numAnswers);
   // System.out.println("setNumAnswers correct "+ correct + "total "  +exidToCorrect.size() + " % = " + correctPercent);
    exids = null;
    exidToCorrect = null;
    exidToScore = null;
  }

  private int calcCorrect() {
    int count = 0;
    for (Boolean correct : exidToCorrect.values()) {
      if (correct) count++;
    }
    return count;
  }

  private float calcAvgScore() {
    float total = 0f;
    Collection<Float> values = exidToScore.values();
    int num = 0;
    for (Float score : values) {
      if (score > 0f) {
        num++;
        total += score;
      }
    }
    float v = num == 0 ? 0f : (total / (float) num);
    //System.out.println("calcAvgScore scores "+ values + " = total " + total + " num " + num + " = "  +v);

    return v;
  }

  @Override
  public int getCorrect() {
    return correct;
  }

  @Override
  public float getAvgScore() { return avgScore; }

  public void incrementCorrect(int id, boolean correct) {
    exidToCorrect.put(id, correct);
  }

  public void setScore(int id, float pronScore) {
    exidToScore.put(id, pronScore);
  }

  @Override
  public int getUserid() {
    return userid;
  }

//  public void setUserid(int userid) {
//    this.userid = userid;
//  }

  public float getCorrectPercent() {
    return correctPercent;
  }

  /**
   * @see ResultDAO#partitionIntoSessions2(java.util.List, java.util.Collection, long)
   * @param latest
   */
  public void setLatest(boolean latest) {
    this.latest = latest;
  }

  public boolean isLatest() {
    return latest;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String toString() {
    return "id " + id+
      " user " + userid+
      " num " + getNumAnswers() + " dur " + duration/(60*1000) + " minutes, avg " + getAverageDurMillis()/1000 +
      " secs, correct = " + correct + "(" + correctPercent+
      "%) avg score : " + avgScore;
  }
}
