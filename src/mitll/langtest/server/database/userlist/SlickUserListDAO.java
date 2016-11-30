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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickUserExerciseList;
import mitll.npdata.dao.userexercise.UserExerciseListDAOWrapper;
import mitll.npdata.dao.userexercise.UserExerciseListVisitorDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(PathHelper)
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

  public SlickUserExerciseList toSlick(UserList<CommonShell> shared, int projid) {
    return toSlick2(shared, shared.getCreator().getID(), projid);
  }

  public SlickUserExerciseList toSlick2(UserList<CommonShell> shared, int userid, int projid) {
    return new SlickUserExerciseList(-1,
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
        ""
    );
  }

  private UserList<CommonShell> fromSlick(SlickUserExerciseList slick) {
    return new UserList<>(
        slick.id(),
        userDAO.getUserWhere(slick.userid()),
        slick.name(),
        slick.description(),
        slick.classmarker(),
        slick.isprivate(),
        slick.modified().getTime());
  }

  public void insert(SlickUserExerciseList UserExercise) {
    dao.insert(UserExercise);
  }

  public void addBulk(List<SlickUserExerciseList> bulk) {
    dao.addBulk(bulk);
  }

  private List<UserList<CommonShell>> fromSlick(Collection<SlickUserExerciseList> all) {
    List<UserList<CommonShell>> copy = new ArrayList<>();
    for (SlickUserExerciseList list : all) copy.add(fromSlick(list));
    return copy;
  }

  @Override
  public void addVisitor(long listid, long userid) {
    visitorDAOWrapper.insert((int) listid, (int) userid, System.currentTimeMillis());
  }

  @Override
  public void add(UserList<CommonShell> userList, int projid) {
    int assignedID = dao.insert(toSlick(userList, projid));
    userList.setUniqueID(assignedID);
  }

  public void addWithUser(UserList<CommonShell> userList, int userid, int projid) {
    SlickUserExerciseList user = toSlick2(userList, userid, projid);
    userList.setUniqueID(dao.insert(user));
  }

  @Override
  public void updateModified(long uniqueID) {
    dao.updateModified((int) uniqueID);
  }

  @Override
  public int getCount() {
    return dao.getNumRows();
  }

  /**
   * Side effect is to add user exercises to lists.
   *
   * @param userid
   * @param projectID
   * @return
   * @see IUserListManager#getListsForUser(int, boolean, boolean, int)
   */
  @Override
  public List<UserList<CommonShell>> getAllByUser(long userid, int projectID) {
    List<UserList<CommonShell>> userExerciseLists = fromSlick(dao.byUser((int) userid, projectID));

    for (UserList<CommonShell> ul : userExerciseLists) {
      populateList(ul);
    }
    return userExerciseLists;
  }

  /**
   * Could get slow.
   * Why would we need to copy the exercise as in UserListDAO?
   *
   * @param where
   * @see IUserListDAO#getAllByUser(long, int)
   */
  private void populateList(UserList<CommonShell> where) {
    List<CommonShell> onList = userExerciseDAO.getOnList(where.getID());
    where.setExercises(onList);
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
   * Expensive ...?
   *
   * @param userid
   * @param projectID
   * @return non empty public lists
   */
  @Override
  public List<UserList<CommonShell>> getAllPublic(long userid, int projectID) {
    List<UserList<CommonShell>> userExerciseLists = fromSlick(dao.allPublic(projectID));
    populateLists(userExerciseLists, userid);

    List<UserList<CommonShell>> toReturn = new ArrayList<>();
    for (UserList<CommonShell> ul : userExerciseLists) {
      if (!ul.isEmpty()) {
        toReturn.add(ul);
      }
    }

    return toReturn;
  }

  /**
   * @return
   * @throws SQLException
   * @seex #getAllPredef(long)
   * @see IUserListDAO#getAllPublic
   * @see #getWhere(long, boolean)
   */
  private void populateLists(Collection<UserList<CommonShell>> lists, long userid) {
    for (UserList<CommonShell> ul : lists) {
      if (userid == -1 || ul.getCreator().getID() == userid || !ul.isFavorite()) {   // skip other's favorites
        populateList(ul);
      }
    }
  }

  @Override
  public boolean hasByName(long userid, String name, int projid) {
    return dao.hasByName((int) userid, name, projid);
  }

  @Override
  public List<UserList<CommonShell>> getByName(long userid, String name, int projid) {
    return fromSlick(dao.getByName((int) userid, name, projid));
  }

  @Override
  public boolean remove(long unique) {
    return dao.markDeleted((int) unique);
  }

  @Override
  public UserList<CommonShell> getWithExercises(long unique) {
    UserList<CommonShell> where = getWhere(unique, true);
    if (where != null) populateList(where);
    return where;
  }

  @Override
  public UserList<CommonShell> getWhere(long unique, boolean warnIfMissing) {
    Option<SlickUserExerciseList> slickUserExerciseListOption = dao.byID((int) unique);
    if (slickUserExerciseListOption.isDefined()) {
      return fromSlick(slickUserExerciseListOption.get());
    } else {
      if (warnIfMissing) logger.error("getByExID : huh? no user list with id " + unique);
      return null;
    }
  }

  @Override
  public Collection<UserList<CommonShell>> getListsForUser(int userid, int projid) {
    List<UserList<CommonShell>> userLists = fromSlick(dao.getVisitedBy(userid, projid));
    populateLists(userLists, userid);
    return userLists;
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
  public void setPublicOnList(long userListID, boolean isPublic) {
    int i = dao.setPublic((int) userListID, isPublic);
    if (i == 0) logger.error("setPublicOnList : huh? didn't update the userList for " + userListID);
  }

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
