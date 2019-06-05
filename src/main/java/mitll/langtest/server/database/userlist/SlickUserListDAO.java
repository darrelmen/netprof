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

package mitll.langtest.server.database.userlist;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.services.ListServiceImpl;
import mitll.langtest.shared.custom.INameable;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.custom.Nameable;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickLightList;
import mitll.npdata.dao.SlickUserExerciseList;
import mitll.npdata.dao.userexercise.UserExerciseListDAOWrapper;
import mitll.npdata.dao.userexercise.UserExerciseListVisitorDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import scala.Option;

import java.sql.Timestamp;
import java.util.*;

public class SlickUserListDAO extends DAO implements IUserListDAO {
  private static final Logger logger = LogManager.getLogger(SlickUserListDAO.class);
  public static final boolean DEBUG = false;

  private final UserExerciseListDAOWrapper dao;
  private final UserExerciseListVisitorDAOWrapper visitorDAOWrapper;
  private final IUserExerciseDAO userExerciseDAO;
  private final IProjectDAO projectDAO;
  private final IUserDAO userDAO;

  /**
   * @param database
   * @param dbConnection
   * @param userDAO
   * @param userExerciseDAO
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public SlickUserListDAO(Database database,
                          DBConnection dbConnection,
                          IUserDAO userDAO,
                          IUserExerciseDAO userExerciseDAO,
                          IProjectDAO projectDAO) {
    super(database);
    dao = new UserExerciseListDAOWrapper(dbConnection);
    this.projectDAO = projectDAO;
    this.userDAO = userDAO;
    this.visitorDAOWrapper = new UserExerciseListVisitorDAOWrapper(dbConnection);
    this.userExerciseDAO = userExerciseDAO;
  }

  public void createTable() {
    dao.createTable();
  }

  public boolean updateProject(int old, int newprojid) {
    return dao.updateProject(old, newprojid) > 0;
  }

  @Override
  public String getName() {
    return dao.getName();
  }

  private List<UserList<CommonShell>> fromSlick(Collection<SlickUserExerciseList> all) {
    List<UserList<CommonShell>> copy = new ArrayList<>(all.size());
    all.forEach(slickUserExerciseList -> copy.add(fromSlick(slickUserExerciseList)));
    return copy;
  }

  public SlickUserExerciseList toSlick(UserList<?> shared) {
    return toSlick2(shared, shared.getUserID(), shared.getProjid(), shared.getID());
  }

  public SlickUserExerciseList toSlick(UserList<?> shared, int projid) {
    return toSlick2(shared, shared.getUserID(), projid, -1);
  }

  public SlickUserExerciseList toSlick2(UserList<?> shared, int userid, int projid, int id) {
    String listtype = shared.getListType().toString();
//    logger.info("toSlick2 list type " + listtype + " id " + id + " proj " + projid);
    return new SlickUserExerciseList(id,
        userid,
        new Timestamp(shared.getModified()),
        shared.getName(),
        shared.getDescription(),
        shared.getClassMarker(),
        shared.isPrivate(),
        false,
        shared.isFavorite(),
        shared.getID(),
        projid,
        "",
        "",
        listtype,
        new Timestamp(shared.getStart()),
        new Timestamp(shared.getEnd()),
        shared.getDuration(),
        shared.getMinScore(),
        shared.shouldShowAudio(),
        shared.getAccessCode()
    );
  }

  private UserList<CommonShell> fromSlick(SlickUserExerciseList slick) {
    User byID = getUser(slick);

   // logger.info("owner for list " + slick.id() + " " + slick.name() + " is "+byID.getUserID() + " " + byID.isTeacher());

    UserList<CommonShell> commonShellUserList = new UserList<>(
        slick.id(),
        slick.userid(),
        byID.getUserID(),
        byID.getFirstInitialName(),
        slick.name(),
        slick.description(),
        slick.classmarker(),
        slick.isprivate(),
        slick.modified().getTime(),
        slick.contexturl(),
        slick.richtext(),
        slick.projid(),
        getListType(slick),
        slick.start().getTime(),
        slick.endtime().getTime(),
        slick.duration(),
        slick.minscore(),
        slick.showaudio(), slick.accesscode());
    return commonShellUserList.setTeacher(!byID.isStudent());
  }

  private UserList<CommonExercise> fromSlickEx(SlickUserExerciseList slick) {
    User byID = getUser(slick);
    String userChosenID = byID.getUserID();

   // logger.info("fromSlickEx owner for list " + slick.id() + " " + slick.name() + " is "+byID.getUserID() + " " + byID.isTeacher());

    UserList<CommonExercise> commonExerciseUserList = new UserList<>(
        slick.id(),
        slick.userid(),
        userChosenID,
        byID.getFirstInitialName(),
        slick.name(),
        slick.description(),
        slick.classmarker(),
        slick.isprivate(),
        slick.modified().getTime(),
        slick.contexturl(),
        slick.richtext(),
        slick.projid(),
        getListType(slick),
        slick.start().getTime(),
        slick.endtime().getTime(),
        slick.duration(),
        slick.minscore(),
        slick.showaudio(), slick.accesscode());

    return commonExerciseUserList.setTeacher(!byID.isStudent());
  }

  private User getUser(SlickUserExerciseList slick) {
    return userDAO.getByID(slick.userid());
  }

  @NotNull
  private UserList.LIST_TYPE getListType(SlickUserExerciseList slick) {
    String listtype = slick.listtype();

    try {
      return listtype.isEmpty() ? UserList.LIST_TYPE.NORMAL : UserList.LIST_TYPE.valueOf(listtype);
    } catch (IllegalArgumentException e) {
      logger.warn("can't parse " + listtype);
      return UserList.LIST_TYPE.NORMAL;
    }
  }

  public void insert(SlickUserExerciseList UserExercise) {
    dao.insert(UserExercise);
  }

  public void addBulk(List<SlickUserExerciseList> bulk) {
    dao.addBulk(bulk);
  }

  @Override
  public void addVisitor(int listid, int userid) {
    visitorDAOWrapper.insert(listid, userid, System.currentTimeMillis());
  }

  @Override
  public void removeVisitor(int listid, int userid) {
    visitorDAOWrapper.remove(listid, userid);
  }

  @Override
  public int add(UserList<?> userList, int projid) {
    int insert = dao.insert(toSlick(userList, projid));
    userList.setUniqueID(insert);
    return insert;
  }

  @Override
  public void updateModified(long uniqueID) {
    dao.updateModified((int) uniqueID);
  }

  /**
   * Side effect is to add user exercises to lists.
   * <p>
   * TODO : don't do a separate query for each list.
   *
   * @param userid
   * @param projectID
   * @return
   * @see IUserListManager#getListsForUser(int, int, boolean, boolean, boolean)
   */
  @Override
  public List<UserList<CommonShell>> getAllByUser(int userid, int projectID) {
    long then = System.currentTimeMillis();
    List<UserList<CommonShell>> userExerciseLists = fromSlick(getByUser(userid, projectID));
    long now = System.currentTimeMillis();

    if (DEBUG) logger.info("getAllByUser took " + (now - then) + " to get " + userExerciseLists.size());

    then = now;
    boolean shouldSwap = projectDAO.getShouldSwap(projectID);
    userExerciseLists.forEach(where -> populateList(where, shouldSwap));
    now = System.currentTimeMillis();

    if (DEBUG) logger.info("getAllByUser took " + (now - then) + " to populate " + userExerciseLists.size());

    return userExerciseLists;
  }

