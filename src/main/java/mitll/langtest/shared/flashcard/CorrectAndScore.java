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
import mitll.langtest.shared.answer.SimpleAudioAnswer;
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
  private boolean correct;
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

  public CorrectAndScore(SimpleAudioAnswer result) {
    this(result.getPretestScore().getHydecScore(), result.getPath());

    PretestScore pretestScore = result.getPretestScore();
    setScores(pretestScore.getTypeToSegments());
    setJson(pretestScore.getJson());
  }

  /**
   * @param exerciseID
   * @param correct
   * @param score
   * @param timestamp
   * @param path
   * @param scoreJson
   * @see ResultDAO#getScoreResultsForQuery(java.sql.Connection, java.sql.PreparedStatement)
   */
  public CorrectAndScore(int exerciseID, boolean correct, float score, long timestamp,
                         String path, String scoreJson) {
    super(exerciseID, timestamp, score);
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

  public long getTimestamp() {
    return timestamp;
  }

  public String getPath() {
    return path;
  }

  public String getScoreJson() {
    return scoreJson;
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
