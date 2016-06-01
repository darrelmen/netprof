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

package mitll.langtest.server.database;

import mitll.langtest.client.user.Md5Hash;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.instrumentation.IEventDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.User;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.instrumentation.Event;
import mitll.npdata.dao.SlickSlimEvent;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.List;

/**
 * Created by GO22670 on 1/30/14.
 */
public class PostgresTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(PostgresTest.class);
  //public static final boolean DO_ONE = false;

  @Test
  public void testSpanishEventCopy() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IEventDAO eventDAO = spanish.getEventDAO();
    List<Event> all = eventDAO.getAll("spanish");
    for (Event event : all.subList(0, getMin(all))) logger.info("Got " + event);

    List<SlickSlimEvent> spanish1 = eventDAO.getAllSlim("spanish");
    for (SlickSlimEvent event : spanish1.subList(0, getMin(spanish1))) logger.info("Got " + event);

    List<SlickSlimEvent> allDevicesSlim = eventDAO.getAllDevicesSlim("spanish");
    for (SlickSlimEvent event : allDevicesSlim.subList(0, getMin(allDevicesSlim))) logger.info("Got " + event);

    eventDAO.addPlayedMarkings(1,spanish.getExercises().iterator().next());

    logger.info("Got " + eventDAO.getFirstSlim("spanish"));
  //  spanish.doReport(new PathHelper("war"));
  }

  int getMin(List<?> all) {
    return Math.min(all.size(),10);
  }

  @Test
  public void testEvent() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IEventDAO eventDAO = spanish.getEventDAO();

    eventDAO.add(new Event("123","button","2334","testing",1,System.currentTimeMillis(),"device"),"spanish");
    logger.info("Got " + eventDAO.getFirstSlim("spanish"));

    //  spanish.doReport(new PathHelper("war"));
  }

  @Test
  public void testUser() {
    DatabaseImpl<CommonExercise> spanish = getDatabase("spanish");

    IUserDAO dao = spanish.getUserDAO();

    List<User> users = dao.getUsers();

    logger.info("got " +users);

    String userid = "userid";
    String hash = Md5Hash.getHash("g@gmail.com");
    String password = Md5Hash.getHash("password");
    User user = dao.addUser(userid, password, hash, User.Kind.STUDENT, "128.0.0.1", true, 89, "midest", "browser");
    User user2 = dao.addUser(userid, password, hash, User.Kind.STUDENT, "128.0.0.1", true, 89, "midest", "iPad");

    logger.info("made " + user);
    logger.info("made " + user2);
    User user3 = dao.addUser(userid +"ipad", password, hash, User.Kind.STUDENT, "128.0.0.1", true, 89, "midest", "iPad");

    if (user == null) {
      user = dao.getUserByID(userid);
      logger.info("found " +user);
    }

    logger.info("before " +user.isEnabled());

    boolean b = dao.enableUser(user.getId());

    logger.info("enable user " +b);

    logger.info("valid email " +dao.isValidEmail(hash));
    logger.info("valid email " +dao.isValidEmail(password));
    logger.info("user " +dao.getIDForUserAndEmail(userid,hash));
    logger.info("user " +dao.getIDForUserAndEmail(userid,password));
    logger.info("user id " +dao.getIdForUserID(userid));
    logger.info("user " +dao.getUser(userid,password));
    logger.info("user " +dao.getUser(userid,hash));
    logger.info("user " +dao.getUserWithPass(userid,password));
    logger.info("user " +dao.getUserWithPass(userid,hash));
    logger.info("user devices " +dao.getUsersDevices());
    //  spanish.doReport(new PathHelper("war"));
  }
}