  /**
   * Could get slow.
   * Why would we need to copy the exercise as in UserListDAO?
   *
   * TODO : only want # of items on list, not complete populated list most of the time
   *
   * @param where
   * @param shouldSwap
   * @seez #populateLists
   * @see IUserListDAO#getAllByUser(int, int)
   * @see #getWithExercises(int)
   */
  private void populateList(UserList<CommonShell> where, boolean shouldSwap) {
    where.setExercises(userExerciseDAO.getOnList(where.getID(), shouldSwap, database.getProject(where.getProjid()).getSmallVocabDecoder()));
  }

  @Override
  public List<UserList<CommonShell>> getByName(long userid, String name, int projid) {
    return fromSlick(dao.getByName((int) userid, name, projid));
  }

  @Override
  public boolean remove(int unique) {
    return dao.markDeleted(unique);
  }

  /**
   *
   * @param unique
   * @return
   */
  @Override
  public UserList<CommonShell> getWithExercises(int unique) {
    UserList<CommonShell> where = getWhere(unique, true);

    if (where != null) {
      populateList(where, projectDAO.getShouldSwap(where.getProjid()));
    }
    return where;
  }

  /**
   * list might be deleted...
   *
   * @param unique
   * @return null if the list has been deleted
   * @see mitll.langtest.server.database.DatabaseImpl#getUserListByIDExercises
   */
  @Override
  public UserList<CommonExercise> getList(int unique) {
    Option<SlickUserExerciseList> slickUserExerciseListOption = dao.byID(unique);
    UserList<CommonExercise> exlist;

    if (slickUserExerciseListOption.isDefined()) {
      exlist = fromSlickEx(slickUserExerciseListOption.get());
    } else {
      logger.warn("getList : huh? no user list with id " + unique);
      return null;
    }
    return exlist;
  }

  @Override
  public UserList<CommonShell> getWhere(int unique, boolean warnIfMissing) {
    Option<SlickUserExerciseList> slickUserExerciseListOption = dao.byID(unique);
    if (slickUserExerciseListOption.isDefined()) {
      return fromSlick(slickUserExerciseListOption.get());
    } else {
      if (warnIfMissing) logger.warn("getByExID : huh? no user list with id " + unique);
      return null;
    }
  }

