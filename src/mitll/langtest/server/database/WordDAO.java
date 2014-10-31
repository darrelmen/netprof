package mitll.langtest.server.database;

import mitll.langtest.server.LogAndNotify;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class WordDAO extends DAO {
  private static final Logger logger = Logger.getLogger(WordDAO.class);

  public static final String WORD = "word";
//  private static final String WORDS = "Words";
  public static final String RID = "rid";
  public static final String SEQ = "seq";
  public static final String SCORE = "score";
//  private final UserDAO userDAO;
  private LogAndNotify logAndNotify;

  /**
   * @param database
   * @paramx
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public WordDAO(Database database, LogAndNotify logAndNotify) {
    super(database);
    this.logAndNotify = logAndNotify;
    try {
      createTable(database);
      createIndex(database, RID, WORD);
      Connection connection = database.getConnection(this.getClass().toString());
      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  public static class Word {
    long id;
    long rid;
    String word;
    int seq;
    float score;

    public Word(long id, long rid, String word, int seq, float score) {
      this(rid,word,seq,score);
      this.id = id;
    }

    public Word(long rid, String word, int seq, float score) {

      this.rid = rid;
      this.word = word;
      this.seq = seq;
      this.score = score;
    }

    public String toString() {
      return "# " + id + " rid " + rid + " " + word + " at " + seq + " score " + score;
    }
  }

  /**
   * Word – result id – word seq – score – uid
   * Do we care about start and end offsets into audio???
   *
   * @param database
   * @throws java.sql.SQLException
   */
  void createTable(Database database) throws SQLException {
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
   */
  public long addWord(Word word) {
    Connection connection = getConnection();
    //boolean val = true;
    try {
      // there are much better ways of doing this...

      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + WORD +
              "(" +
              RID + "," +
              WORD + "," +
              SEQ + "," +
              SCORE +
              //"," +
              ") " +
              "VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
      int i = 1;

      statement.setLong(i++, word.rid);
      statement.setString(i++, word.word);
      statement.setInt(i++, word.seq);
      statement.setFloat(i++, word.score);

      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
    //    val = false;
      }

      long id = getGeneratedKey(statement);
      if (id == -1) {  logger.error("huh? no key was generated?");  }

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

  public List<Word> getAll() {
    try {
      return getWords("SELECT * from " + WORD);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
    }
    return Collections.emptyList();
  }

/*  public List<Event> getAllForUserAndExercise(long userid, String exid) {
    try {
      String sql = "SELECT * from " + WORD + " where " +
        WIDGETTYPE +
        "='qcPlayAudio' AND " +
        CREATORID +"="+userid + " and " +
        EXERCISEID + "='" +exid+
        "'";

      return getEvents(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return Collections.emptyList();
  }*/

  private List<Word> getWords(String sql) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<Word> lists = new ArrayList<Word>();

    while (rs.next()) {
      lists.add(new Word(
              rs.getLong("ID"),
              rs.getLong("RID"),
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