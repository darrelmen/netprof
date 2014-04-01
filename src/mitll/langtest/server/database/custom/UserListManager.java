package mitll.langtest.server.database.custom;

import audio.tools.FileCopier;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.Exercise;
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
import java.util.TreeSet;

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
  public static final String ITEMS_TO_REVIEW = "Possible defects to fix";
  public static final long REVIEW_MAGIC_ID = -100;
  public static final long COMMENT_MAGIC_ID = -200;

  private final UserDAO userDAO;
  private final ReviewedDAO reviewedDAO;
  private int i = 0;

  private UserExerciseDAO userExerciseDAO;
  private final UserListDAO userListDAO;
  private final UserListExerciseJoinDAO userListExerciseJoinDAO;
  private final AnnotationDAO annotationDAO;

//  private Set<String> reviewedExercises = new TreeSet<String>();
//  private Set<String> incorrect = new TreeSet<String>();
//  private Set<String> fixed = new TreeSet<String>();

 // private Set<String> defects = new TreeSet<String>();
 // private Set<String> comments = new TreeSet<String>();

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
                         AnnotationDAO annotationDAO, ReviewedDAO reviewedDAO, PathHelper pathHelper) {
    this.userDAO = userDAO;
    this.userListDAO = userListDAO;
    this.userListExerciseJoinDAO = userListExerciseJoinDAO;
    this.annotationDAO = annotationDAO;
    this.reviewedDAO = reviewedDAO;

//    incorrect = annotationDAO.getDefectIds();
//    fixed = annotationDAO.getFixedIds();
   // logger.debug("incorrect are " + incorrect.size());
//    reviewedExercises = reviewedDAO.getReviewed();

    // set predef exercises state
    //setStateOnExercises(annotationDAO, reviewedDAO);

    this.pathHelper = pathHelper;
  }

  public void setStateOnExercises(AnnotationDAO annotationDAO, ReviewedDAO reviewedDAO) {
    Map<String, String> stateMap = reviewedDAO.getStateMap();
    logger.debug("got " + stateMap.size() +" in state map");
    Set<String> defectIds = annotationDAO.getDefectIds();
    logger.debug("got " + defectIds.size() +" in defectIds");

    for (String exid : defectIds) {   // incorrect - could be defect or comment for now
      stateMap.put(exid, "defect");
    }

    Set<String> userExercisesRemaining = new HashSet<String>(stateMap.keySet());
    for (Map.Entry<String, String> pair : stateMap.entrySet()) {
      CommonExercise predefExercise = userExerciseDAO.getPredefExercise(pair.getKey());
      if (predefExercise != null) {
        userExercisesRemaining.remove(pair.getKey());
        predefExercise.setState(pair.getValue());
      }
    }
    logger.debug("got " + userExercisesRemaining.size() +" in userExercisesRemaining");

    // fix missing states on user exercises -- only should be done when updating old db
    List<CommonUserExercise> userExercises = userExerciseDAO.getWhere(userExercisesRemaining);

    for (CommonShell commonUserExercise : userExercises) {
      String state = stateMap.get(commonUserExercise.getID());
      if (state == null) {
        logger.error("huh? can't find ex id " + commonUserExercise.getID());
      }
      else {
        if (commonUserExercise.getState().equals("unset")) {
        //  commonUserExercise.setState(state);
       //   userExerciseDAO.updateState(commonUserExercise.getID(), state);
          setState(commonUserExercise,state,false);
        }
      }
    }
  }
/*
  private void setState(CommonShell shell, String state, boolean isPredef) {
    shell.setState(state);
    if (!isPredef) {
      userExerciseDAO.updateState(shell.getID(), state);
    }
    reviewedDAO.setState(shell.getID(), state);
  }*/

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

