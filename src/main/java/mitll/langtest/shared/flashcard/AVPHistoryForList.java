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
import mitll.langtest.shared.monitoring.Session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/18/14.
 */
public class AVPHistoryForList implements IsSerializable {
  private float pbCorrect;
  private float top;
  private float totalCorrect;
  private List<Float> yValuesForUser;
  private boolean useCorrect;

  private float totalNotMe = 0f;
  private float numNotMe = 0f;
  private int numScores;
  private List<UserScore> scores = new ArrayList<UserScore>();

  public AVPHistoryForList() {
  }

  /**
   * @param scores
   * @param userID
   * @param useCorrect when false get score percentages true for percentage correct
   * @see mitll.langtest.server.database.DatabaseImpl#getUserHistoryForList
   */
  public AVPHistoryForList(Collection<Session> scores, long userID, boolean useCorrect) {
    this.useCorrect = useCorrect;
    numScores = scores.size();
    calc(scores, userID);
    if (scores.isEmpty()) System.err.println("huh? scores is empty???");
  }

  public float getPbCorrect() {
    return pbCorrect;
  }

  public float getTop() {
    return top;
  }

  public float getTotalCorrect() {
    return totalCorrect;
  }

  public float getClassAvg() {
    if (numNotMe == 0f) {
      return totalCorrect / (float) numScores;
    } else {
      return totalNotMe / numNotMe;
    }
  }

  public List<Float> getyValuesForUser() {
    return yValuesForUser;
  }

  private void calc(Collection<Session> scores, long userID) {
    pbCorrect = 0;
    top = 0;
    totalCorrect = 0;

    yValuesForUser = new ArrayList<Float>();
    for (SetScore score : scores) {
      float value = getValue(score);

      if (score.getUserid() == userID) {
        if (value > pbCorrect) pbCorrect = value;
        yValuesForUser.add(value);
        //  System.out.println("showLeaderboardPlot : for user " +userID + " got " + score);
      } else {
        //System.out.println("\tshowLeaderboardPlot : for user " +score.getUserid() + " got " + score);
        totalNotMe += value;
        numNotMe++;
      }
      if (value > top) {
        top = value;
        //System.out.println("\tshowLeaderboardPlot : new top score for user " +score.getUserid() + " got " + score);
      }
      totalCorrect += value;
    }
//    if (yValuesForUser.isEmpty()) {
    //   System.err.println("huh? yValuesForUser (" + userID + ") is empty???");
    //  }
  }

  private float getValue(SetScore score) {
    return useCorrect ? Math.round(score.getCorrectPercent()) : Math.round(100f * score.getAvgScore());
  }

  public boolean isUseCorrect() {
    return useCorrect;
  }

  public int getNumScores() {
    return numScores;
  }

  public List<UserScore> getScores() {
    return scores;
  }

  public void setScores(List<UserScore> scores) {
    this.scores = scores;
  }

  public static class UserScore implements IsSerializable {
    private String user;
    private float score;
    private boolean isCurrent;
    private int index;

    public UserScore() {
    }

    public UserScore(int index, String user, float score, boolean isCurrent) {
      this.index = index;
      this.user = user;
      this.score = score;
      this.isCurrent = isCurrent;
    }

    public String getUser() {
      return user;
    }
    public float getScore() {
      return score;
    }

    /**
     * Is this the score for the current session?
     *
     * @return
     * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#bold(mitll.langtest.shared.flashcard.AVPHistoryForList.UserScore, String)
     */
    public boolean isCurrent() {
      return isCurrent;
    }

    public int getIndex() {
      return index;
    }

    public String toString() {
      return "id " + index + " by " + user + " " + ((int) getScore()) + (isCurrent() ? " current! " : "");
    }
  }

  public String toString() {
    return "History " + numScores + " correct " + getTotalCorrect() + " pb correct " + getPbCorrect() +
        " class avg " + getClassAvg() + " scores " + scores;
  }
}
