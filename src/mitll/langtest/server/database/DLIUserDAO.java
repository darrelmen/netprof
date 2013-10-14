package mitll.langtest.server.database;

import mitll.langtest.shared.DLIUser;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DLIUserDAO extends DAO {
  private static Logger logger = Logger.getLogger(DLIUserDAO.class);

  private static final String DLIUSERS = "dliusers";

  public DLIUserDAO(Database database) { super(database); }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @see mitll.langtest.server.database.DatabaseImpl#addDLIUser(mitll.langtest.shared.DLIUser)
   */
  public void addUser(DLIUser dliUser) throws Exception {
  //  try {

      // there are much better ways of doing this...
      logger.info("addUser :dliUser " +dliUser);

      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement(
          "INSERT INTO " +DLIUSERS+
            "(userid,weeksOfExperience,rilrLevel,rilrEstimating,lilrLevel,lilrEstimating,silrLevel,silrEstimating,wilrLevel,wilrEstimating) " +
          "VALUES(?,?,?,?,?,?,?,?,?,?);");
      int i = 1;
      statement.setLong(i++, dliUser.getUserID());
      statement.setInt(i++, dliUser.getWeeksOfExperience());
      statement.setString(i++, dliUser.getReading().getIlrLevel());
      statement.setBoolean(i++, dliUser.getReading().isEstimating());
      statement.setString(i++, dliUser.getListening().getIlrLevel());
      statement.setBoolean(i++, dliUser.getListening().isEstimating());
      statement.setString(i++, dliUser.getSpeaking().getIlrLevel());
      statement.setBoolean(i++, dliUser.getSpeaking().isEstimating());
      statement.setString(i++, dliUser.getWriting().getIlrLevel());
      statement.setBoolean(i++, dliUser.getWriting().isEstimating());

      int i1 = statement.executeUpdate();
      logger.debug("got " + i1);
      statement.close();
      database.closeConnection(connection);

        logger.debug("now " + getCount(DLIUSERS));
/*    } catch (Exception ee) {
      ee.printStackTrace();
    }*/
  }

  void createUserTable(Database database) throws Exception {
    Connection connection = database.getConnection();

    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists " +
      DLIUSERS +
      " (" +
      "userid LONG, " +
      "weeksOfExperience INT, " +
      "rilrLevel VARCHAR, " +
      "rilrEstimating BOOLEAN, " +
      "lilrLevel VARCHAR, " +
      "lilrEstimating BOOLEAN, " +
      "silrLevel VARCHAR, " +
      "silrEstimating BOOLEAN, " +
      "wilrLevel VARCHAR, " +
      "wilrEstimating BOOLEAN, " +
      "FOREIGN KEY(USERID) REFERENCES " +
      "USERS" +
      "(ID)" +
      ")");
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }

  void dropUserTable(Database database) throws Exception {
    System.err.println("----------- dropUserTable -------------------- ");
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement("drop TABLE " +DLIUSERS);
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }

  /**
   * Pulls the list of users out of the database.
   * @return
   */
  public List<DLIUser> getUsers() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement("SELECT * from " + DLIUSERS);
      int i;

      ResultSet rs = statement.executeQuery();
      List<DLIUser> users = new ArrayList<DLIUser>();

      while (rs.next()) {
        i = 1;
        users.add(new DLIUser(rs.getLong(i++), //id
          rs.getInt(i++), // age
          new DLIUser.ILRLevel(rs.getString(i++), rs.getBoolean(i++)), // exp
          new DLIUser.ILRLevel(rs.getString(i++), rs.getBoolean(i++)), // exp
          new DLIUser.ILRLevel(rs.getString(i++), rs.getBoolean(i++)), // exp
          new DLIUser.ILRLevel(rs.getString(i++), rs.getBoolean(i++))
        ));
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);

      return users;
    } catch (Exception ee) {
      logger.error("Got " + ee, ee);
    }
    return new ArrayList<DLIUser>();
  }
}