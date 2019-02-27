/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.result;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class UserSessionHistory {
  private static final Logger logger = LogManager.getLogger(UserSessionHistory.class);

  /**
   * does all average calc on server
   *
   * @return
   * @paramx listid
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserHistoryForList
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#onSetComplete
   */
/*
  public AVPScoreReport getUserHistoryForList(int userid,
                                              Collection<Integer> ids,
                                              int latestResultID,
                                              Collection<Integer> allIDs,
                                              Map<Integer, CollationKey> idToKey,
                                              IResultDAO resultDAO,
                                              String language) {
//    logger.debug("getUserHistoryForList " + userid + " and " + ids.size() + " ids, latest " + latestResultID);

    SessionsAndScores sessionsAndScores = resultDAO.getSessionsForUserIn2(ids, latestResultID, userid, allIDs, idToKey, language);
    */
/*List<Session> sessionsForUserIn2 = sessionsAndScores.getSessions();

    //Map<Integer, User> userMap = userDAO.getUserMap();
  // User byID = userDAO.getByID(userid);
    AVPHistoryForList sessionAVPHistoryForList = new AVPHistoryForList(sessionsForUserIn2, userid, true);
    AVPHistoryForList sessionAVPHistoryForList2 = new AVPHistoryForList(sessionsForUserIn2, userid, false);

    // sort by correct %
    Collections.sort(sessionsForUserIn2, new Comparator<Session>() {
      @Override
      public int compare(Session o1, Session o2) {
        float correctPercent = o1.getCorrectPercent();
        float correctPercent1 = o2.getCorrectPercent();
        return correctPercent < correctPercent1 ? +1 : correctPercent > correctPercent1 ? -1 :
            compareTimestamps(o1, o2);
      }
    });

    int childCount = 0;
    List<AVPHistoryForList.UserScore> scores = new ArrayList<>();

    for (Session session : sessionsForUserIn2) {
      if (childCount++ < 10 || session.isLatest()) {
        scores.add(makeScore(childCount, byID, session, true));
      }
    }

    logger.debug("getUserHistoryForList correct scores " + scores);

    if (scores.size() == 11) {
      scores.remove(9);
    }
    sessionAVPHistoryForList.setScores(scores);

    Collections.sort(sessionsForUserIn2, new Comparator<Session>() {
      @Override
      public int compare(Session o1, Session o2) {
        return o1.getAvgScore() < o2.getAvgScore() ? +1 : o1.getAvgScore() > o2.getAvgScore() ? -1 : compareTimestamps(o1, o2);
      }
    });

    childCount = 0;
    scores = new ArrayList<>();

    for (Session session : sessionsForUserIn2) {
      if (childCount++ < 10 || session.isLatest()) {
        scores.add(makeScore(childCount, byID, session, false));
      }
    }
    logger.debug("getUserHistoryForList pron    scores " + scores);

    if (scores.size() == 11) {
      scores.remove(9);
    }

    sessionAVPHistoryForList2.setScores(scores);*//*


*/
/*    List<AVPHistoryForList> historyForLists = new ArrayList<>();
    historyForLists.add(sessionAVPHistoryForList);
    historyForLists.add(sessionAVPHistoryForList2);*//*


//    logger.debug("returning " + historyForLists);
//    logger.debug("correct/incorrect history " + sessionsAndScores.sortedResults);
    return new AVPScoreReport(//historyForLists,
        sessionsAndScores.getSortedResults());
  }
*/

/*  private int compareTimestamps(Session o1, Session o2) {
    return o1.getTimestamp() < o2.getTimestamp() ? +1 : o1.getTimestamp() > o2.getTimestamp() ? -1 : 0;
  }*/

/*  private AVPHistoryForList.UserScore makeScore(int childCount,
                                                User user,// Map<Integer, User> userMap,
                                                Session session,
                                                boolean useCorrect) {
    float value = useCorrect ? session.getCorrectPercent() : 100f * session.getAvgScore();
    int userid = session.getUserid();
    //User user = userMap.get(userid);
    String userID;
    if (user == null) {
      logger.warn("huh? couldn't find userid " + userid);// + " in map with keys " + userMap.keySet());
      userID = "Default User";
    } else {
      userID = user.getUserID();
    }
    return new AVPHistoryForList.UserScore(childCount,
        userID,
        value,
        session.isLatest());
  }*/

}
