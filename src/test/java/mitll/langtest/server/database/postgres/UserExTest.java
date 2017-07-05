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
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.apache.logging.log4j.*;
import org.junit.Test;

import java.util.Collection;

public class UserExTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(UserExTest.class);

  @Test
  public void testExercise() {
    DatabaseImpl spanish = getDatabase("spanish");
    CommonExercise next = spanish.getExercises().iterator().next();
    logger.info("got " + next.getDirectlyRelated());
  }

  @Test
  public void testUserList() {
    DatabaseImpl spanish = getDatabase("spanish");

    IUserExerciseDAO dao = spanish.getUserExerciseDAO();
    IUserListManager userListManager = spanish.getUserListManager();

    Collection<UserList<CommonShell>> listsForUser = userListManager.getListsForUser(2, 1, true, false);

    for (UserList<CommonShell> list : listsForUser) {
      logger.info("got " + list);
      for (CommonShell ex : list.getExercises()) {
        logger.info("\t" + list + " has " + ex);
      }
    }
//
//    Collection<CommonExercise> all = dao.getAllUserExercises(projid);
//    logger.info("all " + all.size() + " first " + all.iterator().next());
//
//    Collection<CommonExercise> overrides = dao.getOverrides();
//    logger.info("overrides " + overrides.size() + " first " + overrides.iterator().next());
//
//    CommonExercise predefExercise = dao.getPredefExercise(1);
//
//    logger.info("predef " + predefExercise);
//
//    logger.info("got " + dao.getByExID(Arrays.asList(1959, 1962)));
  }

  @Test
  public void testUserListAgain() {
    DatabaseImpl spanish = getDatabase("spanish");

    IUserListManager userListManager = spanish.getUserListManager();
    Collection<UserList<CommonShell>> listsForUser = userListManager.getListsForUser(270, 1, true, false);

    for (UserList<CommonShell> list : listsForUser) {
      logger.info("got " + list);
      for (CommonShell ex : list.getExercises()) {
        logger.info("\t" + list.getID() +"/" +list.getOldID() + " has " + ex);
      }
    }
  }

  @Test
  public void testUserListAddVisitor() {
    DatabaseImpl spanish = getDatabase("spanish");

    IUserListManager userListManager = spanish.getUserListManager();

    Collection<UserList<CommonShell>> listsForUser = userListManager.getListsForUser(2, 1, true, false);

    for (UserList<CommonShell> list : listsForUser) {
      logger.info("got " + list);
      for (CommonShell ex : list.getExercises()) {
        logger.info("\t" + list + " has " + ex);
      }
    }

    Collection<UserList<CommonShell>> before = userListManager.getListsForUser(2, 1, false, true);

    for (UserList<CommonShell> list : before) {
      logger.info("before " + list);
      for (CommonShell ex : list.getExercises()) {
        logger.info("\t" + list + " has " + ex);
      }
    }
    UserList<CommonShell> next = listsForUser.iterator().next();
    int user = 6;
    userListManager.addVisitor(next.getID(), user);

    Collection<UserList<CommonShell>> after = userListManager.getListsForUser(user, 1, false, true);

    for (UserList<CommonShell> list : after) {
      logger.info("after " + list);
      for (CommonShell ex : list.getExercises()) {
        logger.info("\t" + list + " has " + ex);
      }
    }
  }

  @Test
  public void testDelete() {
    DatabaseImpl spanish = getDatabase("spanish");

    int user = 6;

    IUserListManager userListManager = spanish.getUserListManager();

    Collection<UserList<CommonShell>> before = userListManager.getListsForUser(user, 1, false, true);
    logger.info("before " + before);
    userListManager.addVisitor(337, user);
    Collection<UserList<CommonShell>> after = userListManager.getListsForUser(user, 1, false, true);
    logger.info("after " + after);

    Collection<UserList<CommonShell>> createdLists = userListManager.getListsForUser(316, 1, true, false);
    logger.info("createdLists " + createdLists);

    boolean b = userListManager.deleteList(337);
    logger.info("did delete " + b);
    Collection<UserList<CommonShell>> afterDelete = userListManager.getListsForUser(user, 1, false, true);
    logger.info("afterDelete " + afterDelete);

    Collection<UserList<CommonShell>> afterCreatedLists = userListManager.getListsForUser(316, 1, true, false);
    logger.info("afterCreatedLists " + afterCreatedLists);

  }
}
