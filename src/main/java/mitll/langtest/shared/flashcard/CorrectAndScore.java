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

import mitll.langtest.client.scoring.MiniScoreListener;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/13/14.
 */
public class CorrectAndScore extends ExerciseIDAndScore implements Comparable<CorrectAndScore> {
  private int uniqueID;
  private int userid;

  /**
   * For AMAS
   */
  // private int qid;
  private boolean correct;
  /**
   * For AMAS
   */
  // private float classifierScore;
  /**
   * For AMAS
   */
  // private float userScore;

  private String path;

  private transient String scoreJson;
  private Map<NetPronImageType, List<TranscriptSegment>> scores;

  public CorrectAndScore() {
  }

  /**
   * @param score
   * @param path
   * @see MiniScoreListener#gotScore(mitll.langtest.shared.scoring.PretestScore, String)
   */
  public CorrectAndScore(float score, String path) {
    super(score);
    this.path = path;
  }

  public CorrectAndScore(AudioAnswer result) {
    this(result.getPretestScore().getHydecScore(), result.getPath());

    setScores(result.getPretestScore().getTypeToSegments());
    setJson(result.getPretestScore().getJson());
  }

  /**
   * @param uniqueID
   * @param userid
   * @param exerciseID
   * @param correct
   * @param score
   * @param timestamp
   * @param path
   * @param scoreJson
   * @see ResultDAO#getScoreResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   */
  public CorrectAndScore(int uniqueID, int userid, int exerciseID, boolean correct, float score, long timestamp,
                         String path, String scoreJson) {
    super(uniqueID, timestamp, score);
    this.uniqueID = uniqueID;
    this.exid = exerciseID;
    this.userid = userid;
    this.correct = correct;
    this.path = path;
    this.scoreJson = scoreJson;
  }

  @Override
  public int compareTo(CorrectAndScore o) {
    return Long.compare(getTimestamp(), o.getTimestamp());
  }

  public boolean isCorrect() {
    return correct;
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

  public String getScoreJson() {
    return scoreJson;
  }

  /**
   * AMAS
   * TODO : maybe put this back someday
   *
   * @return
   */
  public int getQid() {
    return 0;
  }

  /**
   * AMAS
   *
   * @return
   */
  public boolean hasUserScore() {
    return false;//userScore > -1;
  }

  public float getUserScore() {
    return 0;//userScore;
  }

  public boolean isMatch(HasID ex) {
    return getExid() == ex.getID();
  }

  public void setJson(String json) {
    this.scoreJson = json;
  }

  public Map<NetPronImageType, List<TranscriptSegment>> getScores() {
    return scores;
  }

  public void setScores(Map<NetPronImageType, List<TranscriptSegment>> scores) {
    this.scores = scores;
  }

  public String toString() {
    return "id " + getExid() + " " + (isCorrect() ? "C" : "I") + " score " + getPercentScore() +
        (scores == null ? "" : " segments for " + scores.keySet());
  }
}
