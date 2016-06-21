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

package mitll.langtest.server.database.reviewed;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.exercise.STATE;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickReviewed;
import mitll.npdata.dao.reviewed.ReviewedDAOWrapper;
import mitll.npdata.dao.reviewed.SecondStateDAOWrapper;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.*;

public class SlickReviewedDAO extends DAO implements IReviewedDAO {
  private static final Logger logger = Logger.getLogger(SlickReviewedDAO.class);

  private final ReviewedDAOWrapper dao;

  public SlickReviewedDAO(Database database, DBConnection dbConnection, boolean isReviewed) {
    super(database);
    dao = isReviewed ? new ReviewedDAOWrapper(dbConnection) : new SecondStateDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  public void insert(SlickReviewed word) {
    dao.insert(word);
  }

  public void addBulk(List<SlickReviewed> bulk) {
    dao.addBulk(bulk);
  }

  @Override
  public void remove(String exerciseID) {
    int i = dao.deleteByExID(exerciseID);
    if (i == 0) logger.error("didn't delete reviewed by exercise " + exerciseID);
  }

  @Override
  public void setState(String exerciseID, STATE state, long creatorID) {
    dao.insert(toSlick(exerciseID, state, (int) creatorID));
  }

  SlickReviewed toSlick(String exerciseID, STATE state, int creatorID) {
    long time = System.currentTimeMillis();
    return new SlickReviewed(-1, creatorID, exerciseID, state.toString(), new Timestamp(time));
  }

  public SlickReviewed toSlick(StateCreator stateCreator) {
    return new SlickReviewed(-1, (int) stateCreator.getCreatorID(), stateCreator.getExerciseID(),
        stateCreator.getState().toString(), new Timestamp(stateCreator.getWhen()));
  }

  @Override
  public Map<String, StateCreator> getExerciseToState(boolean skipUnset) {
    return getExerciseToState(skipUnset, false, "");
  }

  @Override
  public STATE getCurrentState(String exerciseID) {
    Map<String, StateCreator> exerciseToState = getExerciseToState(false, true, exerciseID);
    if (exerciseToState.isEmpty()) return STATE.UNSET;
    else return exerciseToState.values().iterator().next().getState();
  }

  /**
   * TODO Fill in
   *
   * @param skipUnset
   * @param selectSingleExercise
   * @param exerciseIDToFind
   * @return
   */
  private Map<String, StateCreator> getExerciseToState(boolean skipUnset,
                                                       boolean selectSingleExercise, String exerciseIDToFind) {
    return new HashMap<>();
  }


  @Override
  public Collection<String> getDefectExercises() {
    Map<String, StateCreator> exerciseToState = getExerciseToState(true);
    Set<String> ids = new HashSet<String>();
    for (Map.Entry<String, StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().getState() == STATE.DEFECT) {
        ids.add(pair.getKey());
      }
    }
    return ids;
  }

  @Override
  public int getCount() {
    return dao.getNumRows();
  }

  public boolean isEmpty() {
    return dao.getNumRows() == 0;
  }
}
