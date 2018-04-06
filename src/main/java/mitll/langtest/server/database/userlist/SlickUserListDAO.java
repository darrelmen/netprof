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

package mitll.langtest.server.database.userlist;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.services.ListServiceImpl;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickUserExerciseList;
import mitll.npdata.dao.userexercise.UserExerciseListDAOWrapper;
import mitll.npdata.dao.userexercise.UserExerciseListVisitorDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import scala.Option;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class SlickUserListDAO extends DAO implements IUserListDAO {
  private static final Logger logger = LogManager.getLogger(SlickUserListDAO.class);

  private final UserExerciseListDAOWrapper dao;
  private final UserExerciseListVisitorDAOWrapper visitorDAOWrapper;
  private final IUserExerciseDAO userExerciseDAO;

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
                          IUserExerciseDAO userExerciseDAO) {
    super(database);
    dao = new UserExerciseListDAOWrapper(dbConnection);
    this.userDAO = userDAO;
    this.visitorDAOWrapper = new UserExerciseListVisitorDAOWrapper(dbConnection);
    this.userExerciseDAO = userExerciseDAO;
  }

  public void createTable() {
    dao.createTable();
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
        false, // not homework list
        "", "",
        shared.getListType().toString()
    );
  }

  private UserList<CommonShell> fromSlick(SlickUserExerciseList slick) {
    UserList.LIST_TYPE list_type = getListType(slick);

    return new UserList<>(
        slick.id(),
        slick.userid(),
        userDAO.getUserChosenID(slick.userid()),
        slick.name(),
        slick.description(),
        slick.classmarker(),
        slick.isprivate(),
        slick.modified().getTime(),
        slick.contexturl(),
        slick.richtext(),
        slick.projid(),
        list_type);
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

  private UserList<CommonExercise> fromSlickEx(SlickUserExerciseList slick) {
    return new UserList<>(
        slick.id(),
        slick.userid(),
        userDAO.getUserChosenID(slick.userid()),
        slick.name(),
        slick.description(),
        slick.classmarker(),
        slick.isprivate(),
        slick.modified().getTime(),
        slick.contexturl(), slick.richtext(),
        slick.projid(),
        getListType(slick));
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

/*
  @Override
  public void updateContext(long uniqueID, String context) {
    dao.updateContext((int) uniqueID, context);
  }

  @Override
  public void updateRichText(long uniqueID, String richText) {
    dao.updateRichText((int) uniqueID, richText);
  }

  @Override
  public void updateName(long uniqueID, String name) {
    dao.updateName((int) uniqueID, name);
  }
*/

  @Override
  public int getCount() {
    return dao.getNumRows();
  }

  /**
   * Side effect is to add user exercises to lists.
   * <p>
   * TODO : don't do a separate query for each list.
   *
   * @param userid
   * @param projectID
   * @return
   * @see IUserListManager#getListsForUser(int, int, boolean, boolean)
   */
  @Override
  public List<UserList<CommonShell>> getAllByUser(int userid, int projectID) {
    long then = System.currentTimeMillis();
    List<UserList<CommonShell>> userExerciseLists = fromSlick(getByUser(userid, projectID));
    long now = System.currentTimeMillis();

    logger.info("getAllByUser took " + (now - then) + " to get " + userExerciseLists.size());

    then = now;
    userExerciseLists.forEach(this::populateList);
    now = System.currentTimeMillis();

    logger.info("getAllByUser took " + (now - then) + " to populate " + userExerciseLists.size());

    return userExerciseLists;
  }

  /**
   * Could get slow.
   * Why would we need to copy the exercise as in UserListDAO?
   *
   * TODO : only want # of items on list, not complete populated list most of the time
   *
   * @param where
   * @see IUserListDAO#getAllByUser(int, int)
   * @see #getWithExercises(int)
   * @see #populateLists(Collection, long)
   */
  private void populateList(UserList<CommonShell> where) {
    //   List<CommonShell> onList = userExerciseDAO.getOnList(where.getID());
    where.setExercises(userExerciseDAO.getOnList(where.getID()));
    // for (CommonShell shell : onList) logger.info("for " + where.getOldID() + " found " + shell);
/*
    Set<String> userExIDs = new HashSet<>();
    for (CommonShell shell : onList) userExIDs.add(shell.getOldID());

    Collection<String> exidsFor = userListExerciseJoinDAO.getExidsFor(where.getRealID());

    for (String exid : exidsFor) {
      if (!userExIDs.contains(exid)) {
        CommonExercise predefExercise = userExerciseDAO.getPredefExercise(exid);
        if (exid == null) logger.warn("can't find " + exid + " for list " + where);
        else onList.add(predefExercise);
      }
    }*/
  }

  /**
   * Add the exercises to the list.
   *
   * @param where
   * @see #getWithExercisesEx
   */
  private void populateListEx(UserList<CommonExercise> where) {
    where.setExercises(userExerciseDAO.getCommonExercises(where.getID()));
  }

  /**
   * Expensive ...?
   *
   * @param userid
   * @param projectID
   * @return non empty public lists
   */
/*  @Override
  public List<UserList<CommonShell>> getAllPublic(long userid, int projectID) {
    List<UserList<CommonShell>> userExerciseLists = fromSlick(dao.allPublic(projectID));
    populateLists(userExerciseLists, userid);

    List<UserList<CommonShell>> toReturn = new ArrayList<>(userExerciseLists.size());
    userExerciseLists.forEach(ul -> {
      if (!ul.isEmpty()) {
        toReturn.add(ul);
      }
    });

    return toReturn;
  }*/

  /**
   * @return
   * @throws SQLException
   * @seex #getAllPredef(long)
   * @see IUserListDAO#getAllPublic
   * @see IUserListDAO#getWhere(int, boolean)
   */
/*  private void populateLists(Collection<UserList<CommonShell>> lists, long userid) {
    for (UserList<CommonShell> ul : lists) {
      if (userid == -1 || ul.getUserID() == userid || !ul.isFavorite()) {   // skip other's favorites
        populateList(ul);
      }
    }
  }*/
  @Override
  public boolean hasByName(long userid, String name, int projid) {
    return dao.hasByName((int) userid, name, projid);
  }

  @Override
  public List<UserList<CommonShell>> getByName(long userid, String name, int projid) {
    return fromSlick(dao.getByName((int) userid, name, projid));
  }

  @Override
  public boolean remove(int unique) {
    return dao.markDeleted(unique);
  }

  @Override
  public void bringBack(long unique) {
    dao.markNotDeleted((int) unique);
  }

  @Override
  public UserList<CommonShell> getWithExercises(int unique) {
    UserList<CommonShell> where = getWhere(unique, true);
    if (where != null) populateList(where);
    return where;
  }

  @Override
  public UserList<CommonExercise> getWithExercisesEx(int unique) {
    UserList<CommonExercise> list = getList(unique);
    if (list == null) return null;
    else {
      populateListEx(list);
      return list;
    }
  }

  @Override
  public UserList<CommonExercise> getList(int unique) {
    Option<SlickUserExerciseList> slickUserExerciseListOption = dao.byID((int) unique);
    UserList<CommonExercise> exlist;

    if (slickUserExerciseListOption.isDefined()) {
      exlist = fromSlickEx(slickUserExerciseListOption.get());
    } else {
      logger.error("getList : huh? no user list with id " + unique);
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
      if (warnIfMissing) logger.error("getByExID : huh? no user list with id " + unique);
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
    getAllListsForUser(userid, projid)
        .forEach(ue -> ret.add(fromSlick(ue)));
    ret.forEach(this::populateList);

    return getNonEmpty(ret);
  }

  /**
   * Both created, visited, and public
   *
   * @param userid don't return lists by this user
   * @param projid for this project
   * @return
   * @see #getAllPublicNotMine
   */
  @NotNull
  private List<SlickUserExerciseList> getAllListsForUser(int userid, int projid) {
    List<SlickUserExerciseList> temp = new ArrayList<>();
    {
      dao.allPublic(projid).forEach(ue -> {
        if (ue.userid() != userid) temp.add(ue);
      });
    }
    return temp;
  }

  @Override public Collection<UserList<CommonShell>> getAllQuiz(int projid) {
/*    List<SlickUserExerciseList> temp = new ArrayList<>();
    {
      dao.allQuiz(projid).forEach(ue -> {
        if (ue.userid() != userid) temp.add(ue);
      });
    }*/
    Collection<SlickUserExerciseList> slickUserExerciseLists = dao.allQuiz(projid);

    List<UserList<CommonShell>> ret = new ArrayList<>(slickUserExerciseLists.size());
    slickUserExerciseLists.forEach(ue -> ret.add(fromSlick(ue)));

    return ret;
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
   * @see mitll.langtest.server.database.custom.UserListManager#getListsForUser(int, int, boolean, boolean)
   */
  @Override
  public Collection<UserList<CommonShell>> getVisitedLists(int userid, int projid) {
    Collection<SlickUserExerciseList> visitedBy = getVisitedBy(userid, projid);

    List<UserList<CommonShell>> ret = new ArrayList<>(visitedBy.size());
    visitedBy.forEach(ue -> ret.add(fromSlick(ue)));
    ret.forEach(this::populateList);

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

  /**
   * @param userid
   * @param projid
   * @return
   * @see mitll.langtest.server.database.custom.UserListManager#getNumLists
   */
  @Override
  public int getNumMineAndPublic(int userid, int projid) {
    return dao.numMineAndPublic(userid, projid);
  }

  @Override
  public void setPublicOnList(long userListID, boolean isPublic) {
    int i = dao.setPublic((int) userListID, isPublic);
    if (i == 0) logger.error("setPublicOnList : huh? didn't update the userList for " + userListID);
  }

  @Override
  public void update(UserList userList) {
    dao.update(toSlick(userList));
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
