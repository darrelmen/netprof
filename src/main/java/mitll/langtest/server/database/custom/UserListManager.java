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

package mitll.langtest.server.database.custom;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.PathWriter;
import mitll.langtest.server.audio.TrackInfo;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.annotation.IAnnotationDAO;
import mitll.langtest.server.database.annotation.UserAnnotation;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.reviewed.IReviewedDAO;
import mitll.langtest.server.database.reviewed.StateCreator;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userlist.IUserExerciseListVisitorDAO;
import mitll.langtest.server.database.userlist.IUserListDAO;
import mitll.langtest.server.database.userlist.IUserListExerciseJoinDAO;
import mitll.langtest.server.database.userlist.SlickUserListDAO;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/2/13
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListManager implements IUserListManager {
  private static final Logger logger = LogManager.getLogger(UserListManager.class);

  public static final String CORRECT = "correct";
  private static final String INCORRECT = "incorrect";
  private static final String FIXED = "fixed";

  private static final String FAST = "regular";
  private static final String SLOW = "slow";
  private static final String MY_FAVORITES = "My Favorites";
  private static final String COMMENTS = "Comments";
  private static final String ALL_ITEMS_WITH_COMMENTS = "All items with comments";
  private static final String REVIEW = "Defects";
  private static final String ATTENTION = "AttentionLL";
  private static final String ITEMS_TO_REVIEW = "Possible defects to fix";
  private static final boolean DEBUG = false;

  private final IUserDAO userDAO;
  private final IReviewedDAO reviewedDAO, secondStateDAO;
  private int i = 0;

  private IUserExerciseDAO userExerciseDAO;
  private final IUserListDAO userListDAO;
  private final IUserExerciseListVisitorDAO visitorDAO;
  private final IUserListExerciseJoinDAO userListExerciseJoinDAO;
  private final IAnnotationDAO annotationDAO;
  private final PathHelper pathHelper;

  /**
   * @param userDAO
   * @param userListDAO
   * @param userListExerciseJoinDAO
   * @param annotationDAO
   * @param reviewedDAO
   * @param pathHelper
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public UserListManager(IUserDAO userDAO,
                         IUserListDAO userListDAO,
                         IUserListExerciseJoinDAO userListExerciseJoinDAO,
                         IAnnotationDAO annotationDAO,
                         IReviewedDAO reviewedDAO,
                         IReviewedDAO secondStateDAO,
                         IUserExerciseListVisitorDAO visitorDAO,
                         PathHelper pathHelper) {
    this.userDAO = userDAO;
    this.userListDAO = userListDAO;
    this.userListExerciseJoinDAO = userListExerciseJoinDAO;
    this.annotationDAO = annotationDAO;
    this.reviewedDAO = reviewedDAO;
    this.secondStateDAO = secondStateDAO;
    this.pathHelper = pathHelper;
    this.visitorDAO = visitorDAO;
  }

  /**
   * TODO : this doesn't really do anything - doesn't touch the exercises?????
   * <p>
   * Turned off setting second state for now -- what does it mean?
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#init()
   */
  @Override
  public void setStateOnExercises() {
    // getAmmendedStateMap();
    Map<Integer, StateCreator> exerciseToState = getExerciseToState(false);
    //setStateOnExercises(exerciseToState, true);
    //setStateOnExercises(secondStateDAO.getExerciseToState(false), false);
  }

  /**
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
   * @see IUserListManager#getCommentedList(Collection, Set)
   */
  @Override
  public Map<Integer, StateCreator> getExerciseToState(boolean skipUnset) {
    return reviewedDAO.getExerciseToState(skipUnset);
  }

  /**
   * Mark the exercise with its states - but not if you're a recorder...
   *
   * @param shells
   * @see #getReviewList
   * @see mitll.langtest.server.services.ExerciseServiceImpl#makeExerciseListWrapper
   */
  @Override
  public void markState(Collection<? extends CommonShell> shells) {
    Map<Integer, StateCreator> exerciseToState = reviewedDAO.getExerciseToState(false);

    //logger.debug("markState " + shells.size() + " shells, " + exerciseToState.size() + " states");
    //int c = 0;
    for (CommonShell shell : shells) {
      StateCreator stateCreator = exerciseToState.get(shell.getID());
      if (stateCreator != null) {
        shell.setState(stateCreator.getState());
        //  logger.debug("\t for " + shell.getOldID() + " state " + stateCreator.getState());
        //  c++;
      }
    }

    // does this help anyone???
    // want to know if we have a new recording AFTER it's been inspected - why did the thing that I fixed now change back to needs inspection
    // maybe turn off for now???
/*    if (false) {
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
    }*/
  }

  /**
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPublic
   * @param projid
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#addUserList
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doCreate
   */
  @Override
  public long addUserList(int userid, String name, String description, String dliClass, boolean isPublic, int projid) {
    UserList userList = createUserList(userid, name, description, dliClass, !isPublic, projid);
    if (userList == null) {
      logger.info("no user list??? for " + userid + " " + name);
      return -1;
    } else {
      return userList.getID();
    }
  }

  /**
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPrivate
   * @param projid
   * @return null if user already has a list with this name
   */
  private UserList createUserList(int userid,
                                  String name,
                                  String description,
                                  String dliClass,
                                  boolean isPrivate,
                                  int projid) {
    User userWhere = userDAO.getUserWhere(userid);
    if (userWhere == null) {
      logger.error("huh? no user with id " + userid);
      return null;
    } else {
      logger.info("found\n\t" + userListDAO.getAllByUser(userid, projid));

      if (hasByName(userid, name, projid)) {
        return null;
      } else {
        long now = System.currentTimeMillis();
        UserList e = new UserList(i++, userid, userWhere.getUserID(), name, description, dliClass, isPrivate, now, "", "");
        userListDAO.add(e, projid);
        logger.debug("createUserList : now there are " + userListDAO.getCount() + " lists total, for " + userid);
        return e;
      }
    }
  }

  private boolean hasByName(int userid, String name, int projid) {
    return userListDAO.hasByName(userid, name, projid);
  }

  @Override
  public Collection<UserList<CommonShell>> getMyLists(int userid, int projid) {
    return getListsForUser(userid, true, false, projid);
  }

  /**
   * TODO : expensive -- could just be a query against your own lists and/or against visited lists...
   *
   * @param userid
   * @param listsICreated
   * @param visitedLists
   * @param projid
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#getListsForUser
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  @Override
  public Collection<UserList<CommonShell>> getListsForUser(int userid, boolean listsICreated, boolean visitedLists,
                                                           int projid) {
    if (userid == -1) {
      return Collections.emptyList();
    }
    if (DEBUG)
      logger.debug("getListsForUser for user #" + userid + " only created " + listsICreated + " visited " + visitedLists);

    List<UserList<CommonShell>> listsForUser = new ArrayList<>();
    UserList<CommonShell> favorite = null;
    Set<Integer> ids = new HashSet<>();
    if (listsICreated) {
      listsForUser = userListDAO.getAllByUser(userid, projid);
      for (UserList<CommonShell> userList : listsForUser) {
        if (userList.isFavorite()) {
          favorite = userList;
        } else {
          //      logger.debug("not favorite " + userList + " " + userList.getName());
        }
        ids.add(userList.getID());
      }
    }
    if (visitedLists) {
      for (UserList<CommonShell> userList : userListDAO.getListsForUser(userid, projid)) {
        if (!ids.contains(userList.getID())) {
          listsForUser.add(userList);
        }
      }
    }

    if (listsForUser.isEmpty()) {
      if (DEBUG) {
        logger.warn("getListsForUser - list is empty for " + userid + " only created " +
            listsICreated + " visited " + visitedLists);
      }
    } else if (favorite != null) {
      listsForUser.remove(favorite);
      listsForUser.add(0, favorite);// put at front
    }

    if (DEBUG) {
      logger.debug("getListsForUser found " + listsForUser.size() +
          " lists for user #" + userid + " only created " + listsICreated + " visited " + visitedLists +
          "\n\tfavorite " + favorite);
      if (listsForUser.size() < 4) {
        for (UserList ul : listsForUser) logger.debug("\t" + ul);
      }
    }

    return listsForUser;
  }

  /**
   * @param userid
   * @param projid
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#rememberProject(int, int)
   */
  @Override
  public UserList createFavorites(int userid, int projid) {
    return createUserList(userid, UserList.MY_LIST, MY_FAVORITES, "", true, projid);
  }

  /**
   * So comments are items with incorrect annotations on their fields that have not been marked as defects.
   * <p>
   * Also, incorrect annotations are the latest annotations- a field can have a history of correct and incorrect
   * annotations - only if the latest is incorrect should the item appear on the comment or defect list.
   *
   * @param typeOrder
   * @param ids
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#getReviewLists
   */
  @Override
  public UserList<CommonShell> getCommentedList(Collection<String> typeOrder, Set<Integer> ids) {
    //Map<String, ReviewedDAO.StateCreator> exerciseToState = getExerciseToState(true); // skip unset items!

    Collection<Integer> incorrectAnnotations = annotationDAO.getExercisesWithIncorrectAnnotations();
    //logger.debug("getCommentedList There are " + defectExercises.size() + " defect items ");
    // logger.debug("getCommentedList There are " + idToCreator.size() + " idToCreator items ");

    // if it's on the defect list, remove it
    for (Integer exid : reviewedDAO.getDefectExercises()) {
      incorrectAnnotations.remove(exid);// what's left are items that are not reviewed
    }
    //logger.debug("getCommentedList After there are " + idToCreator.size() + " idToCreator items ");
    incorrectAnnotations.retainAll(ids);
    Collection<CommonExercise> include = userExerciseDAO.getByExID(incorrectAnnotations);
    //logger.debug("getCommentedList include " + include.size() + " included ");

    UserList<CommonShell> reviewList = getReviewList(include, COMMENTS, ALL_ITEMS_WITH_COMMENTS, incorrectAnnotations, COMMENT_MAGIC_ID, typeOrder);
    reviewList.setUniqueID(COMMENT_MAGIC_ID);
    return reviewList;
  }

  @Override
  public UserList<CommonExercise> getCommentedListEx(Collection<String> typeOrder, Set<Integer> ids) {
    //Map<String, ReviewedDAO.StateCreator> exerciseToState = getExerciseToState(true); // skip unset items!

    Collection<Integer> incorrectAnnotations = annotationDAO.getExercisesWithIncorrectAnnotations();
    //logger.debug("getCommentedList There are " + defectExercises.size() + " defect items ");
    // logger.debug("getCommentedList There are " + idToCreator.size() + " idToCreator items ");

    // if it's on the defect list, remove it
    for (Integer exid : reviewedDAO.getDefectExercises()) {
      incorrectAnnotations.remove(exid);// what's left are items that are not reviewed
    }
    //logger.debug("getCommentedList After there are " + idToCreator.size() + " idToCreator items ");

    incorrectAnnotations.retainAll(ids);

    Collection<CommonExercise> include = userExerciseDAO.getByExID(incorrectAnnotations);
    //logger.debug("getCommentedList include " + include.size() + " included ");

    UserList<CommonExercise> reviewList = getReviewListEx(include, COMMENTS, ALL_ITEMS_WITH_COMMENTS, incorrectAnnotations, COMMENT_MAGIC_ID, typeOrder);
    reviewList.setUniqueID(COMMENT_MAGIC_ID);
    return reviewList;
  }

  @Override
  public UserList<CommonShell> getAttentionList(Collection<String> typeOrder, Set<Integer> ids) {
    Set<Integer> defectIds = getAttentionIDs();
    defectIds.retainAll(ids);
    Collection<CommonExercise> allKnown = userExerciseDAO.getByExID(defectIds);
    logger.debug("\tgetAttentionList ids #=" + allKnown.size());

    return getReviewList(allKnown, ATTENTION, "Items for LL review", defectIds, ATTN_LL_MAGIC_ID, typeOrder);
  }

  @Override
  public UserList<CommonExercise> getAttentionListEx(Collection<String> typeOrder, Set<Integer> ids) {
    Set<Integer> defectIds = getAttentionIDs();
    defectIds.retainAll(ids);
    Collection<CommonExercise> allKnown = userExerciseDAO.getByExID(defectIds);
    logger.debug("\tgetAttentionList ids #=" + allKnown.size());

    return getReviewListEx(allKnown, ATTENTION, "Items for LL review", defectIds, ATTN_LL_MAGIC_ID, typeOrder);
  }

  @NotNull
  private Set<Integer> getAttentionIDs() {
    Map<Integer, StateCreator> exerciseToState = secondStateDAO.getExerciseToState(false);
    //logger.debug("attention " + exerciseToState);

    Set<Integer> defectIds = new HashSet<>();
    for (Map.Entry<Integer, StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().getState().equals(STATE.ATTN_LL)) {
        defectIds.add(pair.getKey());
      }
    }
    return defectIds;
  }

  /**
   * TODO : probably a bad idea to do a massive where in ... ids.
   *
   * @param typeOrder used by sorter to sort first in unit & chapter order
   * @param ids
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#getReviewLists
   * @see UserListManager#getUserListByID
   */
  @Override
  public UserList<CommonShell> getDefectList(Collection<String> typeOrder, Set<Integer> ids) {
    Set<Integer> defectIds = getDefectIDs();
    defectIds.retainAll(ids);
    Collection<CommonExercise> allKnown = userExerciseDAO.getByExID(defectIds);
    //logger.debug("\tgetDefectList ids #=" + allKnown.size() + " vs " + defectIds.size());

    return getReviewList(allKnown, REVIEW, ITEMS_TO_REVIEW, defectIds, REVIEW_MAGIC_ID, typeOrder);
  }

  /**
   * @param typeOrder
   * @param ids
   * @return
   * @see #getUserListByIDExercises(long, int, Collection, Set)
   */

  @Override
  public UserList<CommonExercise> getDefectListEx(Collection<String> typeOrder, Set<Integer> ids) {
    Set<Integer> defectIds = getDefectIDs();
    defectIds.retainAll(ids);
    Collection<CommonExercise> allKnown = userExerciseDAO.getByExID(defectIds);
    //logger.debug("\tgetDefectList ids #=" + allKnown.size() + " vs " + defectIds.size());

    return getReviewListEx(allKnown, REVIEW, ITEMS_TO_REVIEW, defectIds, REVIEW_MAGIC_ID, typeOrder);
  }

  @NotNull
  private Set<Integer> getDefectIDs() {
    Set<Integer> defectIds = new HashSet<>();
    Map<Integer, StateCreator> exerciseToState = reviewedDAO.getExerciseToState(false);
    //logger.debug("\tgetDefectList exerciseToState=" + exerciseToState.size());

    for (Map.Entry<Integer, StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().getState().equals(STATE.DEFECT)) {
        defectIds.add(pair.getKey());
      }
    }
    return defectIds;
  }

  /**
   * @param allKnown
   * @param name
   * @param description
   * @param ids
   * @param userListID
   * @param typeOrder   used by sorter to sort first in unit & chapter order
   * @return
   * @see IUserListManager#getAttentionList(Collection, Set)
   * @see IUserListManager#getCommentedList(Collection, Set)
   * @see IUserListManager#getDefectList(Collection, Set)
   */
  private UserList<CommonShell> getReviewList(Collection<CommonExercise> allKnown,
                                              String name, String description,
                                              Collection<Integer> ids,
                                              int userListID,
                                              Collection<String> typeOrder) {
    List<CommonExercise> onList = getReviewedExercises(allKnown, ids);

    // logger.debug("getReviewList '" +name+ "' ids size = " + allKnown.size() + " yielded " + onList.size());
    User qcUser = getQCUser();
    UserList<CommonShell> userList = new UserList<>(userListID, qcUser.getID(), qcUser.getUserID(), name, description, "",
        false, System.currentTimeMillis(), "", "");

    List<CommonShell> copy = new ArrayList<>();
    for (CommonShell orig : onList) copy.add(orig.getShell());

    logger.debug("getReviewList '" + name + "' ids size = " + allKnown.size() + " yielded " + copy.size());// + " took " + (now - then) + " millis");
    return getCommonUserList(typeOrder, userList, copy);
  }

  private UserList<CommonExercise> getReviewListEx(Collection<CommonExercise> allKnown,
                                                   String name, String description,
                                                   Collection<Integer> ids,
                                                   int userListID,
                                                   Collection<String> typeOrder) {
    List<CommonExercise> onList = getReviewedExercises(allKnown, ids);

    // logger.debug("getReviewList '" +name+ "' ids size = " + allKnown.size() + " yielded " + onList.size());
    User qcUser = getQCUser();
    UserList<CommonExercise> userList = new UserList<>(userListID, qcUser.getID(), qcUser.getUserID(), name, description, "",
        false, System.currentTimeMillis(), "", "");

//    List<CommonShell> copy = new ArrayList<>();
//    for (CommonShell orig : onList) copy.add(orig.getShell());

    logger.debug("getReviewList '" + name + "' ids size = " + allKnown.size() + " yielded " + onList.size());// + " took " + (now - then) + " millis");
    return getCommonUserList(typeOrder, userList, onList);
  }

  /**
   * TODO : should we worry about sort order for english?
   *
   * @param typeOrder
   * @param userList
   * @param copy
   * @param <T>
   * @return
   */
  @NotNull
  private <T extends CommonShell> UserList<T> getCommonUserList(Collection<String> typeOrder, UserList<T> userList, List<T> copy) {
    userList.setReview(true);

    new ExerciseSorter(typeOrder).getSorted(copy, false, false);

    userList.setExercises(copy);
    markState(copy);
    logger.debug("getReviewList returning " + userList + (userList.getExercises().isEmpty() ? "" : " first " + userList.getExercises().iterator().next()));
    return userList;
  }

  private List<CommonExercise> getReviewedExercises(Collection<CommonExercise> allKnown, Collection<Integer> ids) {
    Map<Integer, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise ue : allKnown) idToEx.put(ue.getID(), ue);

    return getReviewedUserExercises(idToEx, ids);
  }

  /**
   * Need a bogus user for the list.
   *
   * @return
   */
  private User getQCUser() {
    List<User.Permission> permissions = new ArrayList<User.Permission>();
    permissions.add(User.Permission.QUALITY_CONTROL);
    return new User(-1, 89, 0, 0, "", "", false, permissions);
  }

  /**
   * Wrap predef exercises with user exercises -- why?
   *
   * @param idToUserExercise
   * @param ids
   * @return
   * @see #getReviewList(Collection, String, String, Collection, int, Collection)
   */
  private List<CommonExercise> getReviewedUserExercises(Map<Integer, CommonExercise> idToUserExercise, Collection<Integer> ids) {
    List<CommonExercise> onList = new ArrayList<>();

    for (Integer id : ids) {
      CommonExercise commonExercise = idToUserExercise.get(id);
      if (commonExercise != null && !commonExercise.isPredefined()) {
        onList.add(commonExercise);
        //}
        //if (id.startsWith(UserExercise.CUSTOM_PREFIX)) {   // add user defined exercises
        //  if (idToUserExercise.containsKey(id)) {
        //    onList.add(idToUserExercise.get(id));
        // }
        //else {
        //  logger.debug("skipping id " + id + " since no in ");
        //}
      } else {                                    // add predef exercises
        CommonExercise byID = userExerciseDAO.getPredefExercise(id);
        if (byID != null) {
          //logger.debug("getReviewedUserExercises : found " + byID + " tooltip " + byID.getTooltip());
          Exercise e = new Exercise(byID);
          onList.add(e); // all predefined references
          //e.setTooltip(byID.getCombinedTooltip());
          //logger.debug("getReviewedUserExercises : found " + e.getOldID() + " tooltip " + e.getTooltip());
        } else {
          logger.warn("getReviewedUserExercises : huh? can't find predef exercise " + id);
        }
      }
    }
/*
    Collections.sort(onList, new Comparator<HasID>() {
      @Override
      public int compare(HasID o1, HasID o2) {
        return o1.getID().compareTo(o2.getOldID());
      }
    });
*/
    Collections.sort(onList);
    return onList;
  }

  /**
   * TODO : do a search over the list fields to find matches
   *
   * @param search NOT YET IMPLEMENTED
   * @param userid
   * @param projid
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#getUserListsForText
   */
  @Override
  public List<UserList<CommonShell>> getUserListsForText(String search, int userid, int projid) {
    return userListDAO.getAllPublic(userid, projid);
  }

  /**
   * Really create a new exercise and associated context exercise in database.
   * Add newly created exercise to the user list.
   *
   * @param userListID
   * @param userExercise notional until now!
   * @param mediaDir
   * @seex mitll.langtest.server.services.AudioServiceImpl#newExercise
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterValidForeignPhrase
   */
  @Override
  public void newExercise(long userListID, CommonExercise userExercise, String mediaDir) {
    UserList where = getUserList(userListID);
    newExerciseOnList(where, userExercise, mediaDir);
  }

  public UserList getUserList(long userListID) {
    return userListDAO.getWhere(userListID, true);
  }

  public void newExerciseOnList(UserList userList, CommonExercise userExercise, String mediaDir) {
    int projectID = userExercise.getProjectID();
    int newExerciseID = userExerciseDAO.add(userExercise, false, false,
        getTypeOrder(projectID));
    logger.debug("newExercise added exercise " + newExerciseID + " from " + userExercise);

    int contextID = 0;
    try {
      contextID = makeContextExercise(userExercise, newExerciseID, contextID, projectID);
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

    logger.debug("newExercise added context exercise " + contextID + " tied to " + newExerciseID + " in " + projectID);

    addItemToGivenList(userList, userExercise.getOldID(), newExerciseID);

    // TODO : necessary?
    fixAudioPaths(userExercise, true, mediaDir);
  }

  private Collection<String> getTypeOrder(int projectID) {
    return userDAO.getDatabase().getTypeOrder(projectID);
  }

  private int makeContextExercise(CommonExercise userExercise, int newExerciseID, int contextID, int projectID) {
    Exercise userExercise1 = new Exercise(-1, userExercise.getCreator(), "", projectID, false);
    contextID = userExerciseDAO.add(userExercise1, false, true, getTypeOrder(projectID));
    userExerciseDAO.addContextToExercise(newExerciseID, contextID, projectID);
    userExercise.getDirectlyRelated().add(userExercise1);
    return contextID;
  }

  /**
   * @param userListID
   * @param exerciseID
   * @param exid
   * @see mitll.langtest.server.services.ListServiceImpl#addItemToUserList
   */
  @Override
  public void addItemToList(long userListID, @Deprecated String exerciseID, int exid) {
    UserList where = getUserList(userListID);

    if (where != null) {
      addItemToGivenList(where, exerciseID, exid);
    } else {
      logger.warn("addItemToList: couldn't find ul with id " + userListID + " and '" + exerciseID + "'");
    }
  }

  private void addItemToGivenList(UserList where, @Deprecated String exerciseID, int exid) {
    long userListID = where.getID();

    userListExerciseJoinDAO.add(where, exerciseID, exid);
    userListDAO.updateModified(userListID);
  }

  /**
   * @param userExercise
   * @param mediaDir
   * @param typeOrder
   * @see mitll.langtest.server.database.DatabaseImpl#editItem
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#editItem
   */
  @Override
  public void editItem(CommonExercise userExercise, String mediaDir, Collection<String> typeOrder) {
    fixAudioPaths(userExercise, true, mediaDir);
    userExerciseDAO.update(userExercise, false, typeOrder);
  }

  /**
   * TODO : why all this foolishness with the id?
   * TODO : put this back?
   *
   * @param userExercise
   * @return
   * @seex mitll.langtest.server.LangTestDatabaseImpl#duplicateExercise
   */
  @Override
  public CommonExercise duplicate(CommonExercise userExercise) {
    logger.error("should call domino instead");
    return userExercise;
/*    String newid = getDupID(userExercise);

    logger.debug("duplicating " + userExercise + " with id " + newid);
    userExercise.getCombinedMutableUserExercise().setOldID(newid);
    userExerciseDAO.add(userExercise, true);
    String assignedID = userExercise.getOldID();

    // copy the annotations
    for (Map.Entry<String, ExerciseAnnotation> pair : userExercise.getFieldToAnnotation().entrySet()) {
      ExerciseAnnotation value = pair.getValue();
      addAnnotation(assignedID, pair.getKey(), value.getStatus(), value.getComment(), userExercise.getCombinedMutableUserExercise().getCreator());
    }

    return userExercise;*/
  }

/*  private String getDupID(CommonExercise userExercise) {
    String id = userExercise.getOldID();
    String newid;
    if (id.contains("dup")) {
      newid = id.split("dup")[0] + DUP + System.currentTimeMillis();
    } else {
      newid = id + DUP + System.currentTimeMillis();
    }
    return newid;
  }*/

  /**
   * TODO : Why is this needed?
   * <p>
   * Remember to copy the audio from the posted location to a more permanent location.
   * <p>
   * If it's already under a media directory -- don't change it.
   *
   * @param userExercise
   * @param overwrite
   * @param mediaDir
   * @see UserListManager#editItem
   * @see #newExercise
   */
  private void fixAudioPaths(CommonExercise userExercise, boolean overwrite, String mediaDir) {
    AudioAttribute regularSpeed = userExercise.getRegularSpeed();
    if (regularSpeed == null) {
      logger.info("fixAudioPaths no audio yet for " + userExercise);
      return;
    }
    long now = System.currentTimeMillis();
    logger.debug("fixAudioPaths : checking regular '" + regularSpeed.getAudioRef() + "' against '" + mediaDir + "'");

    // String foreignLanguage = userExercise.getForeignLanguage();
    int id = userExercise.getID();
    int projectID = userExercise.getProjectID();

    if (!regularSpeed.getAudioRef().contains(mediaDir)) {
      fixAudioPathOfAttribute(userExercise, overwrite, regularSpeed, now, id, projectID, FAST);
      logger.debug("fixAudioPaths : for " + userExercise.getOldID() + " fast is " + regularSpeed.getAudioRef());
    }

    AudioAttribute slowSpeed = userExercise.getSlowSpeed();
    if (slowSpeed != null && !slowSpeed.getAudioRef().isEmpty() &&
        !slowSpeed.getAudioRef().contains(mediaDir)) {
      fixAudioPathOfAttribute(userExercise, overwrite, slowSpeed, now, id, projectID, SLOW);
    }
  }

  /**
   * @param userExercise
   * @param overwrite
   * @param regularSpeed
   * @param now
   * @param id
   * @param projectID
   * @param prefix
   */
  private void fixAudioPathOfAttribute(CommonExercise userExercise,
                                       boolean overwrite,
                                       AudioAttribute regularSpeed,
                                       long now,
                                       int id,
                                       int projectID,
                                       String prefix) {
    File fileRef = pathHelper.getAbsoluteAudioFile(regularSpeed.getAudioRef());

    String fast = prefix + "_" + now + "_by_" + userExercise.getCreator() + ".wav";
    String artist = regularSpeed.getUser().getUserID();
    String refAudio = getRefAudioPath(
        projectID,
        id,
        fileRef,
        fast,
        overwrite,
        new TrackInfo(userExercise.getForeignLanguage(), artist, userExercise.getEnglish(), ""));
    regularSpeed.setAudioRef(refAudio);
  }

  /**
   * Copying audio from initial recording location to new location.
   * <p>
   * Also normalizes the audio level.
   *
   * @param projid
   * @param id
   * @param fileRef
   * @param destFileName
   * @param overwrite
   * @param trackInfo
   * @return new, permanent audio path
   * @see #fixAudioPaths
   */
  private String getRefAudioPath(int projid,
                                 int id,
                                 File fileRef,
                                 String destFileName,
                                 boolean overwrite,
                                 TrackInfo trackInfo) {
    Database database = userDAO.getDatabase();
    ServerProperties serverProps = database.getServerProps();
    Project project = database.getProject(projid);
    return new PathWriter(serverProps).getPermanentAudioPath(
        fileRef,
        destFileName,
        overwrite,
        project.getLanguage(),
        id,
        serverProps,
        trackInfo);
  }

  /**
   * @param userExerciseDAO
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(PathHelper)
   */
  public void setUserExerciseDAO(IUserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
    userListDAO.setUserExerciseDAO(userExerciseDAO);
  }

  /**
   * TODO : consider how to ask for just annotations for a project, instead of getting all of them
   * and then filtering for just those on the requested project
   *
   * @param id
   * @param typeOrder
   * @param ids
   * @return
   * @see
   * @see mitll.langtest.server.database.DatabaseImpl#getUserListByID
   */
  @Override
  public UserList<CommonShell> getUserListByID(long id, Collection<String> typeOrder, Set<Integer> ids) {
    if (id == -1) {
      logger.error("getUserListByID : huh? asking for id " + id);
      return null;
    }
    return
        id == REVIEW_MAGIC_ID ? getDefectList(typeOrder, ids) :
            id == COMMENT_MAGIC_ID ? getCommentedList(typeOrder, ids) :
                id == ATTN_LL_MAGIC_ID ? getAttentionList(typeOrder, ids) :
                    userListDAO.getWithExercises(id);
  }

  /**
   * @param id
   * @param projid
   * @param typeOrder
   * @param ids
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getUserListByIDExercises
   */
  @Override
  public UserList<CommonExercise> getUserListByIDExercises(long id, int projid, Collection<String> typeOrder, Set<Integer> ids) {
    if (id == -1) {
      logger.error("getUserListByID : huh? asking for id " + id);
      return null;
    }
    return
        id == REVIEW_MAGIC_ID ? getDefectListEx(typeOrder, ids) :
            id == COMMENT_MAGIC_ID ? getCommentedListEx(typeOrder, ids) :
                id == ATTN_LL_MAGIC_ID ? getAttentionListEx(typeOrder, ids) :
                    userListDAO.getWithExercisesEx(id);

  }

  /**
   * @param userListID
   * @param user
   * @seex mitll.langtest.client.custom.Navigation#addVisitor
   * @seex mitll.langtest.server.LangTestDatabaseImpl#addVisitor
   */
  @Override
  public void addVisitor(long userListID, long user) {
    //logger.debug("addVisitor - user " + user + " visits " + userList.getRealID());
    UserList where = getUserList(userListID);
    if (where != null) {
      userListDAO.addVisitor(where.getID(), user);
    } else if (userListID > 0) {
      logger.warn("addVisitor - can't find list with id " + userListID);
    }
  }

  private boolean listExists(long id) {
    return userListDAO.getWhere(id, false) != null;
  }

  /**
   * @param exerciseID
   * @param field
   * @param comment
   * @see mitll.langtest.server.database.exercise.BaseExerciseDAO#addDefects
   */
  @Override
  public boolean addDefect(int exerciseID, String field, String comment) {
    if (!annotationDAO.hasDefect(exerciseID, field, INCORRECT, comment)) {
      addAnnotation(exerciseID, field, INCORRECT, comment, userDAO.getDefectDetector());
      markState(exerciseID, STATE.DEFECT, userDAO.getDefectDetector());
      return true;
    } else {
      return false;
    }
  }

  /**
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userid
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation
   */
  @Override
  public void addAnnotation(int exerciseID, String field, String status, String comment, int userid) {
    UserAnnotation annotation = new UserAnnotation(exerciseID, field, status, comment, userid, System.currentTimeMillis());
    //logger.debug("addAnnotation " + annotation);
    annotationDAO.add(annotation);
  }

  /**
   * @param exercise
   * @see mitll.langtest.server.services.ExerciseServiceImpl#addAnnotations
   * @see #markAllFieldsFixed
   */
  @Override
  public void addAnnotations(CommonExercise exercise) {
    if (exercise != null) {
      MutableAnnotationExercise mutableAnnotation = exercise.getMutableAnnotation();
      Map<String, ExerciseAnnotation> latestByExerciseID = annotationDAO.getLatestByExerciseID(exercise.getID());
      for (Map.Entry<String, ExerciseAnnotation> pair : latestByExerciseID.entrySet()) {
        mutableAnnotation.addAnnotation(pair.getKey(), pair.getValue().getStatus(), pair.getValue().getComment());
      }
    } else {
      logger.warn("addAnnotations : on an empty exercise?");
    }
  }

  /**
   * @param id
   * @param correct
   * @param userid
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  @Override
  public void markCorrectness(int id, boolean correct, int userid) {
    markState(id, correct ? STATE.APPROVED : STATE.DEFECT, userid);
  }

  /**
   * @param exid
   * @param state
   * @param creatorID
   * @see #addAnnotation
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  @Override
  public void markState(int exid, STATE state, int creatorID) {
    //logger.debug("mark state " + id + " = " + state + " by " +creatorID);
    CommonExercise predefExercise = userExerciseDAO.getPredefExercise(exid);

    if (predefExercise == null) {
      logger.debug("markState " + exid + " = " + state + " by " + creatorID);
      predefExercise = userExerciseDAO.getByExID(exid);
    }
    if (predefExercise != null) {
      if (state.equals(STATE.ATTN_LL)) {
        setSecondState(predefExercise, state, creatorID);
      } else {
        setState(predefExercise, state, creatorID);
      }
      if (state.equals(STATE.FIXED)) {
        markAllFieldsFixed(predefExercise, creatorID);
      }
    } else {
      logger.error("huh? couldn't find exercise " + exid);
    }
  }

  /**
   * @param shell
   * @param state
   * @param creatorID
   * @see mitll.langtest.server.services.AudioServiceImpl#setExerciseState
   * @see mitll.langtest.server.database.DatabaseImpl#duplicateExercise
   * @see mitll.langtest.server.database.custom.UserListManager#markState(java.util.Collection)
   */
  @Override
  public void setState(Shell shell, STATE state, long creatorID) {
    shell.setState(state);
    reviewedDAO.setState(shell.getID(), state, creatorID);
  }

  /**
   * @param shell
   * @param state
   * @param creatorID
   * @see mitll.langtest.server.services.AudioServiceImpl#setExerciseState
   * @see mitll.langtest.server.database.custom.UserListManager#markState
   */
  @Override
  public void setSecondState(Shell shell, STATE state, long creatorID) {
    shell.setSecondState(state);
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
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem(int, int)
   */
  @Override
  public void removeReviewed(int exerciseid) {
    reviewedDAO.remove(exerciseid);
  }

  /**
   * @param userExercise
   * @param userid
   * @see #markState
   */
  private void markAllFieldsFixed(CommonExercise userExercise, int userid) {
    Collection<String> fields = userExercise.getFields();
    // logger.debug("setExerciseState " + userExercise + "  has " + fields + " user " + userid);
    addAnnotations(userExercise);
    for (String field : fields) {
      ExerciseAnnotation annotation1 = userExercise.getAnnotation(field);
      if (!annotation1.isCorrect()) {
        logger.debug("\tsetExerciseState " + userExercise.getID() + "  has " + annotation1);

        addAnnotation(userExercise.getID(), field, CORRECT, FIXED, userid);
      }
    }
  }


  /**
   * @param id
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#deleteList
   * @see mitll.langtest.client.custom.ListManager#deleteList
   */
  @Override
  public boolean deleteList(long id) {
    logger.debug("deleteList " + id);
    userListExerciseJoinDAO.removeListRefs(id);
    boolean b = listExists(id);
    if (!b) logger.warn("\tdeleteList huh? no list " + id);
    return b && userListDAO.remove(id);
  }

  /**
   * @param listid
   * @param exid
   * @param typeOrder
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#deleteItemFromList
   */
  @Override
  public boolean deleteItemFromList(long listid, int exid, Collection<String> typeOrder) {
    logger.debug("deleteItemFromList " + listid + " " + exid);

    UserList<?> userListByID = getUserListByID(listid, typeOrder, Collections.emptySet());
    if (userListByID == null) {
      logger.warn("deleteItemFromList huh? no user list with id " + listid);
      return false;
    }

    userListByID.remove(exid);
    return userListExerciseJoinDAO.remove(listid, exid);
  }

  @Override
  public void setPublicOnList(long userListID, boolean isPublic) {
    userListDAO.setPublicOnList(userListID, isPublic);
  }

  /**
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#filterByOnlyAudioAnno
   */
  @Override
  public Collection<Integer> getAudioAnnos() {
    return annotationDAO.getAudioAnnos();
  }

  @Override
  public IAnnotationDAO getAnnotationDAO() {
    return annotationDAO;
  }

  @Override
  public IUserListDAO getUserListDAO() {
    return userListDAO;
  }

  @Override
  public IUserListExerciseJoinDAO getUserListExerciseJoinDAO() {
    return userListExerciseJoinDAO;
  }

  @Override
  public IReviewedDAO getReviewedDAO() {
    return reviewedDAO;
  }

  @Override
  public IReviewedDAO getSecondStateDAO() {
    return secondStateDAO;
  }

  @Override
  public void createTables(DBConnection dbConnection, List<String> created) {
    List<IDAO> idaos = Arrays.asList(
        userListDAO,
        userListExerciseJoinDAO,
        reviewedDAO,
        secondStateDAO
    );

    for (IDAO dao : idaos) createIfNotThere(dbConnection, dao, created);

    String userexerciselistvisitor = "userexerciselistvisitor";
    if (!dbConnection.hasTable(userexerciselistvisitor)) {
      (((SlickUserListDAO) userListDAO).getVisitorDAOWrapper()).createTable();
      created.add(userexerciselistvisitor);
    }
  }

  private void createIfNotThere(DBConnection dbConnection, IDAO slickUserDAO, List<String> created) {
    String name = slickUserDAO.getName();
    if (!dbConnection.hasTable(name)) {
      slickUserDAO.createTable();
      created.add(name);
    }
  }

  @Override
  public IUserExerciseListVisitorDAO getVisitorDAO() {
    return visitorDAO;
  }

  public Collection<Integer> getDefectExercises() {
    return reviewedDAO.getDefectExercises();
  }

  public Collection<Integer> getInspectedExercises() {
    return reviewedDAO.getInspectedExercises();
  }
}
