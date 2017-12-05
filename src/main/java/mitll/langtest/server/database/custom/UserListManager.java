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
import mitll.langtest.server.database.project.ProjectServices;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userlist.*;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.user.MiniUser;
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
//  private static final String REVIEW = "Defects";
//  private static final String ATTENTION = "AttentionLL";
//  private static final String ITEMS_TO_REVIEW = "Possible defects to fix";

  private static final boolean DEBUG = false;

  private final IUserDAO userDAO;
  // private final IReviewedDAO reviewedDAO, secondStateDAO;
  private int i = 0;

  private IUserExerciseDAO userExerciseDAO;
  private final IUserListDAO userListDAO;
  private final IUserExerciseListVisitorDAO visitorDAO;
  private final IUserListExerciseJoinDAO userListExerciseJoinDAO;
  private final IAnnotationDAO annotationDAO;
  private final PathHelper pathHelper;
  private IStateManager stateManager;

  /**
   * @param userDAO
   * @param userListDAO
   * @param userListExerciseJoinDAO
   * @param annotationDAO
   * @param pathHelper
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public UserListManager(IUserDAO userDAO,
                         IUserListDAO userListDAO,
                         IUserListExerciseJoinDAO userListExerciseJoinDAO,
                         IAnnotationDAO annotationDAO,
                         IStateManager stateManager,
                         IUserExerciseListVisitorDAO visitorDAO,
                         PathHelper pathHelper) {
    this.userDAO = userDAO;
    this.userListDAO = userListDAO;
    this.userListExerciseJoinDAO = userListExerciseJoinDAO;
    this.annotationDAO = annotationDAO;
    this.pathHelper = pathHelper;
    this.visitorDAO = visitorDAO;
    this.stateManager = stateManager;
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
  public UserList addUserList(int userid, String name, String description, String dliClass, boolean isPublic, int projid) {
    UserList userList = createUserList(userid, name, description, dliClass, !isPublic, projid);
    if (userList == null) {
      logger.warn("addUserList no user list??? for " + userid + " " + name);
      return null;
    } else {
      return userList;
    }
  }

  @Override
  public int getNumLists(int userid, int projid) {
    return userListDAO.getNumMineAndPublic(userid, projid);
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
      logger.error("createUserList huh? no user with id " + userid);
      return null;
    } else {
      //logger.info("found\n\t" + userListDAO.getAllByUser(userid, projid));

      List<UserList<CommonShell>> byName = userListDAO.getByName(userid, name, projid);
      if (!byName.isEmpty()) {
        UserList<CommonShell> commonShellUserList = byName.get(0);
        if (commonShellUserList.isDeleted()) {
          userListDAO.bringBack(commonShellUserList.getID());
          return commonShellUserList;
        } else {
          return null;
        }
      }
      //if (hasByName(userid, name, projid)) {
      //  return null;
      //}
      else {
        long now = System.currentTimeMillis();
        UserList e = new UserList(i++, userid, userWhere.getUserID(), name, description, dliClass, isPrivate,
            now, "", "", projid, false);
        userListDAO.add(e, projid);
        logger.debug("createUserList : now there are " + userListDAO.getCount() + " lists total, for " + userid);
        return e;
      }
    }
  }

/*
  private boolean hasByName(int userid, String name, int projid) {
    return userListDAO.hasByName(userid, name, projid);
  }
*/

