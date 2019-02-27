/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.reviewed.IReviewedDAO;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.STATE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

/**
 * Created by go22670 on 7/5/17.
 */
public class StateManager implements IStateManager {
  private static final Logger logger = LogManager.getLogger(StateManager.class);

  private final IReviewedDAO reviewedDAO, secondStateDAO;

  public StateManager(IReviewedDAO reviewedDAO, IReviewedDAO secondStateDAO) {
    this.reviewedDAO = reviewedDAO;
    this.secondStateDAO = secondStateDAO;
  }

  public IReviewedDAO getReviewedDAO() {
    return reviewedDAO;
  }

  public IReviewedDAO getSecondStateDAO() {
    return secondStateDAO;
  }

  /**
   * TODO put this back
   * @see  #setStateOnExercises()
   * @param exerciseToState
   * @param firstState
   */
/*  private void setStateOnExercises(Map<Integer, StateCreator> exerciseToState, boolean firstState) {
    //logger.debug("found " + exerciseToState.size() + " state markings");
    Set<Integer> userExercisesRemaining = setStateOnPredefExercises(exerciseToState, firstState);
    setStateOnUserExercises(exerciseToState, userExercisesRemaining, firstState);
  }*/

  /**
   * TODO put this back
   * @param exerciseToState
   * @param firstState
   * @return
   * @see #setStateOnExercises(java.util.Map, boolean)
   */
  // set state on predef exercises
/*  private Set<Integer> setStateOnPredefExercises(Map<Integer, StateCreator> exerciseToState, boolean firstState) {
    int childCount = 0;
    Set<Integer> userExercisesRemaining = new HashSet<>(exerciseToState.keySet());
    for (Map.Entry<Integer, StateCreator> pair : exerciseToState.entrySet()) {
      CommonExercise predefExercise = userExerciseDAO.getPredefExercise(pair.getKey());
      if (predefExercise != null) {
        userExercisesRemaining.remove(pair.getKey());
        if (firstState) {
          predefExercise.setState(pair.getValue().getState());
        } else {
          predefExercise.setSecondState(pair.getValue().getState());
        }
        childCount++;
      }
    }
    if (childCount > 0) {
      logger.debug("got " + userExercisesRemaining.size() + " in userExercisesRemaining, updated " + childCount + " predef exercises");
    }
    return userExercisesRemaining;
  }*/

  /**
   * TODO : this should probably do something on the actual exercises in the exercise dao
   */
  // set states on user exercises
/*  private void setStateOnUserExercises(Map<Integer, StateCreator> exerciseToState,
                                       Set<Integer> userExercisesRemaining,
                                       boolean firstState) {
    int childCount = 0;
    Collection<CommonExercise> userExercises = userExerciseDAO.getByExID(userExercisesRemaining);

    for (Shell commonUserExercise : userExercises) {
      int id = commonUserExercise.getID();
      StateCreator state = exerciseToState.get(id);
      if (state == null) {
        logger.error("huh? can't find ex id " + id);
      } else {
        if (firstState) {
          commonUserExercise.setState(state.getState());
        } else {
          commonUserExercise.setSecondState(state.getState());
        }
        childCount++;
      }
    }
    if (childCount > 0) {
      logger.debug("updated " + childCount + " user exercises");
    }
  }*/

  /**
   * Update an old db where the review table doesn't have a state column.
   *
   * @return
   */
/*  private void getAmmendedStateMap() {
    Map<String, ReviewedDAO.StateCreator> stateMap = getExerciseToState(false);
    // logger.debug("got " + stateMap.size() +" in state map");
    Map<String, Long> exerciseToCreator = annotationDAO.getAnnotatedExerciseToCreator();
    //logger.debug("got " + exerciseToCreator.size() +" in defectIds");

    int childCount = 0;
    Set<String> reviewed = new HashSet<String>(stateMap.keySet());
    long now = System.currentTimeMillis();

    for (String exid : reviewed) {   // incorrect - could be defect or comment for now
      if (exerciseToCreator.keySet().contains(exid)) {
        ReviewedDAO.StateCreator stateCreator = stateMap.get(exid);
        if (stateCreator.getState().equals(STATE.UNSET)) { // only happen when we have an old db
          stateMap.put(exid, new ReviewedDAO.StateCreator(STATE.DEFECT, stateCreator.getCreatorID(), now));
          reviewedDAO.setState(exid, STATE.DEFECT, stateCreator.getCreatorID());
          childCount++;
        }
      }
    }

    if (childCount > 0) {
      logger.info("updated " + childCount + " rows in review table");
    }
    //  return stateMap;
  }*/

