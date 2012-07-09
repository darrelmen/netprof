package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AnswerDAO {
  private final Database database;
  private static final boolean LOG_RESULTS = false;

  public AnswerDAO(Database database) {
    this.database = database;
  }

  /**
   * Creates the result table if it's not there.
   *
   * @param userID
   * @param e
   * @param questionID
   * @param answer
   * @param audioFile
   * @see mitll.langtest.client.ExercisePanel#postAnswers(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.UserFeedback, mitll.langtest.client.ExerciseController, mitll.langtest.shared.Exercise)
   */
  public void addAnswer(int userID, Exercise e, int questionID, String answer, String audioFile) {
    String plan = e.getPlan();
    String id = e.getID();
    addAnswer(userID, plan, id, questionID, answer, audioFile, true, database);
  }

  public boolean isAnswerValid(int userID, Exercise e, int questionID, Database database) {
    boolean val = false;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(
        "SELECT valid, " + Database.TIME +
        " FROM results " +
        "WHERE userid = ? AND plan = ? AND " +
          Database.EXID +
          " = ? AND qid = ? " +
        "order by " + Database.TIME+ " desc");

      statement.setInt(1,userID);
      statement.setString(2, e.getPlan());
      statement.setString(3, e.getID());
      statement.setInt(4, questionID);

      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        val = rs.getBoolean(1);
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    return val;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile(String, String, String, String, String)
   * @param userID
   * @param plan
   * @param id
   * @param questionID
   * @param answer
   * @param audioFile
   * @param valid
   * @param database
   */
  public void addAnswer(int userID, String plan, String id, int questionID, String answer, String audioFile, boolean valid, Database database) {
    try {
      Connection connection = database.getConnection();
      addAnswerToTable(userID, plan, id, questionID, answer, audioFile, connection, valid);
      database.closeConnection(connection);

 /*     if (LOG_RESULTS) { // true to see what is in the table
        try {
          database.showResults();
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }*/

    } catch (Exception ee) {
      ee.printStackTrace();
    }
  }

  /**
   *
   *
   * @param userid
   * @param plan
   * @param id
   * @param questionID
   * @param answer
   * @param audioFile
   * @param connection
   * @param valid
   * @throws java.sql.SQLException
   * @see #addAnswer
   */
  private void addAnswerToTable(int userid, String plan, String id, int questionID, String answer, String audioFile,
                                Connection connection, boolean valid) throws SQLException {
    PreparedStatement statement;
    statement = connection.prepareStatement("INSERT INTO results(userid,plan," +
      Database.EXID +
      ",qid,answer,valid) VALUES(?,?,?,?,?,?)");
    int i = 1;
    statement.setInt(i++, userid);
    statement.setString(i++, plan);
    statement.setString(i++, id);
    statement.setInt(i++, questionID);
    //System.err.println("got " + userid + ", " + plan +", "+ id +", " + questionID + ", " +answer + ", " +audioFile +", " + valid);

    boolean isAudioAnswer = answer == null || answer.length() == 0;
    String x = isAudioAnswer ? audioFile : answer;
    //  System.err.println("got " + answer + " and " + audioFile + " -> " + x);
    statement.setString(i++, x);
    //statement.setString(i++, audioFile);
    statement.setBoolean(i++, valid);
    statement.executeUpdate();
    statement.close();
  }
}