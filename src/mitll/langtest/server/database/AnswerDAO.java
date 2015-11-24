/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database;

import mitll.langtest.shared.scoring.PretestScore;
import org.apache.log4j.Logger;

import java.sql.*;

/**
 * Does writing to the results table.
 * Reading, etc. happens in {@link ResultDAO} - might be a little confusing... :)
 */
public class AnswerDAO extends DAO {
  private static final Logger logger = Logger.getLogger(AnswerDAO.class);
  private static final String PLAN = "plan";
  private final ResultDAO resultDAO;

  public AnswerDAO(Database database, ResultDAO resultDAO) {
    super(database);
    this.resultDAO = resultDAO;
  }

  /**
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   */
  public long addAnswer(Database database, int userID, String id, int questionID, String answer,
                        String audioFile, boolean valid, String audioType, long durationInMillis,
                        boolean correct, float pronScore, String deviceType, String device, String scoreJson,
                        boolean withFlash, int processDur, int roundTripDur, String validity, double snr) {
    Connection connection = database.getConnection(this.getClass().toString());
    try {
      long then = System.currentTimeMillis();
      long newid = addAnswerToTable(connection, userID, id, questionID, answer, audioFile, valid,
          audioType, durationInMillis, correct, pronScore, deviceType, device, scoreJson, withFlash, processDur, roundTripDur,
          validity, snr);
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

  public static class AnswerInfo {
    int userid;
    String id;
    int questionID;
    String answer;
    String audioFile;
    boolean valid;
    String audioType;
    int durationInMillis;
    boolean correct;
    float pronScore;
    String deviceType;
    String device;
    String scoreJson;
    boolean withFlash;
    String validity;
    float snr;

    public AnswerInfo(int userid, String id, int questionID,
                      String answer, String audioFile,
                      boolean valid, String audioType, int durationInMillis,
                      boolean correct, float pronScore, String deviceType, String device, String scoreJson,
                      boolean withFlash, String validity, float snr) {
      this.userid = userid;
      this.id = id;
      this.questionID = questionID;
      this.answer = answer;
      this.audioFile = audioFile;
      this.valid = valid;
      this.audioType = audioType;
      this.durationInMillis = durationInMillis;
      this.correct = correct;
      this.pronScore = pronScore;
      this.deviceType = deviceType;
      this.device = device;
      this.scoreJson = scoreJson;
      this.withFlash = withFlash;
      this.validity = validity;
      this.snr = snr;

    }
  }

  /**
   * Add a row to the table.
   * Each insert is marked with a timestamp.
   * This allows us to determine user completion rate.
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
                                boolean withFlash, int processDur, int roundTripDur, String validity, double snr) throws SQLException {
    logger.debug("adding answer for exid #" + id + " correct " + correct + " score " + pronScore +
        " audio type " + audioType + " answer " + answer + " process " + processDur +

        " validity " + validity + " snr " + snr+

        " json " + scoreJson);

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
        ResultDAO.DEVICE_TYPE + "," +
        ResultDAO.DEVICE + "," +
        ResultDAO.SCORE_JSON + "," +
        ResultDAO.WITH_FLASH + "," +
        ResultDAO.PROCESS_DUR + "," +
        ResultDAO.ROUND_TRIP_DUR + "," +
        ResultDAO.VALIDITY + "," +
        ResultDAO.SNR +
        ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

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
    statement.setString(i++, deviceType);
    statement.setString(i++, device);
    statement.setString(i++, scoreJson);
    statement.setBoolean(i++, withFlash);
    statement.setInt(i++, processDur);
    statement.setInt(i++, roundTripDur);
    statement.setString(i++, validity);
    statement.setFloat(i++, (float) snr);

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
          ResultDAO.ROUND_TRIP_DUR + "='" + roundTrip + "' " +
          "WHERE id=" + resultID;
      PreparedStatement statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? didn't change the answer for " + resultID + " sql " + sql);
      }

      statement.close();
    } catch (Exception e) {
      logger.error("got " + e, e);
    } finally {
      database.closeConnection(connection);
    }
  }

  /**
   * @param id
   * @param processDur
   * @see mitll.langtest.server.LangTestDatabaseImpl#getPretestScore(int, long, String, String, int, int, boolean, String, boolean)
   * @see mitll.langtest.server.database.DatabaseImpl#rememberScore(long, PretestScore)
   */
  public void changeAnswer(long id, float score, int processDur, String json) {
    //logger.info("Setting id " + id + " score " + score + " process dur " + processDur + " json " + json);
    Connection connection = getConnection();
    try {
      String sql = "UPDATE " +
          "results " +
          "SET " +
          ResultDAO.PRON_SCORE +  "='" + score + "', " +
          ResultDAO.PROCESS_DUR + "='" + processDur + "', " +
          ResultDAO.SCORE_JSON +  "='" + json + "' " +
          "WHERE id=" + id;

      PreparedStatement statement = connection.prepareStatement(sql);
      if (statement.executeUpdate() == 0) {
        logger.error("huh? didn't change the answer for " + id + " sql " + sql);
      }

      statement.close();
    } catch (Exception e) {
      logger.error("got " + e, e);
    } finally {
      database.closeConnection(connection);
    }
  }

  protected Connection getConnection() {
    return database.getConnection(this.getClass().toString());
  }

  private String copyStringChar(String plan) {
    return new String(plan.toCharArray());
  }
}