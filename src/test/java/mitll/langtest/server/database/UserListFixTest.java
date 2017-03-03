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

import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.apache.logging.log4j.*;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

public class UserListFixTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(UserListFixTest.class);
  public static final int MAX_EXERCISE = 701;
  //public static final boolean DO_ONE = false;

/*
  @Test
  public void testMSAAddList() {
    DatabaseImpl<CommonExercise> msa = getDatabase("msa");
    UserListManager userListManager = msa.getUserListManager();
    long l = userListManager.addUserList(643, "UserList_1210", "UserList_1210", "", false);
    UserList<CommonShell> userListByID = userListManager.getUserListByID(l, Collections.emptyList());
    logger.info("list has " + userListByID.getExercises());
  }
*/

/*  @Test
  public void testMSAGetOrpans() {
    DatabaseImpl<CommonExercise> msa = getDatabase("msa");
    UserListManager userListManager = msa.getUserListManager();

    UserList<CommonShell> userListByID = userListManager.getUserListByID(1515, Collections.emptyList());
    Collection<CommonExercise> byUser = userListManager.getUserExerciseDAO().getByUser(643);
    for (CommonExercise exercise : byUser) {

      String custom = exercise.getID().split("Custom_")[1];
      int i = Integer.parseInt(custom);
      if (i < MAX_EXERCISE) {
        logger.info(exercise);
        userListManager.addItemToUserList(1515, exercise.getID());
      }
    }

    //UserList<CommonShell> after = userListManager.getUserListByID(1515, Collections.emptyList());
    Collection<CommonShell> exercises = userListByID.getExercises();
    logger.info("list has " + exercises);
    for (CommonShell exercise : exercises) {
      logger.info("got " + exercise);
    }
  }*/

/*  @Test
  public void testMSARestoreList() {
    int userid = 643;

    DatabaseImpl<CommonExercise> msa = getDatabase("msa");
    UserListManager userListManager = msa.getUserListManager();
    long l = userListManager.addUserList(userid, "UserList_1210", "UserList_1210", "", false);

    Collection<CommonExercise> byUser = userListManager.getUserExerciseDAO().getByUser(userid);
    for (CommonExercise exercise : byUser) {
      String custom = exercise.getID().split("Custom_")[1];
      int i = Integer.parseInt(custom);
      if (i < MAX_EXERCISE) {
        logger.info(exercise);
        userListManager.addItemToUserList(l, exercise.getID());
      }
    }

    Collection<CommonShell> exercises = userListManager.getUserListByID(l, Collections.emptyList()).getExercises();
    logger.info("list has " + exercises.size());
    for (CommonShell exercise : exercises) {
      logger.info("got " + exercise);
    }
  }

  @Test
  public void testGotThem() {
    DatabaseImpl<CommonExercise> msa = getDatabase("msa");
    UserListManager userListManager = msa.getUserListManager();
    UserList<CommonShell> after = userListManager.getUserListByID(1515, Collections.emptyList());
    Collection<CommonShell> exercises = after.getExercises();
    logger.info("list has " + exercises);
    for (CommonShell exercise : exercises) {
      logger.info("got " + exercise);
    }
  }*/
}
