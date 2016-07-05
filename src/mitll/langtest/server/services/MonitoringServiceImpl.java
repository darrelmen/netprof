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
import mitll.langtest.shared.User;
import mitll.langtest.shared.monitoring.Session;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class MonitoringServiceImpl extends MyRemoteServiceServlet implements MonitoringService {
  private static final Logger logger = Logger.getLogger(MonitoringServiceImpl.class);

  @Override
  public void init() {
    logger.info("init called for MonitoringServiceImpl");
    findSharedDatabase();
    readProperties(getServletContext());
  }

  @Override
  public Map<User, Integer> getUserToResultCount() {
    return db.getUserToResultCount();
  }

  public Map<Integer, Integer> getResultCountToCount() {
    return db.getResultCountToCount();
  }


  @Override
  public Map<String, Integer> getResultByDay() {
    return db.getResultByDay();
  }

  @Override
  public Map<String, Integer> getResultByHourOfDay() {
    return db.getMonitoringSupport().getResultByHourOfDay();
  }

  /**
   * Map of overall, male, female to list of counts (ex 0 had 7, ex 1, had 5, etc.)
   *
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doResultLineQuery
   */
  public Map<String, Map<String, Integer>> getResultPerExercise() {
    return db.getResultPerExercise();
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doGenderQuery(com.google.gwt.user.client.ui.Panel)
   */
  @Override
  public Map<String, Map<Integer, Integer>> getResultCountsByGender() {
    return db.getResultCountsByGender();
  }

  public Map<String, Map<Integer, Map<Integer, Integer>>> getDesiredCounts() {
    return db.getDesiredCounts();
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery
   */
  public List<Session> getSessions() {
    return db.getSessions();
  }

  /**
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doSessionQuery
   */
  public Map<String, Number> getResultStats() {
    return db.getMonitoringSupport().getResultStats();
  }

  /**
   * Filter out the default audio recordings...
   *
   * @return
   * @see mitll.langtest.client.monitoring.MonitoringManager#doMaleFemale
   */
  @Override
  public Map<String, Float> getMaleFemaleProgress() {
    return db.getMaleFemaleProgress();
  }
}