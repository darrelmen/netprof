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
import mitll.langtest.server.audio.PathWriter;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import org.apache.log4j.Logger;

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
public class UserListManager {
  private static final Logger logger = Logger.getLogger(UserListManager.class);

  public static final String CORRECT = "correct";
  private static final String INCORRECT = "incorrect";
  private static final String FIXED = "fixed";

  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";
  private static final String MY_FAVORITES = "My Favorites";
  private static final String COMMENTS = "Comments";
  private static final String ALL_ITEMS_WITH_COMMENTS = "All items with comments";
  private static final String REVIEW = "Defects";
  private static final String ATTENTION = "AttentionLL";
  private static final String ITEMS_TO_REVIEW = "Possible defects to fix";
  public static final long REVIEW_MAGIC_ID = -100;
  public static final long COMMENT_MAGIC_ID = -200;
  private static final long ATTN_LL_MAGIC_ID = -300;

  private static final boolean DEBUG = false;

  private static final String DUP = "_dup_";

  private final UserDAO userDAO;
  private final ReviewedDAO reviewedDAO, secondStateDAO;
  private int i = 0;

  private UserExerciseDAO userExerciseDAO;
  private final UserListDAO userListDAO;
  private final UserListExerciseJoinDAO userListExerciseJoinDAO;
  private final AnnotationDAO annotationDAO;
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
  public UserListManager(UserDAO userDAO, UserListDAO userListDAO, UserListExerciseJoinDAO userListExerciseJoinDAO,
                         AnnotationDAO annotationDAO, ReviewedDAO reviewedDAO, ReviewedDAO secondStateDAO, PathHelper pathHelper) {
    this.userDAO = userDAO;
    this.userListDAO = userListDAO;
    this.userListExerciseJoinDAO = userListExerciseJoinDAO;
    this.annotationDAO = annotationDAO;
    this.reviewedDAO = reviewedDAO;
    this.secondStateDAO = secondStateDAO;
    this.pathHelper = pathHelper;
  }

  /**
   * Turned off setting second state for now -- what does it mean?
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#init()
   */
  public void setStateOnExercises() {
    getAmmendedStateMap();

    Map<String, ReviewedDAO.StateCreator> exerciseToState = getExerciseToState(false);

    setStateOnExercises(exerciseToState, true);
    //setStateOnExercises(secondStateDAO.getExerciseToState(false), false);
  }

  private void setStateOnExercises(Map<String, ReviewedDAO.StateCreator> exerciseToState, boolean firstState) {
    //logger.debug("found " + exerciseToState.size() + " state markings");
    Set<String> userExercisesRemaining = setStateOnPredefExercises(exerciseToState, firstState);
    setStateOnUserExercises(exerciseToState, userExercisesRemaining, firstState);
  }

  /**
   * @param exerciseToState
   * @param firstState
   * @return
   * @see #setStateOnExercises(java.util.Map, boolean)
   */
  // set state on predef exercises
  private Set<String> setStateOnPredefExercises(Map<String, ReviewedDAO.StateCreator> exerciseToState, boolean firstState) {
    int count = 0;
    Set<String> userExercisesRemaining = new HashSet<String>(exerciseToState.keySet());
    for (Map.Entry<String, ReviewedDAO.StateCreator> pair : exerciseToState.entrySet()) {
      CommonExercise predefExercise = userExerciseDAO.getPredefExercise(pair.getKey());
      if (predefExercise != null) {
        userExercisesRemaining.remove(pair.getKey());
        if (firstState) {
          predefExercise.setState(pair.getValue().getState());
        } else {
          predefExercise.setSecondState(pair.getValue().getState());
        }
        count++;
      }
    }
    if (count > 0) {
      logger.debug("got " + userExercisesRemaining.size() + " in userExercisesRemaining, updated " + count + " predef exercises");
    }
    return userExercisesRemaining;
  }

