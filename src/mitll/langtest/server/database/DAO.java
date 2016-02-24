/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database;

import mitll.langtest.server.LogAndNotify;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/16/12
 * Time: 2:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class DAO {
  private static final Logger logger = Logger.getLogger(DAO.class);
  protected final LogAndNotify logAndNotify;
  private final boolean isMYSQL;
  protected final boolean isPostgreSQL;

  public static final String ID = "ID";

  protected final Database database;

  protected DAO(Database database) {
    this.database = database;
    this.logAndNotify = database.getLogAndNotify();
    isMYSQL = database.getServerProps().useMYSQL();
    isPostgreSQL = database.getServerProps().usePostgres();
  }

  protected int getNumColumns(Connection connection, String table) throws SQLException {
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " LIMIT 1");

    // Get result set meta data
    ResultSetMetaData rsmd = rs.getMetaData();
    int numColumns = rsmd.getColumnCount();
    rs.close();
    stmt.close();
    return numColumns;
  }

  /**
   * @param table
   * @return column names, all lower case
   * @see mitll.langtest.server.database.custom.UserListVisitorJoinDAO#createUserListTable(Database)
   */
  protected Collection<String> getColumns(String table) {
    Set<String> columns = new HashSet<String>();
    try {
      Connection connection = database.getConnection(this.getClass().toString());

      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " LIMIT 1");

      // Get result set meta data
      ResultSetMetaData rsmd = rs.getMetaData();
      for (int i = 1; i < rsmd.getColumnCount() + 1; i++) {
        columns.add(rsmd.getColumnName(i).toLowerCase());
      }

      rs.close();
      stmt.close();

      database.closeConnection(connection);
    } catch (Exception e) {
      logger.error("doing getColumns: got " + e, e);
    }
    //logger.info("table " +table + " has " +columns);
    return columns;
  }

  protected int getCount(String table) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) from " + table);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) return rs.getInt(1);
      statement.close();

      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }

  protected boolean remove(String table, String idField, long itemId) {
    String sql = "DELETE FROM " + table + " WHERE " + idField + "=" + itemId;

    return doSqlOn(sql, table, true);
  }

  protected boolean remove(String table, String idField, String itemId, boolean warnIfDidntAlter) {
    String sql = "DELETE FROM " + table + " WHERE " +
        idField +
        "='" + itemId +
        "'";

    return doSqlOn(sql, table, warnIfDidntAlter);
  }

  protected boolean doSqlOn(String sql, String table, boolean warnIfDidntAlter) {
    try {
      int before = getCount(table);

      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);
      boolean changed = statement.executeUpdate() == 1;

      if (!changed && warnIfDidntAlter) {
        logger.warn("doSqlOn : didn't alter row for " + table + " sql " + sql);
      }

      statement.close();
      database.closeConnection(connection);

      int count = getCount(table);
      //  logger.debug("now " + count + " in " + table);
      if (before - count != 1 && warnIfDidntAlter) {
        logger.warn("DAO.doSqlOn : now " + count +
            " there were " + before + " before for " + table);
      }

      return changed;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return false;
  }

  public Database getDatabase() {
    return database;
  }

  protected void addVarchar(Connection connection, String table, String col) throws SQLException {
    alterTable(connection, table, col, "VARCHAR");
  }

  protected void addBoolean(Connection connection, String table, String col) throws SQLException {
    alterTable(connection, table, col, "BOOLEAN");
  }

  protected void addFloat(Connection connection, String table, String col) throws SQLException {
    alterTable(connection, table, col, "FLOAT");
  }

  protected void addInt(Connection connection, String table, String col) throws SQLException {
    alterTable(connection, table, col, "INTEGER");
  }

  private void alterTable(Connection connection, String table, String col, String type) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + table + " ADD " + col + " " + type);
    statement.execute();
    statement.close();
  }

  protected void createIndex(Database database, String column, String table) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("" +
        "CREATE INDEX IF NOT EXISTS " +
        "IDX_" + column +
        " ON " +
        table +
        "(" +
        column +
        ");");
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }

  protected void finish(Connection connection, Statement statement, ResultSet rs) throws SQLException {
    rs.close();
    statement.close();
    database.closeConnection(connection);
  }

  protected void finish(Database database, Connection connection, PreparedStatement statement) throws SQLException {
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }

  public void finish(Connection connection, PreparedStatement statement) throws SQLException {
    statement.close();
    database.closeConnection(connection);
  }

  protected long getGeneratedKey(PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN

    long newID = -1;
    if (rs.next()) {
      newID = rs.getLong(1);
    }
    return newID;
  }

  protected Connection getConnection() {
    return database.getConnection(this.getClass().toString());
  }

  protected <T> String getInList(Collection<T> ids) {
    StringBuilder b = new StringBuilder();
    for (T id : ids) b.append("'").append(id).append("'").append(",");
    String list = b.toString();
    list = list.substring(0, Math.max(0, list.length() - 1));
    return list;
  }

  /**
   * @see RefResultDAO#createResultTable(Connection)
   */
  void drop(String table, Connection connection) {
    try {
      PreparedStatement statement = connection.prepareStatement("DROP TABLE " + table + " IF EXISTS ");
      if (!statement.execute()) {
        statement.close();

        logger.error("couldn't drop table " + table);
        PreparedStatement preparedStatement = connection.prepareStatement("DROP TABLE " + table.toUpperCase() + " IF EXISTS ");
        if (!preparedStatement.execute()) {
          logger.error("2 couldn't drop table " + table);
        }
      } else {
        statement.close();
      }
      database.closeConnection(connection);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void logException(Exception ee) {
    logger.error("got " + ee, ee);
    logAndNotify.logAndNotifyServerException(ee);
  }

  protected String getIdentity() {
    return isMYSQL ? "BIGINT NOT NULL AUTO_INCREMENT" : isPostgreSQL ? "SERIAL" : "IDENTITY";
  }

  protected String getVarchar() {
    return isMYSQL ? "VARCHAR(256)" : "VARCHAR";
  }

  protected String getPrimaryKey() { return getPrimaryKey(ID); }
  protected String getPrimaryKey(String col) { return isMYSQL ? "PRIMARY KEY (" + col +  "), " : ""; }


  /**
   * Does not seem to work with h2
   * @param connection
   * @param table
   * @return
   * @throws SQLException
   */
/*  protected int getNewNumColumns(Connection connection, String table) throws SQLException {
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);

    // Get result set meta data
    DatabaseMetaData md = connection.getMetaData();
    ResultSet columns = md.getColumns(null, null, table, null);
    System.out.println("Got " + md + " and " + columns);
    int numColumns = 0;
    while (columns.next()) numColumns++;
    System.out.println("Got " + numColumns);

    return numColumns;
  }*/

  /**
   * Does not seem to work with h2
   * @param connection
   * @param table
   * @param column
   * @return
   * @throws SQLException
   */
/*  protected boolean columnExists(Connection connection, String table, String column) throws SQLException {
    DatabaseMetaData md = connection.getMetaData();
    ResultSet columns = md.getColumns(null, null, table, column);
    System.out.println("Got " + md + " and " + columns);

    int numColumns = 0;
    while (columns.next()) numColumns++;
    System.out.println("Got " + numColumns);

    return numColumns == 1;
  }*/
}
