package mitll.langtest.server.database.custom;

import audio.tools.FileCopier;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.AudioExercise;
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

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListManager {
  private static Logger logger = Logger.getLogger(UserListManager.class);
  private static final String FAST = "Fast";
  private static final String SLOW = "Slow";
  private static final String MY_FAVORITES = "My Favorites";

  private final UserDAO userDAO;
  private final ReviewedDAO reviewedDAO;
  private int i = 0;

  private UserExerciseDAO userExerciseDAO;
  private int userExerciseCount = 0;
  private UserListDAO userListDAO;
  private UserListExerciseJoinDAO userListExerciseJoinDAO;
  private AnnotationDAO annotationDAO;

  private Set<String> reviewedExercises = new TreeSet<String>();
  private Set<String> incorrect = new TreeSet<String>();
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

    incorrect = annotationDAO.getIncorrectIds();
    reviewedExercises = reviewedDAO.getReviewed();
    this.pathHelper = pathHelper ;

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
   * @see mitll.langtest.server.LangTestDatabaseImpl#getListsForUser
   * @param userid
   * @param onlyCreated
   * @return
   */
  public Collection<UserList> getListsForUser(long userid, boolean onlyCreated) {
    if (userid == -1) return Collections.emptyList();
    logger.debug("getListsForUser for user #" + userid);

    List<UserList> listsForUser = new ArrayList<UserList>();
    UserList favorite = null;
    for (UserList userList : userListDAO.getAll(userid)) {
      boolean isCreator = userList.getCreator().id == userid;
      if (isCreator || !isFavorite(userList)) {
        if (onlyCreated) {
          logger.debug("\tgetListsForUser  list " + userList);
          if (isCreator) {
            if (isFavorite(userList)) favorite = userList;
            listsForUser.add(userList);
          }
        } else {
          logger.debug("\tgetListsForUser  list " + userList);
          if (userList.getVisitorIDs().contains(userid) || isCreator) {
            if (isFavorite(userList)) favorite = userList;
            listsForUser.add(userList);
          }
        }
      }
    }

    if (listsForUser.isEmpty()) {
      logger.error("getListsForUser - list is empty?? " + listsForUser.size() + "(" +listsForUser+ ") for " + userid);
    }
    else if (favorite != null) {
      listsForUser.remove(favorite);
      listsForUser.add(0,favorite);// put at front
    }

    logger.debug("getListsForUser " + listsForUser.size() + "(" +listsForUser+ ") for " + userid);

    return listsForUser;
  }

  public UserList createFavorites(long userid) {
    //logger.debug("createFavorites for " + userid);
    return createUserList(userid, UserList.MY_LIST, MY_FAVORITES, "", true);
  }

  private boolean isFavorite(UserList userList) {
    return userList.getName().equals(UserList.MY_LIST);
  }

  public UserList getCommentedList() {
    List<UserExercise> allKnown = userExerciseDAO.getWhere(incorrect);

    Set<String> allIncorrect = new HashSet<String>(incorrect);
    allIncorrect.removeAll(reviewedExercises);

    List<UserExercise> include = new ArrayList<UserExercise>();
    for (UserExercise ue : allKnown) if (!reviewedExercises.contains(ue.getID())) include.add(ue);

    return getReviewList(include, "Comments", "All items with comments", allIncorrect);
  }

  /**
   * TODO : probably a bad idea to do a massive where in ... ids.
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#getReviewList()
   * @see mitll.langtest.client.custom.Navigation#viewReview
   * @see #markIncorrect(String)
   * @return
   */
  public UserList getReviewList() {
    logger.debug("getReviewList ids #=" + reviewedExercises);
    Set<String> incorrectReviewed = new HashSet<String>(reviewedExercises);
    boolean b = incorrectReviewed.retainAll(incorrect);

    List<UserExercise> allKnown = userExerciseDAO.getWhere(incorrectReviewed);
    logger.debug("\tgetReviewList ids #=" + allKnown.size());

    return getReviewList(allKnown, "Review", "Items to review",incorrectReviewed);
  }

  private UserList getReviewList(List<UserExercise> allKnown, String name, String description, Collection<String> ids) {
    Map<String, UserExercise> idToUser = new HashMap<String, UserExercise>();
    for (UserExercise ue : allKnown) idToUser.put(ue.getID(), ue);

    List<UserExercise> onList = getReviewedUserExercises(idToUser,ids);

    logger.debug("getReviewList ids #=" + allKnown.size() + " yielded " + onList.size());
    User user = new User(-1, 89, 0, 0, "", "", false);
    UserList userList = new UserList(Long.MAX_VALUE, user, name, description, name, false);
    userList.setReview(true);
    userList.setExercises(onList);
    return userList;
  }

  /**
   * @see #getReviewList()
   * @param idToUser
   * @paramx incorrect redundant, but guarantees same order as they are reviewed
   * @return
   */
  private List<UserExercise> getReviewedUserExercises(Map<String, UserExercise> idToUser, Collection<String> ids) {
    List<UserExercise> onList = new ArrayList<UserExercise>();

    for (String id : ids) {
      if (!id.startsWith(UserExercise.CUSTOM_PREFIX)) {
        Exercise byID = userExerciseDAO.getExercise(id);
        logger.debug("found " + byID + " tooltip " + byID.getTooltip());
        if (byID != null) {
          onList.add(new UserExercise(byID)); // all predefined references
        }
      }
      else {
        if (idToUser.containsKey(id)) onList.add(idToUser.get(id));
      }
    }
    Collections.sort(onList, new Comparator<UserExercise>() {
      @Override
      public int compare(UserExercise o1, UserExercise o2) {
        return o1.getID().compareTo(o2.getID());
      }
    });
    return onList;
  }

  /**
   * TODO : do a search over the list fields to find matches
   * @param search
   * @return
   */
  public List<UserList> getUserListsForText(String search) {
    return userListDAO.getAllPublic();
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#createNewItem
   * @see mitll.langtest.client.custom.NewUserExercise.CreateFirstRecordAudioPanel#makePostAudioRecordButton()
   * @param userid
   * @paramx english
   * @paramx foreign
   * @paramx transliteration
   * @return
   */
  public UserExercise createNewItem(long userid) {//}, String english, String foreign, String transliteration) {
    int uniqueID = userExerciseCount++;
    return new UserExercise(uniqueID, UserExercise.CUSTOM_PREFIX+uniqueID, userid, " ", "", "");
  }

  /**
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices
   * @see mitll.langtest.server.LangTestDatabaseImpl#addItemToUserList(long, mitll.langtest.shared.custom.UserExercise)
   * @param userListID
   * @param userExercise
   * @return
   */
  public void addItemToUserList(long userListID, UserExercise userExercise) {
    addItemToList(userListID, userExercise);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#reallyCreateNewItem
   * @see #addItemToUserList(long, mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.client.custom.NewUserExercise#onClick
   * @param userListID
   * @param userExercise
   */
  public void reallyCreateNewItem(long userListID, UserExercise userExercise) {
    userExerciseDAO.add(userExercise, false);

    addItemToList(userListID, userExercise);
    //fixAudioPaths(userExercise, true); // do this after the id has been made

    editItem(userExercise, false);
  }

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
   * @see mitll.langtest.client.custom.NewUserExercise#onClick
   *
   * @param userExercise
   * @param createIfDoesntExist
   */
  public void editItem(UserExercise userExercise, boolean createIfDoesntExist) {
    fixAudioPaths(userExercise, true);
    userExerciseDAO.update(userExercise, createIfDoesntExist);
  }

  /**
   * Remember to copy the audio from the posted location to a more permanent location.
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
    logger.debug("fixAudioPaths : for " + userExercise.getID() + " fast is " + fast + " size " + FileUtils.size(refAudio));

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
    logger.debug("getRefAudioPath : copying from " + fileRef +  " to " + destination.getAbsolutePath());
    String s = "bestAudio" + File.separator + userExercise.getID() + File.separator + fast;
    logger.debug("getRefAudioPath : dest path    " + bestDirForExercise.getPath() + " vs " +s);
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
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getExerciseIds(int, long, String, long)
   * @param id
   * @return
   */
  public UserList getUserListByID(long id) {
    if (id == -1) {
      logger.error("getUserListByID : huh? asking for id " + id);
      return null;
    }
    return id == Long.MAX_VALUE ? getReviewList() : userListDAO.getWithExercises(id);
  }

  public void addVisitor(UserList userList, long user) {
    UserList where = userListDAO.getWhere(userList.getUniqueID(), true);
    if (where != null) {
      userListDAO.addVisitor(where.getUniqueID(), user);
    }
  }

  public boolean listExists(long id) {  return userListDAO.getWhere(id, false) != null; }

  public void addAnnotation(String exerciseID, String field, String status, String comment, long userID) {
    logger.info("addAnnotation write to database! " + exerciseID + " " + field + " " + status + " " + comment);
    annotationDAO.add(new UserAnnotation(exerciseID, field, status, comment, userID,System.currentTimeMillis()));

    if (status.equalsIgnoreCase("incorrect")) {
       markIncorrect(exerciseID);
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#addAnnotations(mitll.langtest.shared.Exercise)
   * @param exercise
   * @param <T>
   */
  public <T extends AudioExercise> void addAnnotations(T exercise) {
    if (exercise != null) {
      Map<String, ExerciseAnnotation> latestByExerciseID = annotationDAO.getLatestByExerciseID(exercise.getID());

      if (!latestByExerciseID.isEmpty()) logger.debug("addAnnotations : found " + latestByExerciseID + " for " + exercise.getID());
      for (Map.Entry<String, ExerciseAnnotation> pair : latestByExerciseID.entrySet()) {
        exercise.addAnnotation(pair.getKey(), pair.getValue().status, pair.getValue().comment);
      }
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#markReviewed
   * @see #addAnnotation(String, String, String, String, long)
   * @see mitll.langtest.client.custom.QCNPFExercise#markReviewed
   * @param id
   * @param creatorID
   */
  public void markReviewed(String id, long creatorID) {
    if (!reviewedExercises.add(id)) {
      reviewedDAO.add(id,creatorID);
    }
    logger.debug("markReviewed now " + reviewedExercises.size() + " reviewed exercises");
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#markReviewed(String, boolean, long)
   * @param exerciseid
   */
  public void removeReviewed(String exerciseid) {
    List<UserExercise> toRemove = userExerciseDAO.getWhere(Collections.singletonList(exerciseid));

    reviewedExercises.remove(exerciseid);
    incorrect.remove(exerciseid);
    reviewedDAO.remove(exerciseid);

    if (toRemove.isEmpty()) {
      logger.error("removeReviewed couldn't find " + exerciseid);
      if (incorrect.contains(exerciseid)) {
        incorrect.remove(exerciseid);
        logger.debug("now " + incorrect.size() + " commented items.");
      }
    }
    else {
      UserExercise newUserExercise = toRemove.get(0);
      Collection<String> fields = newUserExercise.getFields();
      logger.debug("removeReviewed " + newUserExercise  + "  has " + fields);
      addAnnotations(newUserExercise);
      for (String field : fields) {
        ExerciseAnnotation annotation1 = newUserExercise.getAnnotation(field);
        if (!annotation1.isCorrect()) {
          logger.debug("\tremoveReviewed " + newUserExercise.getID()  + "  has " + annotation1);

          addAnnotation(newUserExercise.getID(), field, "correct", "fixed", newUserExercise.getCreator());
        }
      }
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getCompletedExercises(int, boolean)
   * @return
   */
  public Set<String> getReviewedExercises() {
    logger.debug("getReviewedExercises now " + reviewedExercises.size() + " reviewed exercises");

    return reviewedExercises;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#markReviewed(String, boolean, long)
   * @see mitll.langtest.client.custom.QCNPFExercise#markReviewed
   * @param id
   */
  public void markIncorrect(String id) {
    incorrect.add(id);
    logger.debug("markIncorrect incorrect now " + incorrect.size());
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
