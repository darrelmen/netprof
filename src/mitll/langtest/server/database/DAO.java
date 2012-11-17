package mitll.langtest.server.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/16/12
 * Time: 2:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class DAO {
  protected final Database database;

  public DAO(Database database) {
    this.database = database;
  }

  protected int getNumColumns(Connection connection, String table) throws SQLException {
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);

    // Get result set meta data
    ResultSetMetaData rsmd = rs.getMetaData();
    int numColumns = rsmd.getColumnCount();
    stmt.close();
    return numColumns;
  }
}