  /**
   * Don't return empty lists.
   *
   * @param userid
   * @param projid
   * @return
   * @see ListServiceImpl#getLists
   */
  @Override
  public Collection<UserList<CommonShell>> getAllPublicNotMine(int userid, int projid) {
    List<UserList<CommonShell>> ret = new ArrayList<>();
    boolean shouldSwap = projectDAO.getShouldSwap(projid);

    dao.allPublicNotThisUser(projid,userid)
        .forEach(ue -> ret.add(fromSlick(ue)));
    ret.forEach(where -> populateList(where, shouldSwap));

    return getNonEmpty(ret);
  }

  /**
   * @param projid
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#getListsForUser
   */
  @Override
  public Collection<UserList<CommonShell>> getAllQuiz(int projid) {
    Collection<SlickUserExerciseList> slickUserExerciseLists = dao.allQuiz(projid);
    List<UserList<CommonShell>> ret = new ArrayList<>(slickUserExerciseLists.size());
    slickUserExerciseLists.forEach(ue -> ret.add(fromSlick(ue)));
    return ret;
  }

  /**
   * First we need to get the names of the lists or quizzes, so we can build a trie...?
   *
   * so when you don't search, you just see all of the lists or all quizzes
   *
   * then you choose one and see the content for that list or quiz
   *
   * @param projid
   * @param userid
   * @param isQuiz
   * @return
   * @see ListServiceImpl#getAllQuiz
   * @see UserContainer#getQuizListBox
   */
  @Override
  public List<INameable> getAllOrMineLight(int projid, int userid, boolean isQuiz) {
    Collection<SlickLightList> slickUserExerciseLists = getSlickAllOrMineLight(projid, userid, isQuiz);
    List<INameable> names = new ArrayList<>(slickUserExerciseLists.size());
    slickUserExerciseLists
        .forEach(slickUserExerciseList ->
            names.add(new Nameable(slickUserExerciseList.id(), slickUserExerciseList.name())));
    return names;
  }

  /**
   * All public or mine.
   *
   * @param projid
   * @param userID
   * @param isQuiz
   * @return
   */
  public Collection<SlickUserExerciseList> getSlickAllPublicOrMine(int projid, int userID, boolean isQuiz) {
    return dao.allPublicOrMine(projid, userID,isQuiz);
  }

  @Override
  public Collection<SlickLightList> getSlickAllOrMineLight(int projid, int userID, boolean isQuiz){
    return dao.allPublicOrMineLight(projid, userID, isQuiz);
  }

  @NotNull
  private Collection<UserList<CommonShell>> getNonEmpty(List<UserList<CommonShell>> ret) {
    List<UserList<CommonShell>> nonEmpty = new ArrayList<>();
    ret.forEach(commonShellUserList -> {
      if (!commonShellUserList.isEmpty()) nonEmpty.add(commonShellUserList);
    });
    return nonEmpty;
  }

  /**
   * Adds exercises to list too.
   *
   * @param userid
   * @param projid
   * @return
   * @see IUserListManager#getListsForUser(int, int, boolean, boolean, boolean)
   */
  @Override
  public Collection<UserList<CommonShell>> getVisitedLists(int userid, int projid) {
    Collection<SlickUserExerciseList> visitedBy = getVisitedBy(userid, projid);
    boolean shouldSwap = projectDAO.getShouldSwap(projid);

    List<UserList<CommonShell>> ret = new ArrayList<>(visitedBy.size());
    visitedBy.forEach(ue -> ret.add(fromSlick(ue)));
    ret.forEach(where -> populateList(where, shouldSwap));

    return ret;
  }

  @Override
  public Collection<SlickUserExerciseList> getByUser(int userid, int projid) {
    return dao.byUser(userid, projid);
  }

  @Override
  public Collection<SlickUserExerciseList> getVisitedBy(int userid, int projid) {
    return dao.getVisitedBy(userid, projid);
  }

  /**
   * TODO : not needed?
   *
   * @param userExerciseDAO
   */
  @Override
  public void setUserExerciseDAO(IUserExerciseDAO userExerciseDAO) {
  }

  @Override
  public void update(UserList userList) {
    int update = dao.update(toSlick(userList));
    if (update == 0) {
      logger.warn("huh? didn't update " + userList.getID());
    } else {
      Option<SlickUserExerciseList> slickUserExerciseListOption = dao.byID(userList.getID());
      if (slickUserExerciseListOption.isDefined()) {
        UserList<CommonShell> commonShellUserList = fromSlick(slickUserExerciseListOption.get());
        logger.info("update : Got back " + commonShellUserList);
      }
    }
  }

  /**
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#createTables
   */
  public UserExerciseListVisitorDAOWrapper getVisitorDAOWrapper() {
    return visitorDAOWrapper;
  }

  public Map<Integer, Integer> getOldToNew(int projid) {
    Map<Integer, Integer> oldToNew = new HashMap<>();
    for (SlickUserExerciseList userExerciseList : dao.getAll(projid)) {
      oldToNew.put(userExerciseList.legacyid(), userExerciseList.id());
    }
    return oldToNew;
  }

  public boolean isEmpty() {
    return dao.getNumRows() == 0;
  }
}
