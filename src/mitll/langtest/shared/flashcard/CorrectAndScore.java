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

package mitll.langtest.shared.flashcard;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.result.ResultDAO;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/13/14.
 */
public class CorrectAndScore implements IsSerializable, Comparable<CorrectAndScore> {
  private int uniqueID;
  private int userid;

  private int exid;
  /**
   * For AMAS
   */
  private int qid;
  private boolean correct;
  private float score;
  /**
   * For AMAS
   */
  private float classifierScore;
  /**
   * For AMAS
   */
  private float userScore;

  private long timestamp;
  private String path;
  private transient String scoreJson;

  public CorrectAndScore() {}

  /**
   * @see mitll.langtest.client.gauge.ASRScorePanel#gotScore(mitll.langtest.shared.scoring.PretestScore, boolean, String)
   * @param score
   * @param path
   */
  public CorrectAndScore(float score, String path) {
    this.score = score;
    this.path = path;
  }

  /**
   * @see ResultDAO#getScoreResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   * @param uniqueID
   * @param userid
   * @param exerciseID
   * @param correct
   * @param score
   * @param timestamp
   * @param path
   * @param scoreJson
   */
  public CorrectAndScore(int uniqueID, int userid, int exerciseID, boolean correct, float score, long timestamp,
                         String path, String scoreJson) {
    this.uniqueID = uniqueID;
    this.exid = exerciseID;
    this.userid = userid;
    this.correct = correct;
    this.score = score;
    this.timestamp = timestamp;
    this.path = path;
    this.scoreJson = scoreJson;
  }

  @Override
  public int compareTo(CorrectAndScore o) {
    return getTimestamp() < o.getTimestamp() ? -1 : getTimestamp() > o.getTimestamp() ? +1 : 0;
  }

  public boolean isCorrect() {
    return correct;
  }

  /**
   * @return 0-100
   */
  public float getScore() {
    return score;
  }

  public int getPercentScore() {
    return Math.round(100f * score);
  }

  public int getExid() {
    return exid;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getPath() {
    return path;
  }

  public int getUniqueID() {
    return uniqueID;
  }

  public int getUserid() {
    return userid;
  }

  public String getScoreJson() { return scoreJson;  }

  public int getQid() {
    return qid;
  }

  public boolean hasUserScore() { return userScore > -1; }

  public float getUserScore() {
    return userScore;
  }
/*
  public float getClassifierScore() {
    return classifierScore;
  }
*/


  public String toString() {
    return "id " + getExid() + " " + (isCorrect() ? "C" : "I") + " score " + getPercentScore();
  }
}
