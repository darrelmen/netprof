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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/13/14.
 */
public class ExerciseCorrectAndScore implements IsSerializable, Comparable<ExerciseCorrectAndScore> {
  private static final int MAX_TO_USE = 5;
  private List<CorrectAndScore> correctAndScores = new ArrayList<CorrectAndScore>();
  private int id;

  // required for rpc
  public ExerciseCorrectAndScore() {
  }

  /**
   * @param id
   * @see ResultDAO#getSortedAVPHistory
   */
  public ExerciseCorrectAndScore(int id) {
    this.id = id;
  }

  @Override
  public int compareTo(ExerciseCorrectAndScore o) {
    if (isEmpty() && o.isEmpty()) {
      return compareIDs(o); // TODO : consider compare on tooltip
    } else if (isEmpty()) return -1;
    else if (o.isEmpty()) return +1;
    else { // neither is empty
      return compScores(o);
    }
  }

  private int compareIDs(ExerciseCorrectAndScore o) {
    return Integer.compare(getId(), o.getId());
  }

  /**
   * @param o
   * @param fl
   * @param otherFL
   * @return
   * @see ResultDAO#getSortedAVPHistory
   */
  public int compareTo(ExerciseCorrectAndScore o, String fl, String otherFL) {
    if (isEmpty() && o.isEmpty()) {
      return fl.compareTo(otherFL);
    } else if (isEmpty()) return -1;
    else if (o.isEmpty()) return +1;
    else { // neither is empty
      return compScores(o);
    }
  }

  private int compScores(ExerciseCorrectAndScore o) {
    int myI = getDiff();
    int oI = o.getDiff();
    int i = Integer.compare(myI, oI);
    if (i == 0) {
      float myScore = getAvgScore();
      float otherScore = o.getAvgScore();

      return myScore < otherScore ? -1 : myScore > otherScore ? +1 : compareIDs(o);
    } else {
      return i;
    }
  }

  public boolean isEmpty() {
    return correctAndScores.isEmpty();
  }

  private int getNumCorrect() {
    int c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScoresLimited();
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.isCorrect()) c++;
    }
    return c;
  }

  private int getNumIncorrect() {
    int c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScoresLimited();
    for (CorrectAndScore correctAndScore : toUse) {
      if (!correctAndScore.isCorrect()) c++;
    }
    return c;
  }


  /**
   * Difference in terms of # of correct over incorrect
   *
   * @return
   */
  public int getDiff() {
    int c = 0;
    List<CorrectAndScore> toUse = getCorrectAndScoresLimited();
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.isCorrect()) c++;
      else c--;
    }
    return c;
  }

  public float getAvgScore() {
    if (isEmpty()) return 0f;

    float c = 0f;
    float n = 0f;
    List<CorrectAndScore> toUse = getCorrectAndScoresLimited();
    for (CorrectAndScore correctAndScore : toUse) {
      if (correctAndScore.getScore() > 0f) {
        c += correctAndScore.getScore();
        n++;
      }
    }
    return n > 0f ? (c / n) : 0f;
  }

  public List<CorrectAndScore> getCorrectAndScoresLimited() {
    List<CorrectAndScore> toUse = getCorrectAndScores();
    if (toUse.size() > MAX_TO_USE) toUse = toUse.subList(toUse.size() - MAX_TO_USE, toUse.size());
    return toUse;
  }

  public float getScore() {
    long latest = Long.MIN_VALUE;
    float score = -1f;

    for (CorrectAndScore correctAndScore : getCorrectAndScores()) {
      if (correctAndScore.getTimestamp() > latest) score = correctAndScore.getScore();
    }
    return score;
  }

  public int getAvgScorePercent() {
    return Math.round(getAvgScore() * 100f);
  }

  /**
   * @param correctAndScore
   * @see ResultDAO#getExerciseCorrectAndScores(List, Collection)
   */
  public void add(CorrectAndScore correctAndScore) {
    getCorrectAndScores().add(correctAndScore);
  }

  public void sort() {
    Collections.sort(getCorrectAndScores());
  }

  public int getId() {
    return id;
  }

  public List<CorrectAndScore> getCorrectAndScores() {
    return correctAndScores;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (CorrectAndScore correctAndScore : getCorrectAndScores()) {
      if (correctAndScore.isCorrect()) builder.append("+");
      else builder.append("-");
    }

    return "id " + id + " " + builder + " correct " + getNumCorrect() + "/" + getNumIncorrect() + " score " + (int) getAvgScore();
  }
}
