package mitll.langtest.server.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.Result;

import org.apache.log4j.Logger;

/**
 * Create, drop, alter, read from the results table.
 * Note that writing to the table takes place in the {@link mitll.langtest.server.database.AnswerDAO}. Not sure if that's a good idea or not. :)
 */
public class AudioDAO extends DAO {
  private static final Logger logger = Logger.getLogger(AudioDAO.class);

  public static final String ID = "id";
  private static final String USERID = "userid";
  private static final String AUDIO_REF = "audioRef";

  public static final String AUDIO = "audio";

  static final String AUDIO_TYPE = "audioType";
  static final String DURATION = "duration";
  static final String DEFECT = "defect";

  private final boolean debug = false;
//  private Map<Long, MiniUser> miniUsers;
  private final Connection connection;
  UserDAO userDAO;

  public AudioDAO(Database database, UserDAO userDAO) {
    super(database);
    connection = database.getConnection();

    this.userDAO = userDAO;
  //  miniUsers = userDAO.getMiniUsers();
    try {
      createTable(database.getConnection());
    } catch (SQLException e) {
      logger.error("Got " +e,e);
    }
    if (!getColumns(AUDIO).contains(DEFECT)) {
      try {
        addBoolean(connection,AUDIO,DEFECT);
      } catch (SQLException e) {
        logger.error("got "+e,e);
      }
    }
  }

