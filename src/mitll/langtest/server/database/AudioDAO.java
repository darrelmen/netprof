package mitll.langtest.server.database;

import com.google.gwt.media.client.Audio;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

/**
 * Create, drop, alter, read from the results table.
 * Note that writing to the table takes place in the {@link mitll.langtest.server.database.AnswerDAO}. Not sure if that's a good idea or not. :)
 */
public class AudioDAO extends DAO {
  private static final Logger logger = Logger.getLogger(AudioDAO.class);

  private static final String ID = "id";
  private static final String USERID = "userid";
  private static final String AUDIO_REF = "audioRef";

  private static final String AUDIO = "audio";

  private static final String AUDIO_TYPE = "audioType";
  private static final String DURATION = "duration";
  private static final String DEFECT = "defect";
  private static final String REGULAR = "regular";
  private static final String SLOW = "slow";

  // private final boolean debug = false;
  private final Connection connection;
  private final UserDAO userDAO;

  public AudioDAO(Database database, UserDAO userDAO) {
    super(database);
    connection = database.getConnection();

    this.userDAO = userDAO;
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

  /**
   * @see mitll.langtest.server.database.ExcelImport#setAudioDAO(AudioDAO)
   * @return
   */
  public Map<String, List<AudioAttribute>> getExToAudio() {
    Map<String, List<AudioAttribute>> exToAudio = new HashMap<String, List<AudioAttribute>>();
    Map<String,Set<String>> idToPaths = new HashMap<String, Set<String>>();
    for (AudioAttribute audio : getAudioAttributes()) {
      String exid = audio.getExid();
      List<AudioAttribute> audioAttributes = exToAudio.get(exid);
      Set<String> paths = idToPaths.get(exid);
      if (audioAttributes == null) {
        exToAudio.put(exid, audioAttributes = new ArrayList<AudioAttribute>());
        idToPaths.put(exid, paths = new HashSet<String>());
      }
      String audioRef = audio.getAudioRef();
      if (!paths.contains(audioRef)) {
        if (exid.startsWith("25")) {
          logger.warn("adding " + audio + " to " + exid);
        }
        audioAttributes.add(audio);
        paths.add(audioRef);
      }
      else {
        logger.warn("skipping " +audioRef + " on " + exid);
      }
    }
    return exToAudio;
  }

  /**
   * Pulls the list of audio recordings out of the database.
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

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#attachAudio(mitll.langtest.shared.CommonExercise)
   * @param firstExercise
   * @param installPath
   * @param relativeConfigDir
   */
  public void attachAudio(CommonExercise firstExercise,  String installPath, String relativeConfigDir) {
    List<AudioAttribute> audioAttributes = getAudioAttributes(firstExercise.getID());

/*    logger.debug("\tfound " + audioAttributes.size() + " for " + firstExercise.getID());
    for (AudioAttribute attribute : audioAttributes) logger.debug("audio " + attribute);*/

    attachAudio(firstExercise, installPath, relativeConfigDir, audioAttributes);
  }

  /**
   * Complicated, but separates old school "Default Speaker" audio into a second pile.
   * If we've already added an audio attribute with the path for a default speaker, then we remove it.
   *
   * @see mitll.langtest.server.database.AudioExport#writeFolderContents(java.util.zip.ZipOutputStream, java.util.List, AudioDAO, String, String, String, boolean)
   * @see #attachAudio(mitll.langtest.shared.CommonExercise, String, String, java.util.List)
   * @param firstExercise
   * @param installPath
   * @param relativeConfigDir
   * @param audioAttributes
   */
  public void attachAudio(CommonExercise firstExercise, String installPath, String relativeConfigDir, List<AudioAttribute> audioAttributes) {
    AudioConversion audioConversion = new AudioConversion();

    List<AudioAttribute> defaultAudio = new ArrayList<AudioAttribute>();
    Set<String> audioPaths = new HashSet<String>();
    Set<String> initialPaths = new HashSet<String>();

    for (AudioAttribute initial : firstExercise.getAudioAttributes()) {
      //logger.debug("predef audio " +initial + " for " + firstExercise.getID());
      initialPaths.add(initial.getAudioRef());
    }

    for (AudioAttribute attr : audioAttributes) {
      if (initialPaths.contains(attr.getAudioRef())) {
    //    logger.debug("skipping " + attr + " on " +firstExercise);
      }
      else {
        if (attr.getUser().isDefault()) {
          defaultAudio.add(attr);
        } else {
          audioPaths.add(attr.getAudioRef());
          attachAudio(firstExercise, installPath, relativeConfigDir, audioConversion, attr);
         // logger.debug("\tadding path '" + attr.getAudioRef() + "' " + attr + " to " + firstExercise.getID());
        }
      }
    }
    for (AudioAttribute attr : defaultAudio) {
      if (!audioPaths.contains(attr.getAudioRef())) {
        attachAudio(firstExercise, installPath, relativeConfigDir, audioConversion, attr);
      }
    }
    List<AudioAttribute> toRemove = new ArrayList<AudioAttribute>();
    for (AudioAttribute attr: firstExercise.getAudioAttributes()) {
     //logger.debug("\treviewing " + attr + " : " + attr.getUser().isDefault());
      if (attr.getUser().isDefault() && audioPaths.contains(attr.getAudioRef())) {
        toRemove.add(attr);
      }
    }
/*    if (!toRemove.isEmpty()) {
      logger.debug("removing  " + toRemove.size());
    }*/
    for (AudioAttribute attr : toRemove) {
      if (!firstExercise.removeAudio(attr)) logger.warn("huh? didn't remove " + attr);
    }
  }

  private void attachAudio(CommonExercise firstExercise, String installPath, String relativeConfigDir, AudioConversion audioConversion, AudioAttribute attr) {
    firstExercise.addAudio(attr);
    if (attr.getAudioRef() == null) logger.error("huh? no audio ref for " + attr + " under " + firstExercise);
    else if (!audioConversion.exists(attr.getAudioRef(), installPath)) {
      //   logger.debug("\twas '" + attr.getAudioRef() + "'");
      attr.setAudioRef(relativeConfigDir + File.separator + attr.getAudioRef());
      //   logger.debug("\tnow '" + attr.getAudioRef() + "'");
    }
  }

  private List<AudioAttribute> getAudioAttributes(String exid) {
    try {
      String sql = "SELECT * FROM " + AUDIO + " WHERE " +Database.EXID +"='" + exid+ "' AND "+DEFECT +"=false";
      List<AudioAttribute> resultsSQL = getResultsSQL(sql);
      Set<String> paths = new HashSet<String>();

      List<AudioAttribute> ret = new ArrayList<AudioAttribute>();

      for (AudioAttribute audioAttribute : resultsSQL) {
        String audioRef = audioAttribute.getAudioRef();
        if (!paths.contains(audioRef)) {
            ret.add(audioAttribute);
          paths.add(audioRef);
        }
        else {
          //logger.info("skipping duplicate audio attr " + audioAttribute + " for " + exid);
        }
      }
      return ret;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<AudioAttribute>();
  }

  public Set<String> getRecordedBy(long userid) {
    User user = userDAO.getUserMap().get(userid);
    boolean isMale = (user != null && user.isMale());
    Map<Long, User> userMap = userDAO.getUserMap(isMale);
    //logger.debug("found " + (isMale ? " male " : " female ") + " users : " + userMap.keySet());
    // find set of users of same gender
    Set<String> validAudioAtReg  = getAudioForGender(userMap, REGULAR);

    Set<String> validAudioAtSlow = getAudioForGender(userMap, SLOW);
      /*boolean b =*/
    validAudioAtReg.retainAll(validAudioAtSlow);
    return validAudioAtReg;
  }

  private Set<String> getAudioForGender( Map<Long, User> userMap, String audioSpeed) {
    Set<String> results = new HashSet<String>();
    try {
      Connection connection = database.getConnection();
      StringBuffer buffer = new StringBuffer();
      for (long id : userMap.keySet()) {
        buffer.append(id).append(",");
      }
      String s = buffer.toString();
      if (!s.isEmpty()) s = s.substring(0,s.length()-1);
      String sql = "SELECT * FROM " + AUDIO + " WHERE " +
        (s.isEmpty() ? "" : USERID+" IN (" + s+ ") AND ")+
        DEFECT +"<>true " +
        " AND "+AUDIO_TYPE +"='" +
        audioSpeed +
        "' ";
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String exid = rs.getString(Database.EXID);
        results.add(exid);
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);

    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return results;
  }


  /**
   * Items that are recorded must have both regular and slow speed audio.
   * @see mitll.langtest.server.LangTestDatabaseImpl#markRecordedState(int, String, java.util.Collection)
   * @param userid
   * @return
   */
  public Set<String> getRecordedForUser(long userid) {
    try {
      Set<String> validAudioAtReg  = getValidAudioAtSpeed(userid, REGULAR);
      Set<String> validAudioAtSlow = getValidAudioAtSpeed(userid, SLOW);
      /*boolean b =*/ validAudioAtReg.retainAll(validAudioAtSlow);
      return validAudioAtReg;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<String>();
  }

  Set<String> getValidAudioAtSpeed(long userid, String audioSpeed) throws SQLException {
    String sql = "SELECT " + Database.EXID+
      " FROM " + AUDIO + " WHERE " +USERID +"=" + userid+
      " AND "+DEFECT +"<>true " +
      " AND "+AUDIO_TYPE +"='" +
      audioSpeed +
      "' "
      ;
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    return getExidResultsForQuery(connection, statement);
  }

  /**
   * @see #getAudioAttributes()
   * @see #getAudioAttributes(String)
   * @param sql
   * @return
   * @throws SQLException
   */
  private List<AudioAttribute> getResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    return getResultsForQuery(connection, statement);
  }

  /**
   * Get a list of audio attributes for this Query.
   *
   * @param connection
   * @param statement
   * @return
   * @throws java.sql.SQLException
   * @see #getResultsSQL(String)
   */
  private List<AudioAttribute> getResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    List<AudioAttribute> results = new ArrayList<AudioAttribute>();
    Map<Long, MiniUser> miniUsers = userDAO.getMiniUsers();

    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      long userID = rs.getLong(USERID);
      String exid = rs.getString(Database.EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String audioRef = rs.getString(AUDIO_REF);

      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);

      MiniUser user = miniUsers.get(userID);
      if (userID == UserDAO.DEFAULT_USER_ID) {
        user = UserDAO.DEFAULT_USER;
      } else  if (userID == UserDAO.DEFAULT_MALE_ID) {
        user = UserDAO.DEFAULT_MALE;
      } else  if (userID == UserDAO.DEFAULT_FEMALE_ID) {
        user = UserDAO.DEFAULT_FEMALE;
      }

      AudioAttribute audioAttr = new AudioAttribute(uniqueID, userID, //id
        exid, // id
        audioRef, // answer
        timestamp.getTime(),
        dur, type,
        user);

      if (user == null) {
        logger.warn("can't find user " + userID+ " for " + audioAttr + " in " + miniUsers.keySet());
      }
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
   * @see mitll.langtest.server.database.ImportCourseExamples#copyAudio
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#markGender(mitll.langtest.shared.AudioAttribute, boolean)
   * @param userid
   * @param attr
   * @return
   */
  public AudioAttribute addOrUpdateUser(int userid, AudioAttribute attr) {
    return addOrUpdateUser(userid, attr.getAudioRef(), attr.getExid(), attr.getTimestamp(), attr.getAudioType(), (int)attr.getDuration());
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#addMissingAudio(mitll.langtest.shared.custom.UserExercise, String, String)
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
      if (isBadUser(userid)) {
        logger.error("huh? userid is " +userid);
        new Exception().printStackTrace();
      }

      //logger.debug("added exerciseID " + exerciseID + " ref '" + audioRef + "' " +audioType + " for " + userid);
      long newid = addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis);
      database.closeConnection(connection);
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
   * @see #add(mitll.langtest.shared.Result, int, String)
   */
  private long add(Connection connection, Result result, int userid, String audioRef) throws SQLException {
    String exerciseID = result.id;
    long timestamp = result.timestamp;
    String audioType = result.getAudioType();
    int durationInMillis = result.durationInMillis;

    return addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis);
  }


  /**
   * Why does this have to be so schizo? add or update -- should just choose
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#addToAudioTable(int, String, mitll.langtest.shared.CommonExercise, String, mitll.langtest.shared.AudioAnswer)
   * @param userid   part of unique id
   * @param audioRef
   * @param exerciseID part of unique id
   * @param timestamp
   * @param audioType    part of unique id
   * @param durationInMillis
   * @return AudioAttribute that represents the audio that has been added to the exercise
   */
  private AudioAttribute addOrUpdateUser(int userid, String audioRef, String exerciseID, long timestamp, String audioType, int durationInMillis) {
    if (isBadUser(userid)) {
      logger.error("huh? userid is " +userid);
      new Exception().printStackTrace();
    }
    try {
      logger.debug("addOrUpdate " + userid + " " + audioRef + " ex " + exerciseID + " at " + new Date(timestamp) + " type " + audioType + " dur " + durationInMillis);
      Connection connection = database.getConnection();
      String sql = "UPDATE " + AUDIO + " " +
          "SET " + USERID + "=? "+
          "WHERE " +
          Database.EXID + "=?" + " AND " +
         // USERID + "=?" + " AND " +
          AUDIO_TYPE + "=? AND " +
          DEFECT +"=false";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      //statement.setString(ii++, audioRef);
      //statement.setTimestamp(ii++, new Timestamp(timestamp));
      //statement.setInt(ii++, durationInMillis);

      statement.setInt(ii++, userid);
      statement.setString(ii++, exerciseID);
      statement.setString(ii++, audioType);

      int i = statement.executeUpdate();

      AudioAttribute audioAttr = null;
      if (i == 0) {
        logger.debug("\taddOrUpdate adding entry for  " + userid + " " + audioRef + " ex " + exerciseID + " at " + new Date(timestamp) + " type " + audioType + " dur " + durationInMillis);

        long l = addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis);
        audioAttr = getAudioAttribute((int)l,userid, audioRef, exerciseID, timestamp, audioType, durationInMillis);
      }
      else {
        List<AudioAttribute> audioAttributes = getAudioAttributes(exerciseID);
        //logger.debug("for  " +exerciseID + " found " + audioAttributes);

        for (AudioAttribute audioAttribute : audioAttributes) {
          //logger.debug("\tfor  " +audioAttribute + " against " + userid + "/" + audioType );
          if (audioAttribute.getUserid() == userid && audioAttribute.getAudioType().equalsIgnoreCase(audioType)) {
            //logger.debug("\tfound  " +audioAttribute + " for " + userid + "/" + audioType );
            audioAttr = audioAttribute;
            break;
          }
        }
      }
      logger.debug("returning " + audioAttr);
      statement.close();
      database.closeConnection(connection);

      return audioAttr;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  /**
   * Why does this have to be so schizo? add or update -- should just choose
   *
   * @see mitll.langtest.server.LangTestDatabaseImpl#addToAudioTable(int, String, mitll.langtest.shared.CommonExercise, String, mitll.langtest.shared.AudioAnswer)
   * @param userid   part of unique id
   * @param audioRef
   * @param exerciseID part of unique id
   * @param timestamp
   * @param audioType    part of unique id
   * @param durationInMillis
   * @return AudioAttribute that represents the audio that has been added to the exercise
   */
  public AudioAttribute addOrUpdate(int userid, String audioRef, String exerciseID, long timestamp, String audioType, int durationInMillis) {
    if (isBadUser(userid)) {
      logger.error("huh? userid is " +userid);
      new Exception().printStackTrace();
    }
    try {
      logger.debug("addOrUpdate " + userid + " " + audioRef + " ex " + exerciseID + " at " + new Date(timestamp) + " type " + audioType + " dur " + durationInMillis);
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

      AudioAttribute audioAttr = null;
      if (i == 0) {
        logger.debug("\taddOrUpdate adding entry for  " + userid + " " + audioRef + " ex " + exerciseID + " at " + new Date(timestamp) + " type " + audioType + " dur " + durationInMillis);

        long l = addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis);
        audioAttr = getAudioAttribute((int)l,userid, audioRef, exerciseID, timestamp, audioType, durationInMillis);
      }
      else {
        List<AudioAttribute> audioAttributes = getAudioAttributes(exerciseID);
        //logger.debug("for  " +exerciseID + " found " + audioAttributes);

        for (AudioAttribute audioAttribute : audioAttributes) {
          String audioType1 = audioAttribute.getAudioType();
          //logger.debug("\tfor  " +audioAttribute + " against " + userid + "/" + audioType  + " audio type " + audioType1);
          if (audioAttribute.getUserid() == userid && audioType1.equalsIgnoreCase(audioType)) {
                      //logger.debug("\tfound  " +audioAttribute + " for " + userid + "/" + audioType );
            audioAttr = audioAttribute;
            break;
          }
        }
      }
      logger.debug("returning " + audioAttr);
      statement.close();
      database.closeConnection(connection);

      return audioAttr;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  /**
   * @see #addOrUpdate(int, String, String, long, String, int)
   * @param i
   * @param userid
   * @param audioRef
   * @param exerciseID
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @return
   */
  private AudioAttribute getAudioAttribute(int i,
                                           int userid, String audioRef, String exerciseID, long timestamp, String audioType, int durationInMillis) {
    return new AudioAttribute(i, userid, //id
        exerciseID, // id
        audioRef, // answer
        timestamp,
        durationInMillis, audioType,
        userDAO.getMiniUser(userid));
  }

  public void updateExerciseID(int uniqueID, String exerciseID) {
    try {
      Connection connection = database.getConnection();
      String sql = "UPDATE " + AUDIO +
        " " +
        "SET " +
        Database.EXID +
        "=? " +
        "WHERE " +
        ID + "=?";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setString(ii++, exerciseID);
      statement.setInt(ii++, uniqueID);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? couldn't update " + uniqueID + " to " + exerciseID);
      }

      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @param attribute
   * @return
   */
  public int markDefect(AudioAttribute attribute) {
    return markDefect((int) attribute.getUserid(), attribute.getExid(), attribute.getAudioType());
  }

  /**
   * An audio cut is uniquely identified by by exercise id, speed (reg/slow), and who recorded it.
   * @param userid recorded by this user
   * @param exerciseID on this exercise
   * @param audioType at this speed
   * @return > 0 if audio was marked defective
   * @see mitll.langtest.server.database.DatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.client.custom.EditableExercise#postEditItem
   */
  private int markDefect(int userid, String exerciseID, String audioType) {
    try {
      if (audioType.equals(AudioAttribute.REGULAR_AND_SLOW)) {
        audioType = Result.AUDIO_TYPE_FAST_AND_SLOW;
      }
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
        logger.debug(i+" marked audio defect by " + userid + " ex " +exerciseID + " speed " + audioType);
      }

      statement.close();
      database.closeConnection(connection);
      return i;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return -1;
  }

  /**
   * @see #add(java.sql.Connection, mitll.langtest.shared.Result, int, String)
   * @param connection
   * @param userid
   * @param audioRef
   * @param exerciseID
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @return
   * @throws SQLException
   */
  private long addAudio(Connection connection, int userid, String audioRef, String exerciseID, long timestamp,
                        String audioType, long durationInMillis) throws SQLException {
    if (isBadUser(userid)) {
      logger.error("huh? userid is " +userid);
      new Exception().printStackTrace();
    }

    logger.debug("addAudio : by " + userid + " for ex " + exerciseID + " type " + audioType + " ref " + audioRef);

    PreparedStatement statement = connection.prepareStatement("INSERT INTO " + AUDIO +
      "(" +
      USERID + "," +
      Database.EXID + "," +
      Database.TIME + "," +
      AUDIO_REF + "," +
      ResultDAO.AUDIO_TYPE + "," +
      ResultDAO.DURATION + "," +
      DEFECT +
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
      logger.error("addAudio : huh? no key was generated?");
    }

    statement.close();

    return newID;
  }

  private boolean isBadUser(int userid) { return userid < UserDAO.DEFAULT_FEMALE_ID; }

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
      ID +
      " IDENTITY, " +
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