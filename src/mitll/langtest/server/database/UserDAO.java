package mitll.langtest.server.database;

import mitll.langtest.shared.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
  private final Database database;

  public UserDAO(Database database) {
    this.database = database;
  }

  /**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * @param age
   * @param gender
   * @param experience
   * @param ipAddr
   * @return
   */
  public long addUser(int age, String gender, int experience, String ipAddr) {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      //System.out.println("adding " + age + " and " + gender + " and " + experience);
      statement = connection.prepareStatement("INSERT INTO users(age,gender,experience,ipaddr) VALUES(?,?,?,?);",
        Statement.RETURN_GENERATED_KEYS);
      int i = 1;
      statement.setInt(i++, age);
      statement.setInt(i++, gender.equalsIgnoreCase("male") ? 0 : 1);
      statement.setInt(i++, experience);
      statement.setString(i++, ipAddr);
      statement.executeUpdate();

      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN

      long id = 0;
      while (rs.next()) {
        id = rs.getLong(1);
        //System.out.println("Database : addUser got user #" + id);
        //  System.out.println(rs.getString(1) + "," + rs.getString(2) + "," + rs.getInt(3) + "," + rs.getString(4) + "," + rs.getTimestamp(5));
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);

      return id;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return 0;
  }

  void createUserTable(Database database) throws Exception {
    Connection connection = database.getConnection();

    PreparedStatement statement;

    statement = connection.prepareStatement("CREATE TABLE if not exists users (id IDENTITY, " +
      "age INT, gender INT, experience INT, ipaddr VARCHAR, password VARCHAR, timestamp TIMESTAMP AS CURRENT_TIMESTAMP, CONSTRAINT pkusers PRIMARY KEY (id))");
    statement.execute();
    statement.close();
    database.closeConnection(connection);

  }

  void dropUserTable(Database database) throws Exception {
    System.err.println("----------- dropUserTable -------------------- ");
    Connection connection = database.getConnection();
    PreparedStatement statement;
    statement = connection.prepareStatement("drop TABLE users");
    statement.execute();
    statement.close();
    database.closeConnection(connection);
  }

  /**
   * Pulls the list of users out of the database.
   * @return
   * @paramx database
   */
  public List<User> getUsers() {
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement;

      statement = connection.prepareStatement("SELECT * from users;");
      int i;

      ResultSet rs = statement.executeQuery();
      List<User> users = new ArrayList<User>();
      while (rs.next()) {
    	  i = 1;
        Timestamp timestamp;
        if (rs.getMetaData().getColumnCount() == 7) { // if we have a timestamp column --
          timestamp = rs.getTimestamp(i++);
        }
        else { // Wade's db schema doesn't have a timestamp column, currently
          timestamp = new Timestamp(System.currentTimeMillis());
        }
        users.add(new User(rs.getLong(i++), //id
          rs.getInt(i++), // age
          rs.getInt(i++), //gender
          rs.getInt(i++), // exp
          rs.getString(i++), // ip
          rs.getString(i++), // password
          timestamp.getTime()
        ));
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);

      return users;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return new ArrayList<User>();
  }
}