package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Result;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Does writing to the results table.
 * Reading, etc. happens in {@link ResultDAO} - might be a little confusing... :)
 */
public class AnswerDAO {
  private static Logger logger = Logger.getLogger(AnswerDAO.class);

  private final Database database;

  public AnswerDAO(Database database) {
    this.database = database;
  }

  /**
   *
   * @see DatabaseImpl#addAnswer(int, mitll.langtest.shared.Exercise, int, String)
   * @param userID
   * @param e
   * @param questionID
   * @param answer
   * @param audioFile
   * @param flq
   *@param spoken
   * @param audioType @see mitll.langtest.client.exercise.ExercisePanel#postAnswers(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.shared.Exercise)
   */
  public void addAnswer(int userID, Exercise e, int questionID, String answer, String audioFile,
                        boolean flq, boolean spoken, String audioType) {
    String plan = e.getPlan();
    String id = e.getID();
    addAnswer(database,userID, plan, id, questionID, answer, audioFile, true,  flq, spoken, audioType, 0);
  }

  /**
   * @see DatabaseImpl#isAnswerValid(int, mitll.langtest.shared.Exercise, int, Database)
   * @param userID
   * @param e
   * @param questionID
   * @param database
   * @return
   */
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @param database
   * @param userID
   * @param plan
   * @param id
   * @param questionID
   * @param answer
   * @param audioFile
   * @param valid
   * @param spoken
   */
  public void addAnswer(Database database, int userID, String plan, String id, int questionID, String answer,
                        String audioFile, boolean valid, boolean flq, boolean spoken, String audioType, int durationInMillis) {
    try {
      Connection connection = database.getConnection();
      addAnswerToTable(connection, userID, plan, id, questionID, answer, audioFile, valid,flq,spoken,audioType,durationInMillis);
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
   * Add a row to the table.
   * Each insert is marked with a timestamp.
   * This allows us to determine user completion rate.
   *
   * @param connection
   * @param userid
   * @param plan
   * @param id
   * @param questionID
   * @param answer
   * @param audioFile
   * @param valid
   * @throws java.sql.SQLException
   * @see #addAnswer
   */
  private void addAnswerToTable(Connection connection, int userid, String plan, String id, int questionID, String answer, String audioFile,
                                boolean valid, boolean flq, boolean spoken, String audioType, int durationInMillis) throws SQLException {
    PreparedStatement statement;
    statement = connection.prepareStatement("INSERT INTO results(" +
        "userid," +
        "plan," +
      Database.EXID +"," +
        "qid," +
        Database.TIME+"," +
        "answer," +
        "valid," +
        ResultDAO.FLQ +"," +
        ResultDAO.SPOKEN +"," +
        ResultDAO.AUDIO_TYPE +","+
        ResultDAO.DURATION+
        ") VALUES(?,?,?,?,?,?,?,?,?,?,?)");
    int i = 1;

    boolean isAudioAnswer = answer == null || answer.length() == 0;
    String x = isAudioAnswer ? audioFile : answer;

    statement.setInt(i++, userid);
    statement.setString(i++, plan);
    statement.setString(i++, id);
    statement.setInt(i++, questionID);
    statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
    statement.setString(i++, x);
    statement.setBoolean(i++, valid);
    statement.setBoolean(i++, flq);
    statement.setBoolean(i++, spoken);
    statement.setString(i++, audioType);
    statement.setInt(i, durationInMillis);

    //logger.info("valid is " +valid + " for " +statement);

    statement.executeUpdate();
    statement.close();
  }
}