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
import mitll.langtest.server.database.instrumentation.IEventDAO;
import mitll.langtest.shared.instrumentation.Event;
import mitll.npdata.dao.SlickSlimEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.List;

public class EventTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(EventTest.class);

  @Test
  public void testSpanishEventCopy() {
    DatabaseImpl spanish = getDatabase();

    IEventDAO eventDAO = spanish.getEventDAO();
    List<Event> all = eventDAO.getAll();
    for (Event event : all.subList(0, getMin(all))) logger.info("Got " + event);

    List<SlickSlimEvent> spanish1 = eventDAO.getAllSlim();
    for (SlickSlimEvent event : spanish1.subList(0, getMin(spanish1))) logger.info("Got " + event);
   // List<SlickSlimEvent> allDevicesSlim = eventDAO.getAllDevicesSlim("spanish");
  //  for (SlickSlimEvent event : allDevicesSlim.subList(0, getMin(allDevicesSlim))) logger.info("Got " + event);
    eventDAO.addPlayedMarkings(1, spanish.getExercises(-1, false).iterator().next());
   // logger.info("Got " + eventDAO.getFirstSlim("spanish"));
    //  spanish.doReportForYear(new PathHelper("war"));
  }

  private int getMin(List<?> all) {
    return Math.min(all.size(), 10);
  }

  @Test
  public void testEvent() {
    DatabaseImpl spanish = getDatabase();
    IEventDAO eventDAO = spanish.getEventDAO();
    eventDAO.addToProject(new Event("123", "button", "2334", "testing", 1, System.currentTimeMillis(), "device", -1), 0);
//    logger.info("Got " + eventDAO.getFirstSlim("spanish"));
  }
}
