package mitll.langtest.server.database.custom;

import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.AudioExercise;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.User;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListManager {
  private static Logger logger = Logger.getLogger(UserListManager.class);

  public static final String MY_LIST = "Favorites";

  private final UserDAO userDAO;
  private int i = 0;

  private UserExerciseDAO userExerciseDAO;
  private int userExerciseCount = 0;
  private UserListDAO userListDAO;
  private UserListExerciseJoinDAO userListExerciseJoinDAO;

  public UserListManager(UserDAO userDAO, UserListDAO userListDAO,UserListExerciseJoinDAO userListExerciseJoinDAO ) {
    this.userDAO = userDAO;
    this.userListDAO = userListDAO;
    this.userListExerciseJoinDAO = userListExerciseJoinDAO;
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

  private UserList createUserList(long userid, String name, String description, String dliClass, boolean isPrivate) {
    User userWhere = userDAO.getUserWhere(userid);
    if (userWhere == null) {
      logger.error("huh? no user with id " + userid);
      return null;
    } else {
      UserList e = new UserList(i++, userWhere, name, description, dliClass, System.currentTimeMillis(), isPrivate);
      userListDAO.add(e);
      logger.debug("createUserList : now there are " + userListDAO.getCount() + " lists for " + userid);
      return e;
    }
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
      UserList userList = createUserList(userid, MY_LIST, "My Favorites", "", true);
      if (userList == null) return Collections.emptyList();
      else return Collections.singletonList(userList);
    }
    else {
      listsForUser.remove(favorite);
      listsForUser.add(0,favorite);// put at front
    }

    logger.debug("getListsForUser " + listsForUser.size() + "(" +listsForUser+ ") for " + userid);

    return listsForUser;
  }

  private boolean isFavorite(UserList userList) {
    return userList.getName().equals(MY_LIST);
  }

  public UserList getReviewList() {
    UserList e = new UserList(i++, new User(-1,89,0,0,"","",false), "Review", "Items to review", "Review", System.currentTimeMillis(), false);

    List<UserExercise> onList = new ArrayList<UserExercise>();
    List<UserExercise> allKnown = userExerciseDAO.getWhere(incorrect);

    Map<String, UserExercise> idToUser = new HashMap<String, UserExercise>();
    for (UserExercise ue : allKnown) idToUser.put(ue.getID(),ue);
    for (String id : incorrect) {
      if (!id.startsWith(UserExercise.CUSTOM_PREFIX)) {
        Exercise byID = userExerciseDAO.getExercise(id);

        if (byID != null) {
          onList.add(new UserExercise(byID)); // all predefined references
        }
      }
      else {
        if (idToUser.containsKey(id)) onList.add(idToUser.get(id));
      }
    }

    logger.debug("getReviewList ids " + incorrect + " yielded " + onList.size() + " : " + onList);

    e.setExercises(onList);
    return e;
  }

  /**
   * TODO : do a search over the list fields to find matches
   * @param search
   * @return
   */
  public List<UserList> getUserListsForText(String search) {
    List<UserList> listsForUser = new ArrayList<UserList>(userListDAO.getAllPublic());
    return listsForUser;
  }

  public UserExercise createNewItem(long userid, String english, String foreign) {
    int uniqueID = userExerciseCount++;
    return new UserExercise(uniqueID, UserExercise.CUSTOM_PREFIX+uniqueID, userid, english, foreign);
  }

  /**
   * @see mitll.langtest.client.custom.NPFExercise#populateListChoices(mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseController, com.github.gwtbootstrap.client.ui.SplitDropdownButton)
   * @see mitll.langtest.server.LangTestDatabaseImpl#addItemToUserList(long, mitll.langtest.shared.custom.UserExercise)
   * @param userListID
   * @param userExercise
   * @return
   */
  public void addItemToUserList(long userListID, UserExercise userExercise) {
    reallyCreateNewItem(userListID, userExercise);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#reallyCreateNewItem
   * @param userListID
   * @param userExercise
   */
  public void reallyCreateNewItem(long userListID, UserExercise userExercise) {
    userExerciseDAO.add(userExercise);

    UserList where = userListDAO.getWhere(userListID);
    if (where != null) {
      userListExerciseJoinDAO.add(where, userExercise);
      userListDAO.updateModified(userListID);
    }
    if (where == null) {
      logger.error("\n\nreallyCreateNewItem : couldn't find ul with id " + userListID);
    }
  }

  public void editItem(UserExercise userExercise) {
    userExerciseDAO.update(userExercise);
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
    return userListDAO.getWithExercises(id);
  }

  public void addVisitor(UserList userList, long user) {
    UserList where = userListDAO.getWhere(userList.getUniqueID());
    if (where != null) {
      userListDAO.addVisitor(where.getUniqueID(), user);
    }
  }

  // TODO : replace with DAO for this
  private List<UserAnnotation> annotations = new ArrayList<UserAnnotation>();

  public void addAnnotation(String exerciseID, String field, String status, String comment) {
    //exerciseID
    logger.info("write to database!");
    annotations.add(new UserAnnotation(exerciseID, field, status, comment));
  }

  public <T extends AudioExercise> void addAnnotations(T exercise) {
    for (UserAnnotation annotation : annotations) {
      if (annotation.exerciseID.equals(exercise.getID())) {
        exercise.addAnnotation(annotation.field, annotation.status, annotation.comment);
      }
    }
  }

  Set<String> reviewedExercises = new HashSet<String>();
  Set<String> incorrect = new HashSet<String>();
  public void markReviewed(String id) {
    reviewedExercises.add(id);
    logger.debug("markReviewed now " + reviewedExercises.size() + " reviewed exercises");
  }
  public Set<String> getReviewedExercises() { return reviewedExercises; }

  public void markIncorrect(String id) {
    incorrect.add(id);
  }
}
