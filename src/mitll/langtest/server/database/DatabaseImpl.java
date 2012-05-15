package mitll.langtest.server.database;

import com.google.gwt.core.client.GWT;
import mitll.langtest.shared.Exercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServlet;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 5/14/12
 * Time: 11:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseImpl {
  private static final String ENCODING = "UTF8";
  private static final boolean DROP_CREATED_TABLES = false;

  // mysql config info
/*  private String url = "jdbc:mysql://localhost:3306/",
    dbOptions = "?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "com.mysql.jdbc.Driver";*/

  // h2 config info
  private String url = "jdbc:h2:new;IFEXISTS=TRUE",
    dbOptions = "",//"?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "org.h2.Driver";

  private HttpServlet servlet;
  private String plan;

  public DatabaseImpl(HttpServlet s) { this.servlet = s; }


  /**
   * Hit the database for the exercises
   * @return
   */
  public List<Exercise> getExercises() {
    List<Exercise> exercises = new ArrayList<Exercise>();
    Connection connection = null;

    try {
      // connection = this.dbLogin();
      connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM exercises");

      ResultSet rs = statement.executeQuery();
      while(rs.next()) {
        String plan = rs.getString(1);
        String exid = rs.getString(2);
        String exType = rs.getString(3);
        Clob clob = rs.getClob(4);

        InputStreamReader utf8 = new InputStreamReader(clob.getAsciiStream(), ENCODING);
        BufferedReader br = new BufferedReader(utf8);
        int c= 0;
        char cbuf[] = new char[1024];
        StringBuilder b = new StringBuilder();
        while((c = br.read(cbuf)) != -1) {
          b.append(cbuf,0,c);
        }

        //  System.out.println("b " +b.toString());

        if (b.toString().startsWith("{")) {
          net.sf.json.JSONObject obj = net.sf.json.JSONObject.fromObject(b.toString());
          Exercise e = getExercise(plan,obj);
          exercises.add(e);
        }
      }
      rs.close();
      statement.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (connection != null) {
        /*   try {
          //connection.close();
        } catch (SQLException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
      }
    }
    return exercises;
  }




  /**
   * Parse the json that represents the exercise.  Created during ingest process (see ingest.scala).
   * @param obj
   * @return
   */
  private Exercise getExercise(String plan, JSONObject obj) {
    Exercise exercise = new Exercise(plan,(String) obj.get("exid"), (String) obj.get("content"));
    Collection<JSONObject> qa = JSONArray.toCollection((JSONArray) obj.get("qa"), JSONObject.class);
    for (JSONObject o : qa) {
      Set<String> keys = o.keySet();
      for (String k : keys) {
        JSONObject qaForLang = (JSONObject)o.get(k);
        exercise.addQuestion(k, (String) qaForLang.get("question"), (String) qaForLang.get("answerKey"));
      }
    }
    return exercise;
  }

  /**
   * Not necessary if we use the h2 DBStarter service -- see web.xml reference
   * @return
   * @throws Exception
   */
  private Connection dbLogin() throws Exception {
    try {
      Class.forName(driver).newInstance();
      System.out.println("connecting to " + url);

      GWT.log("connecting to " + url);
      File f = new java.io.File("new.h2.db");
      if (!f.exists()) {
        String s = "huh? no file at " + f.getAbsolutePath();
        System.err.println(s);

        GWT.log(s);
      }
      Connection connection = DriverManager.getConnection(url
        + /*getInitParameter(PRETEST_DATABASE) +*/ dbOptions/*,
        "",
        ""*/);
      connection.setAutoCommit(false);
      boolean closed = connection.isClosed();
      System.out.println("closed " + closed);
      return connection;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  private Connection getConnection() throws Exception {
    try {
      return (Connection)servlet.getServletContext().getAttribute("connection");
    } catch (Exception e) {  // for standalone testing
      return this.dbLogin();
    }
  }

  public int addUser(int age, String gender) {
    try {
      Connection connection = getConnection();
      PreparedStatement statement;
      if (DROP_CREATED_TABLES) {
        statement = connection.prepareStatement("drop TABLE users");
        statement.execute();
        statement.close();
      }

      statement = connection.prepareStatement( "CREATE TABLE if not exists users (id INT AUTO_INCREMENT, " +
        "age INT, gender INT, experience INT, password VARCHAR)");
      statement.execute();
      statement.close();

       System.out.println("adding " +age + " and " + gender);
      statement = connection.prepareStatement("INSERT INTO users(age,gender) VALUES(?,?)");
      int i = 1;
      //statement.setString(i++,plan);
     // statement.setString(i++,id);
      statement.setInt(i++, age);
      statement.setInt(i++, gender.equalsIgnoreCase("male") ? 0 : 1);
     // statement.setString(i++, audioFile);
      statement.executeUpdate();
      ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN
      //statement.close();

      int id = 0;
     //  statement = getConnection().prepareStatement("SELECT max(id) FROM users order by timestamp");
       // ResultSet rs = statement.executeQuery();
        while (rs.next()) {
          id = rs.getInt(1);
          System.out.println("addUser got user #" +id);
          //  System.out.println(rs.getString(1) + "," + rs.getString(2) + "," + rs.getInt(3) + "," + rs.getString(4) + "," + rs.getTimestamp(5));
        }
        rs.close();
        statement.close();
        return id;
    } catch (Exception ee) {
      ee.printStackTrace();
    }
    return 0;
  }

  /**
   * Creates the result table if it's not there.
   *
   * TODO : add user
   * @see mitll.langtest.client.ExercisePanel#postAnswers(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.UserFeedback, mitll.langtest.client.ExerciseController, mitll.langtest.shared.Exercise)
   * @param e
   * @param questionID
   * @param answer
   * @param audioFile
   */
  public void addAnswer(Exercise e, int questionID, String answer, String audioFile) {
    String ip = "";//servlet.getThreadLocalRequest().getRemoteAddr();
    System.out.println("Got " +e + " and " + questionID + " and " + answer + " at " +ip);


    if (DROP_CREATED_TABLES) {
      dropResults();
    }

    String plan = e.getPlan();
    String id = e.getID();
    addAnswer(plan, id, questionID, answer, audioFile);
  }

  public void addAnswer(String plan, String id, int questionID, String answer, String audioFile) {
    try {
      Connection connection = getConnection();
      createResultTable(connection);

      addAnswerToTable(plan, id, questionID, answer, audioFile, connection);

      if (true) { // true to see what is in the table
        try {
          showResults();
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    } catch (Exception ee) {
      ee.printStackTrace();
    }
  }

  private void dropResults() {
    try {
      Connection connection = getConnection();
      PreparedStatement statement = connection.prepareStatement("DROP TABLE if exists results");
      if (!statement.execute()) {
        System.err.println("couldn't create table?");
      }
      statement.close();

    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  }
    }
  }



  public void addUser() {
    String sql = "CREATE TABLE if not exists users (id INT AUTO_INCREMENT, " +
      "age INT, gender INT, experience INT, password VARCHAR, CONSTRAINT pkusers PRIMARY KEY (id))";
  }

  /**
   * TODO : add user
   * @see #addAnswer
   * @param plan
   * @param id
   * @param questionID
   * @param answer
   * @param connection
   * @throws SQLException
   */
  private void addAnswerToTable(String plan, String id, int questionID, String answer, String audioFile, Connection connection) throws SQLException {
    if (DROP_CREATED_TABLES) {
      dropResults();
    }
	  createResultTable(connection);

    PreparedStatement statement;
    statement = connection.prepareStatement("INSERT INTO results(plan,id,qid,answer,audioFile) VALUES(?,?, ?, ?,?)");
    int i = 1;
    statement.setString(i++,plan);
    statement.setString(i++,id);
    statement.setInt(i++, questionID);
    statement.setString(i++, answer);
    statement.setString(i++, audioFile);
    statement.executeUpdate();
    statement.close();
  }

  private void showResults() throws Exception {
    PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM results order by timestamp");
    ResultSet rs = statement.executeQuery();
    while (rs.next()) {
      int i = 1;
      System.out.println(rs.getInt(i++) +","+rs.getString(i++) + "," +
        rs.getString(i++) + "," +
        rs.getInt(i++) + "," +
        rs.getString(i++) + "," +
        rs.getString(i++) + "," +
        rs.getTimestamp(i++));
    }
    rs.close();
    statement.close();
  }

  private void createResultTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      "results (userid INT, plan VARCHAR, id VARCHAR, qid INT, answer VARCHAR, audioFile VARCHAR, timestamp TIMESTAMP AS CURRENT_TIMESTAMP)");
    statement.execute();
    statement.close();
  }

  public static void main(String[] arg) {
    DatabaseImpl langTestDatabase = new DatabaseImpl(null);
    int id = langTestDatabase.addUser(23,"male");
    System.out.println("id =" +id);
    for (Exercise e : langTestDatabase.getExercises()) System.err.println("e " + e);
 }
}
