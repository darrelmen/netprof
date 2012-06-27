package mitll.langtest.server.database;

import mitll.langtest.shared.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ResultDAO {
  private final Database database;

  public ResultDAO(Database database) {
    this.database = database;
  }

  /**
   * Pulls the list of results out of the database.
   *
   * @return
   */
  public List<Result> getResults() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * from results;");

      ResultSet rs = statement.executeQuery();
      List<Result> results = new ArrayList<Result>();
      while (rs.next()) {
        int i = 1;
        rs.getInt(i++);
        long userID = rs.getLong(i++);
        String plan = rs.getString(i++);
        String exid = rs.getString(i++);
        int qid = rs.getInt(i++);
        Timestamp timestamp = rs.getTimestamp(i++);
        String answer = rs.getString(i++);
        boolean valid = rs.getBoolean(i++);
        results.add(new Result(userID, //id
          plan, // plan
          exid, // id
          qid, // qid
          answer, // answer
          //rs.getString(i++), // audioFile
          valid, // valid
          timestamp.getTime()
        ));
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);

      return results;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<Result>();
  }

  void dropResults(Database database) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("DROP TABLE if exists results");
      if (!statement.execute()) {
        System.err.println("couldn't create table?");
      }
      statement.close();
      database.closeConnection(connection);

    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  }
    }
  }

  private void showResults(Database database) throws Exception {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement("SELECT * FROM results order by " + Database.TIME);
    ResultSet rs = statement.executeQuery();
    int c = 0;
    while (rs.next()) {
      c++;
      int i = 1;
      if (false) {
        System.out.println(rs.getInt(i++) + "," + rs.getString(i++) + "," +
          rs.getString(i++) + "," +
          rs.getInt(i++) + "," +
          rs.getString(i++) + "," +
          rs.getString(i++) + "," +
          rs.getTimestamp(i++));
      }
    }
 //   System.out.println("now " + c + " answers");
    rs.close();
    statement.close();
    database.closeConnection(connection);
  }

  void createResultTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      "results (id IDENTITY, userid INT, plan VARCHAR, " +
      Database.EXID +" VARCHAR, " +
      "qid INT," +
      Database.TIME + " TIMESTAMP AS CURRENT_TIMESTAMP," +
      "answer CLOB," +
      //"audioFile VARCHAR, " +
      "valid BOOLEAN" +
      ")");
    statement.execute();
    statement.close();
  }
}