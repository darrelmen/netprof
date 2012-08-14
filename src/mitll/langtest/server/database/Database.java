package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 6/26/12
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Database {
  Connection getConnection() throws Exception;

  void closeConnection(Connection connection) throws SQLException;

  String TIME = "time";
  String EXID = "exid";
}
