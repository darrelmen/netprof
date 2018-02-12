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
import mitll.langtest.client.analysis.UserContainer;
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.user.FirstLastUser;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/29/15.
 */
public class UserInfo implements HasID {
  private int current;
  private int lastSession;
  private int num;
  private long startTime;

  private transient List<BestScore> bestScores;

  private int id;
  private String userID;
  private String first;
  private String last;

  public UserInfo() {
  }

  /**
   * @param bestScores
   * @see mitll.langtest.server.database.analysis.Analysis#getBestForQuery
   */
  public UserInfo(List<BestScore> bestScores, long startTime) {
    this.bestScores = bestScores;
    this.num = bestScores.size();
    this.startTime = startTime;

    // done on server
    bestScores.sort(Comparator.comparingLong(SimpleTimeAndScore::getTimestamp));

//    List<BestScore> initialScores = bestScores.subList(0, Math.min(initialSamples, bestScores.size()));
    //   List<BestScore> finalScores = bestScores.subList(num - Math.min(finalSamples, num), num);
//    this.finalScores = getPercent(finalScores);
    setCurrent(getPercent(bestScores));

    setLastSession(bestScores);
  }

  private void setLastSession(List<BestScore> bestScores) {
    long maxSession = Long.MIN_VALUE;
    Map<Long, List<BestScore>> sessionToScores = new HashMap<>();
    for (BestScore bestScore : bestScores) {
      long sessionStart = bestScore.getSessionStart();
      if (sessionStart > maxSession) {
        maxSession = sessionStart;
      }
      List<BestScore> bestScores1 = sessionToScores.computeIfAbsent(sessionStart, k -> new ArrayList<>());
      bestScores1.add(bestScore);
    }
    List<BestScore> bestScores1 = sessionToScores.get(maxSession);

    lastSession = getPercent(bestScores1);
  }

  private int getPercent(List<BestScore> bestScores1) {
    float total = 0;
    for (BestScore bs : bestScores1) {
      total += bs.getScore();
    }
    return toPercent(total, bestScores1.size());
  }

  private static int toPercent(float total, float size) {
    return (int) Math.ceil(100 * total / size);
  }

  public long getTimestampMillis() {
    return startTime;
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

  public String getUserID() {
    return userID;
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

  public void setFrom(FirstLastUser firstLastUser) {
    setId(firstLastUser.getID()); // necessary?
    setUserID(firstLastUser.getUserID());
    setFirst(firstLastUser.getFirst());
    setLast(firstLastUser.getLast());
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    return Integer.compare(id, o.getID());
  }

/*
  private int getFinalScores() {
    return finalScores;
  }
*/

  /**
   * @param first
   * @see Analysis#getUserInfos
   */
  public void setFirst(String first) {
    this.first = first;
  }

  public String getFirst() {
    return first;
  }

  public void setLast(String last) {
    this.last = last;
  }

  public String getLast() {
    return last;
  }

  public String toString() {
    //MiniUser user = getUser();
    return getID() + "/" + getUserID() + " :\t\t# = " + getNum() + "\tavg " + getCurrent();// + "\tfinal " + getFinalScores() + "\tdiff " +  getDiff();
  }

  public int getLastSession() {
    return lastSession;
  }
}