  @Override
  protected void addBoolean(Connection connection, String table, String col) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " +
      table + " ADD " + col + " BOOLEAN DEFAULT FALSE ");
    statement.execute();
    statement.close();
  }

  public Map<String, List<AudioAttribute>> getExToAudio() {
    Map<String, List<AudioAttribute>> exToAudio = new HashMap<String, List<AudioAttribute>>();
    for (AudioAttribute audio : getAudioAttributes()) {
      List<AudioAttribute> audioAttributes = exToAudio.get(audio.getExid());
      if (audioAttributes == null) exToAudio.put(audio.getExid(), audioAttributes = new ArrayList<AudioAttribute>());
      audioAttributes.add(audio);
    }
    return exToAudio;
  }
  /**
   * Pulls the list of results out of the database.
   *
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getResultsWithGrades()
   */
  private List<AudioAttribute> getAudioAttributes() {
    try {
      String sql = "SELECT * FROM " + AUDIO + " WHERE " +DEFECT +"=false";
      return getResultsSQL(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<AudioAttribute>();
  }

  public List<AudioAttribute> getAudioAttributes(String exid) {
    try {
      String sql = "SELECT * FROM " + AUDIO + " WHERE " +Database.EXID +"='" + exid+ "' AND "+DEFECT +"=false";
      return getResultsSQL(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<AudioAttribute>();
  }

  public Set<String> getRecordedForUser(long userid) {
    try {
      String sql = "SELECT " +Database.EXID+
        " FROM " + AUDIO + " WHERE " +USERID +"=" + userid+ " AND "+DEFECT +"<>true";
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(sql);
      Set<String> exidResultsForQuery = getExidResultsForQuery(connection, statement);
     // logger.debug("sql " + sql + " ids " + exidResultsForQuery.size() + " ");
      return exidResultsForQuery;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<String>();
  }

  private List<AudioAttribute> getResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    return getResultsForQuery(connection, statement);
  }

  /**
   * Get a list of Results for this Query.
   *
   * @param connection
   * @param statement
   * @return
   * @throws java.sql.SQLException
   */
  private List<AudioAttribute> getResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<AudioAttribute> results = new ArrayList<AudioAttribute>();
    Map<Long, MiniUser> miniUsers = userDAO.getMiniUsers();
    while (rs.next()) {
     // int uniqueID = rs.getInt(ID);
      long userID = rs.getLong(USERID);
      String exid = rs.getString(Database.EXID);
      //int qid = rs.getInt(QID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String audioRef = rs.getString(AUDIO_REF);

      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);

      MiniUser user = miniUsers.get(userID);
      AudioAttribute audioAttr = new AudioAttribute(userID, //id
        exid, // id
        // exid, // id
        // qid == 1, // qid
        audioRef, // answer
        timestamp.getTime(),
        dur, type,
        user);
      if (user == null) {
        logger.error("can't find user " + userID+ " for " + audioAttr + " in " + miniUsers.keySet());
      }
      //if (!audioRef.contains("bestAudio"))  logger.debug("audio ref " + audioRef);

     // trimPathForWebPage(audioAttr);
      results.add(audioAttr);
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return results;
  }

  private Set<String> getExidResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    Set<String> results = new HashSet<String>();
    while (rs.next()) {
      String exid = rs.getString(Database.EXID);
      results.add(exid);
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return results;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#copyAudio(java.util.Map, java.util.Map, AudioDAO)
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   */
  public long add(Result result, int userid, String path) {
    try {
      long then = System.currentTimeMillis();
    //  logger.debug("path was " + result.answer + " now '" + audioRef + "'");
      if (userid <1) {
        logger.error("huh? userid is " +userid);
        new Exception().printStackTrace();
      }

      long newid = add(connection, result, userid, path);
      database.closeConnection(connection);
      long now = System.currentTimeMillis();
      if (now - then > 100) System.out.println("took " + (now - then) + " millis to record answer.");
      return newid;

    } catch (Exception ee) {
      logger.error("addAnswer got " + ee, ee);
    }
    return -1;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @param userid
   * @param audioRef
   * @param exerciseID
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @return
   */
  public long add(int userid, String audioRef, String exerciseID, long timestamp, String audioType, long durationInMillis) {
    try {
      long then = System.currentTimeMillis();

      if (userid <1) {
        logger.error("huh? userid is " +userid);
        new Exception().printStackTrace();
      }

      //  logger.debug("path was " + result.answer + " now '" + audioRef + "'");
      long newid = addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis);
      database.closeConnection(connection);
      long now = System.currentTimeMillis();
      if (now - then > 100) System.out.println("took " + (now - then) + " millis to record answer.");
      return newid;

    } catch (Exception ee) {
      logger.error("addAnswer got " + ee, ee);
    }
    return -1;
  }

  /**
   * Add a row to the table.
   * Each insert is marked with a timestamp.
   * This allows us to determine user completion rate.
   *
   *
   *   "id IDENTITY, " +
   "userid INT, " +
   Database.EXID + " VARCHAR, " +
   Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
   "audioRef CLOB," +
   AUDIO_TYPE + " VARCHAR," +
   DURATION + " INT" +
   *
   * @param connection
   * @throws java.sql.SQLException
   */
  private long add(Connection connection, Result result, int userid, String audioRef) throws SQLException {
    String exerciseID = result.id;
    long timestamp = result.timestamp;
    String audioType = result.getAudioType();
    int durationInMillis = result.durationInMillis;

    return addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis);
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile(String, String, String, int, int, int, boolean, String, boolean, boolean, boolean)
   * @param userid
   * @param audioRef
   * @param exerciseID
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @return
   */
  public int addOrUpdate(int userid, String audioRef, String exerciseID, long timestamp, String audioType, int durationInMillis) {
    if (userid <1) {
      logger.error("huh? userid is " +userid);
      new Exception().printStackTrace();
    }
    try {
      Connection connection = database.getConnection();
      String sql = "UPDATE " + AUDIO +
        " " +
        "SET " +
        AUDIO_REF +
        "=?," +
        Database.TIME +
        "=?," + ResultDAO.DURATION + "=? " +
        "WHERE " +
        Database.EXID + "=?" + " AND " +
        USERID + "=?" + " AND " +
        AUDIO_TYPE + "=? AND " +
        DEFECT +"=false";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setString(ii++, audioRef);
      statement.setTimestamp(ii++, new Timestamp(timestamp));
      statement.setInt(ii++, durationInMillis);

      statement.setString(ii++, exerciseID);
      statement.setInt(ii++, userid);
      statement.setString(ii++, audioType);

      int i = statement.executeUpdate();

      if (i == 0) {
        long l = addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis);
        i = (int)l;
      }

      statement.close();
      database.closeConnection(connection);
      return i;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return -1;
  }

  public int markDefect(AudioAttribute attribute) {
    return markDefect((int) attribute.getUserid(), attribute.getExid(), attribute.getAudioType());
  }

  /**
   * @param userid
   * @param exerciseID
   * @param audioType
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.client.custom.EditableExercise#postEditItem
   */
  public int markDefect(int userid, String exerciseID, String audioType) {
    try {
      Connection connection = database.getConnection();
      String sql = "UPDATE " + AUDIO +
        " " +
        "SET " +
        DEFECT +
        "=TRUE" +
        " WHERE " +
        Database.EXID + "=?" + " AND " +
        USERID + "=?" + " AND " +
        AUDIO_TYPE + "=?";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setString(ii++, exerciseID);
      statement.setInt(ii++, userid);
      statement.setString(ii++, audioType);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? couldn't find audio by " + userid + " for ex " +exerciseID + " and " + audioType);
      }
      else {
        logger.debug("\n\n\n" + i+
          " fixed audio defect by " + userid + " ex " +exerciseID + " speed " + audioType);
      }

      statement.close();
      database.closeConnection(connection);
      return i;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return -1;
  }

  private long addAudio(Connection connection, int userid, String audioRef, String exerciseID, long timestamp,
                        String audioType, long durationInMillis) throws SQLException {
    if (userid <1) {
      logger.error("huh? userid is " +userid);
      new Exception().printStackTrace();
    }

    PreparedStatement statement;
    statement = connection.prepareStatement("INSERT INTO " +AUDIO+
      "(" +
      "userid," +
      Database.EXID + "," +
     Database.TIME + "," +
      AUDIO_REF +
      "," +
      ResultDAO.AUDIO_TYPE + "," +
      ResultDAO.DURATION + "," +
      DEFECT + "," +
      ") VALUES(?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

    int i = 1;

    statement.setInt(i++, userid);
    statement.setString(i++, exerciseID);
    statement.setTimestamp(i++, new Timestamp(timestamp));
    statement.setString(i++, audioRef);
    statement.setString(i++, audioType);
    statement.setLong(i++, durationInMillis);
    statement.setBoolean(i++, false);

    statement.executeUpdate();

    ResultSet rs = statement.getGeneratedKeys(); // will return the ID in ID_COLUMN

    long newID = -1;
    if (rs.next()) {
      newID = rs.getLong(1);
    } else {
      logger.error("huh? no key was generated?");
    }

    statement.close();

    return newID;
  }

  /**
   *
   * @see DatabaseImpl#importCourseExamples()
   */
  void drop() {
    try {
      PreparedStatement statement = connection.prepareStatement("DROP TABLE if exists "+AUDIO);
      if (!statement.execute()) {
        logger.error("couldn't drop table?");
      }
      statement.close();
      database.closeConnection(connection);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * So we don't want to use CURRENT_TIMESTAMP as the default for TIMESTAMP
   * b/c if we ever alter the table, say by adding a new column, we will effectively lose
   * the timestamp that was put there when we inserted the row initially.
   * <p></p>
   * Note that the answer column can be either the text of an answer for a written response
   * or a relative path to an audio file on the server.
   *
   * @param connection to make a statement from
   * @throws java.sql.SQLException
   */
  private void createTable(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE if not exists " +
      AUDIO +
      " (" +
      "id IDENTITY, " +
      USERID +
      " INT, " +
      Database.EXID + " VARCHAR, " +
      Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
      "audioRef CLOB," +
      AUDIO_TYPE + " VARCHAR," +
      DURATION + " INT, " +
      DEFECT + " BOOLEAN DEFAULT FALSE" +
      ")");
    statement.execute();
    statement.close();
  }
}