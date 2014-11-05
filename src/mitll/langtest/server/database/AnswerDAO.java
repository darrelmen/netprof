package mitll.langtest.server.database;

import mitll.langtest.shared.CommonExercise;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Does writing to the results table.
 * Reading, etc. happens in {@link ResultDAO} - might be a little confusing... :)
 */
public class AnswerDAO extends DAO {
  private static final Logger logger = Logger.getLogger(AnswerDAO.class);
  //public static final String AVP_SKIP = "avp_skip";

  private final ResultDAO resultDAO;

  public AnswerDAO(Database database, ResultDAO resultDAO) {
    super(database);
    this.resultDAO = resultDAO;
  }

  /**
   *
   * @see DatabaseImpl#addAnswer
   * @param userID
   * @param e
   * @param questionID
   * @param answer
   * @param audioFile
   * @param audioType
   * @param correct
   * @param pronScore
   * @param deviceType
   * @param device
   * @see mitll.langtest.client.exercise.PostAnswerProvider#postAnswers
   */
/*
  public void addAnswer(int userID, CommonExercise e, int questionID, String answer, String audioFile,
                        String audioType, boolean correct, float pronScore, String deviceType, String device) {
    String id = e.getID();
    addAnswer(database,userID, id, questionID, answer, audioFile, true, audioType, 0, correct, pronScore, deviceType, device, scoreJson, recordedWithFlash);
  }
*/

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoreForAnswer
   * @param userID
   * @param plan
   * @param exerciseID
   * @param questionID
   * @param stimulus
   * @param answer
   * @param answerType
   * @param correct
   * @param pronScore
   */
/*  public void addAnswer(int userID, String plan, String exerciseID, int questionID, String stimulus, String answer,
                        String answerType, boolean correct, float pronScore) {
    addAnswer(database, userID, plan, exerciseID, questionID, answer, "", true, false, false, answerType, 0, correct, pronScore, stimulus);
  }*/

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @see #addAnswer(Database, int, String, int, String, String, boolean, String, int, boolean, float, String, String, String, boolean)
   * @param database
   * @param userID
   * @param id
   * @param questionID
   * @param answer
   * @param audioFile
   * @param valid
   * @param correct
   * @param pronScore
   * @param deviceType
   * @param device
   * @param scoreJson
   * @param withFlash
   *  @return id of new row in result table
   */
  public long addAnswer(Database database, int userID, String id, int questionID, String answer,
                        String audioFile, boolean valid, String audioType, int durationInMillis,
                        boolean correct, float pronScore, String deviceType, String device, String scoreJson, boolean withFlash) {
    Connection connection = database.getConnection(this.getClass().toString());
    try {
      long then = System.currentTimeMillis();
      long newid = addAnswerToTable(connection, userID, id, questionID, answer, audioFile, valid,
          audioType, durationInMillis, correct, pronScore, deviceType, device, scoreJson, withFlash);
      long now = System.currentTimeMillis();
      if (now - then > 100) System.out.println("took " + (now - then) + " millis to record answer.");
      return newid;

    } catch (Exception ee) {
      logger.error("addAnswer got " + ee, ee);
    } finally {
      database.closeConnection(connection);
    }
    return -1;
  }

