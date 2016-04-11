/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database;

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.scoring.AudioContext;
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
   * TODO : consider moving device type through...
   *
   * @param userID
   * @param exerciseID
   * @param questionID
   * @param answer
   * @param answerType
   * @param correct
   * @param pronScore
   * @param classifierScore
   * @param session
   * @param timeSpent
   * @see mitll.langtest.server.LangTestDatabaseImpl#getScoreForAnswer
   * @see mitll.langtest.client.amas.TextResponse#getScoreForGuess
   */
  public long addTextAnswer(AudioContext audioContext,
                            //int userID,
                            // String exerciseID, int questionID,

                            String answer,
//                            String answerType,

                            boolean correct,

                            float pronScore,

                            float classifierScore,

                            String session, long timeSpent) {
    AnswerInfo answerInfo = new AnswerInfo(
        audioContext,
        new AnswerInfo.RecordingInfo(answer, answer, "", "", true),
        new AudioCheck.ValidityAndDur(0));

    return addAnswer(database,
        new AnswerInfo(answerInfo, new AnswerInfo.ScoreInfo(correct, pronScore, "", 0)));
  }

  /**
   * @param database
   * @param answerInfo
   * @return id of new row in result table
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   */
  public long addAnswer(Database database,
                        AnswerInfo answerInfo) {
    Connection connection = database.getConnection(this.getClass().toString());
    try {
      long then = System.currentTimeMillis();
      long newid = addAnswerToTable(connection, answerInfo);
      long now = System.currentTimeMillis();
      if (now - then > 100) logger.debug("took " + (now - then) + " millis to record answer.");
      return newid;
    } catch (Exception ee) {
      logger.error("addAnswer got " + ee, ee);
    } finally {
      database.closeConnection(connection);
    }
    return -1;
  }

  public long addResultToTable(MonitorResult info) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());

    logger.debug("addAnswerToTable : adding answer for " + info);

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

 //   boolean isAudioAnswer = info.getAnswer() == null || info.getAnswer().length() == 0;

    statement.setInt(i++, (int)info.getUserid());
    statement.setString(i++, PLAN); // obsolete
    statement.setString(i++, copyStringChar(info.getId()));
    statement.setInt(i++, 1);
    statement.setTimestamp(i++, new Timestamp(info.getTimestamp()));
    statement.setString(i++, copyStringChar(info.getAnswer()));
    statement.setBoolean(i++, info.isValid());
    statement.setBoolean(i++, true); // obsolete
    statement.setBoolean(i++, true); // obsolete
    statement.setString(i++, copyStringChar(info.getAudioType()));
    statement.setInt(i++, (int) info.getDurationInMillis());

    statement.setBoolean(i++, info.isCorrect());
    statement.setFloat(i++, info.getPronScore());
/*    statement.setString(i++, info.getDeviceType());
    statement.setString(i++, info.getSimpleDevice());
    statement.setString(i++, info.getScoreJSON());*/
    statement.setBoolean(i++, info.isWithFlash());
    statement.setInt(i++, info.getProcessDur());
    statement.setInt(i++, info.getRoundTripDur()); // always zero?
    statement.setString(i++, info.getValidity());
    statement.setFloat(i++, info.getSnr());

//    resultDAO.invalidateCachedResults();

    statement.executeUpdate();

    long newID = getGeneratedKey(statement);

    statement.close();

    return newID;
  }

  /**
   * Add a row to the table.
   * Each insert is marked with a timestamp.
   * This allows us to determine user completion rate.
   *
   * @param connection
   * @param info
   * @throws java.sql.SQLException
   * @see #addAnswer
   */
  private long addAnswerToTable(Connection connection, AnswerInfo info) throws SQLException {
    logger.debug("addAnswerToTable : adding answer for " + info);

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

    boolean isAudioAnswer = info.getAnswer() == null || info.getAnswer().length() == 0;
    String answerInserted = isAudioAnswer ? info.getAudioFile() : info.getAnswer();

    statement.setInt(i++, info.getUserid());
    statement.setString(i++, PLAN); // obsolete
    statement.setString(i++, copyStringChar(info.getId()));
    statement.setInt(i++, info.getQuestionID());
    statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
    statement.setString(i++, copyStringChar(answerInserted));
    statement.setBoolean(i++, info.isValid());
    statement.setBoolean(i++, true); // obsolete
    statement.setBoolean(i++, true); // obsolete
    statement.setString(i++, copyStringChar(info.getAudioType()));
    statement.setInt(i++, (int) info.getDurationInMillis());

    statement.setBoolean(i++, info.isCorrect());
    statement.setFloat(i++, info.getPronScore());
    statement.setString(i++, info.getDeviceType());
    statement.setString(i++, info.getDevice());
    statement.setString(i++, info.getScoreJson());
    statement.setBoolean(i++, info.isWithFlash());
    statement.setInt(i++, info.getProcessDur());
    statement.setInt(i++, info.getRoundTripDur()); // always zero?
    statement.setString(i++, info.getValidity());
    statement.setFloat(i++, (float) info.getSnr());

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

  public void addUserScore(long id, float score) {
    changeScore(id, score, ResultDAO.USER_SCORE);
  }

  private void changeScore(long id, float score, String scoreColumn) {
    Connection connection = getConnection();
    // logger.debug("changing " +scoreColumn + " for " +id + " to " + score);
    try {
      String sql = "UPDATE results " +
          "SET " +
          scoreColumn + "='" + score + "' " +
          "WHERE id=" + id;
      PreparedStatement statement = connection.prepareStatement(sql);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? didn't change the answer for " + id + " sql " + sql);
      }

      statement.close();
      resultDAO.invalidateCachedResults();
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
          ResultDAO.PRON_SCORE + "='" + score + "', " +
          ResultDAO.PROCESS_DUR + "='" + processDur + "', " +
          ResultDAO.SCORE_JSON + "='" + json + "' " +
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