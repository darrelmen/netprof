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

package mitll.langtest.server.database.word;

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.result.ResultDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class WordDAO extends DAO implements IWordDAO {
  private static final Logger logger = LogManager.getLogger(WordDAO.class);

  public static final String WORD = "word";
  private static final String RID = "rid";
  private static final String SEQ = "seq";
  private static final String SCORE = "score";
  private static final String RID1 = "RID";
  private static final String ID = "ID";

  /**
   * @param database
   * @paramx
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public WordDAO(Database database) {
    super(database);
    try {
      createTable(database);
      createIndex(database, RID, WORD);
      Connection connection = database.getConnection(this.getClass().toString());
      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * Word – result id – word seq – score – uid
   * Do we care about start and end offsets into audio???
   *
   * @param database
   * @throws java.sql.SQLException
   */
  private void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        WORD +
        " (" +
        "ID IDENTITY, " +
        RID + " BIGINT, " +
        WORD + " VARCHAR, " +
        "seq INT, " +
        SCORE + " FLOAT, " +

        "FOREIGN KEY(" +
        RID +
        ") REFERENCES " +
        ResultDAO.RESULTS +
        "(ID)" +

        ")");

    finish(database, connection, statement);
  }

  /**
   * @return id of new row
   * @see mitll.langtest.server.database.DatabaseImpl#recordWordAndPhoneInfo
   */
  public long addWord(Word word) {
    Connection connection = getConnection();
    try {
      // there are much better ways of doing this...
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + WORD +
              "(" +
              RID + "," +
              WORD + "," +
              SEQ + "," +
              SCORE +
              ") " +
              "VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
      int i = 1;

      statement.setLong(i++, word.getRid());
      statement.setString(i++, word.getWord());
      statement.setInt(i++, word.getSeq());
      statement.setFloat(i++, word.getScore());

      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
        //    val = false;
      }

      long id = getGeneratedKey(statement);
      if (id == -1) {
        logger.error("huh? no key was generated?");
      }

      statement.close();
      return id;
    } catch (SQLException ee) {
      logger.error("trying to add event " + word + " got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
      //  val = false;
    } finally {
      database.closeConnection(connection);
    }
    return 0;
  }

  /**
   * @see DatabaseImpl#putBackWordAndPhone
   * @return
   */
  public boolean removeWords(long resultid) {
    Connection connection = getConnection();
    boolean val = true;
    try {
      PreparedStatement statement = connection.prepareStatement(
          "DELETE FROM " + WORD +
              " WHERE " +
              RID + "="+resultid
              );
  //    int i = 1;
//      statement.setLong(i++, resultid);
      int j = statement.executeUpdate();

      if (j == 0) {
        logger.error("huh? didn't remove rows for " + resultid + " got " + j);// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
        val = false;
      }
      statement.close();

    } catch (SQLException ee) {
      logger.error("trying to drop words for " + resultid + " got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
      val = false;
    } finally {
      database.closeConnection(connection);
    }
    return val;
  }

  public List<Word> getAll() {
    try {
      return getWords("SELECT * from " + WORD);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
    }
    return Collections.emptyList();
  }

  private List<Word> getWords(String sql) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<Word> lists = new ArrayList<Word>();

    while (rs.next()) {
      lists.add(new Word(
              rs.getLong(ID),
              rs.getLong(RID1),
              rs.getString(WORD),
              rs.getInt(SEQ),
              rs.getFloat(SCORE)
          )
      );
    }

    finish(connection, statement, rs);
    return lists;
  }
}