  /**
   * Add a row to the table.
   * Each insert is marked with a timestamp.
   * This allows us to determine user completion rate.
   *
   *
   * @param connection
   * @param userid
   * @param id
   * @param questionID
   * @param answer
   * @param audioFile
   * @param valid
   * @param correct
   * @param pronScore
   * @param deviceType
   * @param device
   * @param scoreJson
   * @param withFlash
   * @throws java.sql.SQLException
   * @see #addAnswer(Database, int, String, int, String, String, boolean, String, int, boolean, float, String, String, String, boolean)
   */
  private long addAnswerToTable(Connection connection, int userid, String id, int questionID,
                                String answer, String audioFile,
                                boolean valid, String audioType, int durationInMillis,
                                boolean correct, float pronScore, String deviceType, String device, String scoreJson,
                                boolean withFlash) throws SQLException {
  //  logger.debug("adding answer for exid #" + id + " correct " + correct + " score " + pronScore + " audio type " +audioType + " answer " + answer);

    PreparedStatement statement = connection.prepareStatement("INSERT INTO " +
        ResultDAO.RESULTS +
        "(" +
      "userid," +
      "plan," +
      Database.EXID + "," +
      "qid," +
      Database.TIME + "," +
      "answer," +
      "valid," +
      ResultDAO.FLQ + "," +
      ResultDAO.SPOKEN + "," +
      ResultDAO.AUDIO_TYPE + "," +
      ResultDAO.DURATION + "," +
      ResultDAO.CORRECT + "," +
      ResultDAO.PRON_SCORE + "," +
        ResultDAO.STIMULUS +"," +
        ResultDAO.DEVICE_TYPE +"," +
        ResultDAO.DEVICE +"," +
        ResultDAO.SCORE_JSON +"," +
        ResultDAO.WITH_FLASH +
      ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

    int i = 1;

    boolean isAudioAnswer = answer == null || answer.length() == 0;
    String answerInserted = isAudioAnswer ? audioFile : answer;

    statement.setInt(i++, userid);
    statement.setString(i++, "plan"/*copyStringChar(plan)*/);
    statement.setString(i++, copyStringChar(id));
    statement.setInt(i++, questionID);
    statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
    statement.setString(i++, copyStringChar(answerInserted));
    statement.setBoolean(i++, valid);
    statement.setBoolean(i++, true);
    statement.setBoolean(i++, true);
    statement.setString(i++, copyStringChar(audioType));
    statement.setInt(i++, durationInMillis);

    statement.setBoolean(i++, correct);
    statement.setFloat(i++, pronScore);
    statement.setString(i++, "");
    statement.setString(i++, deviceType);
    statement.setString(i++, device);
    statement.setString(i++, scoreJson);
    statement.setBoolean(i++, withFlash);

    resultDAO.invalidateCachedResults();

    statement.executeUpdate();

    long newID = getGeneratedKey(statement);

    statement.close();

    return newID;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   * @param id
   */
  public void changeAnswer(long id, float score) {
    Connection connection = getConnection();
    try {

      String sql = "UPDATE results " +
        "SET " +
        ResultDAO.PRON_SCORE+"='" + score + "' " +
        "WHERE id=" + id;
      PreparedStatement statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? didn't change the answer for " + id + " sql " + sql);
      }

      statement.close();
    } catch (Exception e) {
      logger.error("got " +e,e);
    } finally {
      database.closeConnection(connection);
    }
  }

  protected Connection getConnection() {
    return database.getConnection(this.getClass().toString());
  }

  /**
   * @seex mitll.langtest.server.LangTestDatabaseImpl#setAVPSkip(java.util.Collection)
   * @see mitll.langtest.client.flashcard.StatsFlashcardFactory.StatsPracticePanel#getRepeatButton()
   * @paramx ids
   */
/*  public void changeType(Collection<Long> ids) {
    if (ids.isEmpty()) return;
    Connection connection = getConnection();
    try {
      String list = getInList(ids);

      String sql = "UPDATE " +
        ResultDAO.RESULTS +
        " " +
        "SET " +
        ResultDAO.AUDIO_TYPE + "='" + AVP_SKIP + "' " +
        " where " + ResultDAO.ID + " in (" +
        list + ") ";
      PreparedStatement statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? didn't update the results for " + ids + " sql " + sql);
      }
      else {
        logger.debug("Altered " + i + " rows, given " + ids.size() + " ids");
      }

      statement.close();
    } catch (Exception e) {
      logger.error("got " +e,e);
    } finally {
      database.closeConnection(connection);
    }
  }*/

/*  private String getInList(Collection<Long> ids) {
    StringBuilder b = new StringBuilder();
    for (Long id : ids) b.append(id).append(",");
    String list = b.toString();
    list = list.substring(0, Math.max(0, list.length() - 1));
    return list;
  }*/

  private String copyStringChar(String plan) { return new String(plan.toCharArray());  }
}