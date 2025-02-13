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

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.UserDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListDAO extends DAO {
  private static final String CREATORID = "creatorid";
  private static final Logger logger = Logger.getLogger(UserListDAO.class);

  private static final String NAME = "name";

  static final String USER_EXERCISE_LIST = "userexerciselist";
  private static final String ISPRIVATE = "isprivate";
  private final UserDAO userDAO;
  private UserExerciseDAO userExerciseDAO;
  private final UserListVisitorJoinDAO userListVisitorJoinDAO;

  public UserListDAO(Database database, UserDAO userDAO) {
    super(database);
    try {
      createUserListTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    this.userDAO = userDAO;
    userListVisitorJoinDAO = new UserListVisitorJoinDAO(database);
  }

  /**
   * @see UserListManager#addVisitor(long, long)
   * @see mitll.langtest.client.custom.Navigation#addVisitor(mitll.langtest.shared.custom.UserList)
   * @param listid
   * @param userid
   */
  public void addVisitor(long listid, long userid) {
    userListVisitorJoinDAO.add(listid, userid);
  }

  /**
   * @see UserListDAO#remove
   * @param listid
   */
  private void removeVisitor(long listid) {
    logger.debug("remove visitor reference " + listid);
    userListVisitorJoinDAO.remove(listid);
  }

  private void createUserListTable(Database database) throws SQLException {
//    logger.debug("createUserListTable --- ");

    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      USER_EXERCISE_LIST +
      " (" +
      "uniqueid IDENTITY, " +
      CREATORID +
      " INT, " +
      NAME +
      " VARCHAR, description VARCHAR, classmarker VARCHAR, modified TIMESTAMP, " +
      ISPRIVATE +
      " BOOLEAN, " +
      "FOREIGN KEY(" +
      CREATORID +
      ") REFERENCES " +
      "USERS" +
      "(ID)" +
      ")");

   // createIndex(database, CREATORID, USER_EXERCISE_LIST);
    finish(database, connection, statement);
  }


  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see UserListManager#reallyCreateNewItem(long, mitll.langtest.shared.custom.UserExercise, String)
   */
  public void add(UserList userList) {
    long id = 0;

    try {
      // there are much better ways of doing this...
//      logger.info("add :userList " + userList);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO " + USER_EXERCISE_LIST +
          "(" +
          CREATORID +
          "," +
          NAME +
          ",description,classmarker,modified," +
          ISPRIVATE +
          ") " +
          "VALUES(?,?,?,?,?,?);");
      int i = 1;
      //     statement.setLong(i++, userList.getUserID());
      statement.setLong(i++, userList.getCreator().getId());
      statement.setString(i++, userList.getName());
      statement.setString(i++, userList.getDescription());
      statement.setString(i++, userList.getClassMarker());
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));

      statement.setBoolean(i++, userList.isPrivate());

      int j = statement.executeUpdate();

      if (j != 1)
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);

      id = getGeneratedKey(statement);
      if (id == -1) {  logger.error("huh? no key was generated?");  }
     // logger.debug("unique id = " + id);

      userList.setUniqueID(id);

      finish(connection, statement);

      //logger.debug("add : now " + getCount(USER_EXERCISE_LIST) + " and user exercise is " + userList);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
  }

  public void updateModified(long uniqueID) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());

      String sql = "UPDATE " + USER_EXERCISE_LIST +
        " " +
        "SET " +
        "modified=? "+
        "WHERE uniqueid=?";

      PreparedStatement statement = connection.prepareStatement(sql);
      statement.setTimestamp(1,new Timestamp(System.currentTimeMillis()));
      statement.setLong(2, uniqueID);
      int i = statement.executeUpdate();

      //if (false) logger.debug("UPDATE " + i);
      if (i == 0) {
        logger.error("huh? didn't update the userList for " + uniqueID + " sql " + sql);
      }

      finish(connection, statement);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public int getCount() { return getCount(USER_EXERCISE_LIST); }

  /**
   * @see UserListManager#getListsForUser(long, boolean, boolean)
   * @param userid
   * @return
   */
  public List<UserList<CommonShell>> getAllByUser(long userid) {
    try {
      String sql = "SELECT * from " + USER_EXERCISE_LIST + " where " +
        CREATORID + "=" + userid+
        " order by modified desc";

      List<UserList<CommonShell>> lists = getWhere(sql);

      for (UserList<CommonShell> ul : lists) {
        populateList(ul);
      }
//      logger.debug("getAllByUser by " + userid + " is " + lists.size());
      return lists;

    } catch (Exception ee) {
      logger.error("getAllByUser got " + ee, ee);
    }
    return Collections.emptyList();
  }

  /**
   * Get lists by others that have not yet been visited.
   * Since your lists will appear under your lists, and visited lists will appear under other's lists.
   * @param userid
   * @return
   */
  public List<UserList<CommonShell>> getAllPublic(long userid) {
    try {
      String sql = "SELECT * from " + USER_EXERCISE_LIST + " where " +
        ISPRIVATE +
        "=false" +
        " order by modified DESC ";

      List<UserList<CommonShell>> userLists = getUserLists(sql, userid);
      List<UserList<CommonShell>> toReturn = new ArrayList<>();
      for (UserList<CommonShell> ul : userLists) {
        if (!ul.isEmpty()) {
          //logger.debug("getAllPublic : found userLists for " + userid + " : " +ul);

          toReturn.add(ul);
        }
        else {
        //  logger.info("\tgetAllPublic : skipping for " + userid + " : " +ul);
        }
      }
   //   logger.debug("toReturn for " + userid + " : " +toReturn);

      return toReturn;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  public boolean hasByName(long userid, String name) {
    try {
      return !getByName(userid,name).isEmpty();
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return false;
  }

  public List<UserList<CommonShell>> getByName(long userid, String name) {
    try {
      String sql = "SELECT * from " + USER_EXERCISE_LIST + " where " +
        NAME +
        "=" +
        "'" +name+
        "' AND " +
        CREATORID + "=" + userid;
      return getWhere(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }

  public boolean remove(long unique) {
    removeVisitor(unique);
    logger.debug("remove from " + USER_EXERCISE_LIST + " = " +unique);
    return remove(USER_EXERCISE_LIST, "uniqueid", unique);
  }

  /**
   * TODO don't want to always get all the exercises!
   * @see UserListManager#getUserListByID(long, java.util.Collection)
   * @param unique
   * @return
   */
  public UserList<CommonShell> getWithExercises(long unique) {
    UserList<CommonShell> where = getWhere(unique, true);
    if (where == null) {
      logger.error("couldn't find list by " + unique);
      return new UserList<>();
    }
    populateList(where);
    return where;
  }

  /**
   * @see #getWithExercises(long)
   * @see UserListManager#reallyCreateNewItem
   * @param unique
   * @param warnIfMissing
   * @return
   */
  public UserList<CommonShell> getWhere(long unique, boolean warnIfMissing) {
    if (unique < 0) return null;
    String sql = "SELECT * from " + USER_EXERCISE_LIST + " where uniqueid=" + unique + " order by modified";
    try {
      List<UserList<CommonShell>> lists = getUserLists(sql,-1);
      if (lists.isEmpty()) {
        if (warnIfMissing) logger.error("getVisitorsOfList : huh? no user list with id " + unique + " and sql " + sql);
        return null;
      } else {
        return lists.iterator().next();
      }
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#getListsForUser(long, boolean, boolean)
   * @param userid
   * @return
   */
  public Collection<UserList<CommonShell>> getListsForUser(long userid) {
    final List<Long> listsForVisitor = userListVisitorJoinDAO.getListsForVisitor(userid);
    List<UserList<CommonShell>> objects = Collections.emptyList();
    List<UserList<CommonShell>> userLists = listsForVisitor.isEmpty() ? objects : getIn(listsForVisitor);
    Collections.sort(userLists, new Comparator<UserList<?>>() {
      @Override
      public int compare(UserList<?> o1, UserList<?> o2) {
        int i1 = listsForVisitor.indexOf(o1.getUniqueID());
        int i2 = listsForVisitor.indexOf(o2.getUniqueID());
        return i1 < i2 ? -1 : i2 > i1 ? +1 : 0;
      }
    });
    return userLists;
  }

  private List<UserList<CommonShell>> getIn(Collection<Long> ids) {
    String s = ids.toString();
    s = s.replaceAll("\\[","").replaceAll("\\]","");
    String sql = "SELECT * from " + USER_EXERCISE_LIST + " where uniqueid in (" + s + ") order by modified";
    //logger.debug("sql for get in " + sql);
    try {
      List<UserList<CommonShell>> lists = getUserLists(sql,-1);
      if (lists.isEmpty()) {
      //  if (warnIfMissing) logger.error("getVisitorsOfList : huh? no user list with id " + unique + " and sql " + sql);
        return Collections.emptyList();
      } else {
        return lists;
      }
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return Collections.emptyList();
  }


  /**
   * @seex #getAll(long)
   * @see #getAllPublic
   * @see #getWhere(long, boolean)
   * @param sql
   * @param userid
   * @return
   * @throws SQLException
   */
  private List<UserList<CommonShell>> getUserLists(String sql, long userid) throws SQLException {
    List<UserList<CommonShell>> lists = getWhere(sql);

    for (UserList<CommonShell> ul : lists) {
      if (userid == -1 || ul.getCreator().getId() == userid || !ul.isFavorite()) {   // skip other's favorites
        populateList(ul);
      }
    }
    return lists;
  }

  private List<UserList<CommonShell>> getWhere(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<UserList<CommonShell>> lists = new ArrayList<>();

    while (rs.next()) {
      long uniqueid = rs.getLong("uniqueid");
      lists.add(new UserList<CommonShell>(uniqueid, //id
        userDAO.getUserWhere(rs.getLong(CREATORID)), // age
        rs.getString(NAME), // exp
        rs.getString("description"), // exp
        rs.getString("classmarker"), // exp
        rs.getBoolean(ISPRIVATE)
      )
      );
    }
    //logger.debug("getWhere : got " + lists);
    finish(connection, statement, rs, sql);
    return lists;
  }

  /**
   * TODO : This is going to get slow?
   * @see #getUserLists(String, long)
   * @see #getWithExercises(long)
   * @see #getAllByUser(long)
   * @param where
   */
  private void populateList(UserList<CommonShell> where) {
    if (userExerciseDAO == null) {
      logger.warn("no user exercise DAO????");
    }
    else {
      List<CommonShell> onList = userExerciseDAO.getOnList(where.getUniqueID());
      where.setExercises(onList);
    }
  }

  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {
    this.userExerciseDAO = userExerciseDAO;
  }

  public void setPublicOnList(long userListID, boolean isPublic) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());

      String sql = "UPDATE " + USER_EXERCISE_LIST +
        " " +
        "SET " +
        ISPRIVATE +
        "=? " +
        "WHERE uniqueid=?";

      PreparedStatement statement = connection.prepareStatement(sql);
      statement.setBoolean(1, !isPublic);
      statement.setLong(2, userListID);
      int i = statement.executeUpdate();

      //if (false) logger.debug("UPDATE " + i);
      if (i == 0) {
        logger.error("huh? didn't update the userList for " + userListID + " sql " + sql);
      }
      else {
        logger.debug("updated "+ userListID + " public " + isPublic+  " sql " + sql);
      }

      finish(connection, statement);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }
}