/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.result;

import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

/**
 * Does writing to the results table.
 * Reading, etc. happens in {@link ResultDAO} - might be a little confusing... :)
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class AnswerDAO extends BaseAnswerDAO implements IAnswerDAO {
  private static final Logger logger = LogManager.getLogger(AnswerDAO.class);
  private static final String PLAN = "plan";

  public AnswerDAO(Database database) { super(database);  }

  /**
   * @param answerInfo
   * @param timestamp
   * @return id of new row in result table
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   */
  @Override
  public int addAnswer(AnswerInfo answerInfo, long timestamp) {
    Connection connection = database.getConnection(this.getClass().toString());
    try {
      long then = System.currentTimeMillis();
      long newid = addAnswerToTable(connection, answerInfo);
      long now = System.currentTimeMillis();
      if (now - then > 100) {
        logger.debug(getLanguage() + " : took " + (now - then) + " millis to record answer for " + answerInfo + " and was given id " + newid);
      }
      return (int)newid;
    } catch (Exception ee) {
      logger.error("addAnswer got " + ee, ee);
    } finally {
      database.closeConnection(connection);
    }
    return -1;
  }

  /**
   * JUST for MergeSites.
   * TODO : remove
   * @param info
   * @return
   * @throws SQLException
   */
/*  public long addResultToTable(MonitorResult info) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());

    logger.debug("addResultToTable : adding answer for monitor result " + info);

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

    statement.setInt(i++, (int) info.getUserid());
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
*//*    statement.setString(i++, info.getDeviceType());
    statement.setString(i++, info.getSimpleDevice());
    statement.setString(i++, info.getScoreJSON());*//*
    statement.setBoolean(i++, info.isWithFlash());
    statement.setInt(i++, info.getProcessDur());
    statement.setInt(i++, info.getRoundTripDur()); // always zero?
    statement.setString(i++, info.getValidity());
    statement.setFloat(i++, info.getSnr());

    statement.executeUpdate();

    long newID = getGeneratedKey(statement);

    statement.close();

    return newID;
  }*/

  /**
   * Need to protect call to get generated keys.
   * <p>
   * Add a row to the table.
   * Each insert is marked with a timestamp.
   * This allows us to determine user completion rate.
   *
   * @param connection
   * @param info
   * @throws java.sql.SQLException
   * @see BaseAnswerDAO#addAnswer
   */
  private long addAnswerToTable(Connection connection, AnswerInfo info) throws SQLException {
    long newID = -1;
    synchronized (this) {
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
      statement.setString(i++, ""+info.getId());
      statement.setInt(i++, info.getQuestionID());
      statement.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
      statement.setString(i++, copyStringChar(answerInserted));
      statement.setBoolean(i++, info.isValid());
      statement.setBoolean(i++, true); // obsolete
      statement.setBoolean(i++, true); // obsolete
      statement.setString(i++, copyStringChar(info.getAudioType().toString()));
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

      statement.executeUpdate();

      newID = getGeneratedKey(statement);

      connection.commit();
      statement.close();
//      long now = System.currentTimeMillis();
//      logger.debug(getLanguage() + " : END   : addAnswerToTable : adding answer for (" + (now - then) +
//          ") millis : " + info + " result id " + newID);
    }

//    resultDAO.invalidateCachedResults();

    return newID;
  }

  @Override
  public void addRoundTrip(int resultID, int roundTrip) {
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

  @Override
  public void addUserScore(int id, float score) {
    changeScore(id, score, ResultDAO.USER_SCORE);
  }

  private void changeScore(int id, float score, String scoreColumn) {
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
      //resultDAO.invalidateCachedResults();
    } catch (Exception e) {
      logger.error("got " + e, e);
    } finally {
      database.closeConnection(connection);
    }
  }

  /**
   * @param id
   * @param processDur
   * @param isCorrect
   * @seex mitll.langtest.server.LangTestDatabaseImpl#getPretestScore
   * @see mitll.langtest.server.database.DatabaseImpl#rememberScore(int, PretestScore, boolean)
   */
  @Override
  public void changeAnswer(int id, float score, int processDur, String json, boolean isCorrect) {
    //logger.info("Setting id " + id + " score " + score + " process dur " + processDur + " json " + json);
    Connection connection = getConnection();
    try {
      String currentModel = database.getServerProps().getCurrentModel();
      String sql = "UPDATE " +
          "results " +
          "SET " +
          ResultDAO.PRON_SCORE + "='" + score + "', " +
          ResultDAO.PROCESS_DUR + "='" + processDur + "', " +
          ResultDAO.CORRECT + "=" + isCorrect + ", " +
          ResultDAO.MODELUPDATE + "='" + new Timestamp(System.currentTimeMillis()) + "', " +
          ResultDAO.MODEL + "='" + currentModel + "', " +
          ResultDAO.SCORE_JSON + "='" + json + "' " +

          "WHERE id=" + id;

      PreparedStatement statement = connection.prepareStatement(sql);

      if (statement.executeUpdate() == 0) {
        logger.error("huh? didn't change the answer for " + id + " sql " + sql);
      }

      statement.close();
    } catch (Exception e) {
      logger.error("changeAnswer got " + e, e);
    } finally {
      database.closeConnection(connection);
    }
  }

  @Override
  public void updateUser(int oldUser, int newUser) {

  }

  private String copyStringChar(String plan) {
    return new String(plan.toCharArray());
  }
}