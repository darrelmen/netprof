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
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.reviewed.StateCreator;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.STATE;
import org.apache.logging.log4j.*;
import org.junit.Test;

import java.util.*;

public class ReviewedExTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(ReviewedExTest.class);

  @Test
  public void testReviewed() {
    DatabaseImpl spanish = getDatabase("spanish");

//    IUserExerciseDAO dao = spanish.getUserExerciseDAO();
    IUserListManager userListManager = spanish.getUserListManager();

    STATE currentState = userListManager.getCurrentState(1);
    logger.info("Got " + currentState + " for ");

    Map<Integer, StateCreator> exerciseToState = userListManager.getExerciseToState(true);
    logger.info("got " + exerciseToState.keySet().size());

    Collection<StateCreator> values = exerciseToState.values();
    List<StateCreator> stateCreators = new ArrayList<>(values);
    Collections.sort(stateCreators);
    for (StateCreator stateCreator : stateCreators) logger.info("Got " + stateCreator);

    {
      Map<Integer, StateCreator> exerciseToState2 = userListManager.getExerciseToState(false);
      logger.info("got " + exerciseToState2.keySet().size());
      stateCreators = new ArrayList<>(exerciseToState2.values());
      Collections.sort(stateCreators);

      for (StateCreator stateCreator : stateCreators) logger.info("Got " + stateCreator);
    }
  }

}
