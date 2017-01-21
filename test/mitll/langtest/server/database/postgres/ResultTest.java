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

package mitll.langtest.server.database.postgres;

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.logging.log4j.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ResultTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(ResultTest.class);

//  @Test
//  public void testResult() {
//    DatabaseImpl spanish = getDatabase("spanish", true);
//
//    IResultDAO resultDAO = spanish.getResultDAO();
//
//    List<Result> results = resultDAO.getResults();
//    int size = results.size();
//    Result first = results.get(0);
//    logger.info("got " + size + " first " + first);
//
//    Collection<Result> resultsDevices = resultDAO.getResultsDevices();
//    logger.info("got " + resultsDevices.size() + " first " + resultsDevices.iterator().next());
//
//    int uniqueID = first.getUniqueID();
//    logger.info("Got " + resultDAO.getResultByID(uniqueID));
//    logger.info("Got " + resultDAO.getMonitorResults(projid).size());
//    int  exid = first.getExid();
//    logger.info("Got " + resultDAO.getMonitorResultsByID(exid));
//    Collection<UserAndTime> userAndTimes = resultDAO.getUserAndTimes();
//    logger.info("Got " + userAndTimes.size() + " " + userAndTimes.iterator().next());
//
//    logger.info("Got " + resultDAO.getSessions());
//    logger.info("Got for ex " + exid + " and user 1 " + resultDAO.getResultsForExIDInForUser(Collections.singleton(exid), 1, ""));
//    logger.info("Got " + resultDAO.getNumResults(projid));
//
//    SlickResultDAO dao = (SlickResultDAO) spanish.getResultDAO();
//    logger.info(dao.getMonitorResults(projid).size());
//
//    List<Integer> ids = new ArrayList<>();
//    for (CommonExercise ex : spanish.getExercises()) ids.add(ex.getID());
//    List<Integer> strings = ids.subList(0, 100);
//    logger.info(dao.getResultsForExIDInForUser(strings, 2, "").size());
//    logger.info("match avp " +dao.getResultsForExIDInForUser(strings, true, 2).size());
//    logger.info("!match avp " +dao.getResultsForExIDInForUser(strings, false, 2).size());
//
//    ResultDAO h2 = new ResultDAO(spanish);
//
//    logger.info(h2.getResultsForExIDInForUser(strings, 1, "").size());
//    logger.info("match avp " +h2.getResultsForExIDInForUser(strings,true,1).size());
//    logger.info("!match avp " +h2.getResultsForExIDInForUser(strings,false,1).size());
//  }

/*
  @Test
  public void testAVP() {
    DatabaseImpl spanish = getDatabase("spanish", true);

    SlickResultDAO dao = (SlickResultDAO) spanish.getResultDAO();

    List<Integer> ids = new ArrayList<>();
    for (CommonExercise ex : spanish.getExercises()) ids.add(ex.getID());
    List<Integer> strings = ids.subList(0, 100);

    logger.info(dao.getResultsForExIDInForUser(strings, 2, "", language).size());
    logger.info("match avp " +dao.getResultsForExIDInForUser(strings, true, 2, language).size());
    logger.info("!match avp " +dao.getResultsForExIDInForUser(strings, false, 2, language).size());

    ResultDAO h2 = new ResultDAO(spanish);

    logger.info(h2.getResultsForExIDInForUser(strings, 1, "", language).size());
    logger.info("match avp " +h2.getResultsForExIDInForUser(strings,true,1, language).size());
    logger.info("!match avp " +h2.getResultsForExIDInForUser(strings,false,1, language).size());
  }*/

  @Test
  public void testAnswerDAO() {
    DatabaseImpl spanish = getDatabase("spanish");
  }
}
