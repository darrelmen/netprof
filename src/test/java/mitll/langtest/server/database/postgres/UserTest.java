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

import mitll.langtest.client.user.Md5Hash;
import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.*;
import org.junit.Test;

import java.util.List;

public class UserTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(UserTest.class);

  @Test
  public void testAdmin() {
    DatabaseImpl spanish = getDatabase("spanish");

    IUserDAO dao = spanish.getUserDAO();

    List<User> users = dao.getUsers();
    for (User user : users) if (user.isAdmin()) logger.info(user);

  }

  /**
   * TODO : addExerciseToList option to get mock db to use
   */
  @Test
  public void testUser() {
    DatabaseImpl spanish = getDatabase("spanish");

    IUserDAO dao = spanish.getUserDAO();

    List<User> users = dao.getUsers();

    logger.info("got " + users);

    String userid = "userid";
    String toHash = "g@gmail.com";
    String hash = Md5Hash.getHash(toHash);
    String password = Md5Hash.getHash("password");
    String bueller = Md5Hash.getHash("bueller");
//    User user = dao.addUser(userid, password, hash, toHash, User.Kind.STUDENT, "128.0.0.1", true, 89, "midest", "browser", first, last);
//    User user2 = dao.addUser(userid, password, hash, toHash, User.Kind.STUDENT, "128.0.0.1", true, 89, "midest", "iPad", first, last);
//
//    logger.info("made " + user);
//    logger.info("made " + user2);
//    User user3 = dao.addUser(userid + "ipad", password, hash, toHash, User.Kind.STUDENT, "128.0.0.1", true, 89, "midest", "iPad", first, last);

//    if (user == null) {
//      user = dao.getUserByID(userid);
//      logger.info("found " + user);
//    }
//
//    logger.info("before " + user.isEnabled());
//
//    int id = user.getID();
//    boolean b = dao.enableUser(id);

//    logger.info("enable user " + b);
//
//    logger.info("change enabled " + dao.changeEnabled(id, true) + " : " + dao.getUserWhere(id).isEnabled());
//    logger.info("change enabled " + dao.changeEnabled(id, false) + " : " + dao.getUserWhere(id).isEnabled());
//    int bogusID = 123456789;
//    logger.info("change enabled " + dao.changeEnabled(bogusID, false) + " : " + dao.getUserWhere(id).isEnabled());


    logger.info("user " + dao.getIDForUserAndEmail(userid, hash));
    logger.info("user " + dao.getIDForUserAndEmail(userid, password));
    //logger.info("user id " + dao.getIdForUserID(userid));
//    logger.info("user " + dao.getUser(userid, password));
//    logger.info("user " + dao.getUser(userid, hash));
//    logger.info("user " + dao.getStrictUserWithPass(userid, password));
//    logger.info("user " + dao.getStrictUserWithPass(userid, hash));
    //logger.info("user devices " + dao.getUsersDevices());
    logger.info("mini " + dao.getMiniUsers());
    logger.info("mini first " + dao.getMiniUser(dao.getMiniUsers().keySet().iterator().next()));
//    logger.info("males " + dao.getUserMap(true));
//    logger.info("females " + dao.getUserMap(false));
   // logger.info("all " + dao.getUserMap());

    logger.info("valid email " + dao.isValidEmail(hash));
    logger.info("valid email " + dao.isValidEmail(password));
//    logger.info("change password\n" +
//        dao.getStrictUserWithPass(userid, password) + " :\n" +
//        dao.changePassword(id, bueller) + "\n" +
//        dao.getStrictUserWithPass(userid, password) + " :\n" +
//        dao.getStrictUserWithPass(userid, bueller));
//
//    logger.info("reset key " +
//        dao.getUserWithResetKey("reset") +
//        ":\n" + dao.updateKey(id, true, "reset") +
//        " :\n" + dao.getUserWithResetKey("reset") +
//        ":\n" + dao.clearKey(id, true) +
//        " :\n" + dao.getUserWithResetKey("reset")
//    );
//
//    logger.info("enabled key " +
//        dao.getUserWithResetKey("enabled") +
//        ":\n" + dao.updateKey(id, false, "enabled") +
//        " :\n" + dao.getUserWithEnabledKey("enabled") +
//        ":\n" + dao.clearKey(id, false) +
//        ":\n" + dao.clearKey(bogusID, false) +
//        " :\n" + dao.getUserWithEnabledKey("enabled")
//    );
    //  spanish.doReport(new PathHelper("war"));
  }


}
