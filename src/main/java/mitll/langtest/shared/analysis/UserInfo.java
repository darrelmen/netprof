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

import mitll.langtest.client.analysis.UserContainer;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.exercise.HasID;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/29/15.
 */
public class UserInfo implements HasID {
 // private int start;
  private int finalScores;
  private int current;
  private int num;
  private transient List<BestScore> bestScores;
  private long startTime;
  private int id;
  private String userID;

  public UserInfo() {
  }

  /**
   * @param bestScores
   * @param initialSamples
   * @param finalSamples
   * @see mitll.langtest.server.database.analysis.Analysis#getBestForQuery
   */
  public UserInfo(List<BestScore> bestScores, long startTime, int initialSamples, int finalSamples) {
    this.bestScores = bestScores;
    this.num = bestScores.size();
    this.startTime = startTime;

    // done on server
    Collections.sort(bestScores, new Comparator<BestScore>() {
      @Override
      public int compare(BestScore o1, BestScore o2) {
        return Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());
      }
    });

//    List<BestScore> initialScores = bestScores.subList(0, Math.min(initialSamples, bestScores.size()));
    List<BestScore> finalScores = bestScores.subList(num - Math.min(finalSamples, num), num);

    this.finalScores = getPercent(finalScores);
    setCurrent(getPercent(bestScores));
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

/*  public String getUserID() {
    return getUser() == null ? "UNK" : getUser().getUserID();
  }*/

  public long getTimestampMillis() {
    return startTime;
  }

  /**
   * @return
   */
/*
  public MiniUser getUser() {
    return this;
  }
*/

  /**
   * @param user
   * @see mitll.langtest.server.database.analysis.Analysis#getUserInfos(IUserDAO, Map)
   */
/*  public void setUser(MiniUser user) {
    setFields(user.getID(), user.getAge(), user.isMale(), user.getUserID(), user.isAdmin());
    setTimestampMillis(user.getTimestampMillis());

*//*
    user.setFirst(user.getFirst());
    user.setLast(user.getLast());
    *//*
  }*/

  /**
   * @return
   * @see UserContainer#getStart
   */
//  public int getStart() {
//    return start;
//  }

  public int getCurrent() {
    return current;
  }

  public int getDiff() {
    return getFinalScores() -  getCurrent();// - getStart();
  }

  public int getNum() {
    return num;
  }

  public List<BestScore> getBestScores() {
    return bestScores;
  }
//
//  public void setStart(int start) {
//    this.start = start;
//  }

  public void setCurrent(int current) {
    this.current = current;
  }

  public String getUserID() {
    return userID;
  }

  public void setUserID(String userID) {
    this.userID = userID;
  }

  @Deprecated
  @Override
  public String getOldID() {
    return null;
  }

  @Override
  public int getID() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Override
  public int compareTo(@NotNull HasID o) {
    return Integer.valueOf(id).compareTo(o.getID());
  }

  public int getFinalScores() {
    return finalScores;
  }

  public String toString() {
    //MiniUser user = getUser();
    return getID() + "/" + getUserID() + " :\t\t# = " + getNum() + "\tavg " + getCurrent() + "\tfinal " + getFinalScores() + "\tdiff " +  getDiff();
  }
}
