package mitll.langtest.server.database;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
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
  private static Logger logger = Logger.getLogger(DAO.class);

  protected final Database database;

  public DAO(Database database) { this.database = database;  }

  protected int getNumColumns(Connection connection, String table) throws SQLException {
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);

    // Get result set meta data
    ResultSetMetaData rsmd = rs.getMetaData();
    int numColumns = rsmd.getColumnCount();
    rs.close();
    stmt.close();
    return numColumns;
  }

  protected Collection<String> getColumns(String table) throws SQLException {
    Set<String> columns = new HashSet<String>();
    try {
      Connection connection = database.getConnection();

      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);

      // Get result set meta data
      ResultSetMetaData rsmd = rs.getMetaData();
      for (int i = 1; i < rsmd.getColumnCount()+1; i++) {
        columns.add(rsmd.getColumnName(i).toLowerCase());
      }

      rs.close();
      stmt.close();
    } catch (Exception e) {
      logger.error("doing getColumns: got " +e,e);
    }
    //logger.info("table " +table + " has " +columns);
    return columns;
  }

  protected int getCount(String table) {
    try {
      Connection connection = database.getConnection();
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
    String sql = "DELETE FROM " + table +" WHERE " +
      idField +
      "=" + itemId +
      "";

    return doSqlOn(sql, table);
  }


  protected boolean remove(String table, String idField, String itemId) {
    String sql = "DELETE FROM " + table +" WHERE " +
      idField +
      "='" + itemId +
      "'";

    return doSqlOn(sql, table);
  }

  protected boolean doSqlOn(String sql, String table) {
    try {
      int before = getCount(table);

      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(sql);
      boolean changed = statement.executeUpdate() == 1;

      if (!changed) {
        logger.warn("doSqlOn : didn't alter row for " + table + " sql " + sql);
      }

      statement.close();
      database.closeConnection(connection);

      int count = getCount(table);
      logger.debug("now " + count + " in " + table);
      if (before - count != 1) {
        logger.warn("DAO.doSqlOn : there were " + before + " before for " + table);
      }

      return changed;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return false;
  }

  public Database getDatabase() { return database; }

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
