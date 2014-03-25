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
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
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

  void createUserListTable(Database database) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      USER_EXERCISE_LIST_VISITOR +
      " (" +
      UNIQUEID +
      " IDENTITY, " +
      USERLISTID +
      " LONG, " +
      VISITORID +
      " LONG, " +
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
    database.closeConnection(connection);

    if (!getColumns(USER_EXERCISE_LIST_VISITOR).contains(MODIFIED)) {
      addColumnToTable(connection);
    }
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

        Connection connection = database.getConnection();
        PreparedStatement statement;

 /*     String prefix = "IF NOT EXISTS (SELECT visitorid" +
        " FROM " + USER_EXERCISE_LIST_VISITOR +
        " WHERE visitorid = " + visitor +
        " AND userlistid = " + listID +
        ") ";*/
        statement = connection.prepareStatement(
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

        statement.close();
        database.closeConnection(connection);

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
  public Set<Long> getVisitorsOfList(long listid) {
    String sql = "SELECT * from " + USER_EXERCISE_LIST_VISITOR + " where " +
      USERLISTID +
    "=" + listid + " ORDER BY " +MODIFIED + " DESC";
    try {
      return getVisitors(sql,VISITORID);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  public Set<Long> getListsForVisitor(long userid) {
    String sql = "SELECT * from " + USER_EXERCISE_LIST_VISITOR + " where " +
      VISITORID +
      "=" + userid +
      " ORDER BY " +MODIFIED + " DESC";
    try {
      return getVisitors(sql,USERLISTID);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  private Set<Long> getVisitors(String sql, String column) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    Set<Long> visitors = new HashSet<Long>();

    while (rs.next()) {
      visitors.add(rs.getLong(column));
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

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
      Connection connection = database.getConnection();
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

      statement.close();
      database.closeConnection(connection);
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