/*  @Override
  public Collection<UserList<CommonShell>> getMyLists(int userid, int projid) {
    return getListsForUser(userid, projid, true, false);
  }*/

  /**
   * TODO : expensive -- could just be a query against your own lists and/or against visited lists...
   *
   * @param userid
   * @param projid
   * @param listsICreated
   * @param visitedLists
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#getListsForUser
   * @see #getMyLists
   */
  @Override
  public Collection<UserList<CommonShell>> getListsForUser(int userid,
                                                           int projid,
                                                           boolean listsICreated,
                                                           boolean visitedLists) {
    if (userid == -1) {
      return Collections.emptyList();
    }
    if (DEBUG) {
      logger.debug("getListsForUser for user #" + userid + " only created " + listsICreated + " visited " + visitedLists);
    }

    List<UserList<CommonShell>> listsForUser = new ArrayList<>();
    UserList<CommonShell> favorite = null;
    Set<Integer> ids = new HashSet<>();

    if (listsICreated) {
      listsForUser = userListDAO.getAllByUser(userid, projid);
//      logger.info("found " + listsForUser.size() + " created by " + userid);
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
      //Collection<UserList<CommonShell>> listsForUser1 = userListDAO.getListsForUser(userid, projid, 0, 10);
      //   Collection<UserList<CommonShell>> listsForUser1 = userListDAO.getAllPublicNotMine(userid, projid);
      Collection<UserList<CommonShell>> listsForUser1 = userListDAO.getVisitedLists(userid, projid);
      //    logger.info("found " + listsForUser1.size() + " visited by " + userid);

      for (UserList<CommonShell> userList : listsForUser1) {
        if (!ids.contains(userList.getID())) {
          listsForUser.add(userList);
        }
      }
    }

    // put favorite at front
    {
      if (listsForUser.isEmpty()) {
        if (DEBUG) {
          logger.warn("getListsForUser - list is empty for " + userid + " only created " +
              listsICreated + " visited " + visitedLists);
        }
      } else if (favorite != null) {
        listsForUser.remove(favorite);
        listsForUser.add(0, favorite);// put at front
      }
    }

/*    if (DEBUG) {
      logger.debug("getListsForUser found " + listsForUser.size() +
          " lists for user #" + userid + " only created " + listsICreated + " visited " + visitedLists +
          "\n\tfavorite " + favorite);
      if (listsForUser.size() < 4) {
        for (UserList ul : listsForUser) logger.debug("\t" + ul);
      }
    }*/

    return listsForUser;
  }

  /**
   * @param userid
   * @param projid
   * @return
   * @see ProjectServices#rememberUsersCurrentProject(int, int)
   */
  @Override
  public void createFavorites(int userid, int projid) {
    createUserList(userid, UserList.MY_LIST, MY_FAVORITES, "", true, projid);
  }

  /**
   * So comments are items with incorrect annotations on their fields that have not been marked as defects.
   * <p>
   * Also, incorrect annotations are the latest annotations- a field can have a history of correct and incorrect
   * annotations - only if the latest is incorrect should the item appear on the comment or defect list.
   *
   * @param projID
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#getReviewList
   */
  @Override
  public UserList<CommonShell> getCommentedList(int projID) {
    Set<Integer> exercisesWithIncorrectAnnotations = annotationDAO.getExercisesWithIncorrectAnnotations(projID);
    logger.info("getCommented for "+ projID + " found " +exercisesWithIncorrectAnnotations.size());
    List<CommonExercise> defectExercises = getDefectExercises(projID, exercisesWithIncorrectAnnotations);
    logger.info("getCommented for "+ projID + " found " +defectExercises.size() + " exercises");
    UserList<CommonShell> reviewList = getReviewList(defectExercises, COMMENTS, ALL_ITEMS_WITH_COMMENTS, COMMENT_MAGIC_ID);
    logger.info("getCommented for "+ projID + " list has " +reviewList.getNumItems() + " exercises");
    return reviewList;
  }

  @NotNull
  private List<CommonExercise> getDefectExercises(int projID, Collection<Integer> incorrectAnnotations) {
    List<CommonExercise> defectExercises = new ArrayList<>();
    incorrectAnnotations.forEach(id ->{
      CommonExercise byExID = userExerciseDAO.getByExID(id);
      if (byExID == null) logger.warn("can't find exercise " + id + " in project " +projID);
      else defectExercises.add(byExID);
    });
    return defectExercises;
  }

  /**
   * @param projID
   * @return
   * @see IUserListManager#getUserListByIDExercises
   */
  @Override
  public UserList<CommonExercise> getCommentedListEx(int projID) {
    List<CommonExercise> defectExercises = getDefectExercises(projID, annotationDAO.getExercisesWithIncorrectAnnotations(projID));
    return getReviewListEx(defectExercises, COMMENTS, ALL_ITEMS_WITH_COMMENTS, COMMENT_MAGIC_ID);
  }

  /**
   * @param allKnown
   * @param name
   * @param description
   * @param userListID
   * @return
   * @see IUserListManager#getCommentedList(int)
   */
  private UserList<CommonShell> getReviewList(Collection<CommonExercise> allKnown,
                                              String name,
                                              String description,
                                              int userListID) {
    return getCommonUserList(getQCList(name, description, userListID), getShells(allKnown));
  }

  @NotNull
  private UserList<CommonShell> getQCList(String name, String description, int userListID) {
    User qcUser = getQCUser();
    return new UserList<>(userListID, qcUser.getID(), qcUser.getUserID(), name, description, "",
        false, System.currentTimeMillis(), "", "", -1, false);
  }

  @NotNull
  private List<CommonShell> getShells(Collection<CommonExercise> allKnown) {
    List<CommonShell> commonShells = new ArrayList<>(allKnown.size());
    allKnown.forEach(commonExercise -> commonShells.add(commonExercise.getShell()));
    return commonShells;
  }

  private UserList<CommonExercise> getReviewListEx(List<CommonExercise> allKnown,
                                                   String name, String description,
                                                   int userListID) {
    User qcUser = getQCUser();
    UserList<CommonExercise> userList = new UserList<>(userListID, qcUser.getID(), qcUser.getUserID(), name, description, "",
        false, System.currentTimeMillis(), "", "", -1, false);
    return getCommonUserList(userList, allKnown);
  }

  /**
   * TODO : should we worry about sort order for english?
   *
   * @param <T>
   * @param userList
   * @param copy
   * @return
   * @see #getReviewList(Collection, String, String, int)
   * @see #getReviewListEx(List, String, String, int)
   */
  @NotNull
  private <T extends CommonShell> UserList<T> getCommonUserList(UserList<T> userList, List<T> copy) {
    userList.setReview(true);
    new ExerciseSorter().getSorted(copy, false, false, "");
    userList.setExercises(copy);
    stateManager.markState(copy);
    logger.debug("getCommonUserList returning " + userList + (userList.getExercises().isEmpty() ? "" : " first " + userList.getExercises().iterator().next()));
    return userList;
  }
 /* private List<CommonExercise> getReviewedExercises(Collection<CommonExercise> allKnown, Collection<Integer> ids) {
    Map<Integer, CommonExercise> idToEx = new HashMap<>();
    for (CommonExercise ue : allKnown) idToEx.put(ue.getID(), ue);
    return getReviewedUserExercises(idToEx, ids);
  }*/

  /**
   * Need a bogus user for the list.
   *
   * @return
   */
  private User getQCUser() {
    List<User.Permission> permissions = new ArrayList<User.Permission>();
    permissions.add(User.Permission.QUALITY_CONTROL);
    return new User(-1, 89, 0, MiniUser.Gender.Unspecified, 0, "", "", false, permissions);
  }

  /**
   * Wrap predef exercises with user exercises -- why?
   *
   * @param idToUserExercise
   * @param ids
   * @return
   * @see #getReviewedExercises
   */
  private List<CommonExercise> getReviewedUserExercises(Map<Integer, CommonExercise> idToUserExercise, Collection<Integer> ids) {
    List<CommonExercise> onList = new ArrayList<>();

    logger.info("getReviewed checking " + ids.size() + " against " + idToUserExercise.size());
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
          logger.warn("\n\ngetReviewedUserExercises : huh? can't find predef exercise " + id);
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
  public void newExercise(int userListID, CommonExercise userExercise, String mediaDir) {
    newExerciseOnList(getUserListNoExercises(userListID), userExercise, mediaDir);
  }

  public UserList getUserListNoExercises(int userListID) {
    logger.info("getUserListNoExercises for " + userListID);
    return userListDAO.getWhere(userListID, true);
  }

  public void newExerciseOnList(UserList userList, CommonExercise userExercise, String mediaDir) {
    int projectID = userExercise.getProjectID();
    int newExerciseID = userExerciseDAO.add(userExercise, false, false,
        getTypeOrder(projectID));
    logger.debug("newExercise added exercise " + newExerciseID + " from " + userExercise);

    int contextID = 0;
    try {
      contextID = makeContextExercise(userExercise, newExerciseID, projectID);
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

  private int makeContextExercise(CommonExercise userExercise, int newExerciseID, int projectID) {
    Exercise userExercise1 = new Exercise(-1, userExercise.getCreator(), "", projectID, false);
    int contextID = userExerciseDAO.add(userExercise1, false, true, getTypeOrder(projectID));
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
  public void addItemToList(int userListID, @Deprecated String exerciseID, int exid) {
    UserList where = getUserListNoExercises(userListID);

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
   * TODOx : why all this foolishness with the id?
   * TODOx : put this back?
   *
   * @param userExercise
   * @return
   * @seex mitll.langtest.server.LangTestDatabaseImpl#duplicateExercise
   */
/*  @Override
  public CommonExercise duplicate(CommonExercise userExercise) {
    logger.error("should call domino instead");
    return userExercise;
*//*    String newid = getDupID(userExercise);

    logger.debug("duplicating " + userExercise + " with id " + newid);
    userExercise.getCombinedMutableUserExercise().setOldID(newid);
    userExerciseDAO.add(userExercise, true);
    String assignedID = userExercise.getOldID();

    // copy the annotations
    for (Map.Entry<String, ExerciseAnnotation> pair : userExercise.getFieldToAnnotation().entrySet()) {
      ExerciseAnnotation value = pair.getValue();
      addAnnotation(assignedID, pair.getKey(), value.getStatus(), value.getComment(), userExercise.getCombinedMutableUserExercise().getCreator());
    }

    return userExercise;*//*
  }*/

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
   * @seex IUserListManager#newExercise
   * @see UserListManager#editItem
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
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public void setUserExerciseDAO(IUserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
    userListDAO.setUserExerciseDAO(userExerciseDAO);
  }

  /**
   * consider how to ask for just annotations for a project, instead of getting all of them
   * and then filtering for just those on the requested project
   *
   * @param id
   * @return
   * @see
   * @see IUserListManager#deleteItemFromList(int, int)
   */
  @Override
  public UserList<CommonShell> getUserListByID(int id) {
    if (id == -1) {
      logger.error("getUserListByID : huh? asking for id " + id);
      return null;
    }
    return userListDAO.getWithExercises(id);
  }

  @Override
  public UserList<CommonShell> getSimpleUserListByID(int id) {
    if (id == -1) {
      logger.error("getUserListByID : huh? asking for id " + id);
      return null;
    } else {
      return userListDAO.getWithExercises(id);
    }
  }

  /**
   * the review, comment, and attention LL (if needed) lists need to be in context of project?
   *
   * @param id
   * @param projid    IGNORED HERE
   * @param typeOrder
   * @param ids
   * @return
   * @see UserListServices#getUserListByIDExercises
   */
/*  @Override
  public UserList<CommonExercise> getUserListByIDExercises(int id,
                                                           int projid,
                                                           Collection<String> typeOrder,
                                                           Set<Integer> ids) {
    if (id == -1) {
      logger.error("getUserListByID : huh? asking for id " + id);
      return null;
    }
    return
        id == COMMENT_MAGIC_ID ? getCommentedListEx(typeOrder, projid) :
            userListDAO.getWithExercisesEx(id);

  }*/

  /**
   * @param userListID
   * @param user
   * @seex mitll.langtest.client.custom.Navigation#addVisitor
   * @seex mitll.langtest.server.LangTestDatabaseImpl#addVisitor
   */
  @Override
  public UserList addVisitor(int userListID, int user) {
    logger.debug("addVisitor - user " + user + " visits " + userListID);
    UserList where = getUserListNoExercises(userListID);
    if (where != null) {
      userListDAO.addVisitor(where.getID(), user);
      return where;
    } else if (userListID > 0) {
      logger.warn("addVisitor - can't find list with id " + userListID);
      return null;
    } else {
      return null;
    }
  }

  public void removeVisitor(int userListID, int user) {
    userListDAO.removeVisitor(userListID, user);
  }

  private boolean listExists(int id) {
    return userListDAO.getWhere(id, false) != null;
  }

  /**
   * @param exercise
   * @param field
   * @param comment
   * @see mitll.langtest.server.database.exercise.BaseExerciseDAO#addDefects
   */
  @Override
  public boolean addDefect(CommonExercise exercise, String field, String comment) {
    int id = exercise.getID();
    if (!annotationDAO.hasDefect(id, field, INCORRECT, comment)) {
      addAnnotation(id, field, INCORRECT, comment, userDAO.getDefectDetector());
      markState(exercise, STATE.DEFECT, userDAO.getDefectDetector());
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
   * TODO : really should do this in batch!
   *
   * @param exercise
   * @see mitll.langtest.server.services.ExerciseServiceImpl#addAnnotations
   * @see #markAllFieldsFixed
   */
  @Override
  public void addAnnotations(CommonExercise exercise) {
    if (exercise != null) {
      MutableAnnotationExercise mutableAnnotation = exercise.getMutableAnnotation();
      Map<String, ExerciseAnnotation> latestByExerciseID = annotationDAO.getLatestByExerciseID(exercise.getID());

//      logger.info("addAnnotations to ex " +exercise.getID() + " got " + latestByExerciseID.size()+  " annos to fields " + latestByExerciseID.keySet());

      for (Map.Entry<String, ExerciseAnnotation> pair : latestByExerciseID.entrySet()) {
        mutableAnnotation.addAnnotation(pair.getKey(), pair.getValue().getStatus(), pair.getValue().getComment());
      }
    } else {
      logger.warn("addAnnotations : on an empty exercise?");
    }
  }

  /**
   * @param exercise
   * @param correct
   * @param userid
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  @Override
  public void markCorrectness(CommonExercise exercise, boolean correct, int userid) {
    markState(exercise, correct ? STATE.APPROVED : STATE.DEFECT, userid);
  }

  /**
   * @param exercise
   * @param state
   * @param creatorID
   * @see #addAnnotation
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed
   */
  @Override
  public void markState(CommonExercise exercise, STATE state, int creatorID) {
    logger.info("markState mark state " + exercise + " = " + state + " by " + creatorID);

    stateManager.setState(exercise, state, creatorID);

    if (state.equals(STATE.FIXED)) {
      markAllFieldsFixed(exercise, creatorID);
    }
  }

  /**
   * @param userExercise
   * @param userid
   * @see IUserListManager#markState
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
   * @seez ListManager#deleteList
   * @see mitll.langtest.server.services.ListServiceImpl#deleteList
   */
  @Override
  public boolean deleteList(int id) {
    logger.debug("deleteList " + id);
    userListExerciseJoinDAO.removeListRefs(id);
    boolean b = listExists(id);
    if (!b) logger.warn("\tdeleteList huh? no list " + id);
    return b && userListDAO.remove(id);
  }

  /**
   * @param listid
   * @param exid
   * @return
   * @see mitll.langtest.server.services.ListServiceImpl#deleteItemFromList
   */
  @Override
  public boolean deleteItemFromList(int listid, int exid) {
    logger.debug("deleteItemFromList " + listid + " ex = " + exid);

    UserList<?> userListByID = getUserListByID(listid);
    if (userListByID == null) {
      logger.warn("deleteItemFromList huh? no user list with id " + listid);
      return false;
    }

    userListByID.remove(exid);
    return userListExerciseJoinDAO.remove(listid, exid);
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
  public void createTables(DBConnection dbConnection, List<String> created) {
    List<IDAO> idaos = Arrays.asList(
        userListDAO,
        userListExerciseJoinDAO,
        stateManager.getReviewedDAO(),
        stateManager.getSecondStateDAO()
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

  @Override
  public void update(UserList userList) {
    userListDAO.update(userList);
  }

/*  public Collection<Integer> getDefectExercises() {
    return reviewedDAO.getDefectExercises();
  }

  public Collection<Integer> getInspectedExercises() {
    return reviewedDAO.getInspectedExercises();
  }*/
}