  /**
   * So this returns a map of exercise id to current (latest) state.  The exercise may have gone through
   * many states, but this should return the latest one.
   * <p>
   * If an item has been recorded, the most recent state will be exercise id->UNSET.
   *
   * @param skipUnset
   * @return
   * @see IUserListManager#getCommentedList
   */
/*  @Override
  public Map<Integer, StateCreator> getExerciseToState(boolean skipUnset) {
    return reviewedDAO.getExerciseToState(skipUnset);
  }*/

  /**
   * Mark the exercise with its states - but not if you're a recorder...
   *
   * @param shells
   * @seex #getReviewList
   * @see mitll.langtest.server.services.ExerciseServiceImpl#makeExerciseListWrapper
   */
 /* @Override
  public void markState(Collection<? extends CommonShell> shells) {
    Map<Integer, StateCreator> exerciseToState = reviewedDAO.getExerciseToState(false);

    logger.debug("markState " + shells.size() + " shells, " + exerciseToState.size() + " states");
    int c = 0;
    for (CommonShell shell : shells) {
      StateCreator stateCreator = exerciseToState.get(shell.getID());
      if (stateCreator != null) {
        shell.setState(stateCreator.getState());
        //  logger.debug("\t for " + shell.getOldID() + " state " + stateCreator.getState());
        c++;
      }
    }

    logger.debug("markState " + shells.size() + " shells, " + exerciseToState.size() + " states, marked " + c);


    // does this help anyone???
    // want to know if we have a new recording AFTER it's been inspected - why did the thing that I fixed now change back to needs inspection
    // maybe turn off for now???
*//*    if (false) {
      logger.debug("markState - first state " + c);
      exerciseToState = secondStateDAO.getExerciseToState(false);

      int n = 0;
      for (CommonShell shell : shells) {
        ReviewedDAO.StateCreator stateCreator = exerciseToState.get(shell.getOldID());
        if (stateCreator != null) {
          n++;
          shell.setSecondState(stateCreator.getState());
        }
      }
      logger.debug("markState - sec state " + n);
    }*//*
  }
*/

/*  @Override
  @NotNull
  public Set<Integer> getAttentionIDs() {
    Map<Integer, StateCreator> exerciseToState = secondStateDAO.getExerciseToState(false);
    //logger.debug("attention " + exerciseToState);

    Set<Integer> defectIds = new HashSet<>();
    for (Map.Entry<Integer, StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().getState().equals(STATE.ATTN_LL)) {
        defectIds.add(pair.getKey());
      }
    }
    return defectIds;
  }*/

/*  @Override
  @NotNull
  public Set<Integer> getDefectIDs() {
    Set<Integer> defectIds = new HashSet<>();
    Map<Integer, StateCreator> exerciseToState = reviewedDAO.getExerciseToState(false);
    //logger.debug("\tgetDefectList exerciseToState=" + exerciseToState.size());

    for (Map.Entry<Integer, StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().getState().equals(STATE.DEFECT)) {
        defectIds.add(pair.getKey());
      }
    }
    return defectIds;
  }*/


  /**
   * @param shell
   * @param state
   * @param creatorID
   * @seex mitll.langtest.server.database.DatabaseImpl#duplicateExercise
   * @see mitll.langtest.server.services.AudioServiceImpl#setExerciseState
   * @see mitll.langtest.server.database.custom.UserListManager#markState
   */
  @Override
  public void setState(HasID shell, STATE state, long creatorID) {
   // shell.setState(state);
    reviewedDAO.setState(shell.getID(), state, creatorID);
  }

  /**
   * @param shell
   * @param state
   * @param creatorID
   * @see mitll.langtest.server.services.AudioServiceImpl#setExerciseState
   * @see IUserListManager#markState
   */
  @Override
  public void setSecondState(HasID shell, STATE state, long creatorID) {
    //shell.setSecondState(state);
    secondStateDAO.setState(shell.getID(), state, creatorID);
  }

  /**
   * @param exerciseID
   * @return
   * @see mitll.langtest.server.services.AudioServiceImpl#setExerciseState
   */
  @Override
  public STATE getCurrentState(int exerciseID) {
    return reviewedDAO.getCurrentState(exerciseID);
  }

  /**
   * @param exerciseid
   * @seex mitll.langtest.server.database.DatabaseImpl#deleteItem(int, int)
   */
 /* @Override
  public void removeReviewed(int exerciseid) {
    reviewedDAO.remove(exerciseid);
  }*/


  /**
   * @return
   * @see IUserListManager#getCommentedList
   */
/*
  public Collection<Integer> getDefectExercises() {
    return reviewedDAO.getDefectExercises();
  }
*/

  /**
   * @return
   * @seex mitll.langtest.server.services.ExerciseServiceImpl#filterByUninspected
   */
  public Collection<Integer> getInspectedExercises() {
    return reviewedDAO.getInspectedExercises();
  }
}
