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

package mitll.langtest.server.services;

import mitll.langtest.client.services.MonitoringService;
import mitll.langtest.server.database.MonitoringSupport;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.monitoring.Session;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class MonitoringServiceImpl extends MyRemoteServiceServlet implements MonitoringService {
  private static final Logger logger = LogManager.getLogger(MonitoringServiceImpl.class);



  @Override
  public Map<User, Integer> getUserToResultCount() {
    return getMonitoringSupport().getUserToResultCount();
  }

  @Override
  public Map<String, Integer> getResultByDay() {
    return getMonitoringSupport().getResultByDay();
  }

  @Override
  public Map<String, Integer> getResultByHourOfDay() {
    return getMonitoringSupport().getResultByHourOfDay();
  }

  MonitoringSupport getMonitoringSupport() {
    return db.getMonitoringSupport();
  }

  /**
   * TODO : worry about duplicate userid?
   *
   * @return
   */
  public Map<Integer, Integer> getResultCountToCount() {
    return getMonitoringSupport().getResultCountToCount(getExercises());
  }

  Collection<CommonExercise> getExercises() {
    return db.getExercises(getProjectID());
  }

  /**
   * Map of overall, male, female to list of counts (ex 0 had 7, ex 1, had 5, etc.)
   * Split exid->count by gender.
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getResultPerExercise
   */
  public Map<String, Map<Integer, Integer>> getResultPerExercise() {
    return getMonitoringSupport().getResultPerExercise(getExercises());
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doGenderQuery(com.google.gwt.user.client.ui.Panel)
   */
  public Map<String, Map<Integer, Integer>> getResultCountsByGender() {
    return getMonitoringSupport().getResultCountsByGender(getExercises());
  }

  public Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts() {
    return getMonitoringSupport().getDesiredCounts(getExercises(), db.getResultDAO().getUserAndTimes());
  }

  /**
   * Determine sessions per user.  If two consecutive items are more than {@link IResultDAO#SESSION_GAP} seconds
   * apart, then we've reached a session boundary.
   * Remove all sessions that have just one answer - must be test sessions.
   *
   * @return list of duration and numAnswer pairs
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery
   */
  public List<Session> getSessions() {
    return getMonitoringSupport().getSessions().getSessions();
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery
   */
  public Map<String, Number> getResultStats() {
    return getMonitoringSupport().getResultStats();
  }

  /**
   * Filter out the default audio recordings...
   *
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doMaleFemale
   */
  @Override
  public Map<String, Float> getMaleFemaleProgress() {
    return db.getMaleFemaleProgress(getProjectID());
  }
}