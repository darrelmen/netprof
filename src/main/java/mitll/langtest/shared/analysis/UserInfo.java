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

package mitll.langtest.shared.analysis;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.cellview.client.Column;
import mitll.langtest.client.analysis.UserContainer;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.user.FirstLastUser;
import mitll.langtest.shared.user.SimpleUser;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/29/15.
 */
public class UserInfo extends SimpleUser {
  private int current;
  private int lastSessionScore;
  private int lastSessionNum;
  private int lastSessionSize;
  private int num;

  private transient List<BestScore> bestScores;

  public UserInfo() {
  }

  /**
   * @param bestScores
   * @see mitll.langtest.server.database.analysis.Analysis#getBestForQuery
   */
  public UserInfo(List<BestScore> bestScores, long startTime) {
    this.bestScores = bestScores;
    this.num = bestScores.size();
    setLastChecked(startTime);

    // done on server
    bestScores.sort(Comparator.comparingLong(SimpleTimeAndScore::getTimestamp));
    setCurrent(getPercent(bestScores));

    setLastSessionScore(bestScores);
  }

  private int getPercent(List<BestScore> bestScores1) {
    float total = 0;
    for (BestScore bs : bestScores1) {
      total += bs.getScore();
    }
    return toPercent(total, bestScores1.size());
  }

  private void setLastSessionScore(List<BestScore> bestScores) {
    long maxSession = Long.MIN_VALUE;
    Map<Long, List<BestScore>> sessionToScores = new HashMap<>();
    int lastSessionSize = -1;
    for (BestScore bestScore : bestScores) {
      long sessionStart = bestScore.getSessionStart();
      if (sessionStart > maxSession) {
        maxSession = sessionStart;
        lastSessionSize = bestScore.getSessionSize();
      }
      List<BestScore> bestScores1 = sessionToScores.computeIfAbsent(sessionStart, k -> new ArrayList<>());
      bestScores1.add(bestScore);
    }
    List<BestScore> bestScores1 = sessionToScores.get(maxSession);

    lastSessionScore = getRounded(bestScores1);
    lastSessionNum = bestScores1.size();
    this.lastSessionSize = lastSessionSize;
  }

  private int getRounded(List<BestScore> bestScores1) {
    float total = 0;
    for (BestScore bs : bestScores1) {
      total += bs.getScore();
    }
    return toRound(total, bestScores1.size());
  }

  private static int toRound(float total, float size) {
    return Math.round(1000f * total / size);
  }

  private static int toPercent(float total, float size) {
    return Math.round(100f * total / size);
  }

  /**
   * @return
   * @see UserContainer#getCurrent
   */
  public int getCurrent() {
    return current;
  }

  public int getNum() {
    return num;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.analysis.Analysis#getUserPerformance
   */
  public List<BestScore> getBestScores() {
    return bestScores;
  }

  private void setCurrent(int current) {
    this.current = current;
  }

  public void setUserID(String userID) {
    this.userID = userID;
  }


  /**
   * @return
   * @see UserContainer#addTable(Collection, DivWidget)
   */
  @Override
  public int getID() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  /**
   * @param firstLastUser
   * @see Analysis#getUserInfos(Map, IUserDAO)
   */
  public void setFrom(FirstLastUser firstLastUser) {
    setId(firstLastUser.getID()); // necessary?
    setUserID(firstLastUser.getUserID());
    this.first = firstLastUser.getFirst();
    this.last = firstLastUser.getLast();
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    return Integer.compare(id, o.getID());
  }

  public int getLastSessionScore() {
    return lastSessionScore;//Integer.valueOf(lastSessionScore).floatValue() / 10F;
  }

  /**
   * @return
   * @see UserContainer#getPolyNum
   * @see UserContainer#getPolyNumSorter
   */
  public int getLastSessionNum() {
    return lastSessionNum;
  }

  public int getLastSessionSize() {
    return lastSessionSize;
  }

  public String toString() {
    return getID() + "/" + getUserID() + " :\t\t# = " + getNum() + "\tavg " + getCurrent();// + "\tfinal " + getFinalScores() + "\tdiff " +  getDiff();
  }
}
