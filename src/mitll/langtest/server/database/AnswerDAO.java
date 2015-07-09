package mitll.langtest.server.database;

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
  public static final String PLAN = "plan";
  //public static final String AVP_SKIP = "avp_skip";

  private final ResultDAO resultDAO;

  public AnswerDAO(Database database, ResultDAO resultDAO) {
    super(database);
    this.resultDAO = resultDAO;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
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
   * @param processDur
   * @param roundTripDur
   * @return id of new row in result table
   */
  public long addAnswer(Database database, int userID, String id, int questionID, String answer,
                        String audioFile, boolean valid, String audioType, long durationInMillis,
                        boolean correct, float pronScore, String deviceType, String device, String scoreJson,
                        boolean withFlash, int processDur, int roundTripDur) {
    Connection connection = database.getConnection(this.getClass().toString());
    try {
      long then = System.currentTimeMillis();
      long newid = addAnswerToTable(connection, userID, id, questionID, answer, audioFile, valid,
          audioType, durationInMillis, correct, pronScore, deviceType, device, scoreJson, withFlash, processDur, roundTripDur);
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
   * @param processDur
   * @param roundTripDur
   * @throws java.sql.SQLException
   * @see #addAnswer(Database, int, String, int, String, String, boolean, String, long, boolean, float, String, String, String, boolean, int, int)
   */
  private long addAnswerToTable(Connection connection, int userid, String id, int questionID,
                                String answer, String audioFile,
                                boolean valid, String audioType, long durationInMillis,
                                boolean correct, float pronScore, String deviceType, String device, String scoreJson,
                                boolean withFlash, int processDur, int roundTripDur) throws SQLException {
    logger.debug("adding answer for exid #" + id + " correct " + correct + " score " + pronScore + " audio type " +audioType + " answer " + answer + " process " + processDur);

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
        ResultDAO.STIMULUS + "," +
        ResultDAO.DEVICE_TYPE + "," +
        ResultDAO.DEVICE + "," +
        ResultDAO.SCORE_JSON + "," +
        ResultDAO.WITH_FLASH + ","+
        ResultDAO.PROCESS_DUR + ","+
        ResultDAO.ROUND_TRIP_DUR+
        ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

    int i = 1;

    boolean isAudioAnswer = answer == null || answer.length() == 0;
    String answerInserted = isAudioAnswer ? audioFile : answer;

    statement.setInt(i++, userid);
    statement.setString(i++, PLAN);
    statement.setString(i++, copyStringChar(id));
    statement.setInt(i++, questionID);
    statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
    statement.setString(i++, copyStringChar(answerInserted));
    statement.setBoolean(i++, valid);
    statement.setBoolean(i++, true);
    statement.setBoolean(i++, true);
    statement.setString(i++, copyStringChar(audioType));
    statement.setInt(i++, (int) durationInMillis);

    statement.setBoolean(i++, correct);
    statement.setFloat(i++, pronScore);
    statement.setString(i++, "");
    statement.setString(i++, deviceType);
    statement.setString(i++, device);
    statement.setString(i++, scoreJson);
    statement.setBoolean(i++, withFlash);
    statement.setInt(i++, processDur);
    statement.setInt(i++, roundTripDur);

    resultDAO.invalidateCachedResults();

    statement.executeUpdate();

    long newID = getGeneratedKey(statement);

    statement.close();

    return newID;
  }

  public void addRoundTrip(long resultID, int roundTrip) {
    Connection connection = getConnection();
    try {
      String sql = "UPDATE " +
          "results" +
          " " +
          "SET " +
          ResultDAO.ROUND_TRIP_DUR+"='" + roundTrip + "' " +
          "WHERE id=" + resultID;
      PreparedStatement statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? didn't change the answer for " + resultID + " sql " + sql);
      }

      statement.close();
    } catch (Exception e) {
      logger.error("got " +e,e);
    } finally {
      database.closeConnection(connection);
    }
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#getASRScoreForAudio
   * @param id
   * @param processDur
   */
  public void changeAnswer(long id, float score, int processDur) {
    Connection connection = getConnection();
    try {
      String sql = "UPDATE " +
          "results" +
          " " +
        "SET " +
          ResultDAO.PRON_SCORE+"='" + score + "', " +
          ResultDAO.PROCESS_DUR+"='" + processDur + "' " +
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