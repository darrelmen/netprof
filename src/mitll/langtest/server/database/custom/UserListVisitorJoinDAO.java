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
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserListVisitorJoinDAO extends DAO {
  private static final String UNIQUEID = "uniqueid";
  private static final String USERLISTID = "userlistid";
  private static final String VISITORID = "visitorid";
  private static final String MODIFIED = "modified";

  private static final Logger logger = Logger.getLogger(UserListVisitorJoinDAO.class);

  private static final String USER_EXERCISE_LIST_VISITOR = "userexerciselist_visitor";

  public UserListVisitorJoinDAO(Database database) {
    super(database);
    try {
      createUserListTable(database);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  private void createUserListTable(Database database) throws SQLException {
   // logger.debug("create user list table ");
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      USER_EXERCISE_LIST_VISITOR +
      " (" +
      UNIQUEID +
      " IDENTITY, " +
      USERLISTID +
      " BIGINT, " +
      VISITORID +
      " BIGINT, " +
      MODIFIED +
      " TIMESTAMP,"+
      "FOREIGN KEY(" +
      USERLISTID +
      ") REFERENCES " +
      UserListDAO.USER_EXERCISE_LIST +
      "(" +
      UNIQUEID +
      ")," +
      "FOREIGN KEY(" +
      VISITORID +
      ") REFERENCES " +
      UserDAO.USERS +
      "(ID)" +
      ")");
    statement.execute();
    statement.close();

    if (!getColumns(USER_EXERCISE_LIST_VISITOR).contains(MODIFIED)) {
      addColumnToTable(connection);
    }
    database.closeConnection(connection);
  }

  /**
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see UserListDAO#addVisitor(long, long)
   */
  public void add(long listID, long visitor) {
    if (!update(listID, visitor)) {
      try {
        // there are much better ways of doing this...
        logger.info("add :visitor " + visitor + " to " + listID);

        Connection connection = database.getConnection(this.getClass().toString());
        PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + USER_EXERCISE_LIST_VISITOR +
            "(" +
            USERLISTID + "," +
            VISITORID + "," +
            MODIFIED +
            ") " +
            "VALUES(?,?,?);");
        int i = 1;

        statement.setLong(i++, listID);
        statement.setLong(i++, visitor);
        statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));  // smarter way to do this...?

        int j = statement.executeUpdate();

        if (j != 1)
          logger.error("huh? didn't insert row for " + listID + " and " + visitor);

        finish(connection, statement);

        logger.debug("UserListVisitorJoinDAO.add: now " + getCount(USER_EXERCISE_LIST_VISITOR) + " in " + USER_EXERCISE_LIST_VISITOR);
      } catch (Exception ee) {
        logger.error("got " + ee, ee);
      }
    }
  }

  /**
   *
   * @see mitll.langtest.server.database.custom.UserListDAO#populateList(mitll.langtest.shared.custom.UserList)
   * @param listid
   * @return
   */
/*  public Set<Long> getVisitorsOfList(long listid) {
    String sql = "SELECT * from " + USER_EXERCISE_LIST_VISITOR + " where " +
      USERLISTID +
    "=" + listid + " ORDER BY " +MODIFIED + " DESC";
    try {
      return getVisitors(sql,VISITORID);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }*/

  /**
   * @see mitll.langtest.server.database.custom.UserListDAO#getListsForUser(long)
   * @param userid
   * @return
   */
  public List<Long> getListsForVisitor(long userid) {
    String sql = "SELECT * from " + USER_EXERCISE_LIST_VISITOR + " where " +
      VISITORID +
      "=" + userid +
      " ORDER BY " +MODIFIED + " DESC";
    try {
      return getVisitors(sql);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  private List<Long> getVisitors(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<Long> visitors = new ArrayList<Long>();

    while (rs.next()) {
      visitors.add(rs.getLong(UserListVisitorJoinDAO.USERLISTID));
    }
    finish(connection, statement, rs, sql);

    return visitors;
  }

  /**
   * @see #add
   * @param userListID
   * @param visitor
   * @return
   */
  private boolean update(long userListID, long visitor) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + USER_EXERCISE_LIST_VISITOR +
        " " +
        "SET " +
        "modified=?" +
        "WHERE " +USERLISTID +
        "=" + userListID+
        " AND " +VISITORID + "=" +visitor;

      PreparedStatement statement = connection.prepareStatement(sql);
      statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));  // smarter way to do this...?
      int i = statement.executeUpdate();

      finish(connection, statement);
      boolean b = i > 0;
      if (b) {
        logger.debug("did update for list " + userListID + " and " + visitor + " in " + USER_EXERCISE_LIST_VISITOR);
      }
      else {
        logger.warn("did NOT update for list " + userListID + " and " + visitor + " in " + USER_EXERCISE_LIST_VISITOR);
      }
      return b;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return false;
  }

  public boolean remove(long listid) {
    return remove(USER_EXERCISE_LIST_VISITOR, USERLISTID, listid);
  }

  private void addColumnToTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " +
      USER_EXERCISE_LIST_VISITOR + " ADD " + MODIFIED + " TIMESTAMP ");
    statement.execute();
    statement.close();
  }
}