  // set states on user exercises
  private void setStateOnUserExercises(Map<String, ReviewedDAO.StateCreator> exerciseToState,
                                       Set<String> userExercisesRemaining, boolean firstState) {
    int count = 0;
    Collection<CommonExercise> userExercises = userExerciseDAO.getWhere(userExercisesRemaining);

    for (Shell commonUserExercise : userExercises) {
      ReviewedDAO.StateCreator state = exerciseToState.get(commonUserExercise.getID());
      if (state == null) {
        logger.error("huh? can't find ex id " + commonUserExercise.getID());
      } else {
        if (firstState) {
          commonUserExercise.setState(state.getState());
        } else {
          commonUserExercise.setSecondState(state.getState());
        }
        count++;
      }
    }
    if (count > 0) {
      logger.debug("updated " + count + " user exercises");
    }
  }

  /**
   * Update an old db where the review table doesn't have a state column.
   *
   * @return
   */
  private void getAmmendedStateMap() {
    Map<String, ReviewedDAO.StateCreator> stateMap = getExerciseToState(false);
    // logger.debug("got " + stateMap.size() +" in state map");
    Map<String, Long> exerciseToCreator = annotationDAO.getAnnotatedExerciseToCreator();
    //logger.debug("got " + exerciseToCreator.size() +" in defectIds");

    int count = 0;
    Set<String> reviewed = new HashSet<String>(stateMap.keySet());
    long now = System.currentTimeMillis();

    for (String exid : reviewed) {   // incorrect - could be defect or comment for now
      if (exerciseToCreator.keySet().contains(exid)) {
        ReviewedDAO.StateCreator stateCreator = stateMap.get(exid);
        if (stateCreator.getState().equals(STATE.UNSET)) { // only happen when we have an old db
          stateMap.put(exid, new ReviewedDAO.StateCreator(STATE.DEFECT, stateCreator.getCreatorID(), now));
          reviewedDAO.setState(exid, STATE.DEFECT, stateCreator.getCreatorID());
          count++;
        }
      }
    }

    if (count > 0) {
      logger.info("updated " + count + " rows in review table");
    }
    //  return stateMap;
  }

  /**
   * So this returns a map of exercise id to current (latest) state.  The exercise may have gone through
   * many states, but this should return the latest one.
   * <p>
   * If an item has been recorded, the most recent state will be exercise id->UNSET.
   *
   * @param skipUnset
   * @return
   * @see #getCommentedList(java.util.Collection)
   */
  private Map<String, ReviewedDAO.StateCreator> getExerciseToState(boolean skipUnset) {
    return reviewedDAO.getExerciseToState(skipUnset);
  }

