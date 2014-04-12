package mitll.langtest.server.database;

import com.google.gwt.media.client.Audio;
import mitll.langtest.server.PathHelper;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.Result;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create, drop, alter, read from the results table.
 * Note that writing to the table takes place in the {@link mitll.langtest.server.database.AnswerDAO}. Not sure if that's a good idea or not. :)
 */
public class AudioDAO extends DAO {
  private static final Logger logger = Logger.getLogger(AudioDAO.class);

 // private static final int MINUTE = 60 * 1000;

  public static final String ID = "id";
  private static final String USERID = "userid";
 // private static final String QID = "qid";
  private static final String AUDIO_REF = "audioRef";

  public static final String AUDIO = "audio";

  static final String AUDIO_TYPE = "audioType";
  static final String DURATION = "duration";

  private final boolean debug = false;
  private Map<Long, MiniUser> miniUsers;
  private final Connection connection;

  public AudioDAO(Database database, UserDAO userDAO) {
    super(database);
    connection = database.getConnection();

    miniUsers = userDAO.getMiniUsers();
    try {
      createTable(database.getConnection());
    } catch (SQLException e) {
      logger.error("Got " +e,e);
    }
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
  public List<AudioAttribute> getAudioAttributes() {
    try {
      String sql = "SELECT * FROM " + AUDIO + ";";
      return getResultsSQL(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<AudioAttribute>();
  }

  private List<AudioAttribute> getResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);

    return getResultsForQuery(connection, statement);
  }

/*
  public int getNumResults() {
    int numResults = 0;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + AUDIO + ";");
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        numResults = rs.getInt(1);
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return numResults;
  }
*/

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
    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      long userID = rs.getLong(USERID);
      String exid = rs.getString(Database.EXID);
      //int qid = rs.getInt(QID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String audioRef = rs.getString(AUDIO_REF);

      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);


      AudioAttribute audioAttr = new AudioAttribute(userID, //id
        exid, // id
       // exid, // id
       // qid == 1, // qid
        audioRef, // answer
        timestamp.getTime(),
          dur,type,
        miniUsers.get(userID));
    //  audioAttr.setStimulus(stimulus);
      trimPathForWebPage(audioAttr);
      results.add(audioAttr);
    }
    rs.close();
    statement.close();
    database.closeConnection(connection);

    return results;
  }

  private void trimPathForWebPage(AudioAttribute r) {
    String audioRef = r.getAudioRef();
    int answer = audioRef.indexOf(PathHelper.ANSWERS);
    if (answer == -1) return;

    r.setAudioRef(audioRef.substring(answer));
  }

  /**
   * @param exerciseID
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getResultsForExercise(String, boolean, boolean, boolean)
   */
  public List<AudioAttribute> getAllResultsForExercise(String exerciseID) {
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT * FROM results WHERE EXID='" + exerciseID + "'";
      PreparedStatement statement = connection.prepareStatement(sql);
      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<AudioAttribute>();
  }

  /**
   * @param toExclude
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getNextUngradedExerciseQuick(java.util.Collection, int, boolean, boolean, boolean)
   */
  public Collection<AudioAttribute> getResultExcludingExercises(Collection<String> toExclude) {
    // select results.* from results where results.exid not in ('ac-R0P-006','ac-LOP-001','ac-L0P-013')
    try {
      Connection connection = database.getConnection();

      String list = getInList(toExclude);
      String sql = "SELECT * FROM results WHERE EXID NOT IN (" + list + ")";

      PreparedStatement statement = connection.prepareStatement(sql);
      return getResultsForQuery(connection, statement);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<AudioAttribute>();

  }

  private String getInList(Collection<String> toExclude) {
    StringBuilder b = new StringBuilder();
    for (String id : toExclude) b.append("'").append(id).append("'").append(",");
    String list = b.toString();
    list = list.substring(0, Math.max(0, list.length() - 1));
    return list;
  }

/*
  private void sortByTime(List<Result> answersForUser) {
    Collections.sort(answersForUser, new Comparator<Result>() {
      @Override
      public int compare(Result o1, Result o2) {
        return o1.timestamp < o2.timestamp ? -1 : o1.timestamp > o2.timestamp ? +1 : 0;
      }
    });
  }
*/

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   */
  public long add(Result result, int userid) {
    try {
      long then = System.currentTimeMillis();
      //String audioRef = trimPathForWebPage(result);
    //  logger.debug("path was " + result.answer + " now '" + audioRef + "'");
      long newid = add(connection, result, userid, result.answer );
      database.closeConnection(connection);
      long now = System.currentTimeMillis();
      if (now - then > 100) System.out.println("took " + (now - then) + " millis to record answer.");
      return newid;

    } catch (Exception ee) {
      logger.error("addAnswer got " + ee, ee);
    }
    return -1;
  }

  private String trimPathForWebPage(Result r) {
    String path = r.answer.trim();
    if (path.startsWith(PathHelper.ANSWERS)) {
      return path.substring(PathHelper.ANSWERS.length());
    }
    else return path;
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
    PreparedStatement statement;
    statement = connection.prepareStatement("INSERT INTO " +AUDIO+
      "(" +
      "userid," +
      Database.EXID + "," +
     Database.TIME + "," +
      "audioRef," +
      ResultDAO.AUDIO_TYPE + "," +
      ResultDAO.DURATION + "," +
      ") VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

    int i = 1;

    statement.setInt(i++, userid);
    statement.setString(i++, result.getID());
    statement.setTimestamp(i++, new Timestamp(result.timestamp));
    statement.setString(i++, audioRef);
    statement.setString(i++, result.getAudioType());
    statement.setInt(i++, result.durationInMillis);

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

  void drop() {
    try {
      //Connection connection = database.getConnection();
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
      "userid INT, " +
      Database.EXID + " VARCHAR, " +
      Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
      "audioRef CLOB," +
      AUDIO_TYPE + " VARCHAR," +
      DURATION + " INT" +
      ")");
    statement.execute();
    statement.close();
  }
}