package mitll.langtest.server.database.custom;

import audio.tools.FileCopier;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.PathWriter;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;
import org.h2.store.fs.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListManager {
  private static final Logger logger = Logger.getLogger(UserListManager.class);

  private static final String CORRECT = "correct";
  private static final String FIXED = "fixed";
  private static final String INCORRECT = "incorrect";

  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";
  private static final String MY_FAVORITES = "My Favorites";
  public static final String COMMENTS = "Comments";
  public static final String ALL_ITEMS_WITH_COMMENTS = "All items with comments";
  public static final String REVIEW = "Defects";
  public static final String ATTENTION = "AttentionLL";
  public static final String ITEMS_TO_REVIEW = "Possible defects to fix";
  public static final long REVIEW_MAGIC_ID = -100;
  public static final long COMMENT_MAGIC_ID = -200;
  public static final long ATTN_LL_MAGIC_ID = -300;

  private final UserDAO userDAO;
  private final ReviewedDAO reviewedDAO, secondStateDAO;
  private int i = 0;

  private UserExerciseDAO userExerciseDAO;
  private final UserListDAO userListDAO;
  private final UserListExerciseJoinDAO userListExerciseJoinDAO;
  private final AnnotationDAO annotationDAO;
  private final PathHelper pathHelper;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   * @param userDAO
   * @param userListDAO
   * @param userListExerciseJoinDAO
   * @param annotationDAO
   * @param reviewedDAO
   * @param pathHelper
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#init()
   */
  public void setStateOnExercises() {
    getAmmendedStateMap();

    Map<String, ReviewedDAO.StateCreator> exerciseToState = getExerciseToState();

    setStateOnExercises(exerciseToState,true);
    setStateOnExercises(secondStateDAO.getExerciseToState(), false);
  }

  private void setStateOnExercises(Map<String, ReviewedDAO.StateCreator> exerciseToState, boolean firstState) {
    logger.debug("found " + exerciseToState.size() + " state markings");

    Set<String> userExercisesRemaining = setStateOnPredefExercises(exerciseToState, firstState);
    setStateOnUserExercises(exerciseToState, userExercisesRemaining, firstState);
  }

  /**
   * @see #setStateOnExercises(java.util.Map, boolean)
   * @param exerciseToState
   * @param firstState
   * @return
   */
  // set state on predef exercises
  private Set<String> setStateOnPredefExercises(Map<String, ReviewedDAO.StateCreator> exerciseToState, boolean firstState) {
    int count = 0;
    Set<String> userExercisesRemaining = new HashSet<String>(exerciseToState.keySet());
    for (Map.Entry<String,  ReviewedDAO.StateCreator> pair : exerciseToState.entrySet()) {
      CommonExercise predefExercise = userExerciseDAO.getPredefExercise(pair.getKey());
      if (predefExercise != null) {
        userExercisesRemaining.remove(pair.getKey());
        if (firstState) {
          predefExercise.setState(pair.getValue().state);
        }
        else {
          predefExercise.setSecondState(pair.getValue().state);
        }
        count++;
      }
    }
    logger.debug("got " + userExercisesRemaining.size() +" in userExercisesRemaining, updated " + count + " predef exercises");
    return userExercisesRemaining;
  }

  // set states on user exercises
  protected void setStateOnUserExercises(Map<String, ReviewedDAO.StateCreator> exerciseToState,
                                         Set<String> userExercisesRemaining, boolean firstState) {
    int count = 0;
    List<CommonUserExercise> userExercises = userExerciseDAO.getWhere(userExercisesRemaining);

    for (CommonShell commonUserExercise : userExercises) {
      ReviewedDAO.StateCreator state = exerciseToState.get(commonUserExercise.getID());
      if (state == null) {
        logger.error("huh? can't find ex id " + commonUserExercise.getID());
      }
      else {
        if (firstState) {
          commonUserExercise.setState(state.state);
        }
        else {
          commonUserExercise.setSecondState(state.state);
        }
        count++;
      }
    }
    logger.debug("updated " + count + " user exercises");
  }

  /**
   * Update an old db where the review table doesn't have a state column.
   * @paramx annotationDAO
   * @return
   */
  private Map<String, ReviewedDAO.StateCreator> getAmmendedStateMap() {
    Map<String, ReviewedDAO.StateCreator> stateMap = getExerciseToState();
    logger.debug("got " + stateMap.size() +" in state map");
    Map<String,Long> exerciseToCreator = annotationDAO.getDefectIds();
    logger.debug("got " + exerciseToCreator.size() +" in defectIds");

    int count = 0;
    Set<String> reviewed = new HashSet<String>(stateMap.keySet());
    for (String exid : reviewed) {   // incorrect - could be defect or comment for now
      if (exerciseToCreator.keySet().contains(exid)) {
        ReviewedDAO.StateCreator stateCreator = stateMap.get(exid);
        if (stateCreator.state.equals(CommonShell.STATE.UNSET)) { // only happen when we have an old db
          stateMap.put(exid, new ReviewedDAO.StateCreator(CommonShell.STATE.DEFECT, stateCreator.creatorID));
          reviewedDAO.setState(exid, CommonShell.STATE.DEFECT, stateCreator.creatorID);
          count++;
        }
      }
    }

    if (count > 0) {
      logger.info("updated " + count + " rows in review table");
    }
    return stateMap;
  }

  private Map<String, ReviewedDAO.StateCreator> getExerciseToState() {
    return reviewedDAO.getExerciseToState();
  }

  /**
   * @see #getReviewList
   * @see mitll.langtest.server.LangTestDatabaseImpl#makeExerciseListWrapper
   * @param shells
   */
  public void markState(Collection<? extends CommonShell> shells) {
    Map<String, ReviewedDAO.StateCreator> exerciseToState = reviewedDAO.getExerciseToState();

    logger.debug("markState " + shells.size() + " shells, " + exerciseToState.size() + " states");
    for (CommonShell shell : shells) {
      ReviewedDAO.StateCreator stateCreator = exerciseToState.get(shell.getID());
      if (stateCreator != null) {
        shell.setState(stateCreator.state);
     //   if (shell.getID().equals("0")) logger.debug("\tmarkState on " +shell + " of " + stateCreator.state);

      }
    }

    exerciseToState = secondStateDAO.getExerciseToState();

    for (CommonShell shell : shells) {
      ReviewedDAO.StateCreator stateCreator = exerciseToState.get(shell.getID());
      if (stateCreator != null) {
        shell.setSecondState(stateCreator.state);
      }
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUserList(long, String, String, String)
   * @see mitll.langtest.client.custom.CreateListDialog#doCreate
   * @param userid
   * @param name
   * @param description
   * @param dliClass
   * @return
   */
  public long addUserList(long userid, String name, String description, String dliClass) {
    UserList userList = createUserList(userid, name, description, dliClass, false);
    if (userList == null) return -1;
    else return userList.getUniqueID();
  }

  /**
   *
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
        logger.debug("createUserList : now there are " + userListDAO.getCount() + " lists total, for " + userid);
        return e;
      }
    }
  }

  public boolean hasByName(long userid, String name) {
    return userListDAO.hasByName(userid, name);
  }

  public UserList getByName(long userid, String name) {
    List<UserList> byName = userListDAO.getByName(userid, name);
    return byName == null ? null : byName.iterator().next();
  }

  /**
   * TODO : expensive -- could just be a query against your own lists and/or against visited lists...
   * @see mitll.langtest.server.LangTestDatabaseImpl#getListsForUser
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
   * @param userid
   * @param listsICreated
   * @param visitedLists
   * @return
   */
  public Collection<UserList> getListsForUser(long userid, boolean listsICreated, boolean visitedLists) {
    if (userid == -1) {
      return Collections.emptyList();
    }
    //logger.debug("getListsForUser for user #" + userid + " only created " + listsICreated + " visited " +visitedLists);

    List<UserList> listsForUser = new ArrayList<UserList>();
    UserList favorite = null;
    Set<Long> ids = new HashSet<Long>();
    if (listsICreated) {
      listsForUser = userListDAO.getAllByUser(userid);
      for (UserList userList : listsForUser) {
        if (userList.isFavorite()) {
          favorite = userList;
        }
        else {
          logger.debug("not favorite " + userList + " " + userList.getName());
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
      logger.warn("getListsForUser - list is empty for " + userid + " only created " + listsICreated + " visited " +visitedLists);
    }
    else if (favorite != null) {
      listsForUser.remove(favorite);
      listsForUser.add(0, favorite);// put at front
    }

    logger.debug("getListsForUser found " + listsForUser.size() +
      " lists for user #" + userid + " only created " + listsICreated + " visited " +visitedLists +
      "\n\tfavorite " + favorite);

    return listsForUser;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#addUser(int, String, int, String, String, String, String)
   * @param userid
   * @return
   */
  public UserList createFavorites(long userid) {
    return createUserList(userid, UserList.MY_LIST, MY_FAVORITES, "", true);
  }

  /**
   * So comments are items with incorrect annotations on their fields that have not been marked as defects
   * Also, incorrect annotations are the latest annotations- a field can have a history of correct and incorrect
   * annotations - only if the latest is incorrect should the item appear on the comment or defect list.
   * @return
   */
  public UserList getCommentedList() {
    Map<String, ReviewedDAO.StateCreator> exerciseToState = getExerciseToState();
    Map<String, Long> idToCreator = annotationDAO.getDefectIds();
    logger.debug("getCommentedList There are " + exerciseToState.size() + " reviewed items ");
    logger.debug("getCommentedList There are " + idToCreator.size() + " idToCreator items ");
    for (String id : exerciseToState.keySet()) {
      idToCreator.remove(id);// what's left are items that are not reviewed
    }
    logger.debug("getCommentedList After there are " + idToCreator.size() + " idToCreator items ");

    List<CommonUserExercise> include = userExerciseDAO.getWhere(idToCreator.keySet());

    UserList reviewList = getReviewList(include, COMMENTS, ALL_ITEMS_WITH_COMMENTS, idToCreator.keySet(),COMMENT_MAGIC_ID);
    reviewList.setUniqueID(COMMENT_MAGIC_ID);
    return reviewList;
  }

  public UserList getAttentionList() {
    Map<String, ReviewedDAO.StateCreator> exerciseToState = secondStateDAO.getExerciseToState();

    logger.debug("attention " + exerciseToState);

    Set<String> defectIds = new HashSet<String>();
    for (Map.Entry<String, ReviewedDAO.StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().state.equals(CommonShell.STATE.ATTN_LL)) {
        defectIds.add(pair.getKey());
      }
    }

    List<CommonUserExercise> allKnown = userExerciseDAO.getWhere(defectIds);
    logger.debug("\tgetAttentionList ids #=" + allKnown.size());

    return getReviewList(allKnown, ATTENTION, "Items for LL review", defectIds,ATTN_LL_MAGIC_ID);
  }

  /**
   * TODO : probably a bad idea to do a massive where in ... ids.
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#getDefectList()
   * @see mitll.langtest.client.custom.Navigation#viewReview
   * @see #markCorrectness(String, boolean, long)
   * @return
   */
  public UserList getDefectList() {
    Set<String> defectIds = new HashSet<String>();
    Map<String, ReviewedDAO.StateCreator> exerciseToState = reviewedDAO.getExerciseToState();

    for (Map.Entry<String, ReviewedDAO.StateCreator> pair : exerciseToState.entrySet()) {
      if (pair.getValue().state.equals(CommonShell.STATE.DEFECT)) {
        defectIds.add(pair.getKey());
      }
    }

    List<CommonUserExercise> allKnown = userExerciseDAO.getWhere(defectIds);
    logger.debug("\tgetDefectList ids #=" + allKnown.size() + " vs " + defectIds.size());

    return getReviewList(allKnown, REVIEW, ITEMS_TO_REVIEW, defectIds,REVIEW_MAGIC_ID);
  }

  private UserList getReviewList(List<CommonUserExercise> allKnown, String name, String description, Collection<String> ids,long userListMaginID) {
    Map<String, CommonUserExercise> idToUser = new HashMap<String, CommonUserExercise>();
    for (CommonUserExercise ue : allKnown) idToUser.put(ue.getID(), ue);

    List<CommonUserExercise> onList = getReviewedUserExercises(idToUser, ids);

    logger.debug("getDefectList ids size = " + allKnown.size() + " yielded " + onList.size());
    User user = new User(-1, 89, 0, 0, "", "", false);
    //long userListMaginID = REVIEW_MAGIC_ID;
    UserList userList = new UserList(userListMaginID, user, name, description, "", false);
    userList.setReview(true);
    userList.setExercises(onList);

    markState(onList);
    return userList;
  }

  /**
   * @see #getDefectList()
   * @param idToUserExercise
   * @paramx incorrect redundant, but guarantees same order as they are reviewed
   * @return
   */
  private List<CommonUserExercise> getReviewedUserExercises(Map<String, CommonUserExercise> idToUserExercise, Collection<String> ids) {
    List<CommonUserExercise> onList = new ArrayList<CommonUserExercise>();

    for (String id : ids) {
      if (!id.startsWith(UserExercise.CUSTOM_PREFIX)) {   // add predef exercises
        CommonExercise byID = userExerciseDAO.getPredefExercise(id);
        //logger.debug("getReviewedUserExercises : found " + byID + " tooltip " + byID.getTooltip());
        if (byID != null) {
          onList.add(new UserExercise(byID)); // all predefined references
        }
      }
      else {                                             // add user defined exercises
        if (idToUserExercise.containsKey(id)) onList.add(idToUserExercise.get(id));
      }
    }
    Collections.sort(onList, new Comparator<CommonUserExercise>() {
      @Override
      public int compare(CommonUserExercise o1, CommonUserExercise o2) {
        return o1.getID().compareTo(o2.getID());
      }
    });
    return onList;
  }

  /**
   * TODO : do a search over the list fields to find matches
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUserListsForText
   * @param search
   * @param userid
   * @return
   */
  public List<UserList> getUserListsForText(String search, long userid) {
    return userListDAO.getAllPublic(userid);
  }

  /**
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
   * @see mitll.langtest.server.LangTestDatabaseImpl#addItemToUserList(long, mitll.langtest.shared.custom.UserExercise)
   * @param userListID
   * @param userExercise
   * @return
   */
  public void addItemToUserList(long userListID, UserExercise userExercise) { addItemToList(userListID, userExercise); }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#reallyCreateNewItem
   * @see #addItemToUserList(long, mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.client.custom.NewUserExercise#afterValidForeignPhrase
   * @param userListID
   * @param userExercise
   */
  public void reallyCreateNewItem(long userListID, UserExercise userExercise) {
    userExerciseDAO.add(userExercise, false);

    addItemToList(userListID, userExercise);
    editItem(userExercise, false);
  }

  /**
   * @see #addItemToUserList(long, mitll.langtest.shared.custom.UserExercise)
   * @param userListID
   * @param userExercise
   */
  private void addItemToList(long userListID, UserExercise userExercise) {
    UserList where = userListDAO.getWhere(userListID, true);
    if (where != null) {
      userListExerciseJoinDAO.add(where, userExercise.getID());
      userListDAO.updateModified(userListID);
    }
    if (where == null) {
      logger.error("\n\nreallyCreateNewItem : couldn't find ul with id " + userListID);
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.client.custom.EditableExercise#postEditItem(mitll.langtest.client.list.ListInterface, boolean)
   *
   * @param userExercise
   * @param createIfDoesntExist
   */
  public void editItem(UserExercise userExercise, boolean createIfDoesntExist) {
    fixAudioPaths(userExercise, true);
    userExerciseDAO.update(userExercise, createIfDoesntExist);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#duplicateExercise(mitll.langtest.shared.custom.UserExercise)
   * @param userExercise
   * @return
   */
  public UserExercise duplicate(UserExercise userExercise) {
    String id = userExercise.getID();
    String newid;
    if (id.contains("dup")) {
       newid = id.split("dup")[0] + "_dup_" +System.currentTimeMillis();
    }
    else {
       newid = id + "_dup_" +System.currentTimeMillis();
    }

    logger.debug("duplicating " + userExercise + " with id " + newid);
    userExercise.setID(newid);
    userExerciseDAO.add(userExercise, true);
    String assignedID = userExercise.getID();

    // copy the annotations
    for (Map.Entry<String, ExerciseAnnotation> pair : userExercise.getFieldToAnnotation().entrySet()) {
      ExerciseAnnotation value = pair.getValue();
      addAnnotation(assignedID, pair.getKey(), value.status, value.comment, userExercise.getCreator());
    }

    return userExercise;
  }

  /**
   * Remember to copy the audio from the posted location to a more permanent location.
   * @see #editItem(mitll.langtest.shared.custom.UserExercise, boolean)
   * @param userExercise
   * @param overwrite
   */
  private void fixAudioPaths(UserExercise userExercise, boolean overwrite) {
    String refAudio1 = userExercise.getRefAudio();
    if (refAudio1 == null) {
      logger.warn("huh? no ref audio for " + userExercise);
      return;
    }
    File fileRef = pathHelper.getAbsoluteFile(refAudio1);
    long now = System.currentTimeMillis();
    String fast = FAST + "_"+ now +"_by_" +userExercise.getCreator()+".wav";
    String refAudio = getRefAudioPath(userExercise.getID(), fileRef, fast, overwrite);
    userExercise.setRefAudio(refAudio);
    //logger.debug("fixAudioPaths : for " + userExercise.getID() + " fast is " + fast + " size " + FileUtils.size(refAudio));

    if (userExercise.getSlowAudioRef() != null && !userExercise.getSlowAudioRef().isEmpty()) {
      fileRef = pathHelper.getAbsoluteFile(userExercise.getSlowAudioRef());
      String slow = SLOW + "_"+ now+"_by_" + userExercise.getCreator()+ ".wav";

      refAudio = getRefAudioPath(userExercise.getID(), fileRef, slow, overwrite);
      logger.debug("fixAudioPaths : for " + userExercise.getID()+ " slow is " + refAudio + " size " + FileUtils.size(refAudio));

      userExercise.setSlowRefAudio(refAudio);
    }
  }

  /**
   * Copying audio from initial recording location to new location.
   *
   * Also normalizes the audio level.
   *
   * @param id
   * @param fileRef
   * @param destFileName
   * @param overwrite
   * @return new, permanent audio path
   * @see #fixAudioPaths(mitll.langtest.shared.custom.UserExercise, boolean)
   */
  private String getRefAudioPath(String id, File fileRef, String destFileName, boolean overwrite) {
   // String id = userExercise.getID();
    return new PathWriter().getPermanentAudioPath(pathHelper, fileRef, destFileName, overwrite, id);
  }

/*  private String getPermanentAudioPath(File fileRef, String destFileName, boolean overwrite, String id) {
    final File bestDir = pathHelper.getAbsoluteFile("bestAudio");
    if (!bestDir.exists() && !bestDir.mkdir()) {
      if (!bestDir.exists()) logger.warn("huh? couldn't make " + bestDir.getAbsolutePath());
    }
    File bestDirForExercise = new File(bestDir, id);
    if (!bestDirForExercise.exists() && !bestDirForExercise.mkdir()) {
      if (!bestDirForExercise.exists()) logger.warn("huh? couldn't make " + bestDirForExercise.getAbsolutePath());
    }
    File destination = new File(bestDirForExercise, destFileName);
    //logger.debug("getRefAudioPath : copying from " + fileRef +  " to " + destination.getAbsolutePath());
    String s = "bestAudio" + File.separator + id + File.separator + destFileName;
    //logger.debug("getRefAudioPath : dest path    " + bestDirForExercise.getPath() + " vs " +s);
    if (!fileRef.equals(destination)) {
      new FileCopier().copy(fileRef.getAbsolutePath(), destination.getAbsolutePath());

      new AudioConversion().normalizeLevels(destination);
    }
    else {
      if (FileUtils.size(destination.getAbsolutePath()) == 0) logger.error("\ngetRefAudioPath : huh? " + destination + " is empty???");
    }
    ensureMP3(s, overwrite);
    return s;
  }

  private void ensureMP3(String wavFile, boolean overwrite) {
    if (wavFile != null) {
      new AudioConversion().ensureWriteMP3(wavFile, pathHelper.getInstallPath(), overwrite);
    }
  }*/

  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
    userListDAO.setUserExerciseDAO(userExerciseDAO);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds
   * @param id
   * @return
   */
  public UserList getUserListByID(long id) {
    if (id == -1) {
      logger.error("getUserListByID : huh? asking for id " + id);
      return null;
    }
    return
      id == REVIEW_MAGIC_ID ? getDefectList() :
        id == COMMENT_MAGIC_ID ? getCommentedList() :
          id == ATTN_LL_MAGIC_ID ? getAttentionList() :
            userListDAO.getWithExercises(id);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addVisitor
   * @see mitll.langtest.client.custom.Navigation#addVisitor(mitll.langtest.shared.custom.UserList)
   * @param userListID
   * @param user
   */
  public void addVisitor(long userListID, long user) {
    //logger.debug("addVisitor - user " + user + " visits " + userList.getUniqueID());
    UserList where = userListDAO.getWhere(userListID, true);
    if (where != null) {
      userListDAO.addVisitor(where.getUniqueID(), user);
    }
    else {
      logger.warn("addVisitor - can't find list with id " + userListID);
    }
  }

  public boolean listExists(long id) {  return userListDAO.getWhere(id, false) != null; }

  /**
   * @see mitll.langtest.server.database.ExcelImport#addDefects
   * @param exerciseID
   * @param field
   * @param comment
   */
  public void addDefect(String exerciseID, String field, String comment) {
    if (!annotationDAO.hasAnnotation(exerciseID, field, INCORRECT, comment)) {
      long defectDetector = userDAO.getDefectDetector();
      addAnnotation(exerciseID, field, INCORRECT, comment, defectDetector);
      markState(exerciseID, CommonShell.STATE.DEFECT, defectDetector);
    }
  }

  /**
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation(String, String, String)
   *
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   */
  public void addAnnotation(String exerciseID, String field, String status, String comment, long userID) {
    UserAnnotation annotation = new UserAnnotation(exerciseID, field, status, comment, userID, System.currentTimeMillis());
    logger.debug("addAnnotation " + annotation);
    annotationDAO.add(annotation);
    //markCorrectness(exerciseID, status.equalsIgnoreCase(CORRECT), userID);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addAnnotations
   * @param exercise
   *
   */
  public void addAnnotations(CommonExercise exercise) {
    if (exercise != null) {
      Map<String, ExerciseAnnotation> latestByExerciseID = annotationDAO.getLatestByExerciseID(exercise.getID());
      //if (!latestByExerciseID.isEmpty()) {
      //  logger.debug("addAnnotations : found " + latestByExerciseID + " for " + exercise.getID());
      //}
      for (Map.Entry<String, ExerciseAnnotation> pair : latestByExerciseID.entrySet()) {
        exercise.addAnnotation(pair.getKey(), pair.getValue().status, pair.getValue().comment);
      }
    }
    else {
      logger.warn("addAnnotations : on an empty exercise?");
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#markReviewed
   * @see #addAnnotation(String, String, String, String, long)
   * @see mitll.langtest.client.custom.QCNPFExercise#markReviewed
   * @param id
   * @paramx userID
   * @paramx creatorID
   */
  public void markState(String id, CommonShell.STATE state, long creatorID) {
    logger.debug("mark state " + id + " = " + state + " by " +creatorID);
    CommonExercise predefExercise = userExerciseDAO.getPredefExercise(id);

    if (predefExercise == null) {
      predefExercise = userExerciseDAO.getWhere(id);
    }
    if (predefExercise != null) {
      if (state.equals(CommonShell.STATE.ATTN_LL)) {
        setSecondState(predefExercise, state, creatorID);
      }
      else {
        setState(predefExercise, state, creatorID);
      }
      if (state.equals(CommonShell.STATE.FIXED)) {
        markAllFieldsFixed(predefExercise, creatorID);
      }
    } else {
      logger.error("huh? couldn't find exercise " + id);
    }
  }

  public void setState(CommonShell shell, CommonShell.STATE state, long creatorID) {
    shell.setState(state);
    reviewedDAO.setState(shell.getID(), state, creatorID);
  }

  public void setSecondState(CommonShell shell, CommonShell.STATE state, long creatorID) {
    shell.setSecondState(state);
    secondStateDAO.setState(shell.getID(), state, creatorID);
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#deleteItem(String)
   * @param exerciseid
   */
  public void removeReviewed(String exerciseid) {
    reviewedDAO.remove(exerciseid);
  }

  protected void markAllFieldsFixed(CommonExercise userExercise, long userID) {
    Collection<String> fields = userExercise.getFields();
    logger.debug("setExerciseState " + userExercise  + "  has " + fields + " user " + userID);
    addAnnotations(userExercise);
    for (String field : fields) {
      ExerciseAnnotation annotation1 = userExercise.getAnnotation(field);
      if (!annotation1.isCorrect()) {
        logger.debug("\tsetExerciseState " + userExercise.getID()  + "  has " + annotation1);

        addAnnotation(userExercise.getID(), field, CORRECT, FIXED, userID);
      }
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#markReviewed(String, boolean, long)
   * @see mitll.langtest.client.custom.QCNPFExercise#markReviewed
   * @param id
   * @param correct
   * @param userID
   */
  public void markCorrectness(String id, boolean correct, long userID) {
    markState(id, correct ? CommonShell.STATE.APPROVED : CommonShell.STATE.DEFECT, userID);
  }

  public boolean deleteList(long id) {
    logger.debug("deleteList " + id);
    userListExerciseJoinDAO.removeListRefs(id);
    boolean b = listExists(id);
    if (!b) logger.warn("\thuh? no list " + id);
    return b && userListDAO.remove(id);
  }

  public boolean deleteItemFromList(long listid, String exid) {
    logger.debug("deleteItemFromList " + listid + " " + exid);

    UserList userListByID = getUserListByID(listid);
    if (userListByID == null) return false;
    /*boolean remove =*/ userListByID.remove(exid);
    boolean remove = userListExerciseJoinDAO.remove(listid, exid);

    return remove;
  }
}