  /**
   * Mark the exercise with its states - but not if you're a recorder...
   *
   * @param shells
   * @see #getReviewList
   * @see mitll.langtest.server.LangTestDatabaseImpl#makeExerciseListWrapper
   */
  public void markState(Collection<? extends CommonShell> shells) {
    Map<String, ReviewedDAO.StateCreator> exerciseToState = reviewedDAO.getExerciseToState(false);

    //logger.debug("markState " + shells.size() + " shells, " + exerciseToState.size() + " states");
    //int c = 0;
    for (CommonShell shell : shells) {
      ReviewedDAO.StateCreator stateCreator = exerciseToState.get(shell.getID());
      if (stateCreator != null) {
        shell.setState(stateCreator.getState());
        //  logger.debug("\t for " + shell.getID() + " state " + stateCreator.getState());
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
        ReviewedDAO.StateCreator stateCreator = exerciseToState.get(shell.getID());
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
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUserList
   * @see mitll.langtest.client.custom.dialog.CreateListDialog#doCreate
   */
  public long addUserList(long userid, String name, String description, String dliClass, boolean isPublic) {
    UserList userList = createUserList(userid, name, description, dliClass, !isPublic);
    if (userList == null) {
      logger.info("no user list??? for " + userid + " " + name);
      return -1;
    } else return userList.getUniqueID();
  }

  /**
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @param isPrivate
   * @return null if user already has a list with this name
   */
  private UserList createUserList(long userid, String name, String description, String dliClass, boolean isPrivate) {
    User userWhere = userDAO.getUserWhere(userid);
    if (userWhere == null) {
      logger.error("huh? no user with id " + userid);
      return null;
    } else {
      if (hasByName(userid, name)) {
        return null;
      } else {
        UserList e = new UserList(i++, userWhere, name, description, dliClass, isPrivate);
        userListDAO.add(e);
//        logger.debug("createUserList : now there are " + userListDAO.getCount() + " lists total, for " + userid);
        return e;
      }
    }
  }

  public boolean hasByName(long userid, String name) {
    return userListDAO.hasByName(userid, name);
  }

  public UserList<CommonShell> getByName(long userid, String name) {
    List<UserList<CommonShell>> byName = userListDAO.getByName(userid, name);
    return byName == null ? null : byName.iterator().next();
  }

  /**
   * TODO : expensive -- could just be a query against your own lists and/or against visited lists...
   *
   * @param userid
   * @param listsICreated
   * @param visitedLists
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getListsForUser
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   */
  public Collection<UserList<CommonShell>> getListsForUser(long userid, boolean listsICreated, boolean visitedLists) {
    if (userid == -1) {
      return Collections.emptyList();
    }
    if (DEBUG)
      logger.debug("getListsForUser for user #" + userid + " only created " + listsICreated + " visited " + visitedLists);

    List<UserList<CommonShell>> listsForUser = new ArrayList<>();
    UserList favorite = null;
    Set<Long> ids = new HashSet<Long>();
    if (listsICreated) {
      listsForUser = userListDAO.getAllByUser(userid);
      for (UserList userList : listsForUser) {
        if (userList.isFavorite()) {
          favorite = userList;
        } else {
          //      logger.debug("not favorite " + userList + " " + userList.getName());
        }
        ids.add(userList.getUniqueID());
      }
    }
    if (visitedLists) {
      for (UserList userList : userListDAO.getListsForUser(userid)) {
        if (!ids.contains(userList.getUniqueID())) {
          listsForUser.add(userList);
        }
      }
    }

    if (listsForUser.isEmpty()) {
      if (DEBUG) {
        logger.warn("getListsForUser - list is empty for " + userid + " only created " + listsICreated + " visited " + visitedLists);
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
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#addUser(int, String, int, String, String, String, String, java.util.Collection, String)
   */
  public UserList createFavorites(long userid) {
    return createUserList(userid, UserList.MY_LIST, MY_FAVORITES, "", true);
  }

  /**
   * So comments are items with incorrect annotations on their fields that have not been marked as defects.
   * <p>
   * Also, incorrect annotations are the latest annotations- a field can have a history of correct and incorrect
   * annotations - only if the latest is incorrect should the item appear on the comment or defect list.
   *
   * @param typeOrder
   * @return
   */
  public UserList<CommonShell> getCommentedList(Collection<String> typeOrder) {
    //Map<String, ReviewedDAO.StateCreator> exerciseToState = getExerciseToState(true); // skip unset items!

    Collection<String> defectExercises = reviewedDAO.getDefectExercises();
    Map<String, Long> idToCreator = annotationDAO.getAnnotatedExerciseToCreator();
    //logger.debug("getCommentedList There are " + defectExercises.size() + " defect items ");
    // logger.debug("getCommentedList There are " + idToCreator.size() + " idToCreator items ");

    // if it's on the defect list, remove it
    for (String id : defectExercises) {
      idToCreator.remove(id);// what's left are items that are not reviewed
    }
    //logger.debug("getCommentedList After there are " + idToCreator.size() + " idToCreator items ");

    Collection<CommonExercise> include = userExerciseDAO.getWhere(idToCreator.keySet());
    //logger.debug("getCommentedList include " + include.size() + " included ");

    UserList<CommonShell> reviewList = getReviewList(include, COMMENTS, ALL_ITEMS_WITH_COMMENTS, idToCreator.keySet(), COMMENT_MAGIC_ID, typeOrder);
    reviewList.setUniqueID(COMMENT_MAGIC_ID);
    return reviewList;
  }

  public UserList<CommonShell> getAttentionList(Collection<String> typeOrder) {
    Map<String, ReviewedDAO.StateCreator> exerciseToState = secondStateDAO.getExerciseToState(false);

    //logger.debug("attention " + exerciseToState);

    Set<String> defectIds = new HashSet<String>();
    for (Map.Entry<String, ReviewedDAO.StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().getState().equals(STATE.ATTN_LL)) {
        defectIds.add(pair.getKey());
      }
    }

    Collection<CommonExercise> allKnown = userExerciseDAO.getWhere(defectIds);
    logger.debug("\tgetAttentionList ids #=" + allKnown.size());

    return getReviewList(allKnown, ATTENTION, "Items for LL review", defectIds, ATTN_LL_MAGIC_ID, typeOrder);
  }

  /**
   * TODO : probably a bad idea to do a massive where in ... ids.
   *
   * @param typeOrder used by sorter to sort first in unit & chapter order
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getReviewLists()
   * @see #markCorrectness(String, boolean, long)
   */
  public UserList<CommonShell> getDefectList(Collection<String> typeOrder) {
    Set<String> defectIds = new HashSet<String>();
    Map<String, ReviewedDAO.StateCreator> exerciseToState = reviewedDAO.getExerciseToState(false);
    //logger.debug("\tgetDefectList exerciseToState=" + exerciseToState.size());

    for (Map.Entry<String, ReviewedDAO.StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().getState().equals(STATE.DEFECT)) {
        defectIds.add(pair.getKey());
      }
    }

    Collection<CommonExercise> allKnown = userExerciseDAO.getWhere(defectIds);
    //logger.debug("\tgetDefectList ids #=" + allKnown.size() + " vs " + defectIds.size());

    return getReviewList(allKnown, REVIEW, ITEMS_TO_REVIEW, defectIds, REVIEW_MAGIC_ID, typeOrder);
  }

  /**
   * @param allKnown
   * @param name
   * @param description
   * @param ids
   * @param userListMaginID
   * @param typeOrder       used by sorter to sort first in unit & chapter order
   * @return
   * @see #getAttentionList(java.util.Collection)
   * @see #getCommentedList(java.util.Collection)
   * @see #getDefectList(java.util.Collection)
   */
  private UserList<CommonShell> getReviewList(Collection<CommonExercise> allKnown,
                                              String name,
                                              String description,
                                              Collection<String> ids,
                                              long userListMaginID,
                                              Collection<String> typeOrder) {
    Map<String, CommonExercise> idToUser = new HashMap<>();
    for (CommonExercise ue : allKnown) idToUser.put(ue.getID(), ue);

    List<CommonShell> onList = getReviewedUserExercises(idToUser, ids);

    // logger.debug("getReviewList '" +name+ "' ids size = " + allKnown.size() + " yielded " + onList.size());
    User user = getQCUser();
    UserList<CommonShell> userList = new UserList<CommonShell>(userListMaginID, user, name, description, "", false);
    userList.setReview(true);

    new ExerciseSorter(typeOrder).getSortedByUnitThenAlpha(onList, false);

    userList.setExercises(onList);

    markState(onList);
    logger.debug("getReviewList returning " + userList + (userList.getExercises().isEmpty() ? "" : " first " + userList.getExercises().iterator().next()));
    return userList;
  }

  private User getQCUser() {
    List<User.Permission> permissions = new ArrayList<User.Permission>();
    permissions.add(User.Permission.QUALITY_CONTROL);
    return new User(-1, 89, 0, 0, "", "", false, permissions);
  }

  /**
   * @param idToUserExercise
   * @param ids
   * @return
   * @see #getReviewList(java.util.Collection, String, String, java.util.Collection, long, java.util.Collection)
   */
  private List<CommonShell> getReviewedUserExercises(Map<String, CommonExercise> idToUserExercise, Collection<String> ids) {
    List<CommonShell> onList = new ArrayList<>();

    int c = 0;
    for (String id : ids) {
      if (id.startsWith(UserExercise.CUSTOM_PREFIX)) {   // add user defined exercises
        if (idToUserExercise.containsKey(id)) {
          onList.add(idToUserExercise.get(id));
        }
        //else {
        //  logger.debug("skipping id " + id + " since no in ");
        //}
      } else {                                    // add predef exercises
        CommonExercise byID = userExerciseDAO.getPredefExercise(id);
        if (byID != null) {
          //logger.debug("getReviewedUserExercises : found " + byID + " tooltip " + byID.getTooltip());

          //UserExercise e = new UserExercise(byID, byID.getCreator());
          //onList.add(e); // all predefined references

          onList.add(byID);

          //e.setTooltip(byID.getCombinedTooltip());
          //logger.debug("getReviewedUserExercises : found " + e.getID() + " tooltip " + e.getTooltip());
        } else {
          if (c++ < 10) {
            logger.warn("getReviewedUserExercises : huh? can't find predef exercise " + id);
          }
        }
      }
    }
    logger.warn("getReviewedUserExercises : huh? couldn't find " + c + " predef exercises.");

    Collections.sort(onList, new Comparator<HasID>() {
      @Override
      public int compare(HasID o1, HasID o2) {
        return o1.getID().compareTo(o2.getID());
      }
    });
    return onList;
  }

  /**
   * TODO : do a search over the list fields to find matches
   *
   * @param search
   * @param userid
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserListsForText
   */
  public List<UserList<CommonShell>> getUserListsForText(String search, long userid) {
    return userListDAO.getAllPublic(userid);
  }

  /**
   * @param userListID
   * @param userExercise
   * @return
   * @see mitll.langtest.client.custom.exercise.NPFExercise#populateListChoices
   * @see mitll.langtest.server.LangTestDatabaseImpl#addItemToUserList
   */
  public void addItemToUserList(long userListID, String userExercise) {
    addItemToList(userListID, userExercise);
  }

  /**
   * @param userListID
   * @param userExercise
   * @param mediaDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#reallyCreateNewItem
   * @see #addItemToUserList
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#afterValidForeignPhrase
   */
  public void reallyCreateNewItem(long userListID, CommonExercise userExercise, String mediaDir) {
    userExerciseDAO.add(userExercise, false);

    addItemToList(userListID, userExercise.getID());
    editItem(userExercise, false, mediaDir);
  }

  /**
   * @param userListID
   * @param userExercise
   * @see #addItemToUserList
   */
  private void addItemToList(long userListID, String userExercise) {
    UserList where = userListDAO.getWhere(userListID, true);

    if (where == null) {
      logger.warn("addItemToList: couldn't find ul with id " + userListID + " and '" + userExercise +"'");
    }

    if (where != null) {
      userListExerciseJoinDAO.add(where, userExercise);
      userListDAO.updateModified(userListID);
    }
    if (where == null) {
      logger.error("\n\naddItemToList : couldn't find ul with id " + userListID);
    }
  }

  /**
   * @param userExercise
   * @param createIfDoesntExist
   * @param mediaDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#editItem
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem
   */
  public void editItem(CommonExercise userExercise,
                       boolean createIfDoesntExist,
                       String mediaDir) {
    fixAudioPaths(userExercise, true, mediaDir);
    userExerciseDAO.update(userExercise, createIfDoesntExist);
  }

  /**
   * @param userExercise
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#duplicateExercise
   */
  public CommonExercise duplicate(CommonExercise userExercise) {
    String id = userExercise.getID();
    String newid;
    if (id.contains("dup")) {
      newid = id.split("dup")[0] + DUP + System.currentTimeMillis();
    } else {
      newid = id + DUP + System.currentTimeMillis();
    }

    logger.debug("duplicating " + userExercise + " with id " + newid);
    userExercise.getCombinedMutableUserExercise().setID(newid);
    userExerciseDAO.add(userExercise, true);
    String assignedID = userExercise.getID();

    // copy the annotations
    for (Map.Entry<String, ExerciseAnnotation> pair : userExercise.getFieldToAnnotation().entrySet()) {
      ExerciseAnnotation value = pair.getValue();
      addAnnotation(assignedID, pair.getKey(), value.getStatus(), value.getComment(), userExercise.getCombinedMutableUserExercise().getCreator());
    }

    return userExercise;
  }

  /**
   * Remember to copy the audio from the posted location to a more permanent location.
   * <p>
   * If it's already under a media directory -- don't change it.
   *
   * @param userExercise
   * @param overwrite
   * @param mediaDir
   * @see #editItem
   */
  private void fixAudioPaths(CommonExercise userExercise, boolean overwrite, String mediaDir) {
    AudioAttribute regularSpeed = userExercise.getRegularSpeed();
    if (regularSpeed == null) {
      logger.warn("fixAudioPaths huh? no ref audio for " + userExercise);
      return;
    }
    long now = System.currentTimeMillis();

    //logger.debug("fixAudioPaths : checking regular '" + regularSpeed.getAudioRef() + "' against '" +mediaDir + "'");

    if (!regularSpeed.getAudioRef().contains(mediaDir)) {
      fixAudioPath(userExercise, overwrite, regularSpeed, now, FAST);
    }

    AudioAttribute slowSpeed = userExercise.getSlowSpeed();

    if (slowSpeed != null && !slowSpeed.getAudioRef().isEmpty() &&
        !slowSpeed.getAudioRef().contains(mediaDir)) {
      fixAudioPath(userExercise, overwrite, slowSpeed, now, SLOW);
    }
  }

  private void fixAudioPath(CommonExercise userExercise, boolean overwrite, AudioAttribute regularSpeed, long now, String prefix) {
    File fileRef = pathHelper.getAbsoluteFile(regularSpeed.getAudioRef());

    String fast = prefix + "_" + now + "_by_" + userExercise.getCombinedMutableUserExercise().getCreator() + ".wav";
    String artist   = regularSpeed.getUser().getUserID();
    String refAudio = getRefAudioPath(userExercise.getID(), fileRef, fast, overwrite, userExercise.getForeignLanguage(), artist);
    regularSpeed.setAudioRef(refAudio);
    //  logger.debug("fixAudioPaths : for " + userExercise.getID() + " fast is " + fast + " size " + FileUtils.size(refAudio));
  }

  /**
   * Copying audio from initial recording location to new location.
   * <p>
   * Also normalizes the audio level.
   *
   * @param id
   * @param fileRef
   * @param destFileName
   * @param overwrite
   * @param title
   * @param artist
   * @return new, permanent audio path
   * @see #fixAudioPaths
   */
  private String getRefAudioPath(String id, File fileRef, String destFileName, boolean overwrite, String title, String artist) {
    return new PathWriter().getPermanentAudioPath(pathHelper, fileRef, destFileName, overwrite, id, title, artist,
        userDAO.getDatabase().getServerProps());
  }

  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
    userListDAO.setUserExerciseDAO(userExerciseDAO);
  }

  /**
   * @param id
   * @param typeOrder
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
   * @see mitll.langtest.server.database.DatabaseImpl#getUserListName(long)
   */
  public UserList<CommonShell> getUserListByID(long id, Collection<String> typeOrder) {
    if (id == -1) {
      logger.error("getUserListByID : huh? asking for id " + id);
      return null;
    }
    return
        id == REVIEW_MAGIC_ID ? getDefectList(typeOrder) :
            id == COMMENT_MAGIC_ID ? getCommentedList(typeOrder) :
                id == ATTN_LL_MAGIC_ID ? getAttentionList(typeOrder) :
                    userListDAO.getWithExercises(id);
  }

  /**
   * @param userListID
   * @param user
   * @seex mitll.langtest.client.custom.Navigation#addVisitor
   * @see mitll.langtest.server.LangTestDatabaseImpl#addVisitor
   */
  public void addVisitor(long userListID, long user) {
    //logger.debug("addVisitor - user " + user + " visits " + userList.getUniqueID());
    UserList where = userListDAO.getWhere(userListID, true);
    if (where != null) {
      userListDAO.addVisitor(where.getUniqueID(), user);
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
   * @see mitll.langtest.server.database.exercise.ExcelImport#addDefects
   */
  public boolean addDefect(String exerciseID, String field, String comment) {
    if (!annotationDAO.hasAnnotation(exerciseID, field, INCORRECT, comment)) {
      long defectDetector = userDAO.getDefectDetector();
      addAnnotation(exerciseID, field, INCORRECT, comment, defectDetector);
      markState(exerciseID, STATE.DEFECT, defectDetector);
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
   * @param userID
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation(String, String, String)
   */
  public void addAnnotation(String exerciseID, String field, String status, String comment, long userID) {
    UserAnnotation annotation = new UserAnnotation(exerciseID, field, status, comment, userID, System.currentTimeMillis());
    //logger.debug("addAnnotation " + annotation);
    annotationDAO.add(annotation);
  }

  /**
   * @param exercise
   * @see mitll.langtest.server.LangTestDatabaseImpl#addAnnotations
   * @see #markAllFieldsFixed
   */
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
   * @param state
   * @param creatorID
   * @see mitll.langtest.server.LangTestDatabaseImpl#markReviewed
   * @see #addAnnotation(String, String, String, String, long)
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  public void markState(String id, STATE state, long creatorID) {
    //logger.debug("mark state " + id + " = " + state + " by " +creatorID);
    CommonExercise predefExercise = userExerciseDAO.getPredefExercise(id);

    if (predefExercise == null) {
      predefExercise = userExerciseDAO.getWhere(id);
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
      logger.error("huh? couldn't find exercise " + id);
    }
  }

  /**
   * @param shell
   * @param state
   * @param creatorID
   * @see mitll.langtest.server.LangTestDatabaseImpl#setExerciseState
   * @see mitll.langtest.server.database.DatabaseImpl#duplicateExercise
   * @see mitll.langtest.server.database.custom.UserListManager#markState(java.util.Collection)
   */
  public void setState(Shell shell, STATE state, long creatorID) {
    shell.setState(state);
    reviewedDAO.setState(shell.getID(), state, creatorID);
  }

  /**
   * @param shell
   * @param state
   * @param creatorID
   * @see mitll.langtest.server.LangTestDatabaseImpl#setExerciseState
   * @see mitll.langtest.server.database.custom.UserListManager#markState(String, mitll.langtest.shared.exercise.STATE, long)
   */
  public void setSecondState(Shell shell, STATE state, long creatorID) {
    shell.setSecondState(state);
    secondStateDAO.setState(shell.getID(), state, creatorID);
  }

  /**
   * @param exerciseID
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#setExerciseState
   */
  public STATE getCurrentState(String exerciseID) {
    return reviewedDAO.getCurrentState(exerciseID);
  }

  /**
   * @param exerciseid
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem(String)
   */
  public void removeReviewed(String exerciseid) {
    reviewedDAO.remove(exerciseid);
  }

  /**
   * @see #markState(String, STATE, long)
   * @param userExercise
   * @param userID
   */
  private void markAllFieldsFixed(CommonExercise userExercise, long userID) {
    Collection<String> fields = userExercise.getFields();
    logger.debug("markAllFieldsFixed " + userExercise + "  has " + fields + " user " + userID);
    addAnnotations(userExercise);
    for (String field : fields) {
      ExerciseAnnotation annotation1 = userExercise.getAnnotation(field);
      if (!annotation1.isCorrect()) {
        logger.debug("\tmarkAllFieldsFixed " + userExercise.getID() + "  has " + annotation1);

        addAnnotation(userExercise.getID(), field, CORRECT, FIXED, userID);
      }
    }
  }

  /**
   * @param id
   * @param correct
   * @param userID
   * @see mitll.langtest.server.LangTestDatabaseImpl#markReviewed(String, boolean, long)
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  public void markCorrectness(String id, boolean correct, long userID) {
    markState(id, correct ? STATE.APPROVED : STATE.DEFECT, userID);
  }

  /**
   * @param id
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#deleteList(long)
   * @see mitll.langtest.client.custom.ListManager#deleteList
   */
  public boolean deleteList(long id) {
    logger.debug("deleteList " + id);
    userListExerciseJoinDAO.removeListRefs(id);
    boolean b = listExists(id);
    if (!b) logger.warn("\tdeleteList huh? no list " + id);
    return b && userListDAO.remove(id);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#deleteItemFromList(long, String)
   * @param listid
   * @param exid
   * @param typeOrder
   * @return
   */
  public boolean deleteItemFromList(long listid, String exid, Collection<String> typeOrder) {
    logger.debug("deleteItemFromList " + listid + " " + exid);

    UserList<?> userListByID = getUserListByID(listid, typeOrder);
    if (userListByID == null) {
      logger.warn("deleteItemFromList huh? no user list with id " + listid);
      return false;
    }

    userListByID.remove(exid);
    return userListExerciseJoinDAO.remove(listid, exid);
  }

  public void setPublicOnList(long userListID, boolean isPublic) {
    userListDAO.setPublicOnList(userListID, isPublic);
  }

  /**
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#filterByOnlyAudioAnno
   */
  public Set<String> getAudioAnnos() {
    return annotationDAO.getAudioAnnos();
  }

  public UserExerciseDAO getUserExerciseDAO() {
    return userExerciseDAO;
  }
}