/*  public Set<String> getFixedIds() {
    return fixed;
  }*/

  public UserList getCommentedList() {
   // Set<String> allKnown = userExerciseDAO.getAtState("incorrect");


    Map<String, String> stateMap = reviewedDAO.getStateMap();
    Set<String> incorrectCommentIds = annotationDAO.getDefectIds();
    logger.debug("There are " + stateMap.size() + " reviewed items ");
    logger.debug("There are " + incorrectCommentIds.size() + " incorrectCommentIds items ");
   // Set<String>  commentedNotDefect = new HashSet<String>(incorrectCommentIds);
    incorrectCommentIds.removeAll(stateMap.keySet());// what's left are items that are not reviewed
    logger.debug("After there are " + incorrectCommentIds.size() + " incorrectCommentIds items ");


    //Set<String> allIncorrect = new HashSet<String>(incorrect);
    //allIncorrect.removeAll(reviewedExercises);

    //List<CommonUserExercise> include = new ArrayList<CommonUserExercise>();
    List<CommonUserExercise> include = userExerciseDAO.getWhere(incorrectCommentIds);
    //for (CommonUserExercise ue : allKnown) {
    //  if (!reviewedExercises.contains(ue.getID())) include.add(ue);
    //}

    UserList reviewList = getReviewList(include, COMMENTS, ALL_ITEMS_WITH_COMMENTS, incorrectCommentIds);
    reviewList.setUniqueID(COMMENT_MAGIC_ID);
    return reviewList;
  }

  /**
   * TODO : probably a bad idea to do a massive where in ... ids.
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#getReviewList()
   * @see mitll.langtest.client.custom.Navigation#viewReview
   * @see #markCorrectness(String, boolean)
   * @return
   */
  public UserList getDefectList() {
 //   logger.debug("getDefectList ids #=" + reviewedExercises.size());
  //  Set<String> incorrectReviewed = new HashSet<String>(reviewedExercises);
  //  boolean b = incorrectReviewed.retainAll(incorrect);
    Set<String> defectIds = annotationDAO.getDefectIds();

    List<CommonUserExercise> allKnown = userExerciseDAO.getWhere(defectIds);
    logger.debug("\tgetDefectList ids #=" + allKnown.size());

    return getReviewList(allKnown, REVIEW, ITEMS_TO_REVIEW, defectIds);
  }

  private UserList getReviewList(List<CommonUserExercise> allKnown, String name, String description, Collection<String> ids) {
    Map<String, CommonUserExercise> idToUser = new HashMap<String, CommonUserExercise>();
    for (CommonUserExercise ue : allKnown) idToUser.put(ue.getID(), ue);

    List<CommonUserExercise> onList = getReviewedUserExercises(idToUser, ids);

    logger.debug("getDefectList ids size = " + allKnown.size() + " yielded " + onList.size());
    User user = new User(-1, 89, 0, 0, "", "", false);
    UserList userList = new UserList(REVIEW_MAGIC_ID, user, name, description, "", false);
    userList.setReview(true);
    userList.setExercises(onList);
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
   /// boolean onReviewedList = reviewedExercises.contains(userExercise.getID());
    String id = userExercise.getID();
    String newid;
    if (id.contains("dup")) {
       newid = id.split("dup")[0] + "_dup_" +System.currentTimeMillis();
    }
    else {
       newid = id + "_dup_" +System.currentTimeMillis();
    }

    logger.warn("duplicating " + userExercise + " with id " + newid);
    userExercise.setID(newid);
    userExerciseDAO.add(userExercise, true);
    String assignedID = userExercise.getID();

    // add to review list if it was on the review list - don't think we have to do this...?
    //if (onReviewedList) {
    //  markApproved(assignedID, userExercise.getCreator());
    //}

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
    String refAudio = getRefAudioPath(userExercise, fileRef, fast, overwrite);
    userExercise.setRefAudio(refAudio);
    //logger.debug("fixAudioPaths : for " + userExercise.getID() + " fast is " + fast + " size " + FileUtils.size(refAudio));

    if (userExercise.getSlowAudioRef() != null && !userExercise.getSlowAudioRef().isEmpty()) {
      fileRef = pathHelper.getAbsoluteFile(userExercise.getSlowAudioRef());
      String slow = SLOW + "_"+ now+"_by_" + userExercise.getCreator()+ ".wav";

      refAudio = getRefAudioPath(userExercise, fileRef, slow, overwrite);
      logger.debug("fixAudioPaths : for " + userExercise.getID()+ " slow is " + refAudio + " size " + FileUtils.size(refAudio));

      userExercise.setSlowRefAudio(refAudio);
    }
  }

  /**
   * Copying audio from initial recording location to new location.
   *
   * Also normalizes the audio level.
   *
   * @param userExercise
   * @param fileRef
   * @param fast
   * @param overwrite
   * @return
   */
  private String getRefAudioPath(UserExercise userExercise, File fileRef, String fast, boolean overwrite) {
    final File bestDir = pathHelper.getAbsoluteFile("bestAudio");
    if (!bestDir.exists() && !bestDir.mkdir()) {
      if (!bestDir.exists()) logger.warn("huh? couldn't make " + bestDir.getAbsolutePath());
    }
    File bestDirForExercise = new File(bestDir, userExercise.getID());
    if (!bestDirForExercise.exists() && !bestDirForExercise.mkdir()) {
      if (!bestDirForExercise.exists()) logger.warn("huh? couldn't make " + bestDirForExercise.getAbsolutePath());
    }
    File destination = new File(bestDirForExercise, fast);
    //logger.debug("getRefAudioPath : copying from " + fileRef +  " to " + destination.getAbsolutePath());
    String s = "bestAudio" + File.separator + userExercise.getID() + File.separator + fast;
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
  }

  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
    userListDAO.setUserExerciseDAO(userExerciseDAO);


  //  setStateOnExercises();

  }

  public void setStateOnExercises() {
    setStateOnExercises(annotationDAO, reviewedDAO);
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
      id == REVIEW_MAGIC_ID ?  getDefectList() :
      id == COMMENT_MAGIC_ID ? getCommentedList() : userListDAO.getWithExercises(id);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addVisitor(mitll.langtest.shared.custom.UserList, long
   * @see mitll.langtest.client.custom.Navigation#addVisitor(mitll.langtest.shared.custom.UserList)
   * @param userList
   * @param user
   */
  public void addVisitor(UserList userList, long user) {
    //logger.debug("addVisitor - user " + user + " visits " + userList.getUniqueID());

    UserList where = userListDAO.getWhere(userList.getUniqueID(), true);
    if (where != null) {
      userListDAO.addVisitor(where.getUniqueID(), user);
    }
    else {
      logger.warn("addVisitor - can't find list with id " + userList.getUniqueID());
    }
  }

  public boolean listExists(long id) {  return userListDAO.getWhere(id, false) != null; }

  /**
   * @see mitll.langtest.server.database.ExcelImport#addDefects(java.util.Map, mitll.langtest.shared.CommonExercise)
   * @param exerciseID
   * @param field
   * @param comment
   */
  public void addDefect(String exerciseID, String field, String comment) {
    if (!annotationDAO.hasAnnotation(exerciseID, field, INCORRECT, comment)) {
      long defectDetector = userDAO.getDefectDetector();
      addAnnotation(exerciseID, field, INCORRECT, comment, defectDetector);
      //markApproved(exerciseID, defectDetector);
      markState(exerciseID, "defect");
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
    logger.info("addAnnotation add to db exercise id " + exerciseID + " field " + field + " status " + status + " comment " + comment);
    annotationDAO.add(new UserAnnotation(exerciseID, field, status, comment, userID, System.currentTimeMillis()));

    markCorrectness(exerciseID, status.equalsIgnoreCase(CORRECT));
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
   * @paramx creatorID
   */
/*  public void markApproved(String id, long creatorID) {
    //int before = reviewedExercises.size();
 *//*   if (reviewedExercises.add(id)) {
      reviewedDAO.add(id, creatorID);
    }*//*

    markState(id,"approved");
    //if (before != reviewedExercises.size()){
      //logger.debug("markApproved now " + reviewedExercises.size() + " reviewed exercises");
    //}
  }*/

  public void markState(String id,  String state) {
    logger.debug("mark state " + id + " = " + state);
    CommonExercise predefExercise = userExerciseDAO.getPredefExercise(id);
    //String state = "approved";
    if (predefExercise != null) {
      //predefExercise.setState("approved");
      setState(predefExercise, state,true);
      if (state.equals("fixed")) {
        markAllFieldsFixed(predefExercise);
      }
    }
    else {
      CommonUserExercise where = userExerciseDAO.getWhere(id);
      if (where != null) {
        setState(where, state,false);
      }
      if (state.equals("fixed")) {
        markAllFieldsFixed(where);
      }
    }

  }

  private void setState(CommonShell shell, String state, boolean isPredef) {
    shell.setState(state);
    if (!isPredef) {
      userExerciseDAO.updateState(shell.getID(), state);
    }
    reviewedDAO.setState(shell.getID(), state);
  }


  public void removeReviewed(String exerciseid) {
    reviewedDAO.remove(exerciseid);
  }

    /**
     * @seex mitll.langtest.server.LangTestDatabaseImpl#setExerciseState(String)
     * @see mitll.langtest.server.database.DatabaseImpl#deleteItem(String)
     * @see mitll.langtest.client.custom.ReviewEditableExercise#doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
     * @paramx exerciseid
     */
/*  public void setState(String exerciseid, String state) {
    CommonUserExercise userExercise = userExerciseDAO.getWhere(exerciseid);

    if (userExercise == null) {
      CommonExercise predefExercise = userExerciseDAO.getPredefExercise(exerciseid);

      setState(predefExercise,state,true);
    }
    if (!reviewedExercises.remove(exerciseid)) {
      logger.error("huh? couldn't find " + exerciseid + " in set of " + reviewedExercises.size());
      getReviewedExercises();
    }
    incorrect.remove(exerciseid);
    fixed.add(exerciseid);
    reviewedDAO.remove(exerciseid);

    if (toRemove.isEmpty()) {
      logger.error("setExerciseState couldn't find " + exerciseid);
      if (incorrect.contains(exerciseid)) {
        incorrect.remove(exerciseid);
        logger.debug("now " + incorrect.size() + " defect items.");
      }
      else {
        logger.error("huh? couldn't find " + exerciseid + " in set of " + incorrect.size() + " incorrect");
      }
    }
    else {
    //  CommonUserExercise userExercise = toRemove.get(0);
      markAllFieldsFixed(userExercise);
    }
  }*/

  protected void markAllFieldsFixed(CommonUserExercise userExercise) {
    Collection<String> fields = userExercise.getFields();
    logger.debug("setExerciseState " + userExercise  + "  has " + fields);
    addAnnotations(userExercise);
    for (String field : fields) {
      ExerciseAnnotation annotation1 = userExercise.getAnnotation(field);
      if (!annotation1.isCorrect()) {
        logger.debug("\tsetExerciseState " + userExercise.getID()  + "  has " + annotation1);

        addAnnotation(userExercise.getID(), field, CORRECT, FIXED, userExercise.getCreator());
      }
    }
  }

  protected void markAllFieldsFixed(CommonExercise userExercise) {
    Collection<String> fields = userExercise.getFields();
    logger.debug("setExerciseState " + userExercise  + "  has " + fields);
    addAnnotations(userExercise);
    for (String field : fields) {
      ExerciseAnnotation annotation1 = userExercise.getAnnotation(field);
      if (!annotation1.isCorrect()) {
        logger.debug("\tsetExerciseState " + userExercise.getID()  + "  has " + annotation1);

        addAnnotation(userExercise.getID(), field, CORRECT, FIXED, -1);
      }
    }
  }

  /**
   * @seex mitll.langtest.server.LangTestDatabaseImpl#getCompletedExercises(int, boolean)
   * @return
   */
/*
  public Set<String> getReviewedExercises() {
    logger.debug("getReviewedExercises now " + reviewedExercises.size() + " reviewed exercises");

    return reviewedExercises;
  }
*/

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#markReviewed(String, boolean, long)
   * @see mitll.langtest.client.custom.QCNPFExercise#markReviewed
   * @param id
   * @param correct
   */
  public void markCorrectness(String id, boolean correct) {
    markState(id, correct ? "approved" : "defect");
    //markApproved();
/*    if (correct) {
      incorrect.remove(id);
    }
    else {
      incorrect.add(id);
    }*/
/*    CommonExercise exercise = userExerciseDAO.getPredefExercise(id);
    if (exercise == null) logger.error("couldn't find id " + id);
    else {
      setState(correct ? "fixed" : "defect");
    }*/
    //logger.debug("markCorrectness incorrect now " + incorrect.size());
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
