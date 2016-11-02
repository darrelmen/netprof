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

package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import mitll.langtest.shared.exercise.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.OutputStream;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Create, drop, alter, read from the audio table.
 * * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class AudioDAO extends DAO {
  private static final Logger logger = Logger.getLogger(AudioDAO.class);

  private static final String ID = "id";
  private static final String USERID = "userid";
  private static final String AUDIO_REF = "audioRef";

  private static final String AUDIO = "audio";
  private static final String SELECT_ALL = "SELECT * FROM " + AUDIO;

  private static final String AUDIO_TYPE = "audioType";
  private static final String DURATION = "duration";
  private static final String DEFECT = "defect";
  private static final String REGULAR = "regular";
  private static final String AUDIO_TYPE1 = "context=" + REGULAR;
  private static final String SLOW = "slow";
  private static final String CONTEXT_REGULAR = AUDIO_TYPE1;
  private static final String TRANSCRIPT = "transcript";
  private static final String FAST_WAV = "Fast" + ".wav";
  private static final String SLOW_WAV = "Slow" + ".wav";

  public static final String UNKNOWN = "unknown";
  public static final String TOTAL = "total";
  public static final String TOTAL_CONTEXT = "totalContext";
  public static final String MALE = "male";
  public static final String FEMALE = "female";
  public static final String MALE_FAST = "maleFast";
  public static final String MALE_SLOW = "maleSlow";
  public static final String FEMALE_FAST = "femaleFast";
  public static final String FEMALE_SLOW = "femaleSlow";
  public static final String MALE_CONTEXT = "maleContext";
  public static final String FEMALE_CONTEXT = "femaleContext";
  public static final String DNR = "dnr";

  private final boolean DEBUG = false;
  private static final boolean DEBUG_ATTACH = false;

  private final Connection connection;
  private final UserDAO userDAO;
  private ExerciseDAO<?> exerciseDAO;


  /**
   * @param database
   * @param userDAO
   * @see DatabaseImpl#initializeDAOs(PathHelper)
   */
  public AudioDAO(Database database, UserDAO userDAO) {
    super(database);
    connection = database.getConnection(this.getClass().toString());

    this.userDAO = userDAO;
    try {
      createTable(connection);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
    Collection<String> columns = getColumns(AUDIO);
    if (!columns.contains(DEFECT)) {
      try {
        addBoolean(connection, AUDIO, DEFECT);
      } catch (SQLException e) {
        logger.error("got " + e, e);
      }
    }
    if (!columns.contains(TRANSCRIPT)) {
      try {
        addVarchar(connection, AUDIO, TRANSCRIPT);
      } catch (SQLException e) {
        logger.error("got " + e, e);
      }
    }

    if (!columns.contains(DNR)) {
      try {
        PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + AUDIO + " ADD " + DNR + " " + "REAL" + " DEFAULT '-1'");
        statement.execute();
        statement.close();
      } catch (SQLException e) {
        logger.error("got " + e, e);
      }
    }

    database.closeConnection(connection);
    audioCheck = new AudioCheck(database.getServerProps());
  }

  private AudioCheck audioCheck;

  @Override
  protected void addBoolean(Connection connection, String table, String col) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("ALTER TABLE " +
        table +
        " ADD " + col + " BOOLEAN DEFAULT FALSE");
    statement.execute();
    statement.close();
  }

  private void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public <T extends AudioExercise> int addOldSchoolAudio(String refAudioIndex, T imported, int audioOffset, String mediaDir, String installPath) throws SQLException {
    String audioDir = refAudioIndex.length() > 0 ? findBest(refAudioIndex) : imported.getID();
    if (audioOffset != 0) {
      audioDir = "" + (Integer.parseInt(audioDir.trim()) + audioOffset);
    }

    String parentPath = mediaDir + File.separator + audioDir + File.separator;

    int total = 0;
    MiniUser defaultUser = UserDAO.DEFAULT_USER;
    {
      String fastAudioRef = parentPath + FAST_WAV;
      File test = new File(fastAudioRef);
      boolean exists = test.exists();
      if (!exists) {
        // logger.info("1 no audio at " + test.getAbsolutePath());
        test = new File(installPath, fastAudioRef);
        exists = test.exists();
      }
      if (exists) {
//        imported.addAudioForUser(ensureForwardSlashes(fastAudioRef), defaultUser);
        long duration = (long) (audioCheck.getDurationInSeconds(test) * 1000d);
        sleep(50);
        try {
          fastAudioRef = "bestAudio" + fastAudioRef.split("bestAudio")[1];
          //  logger.info("audio at " + fastAudioRef);

          addAudio(connection, (int) defaultUser.getId(), ensureForwardSlashes(fastAudioRef), imported.getID(),
              test.lastModified(), REGULAR, duration, imported.getForeignLanguage());
          total++;
        } catch (Exception e) {
          logger.error("got " + e, e);
          throw e;
        }
      } else {
        // logger.info("2 no audio at " + test.getAbsolutePath());
      }
    }

    {
      String slowAudioRef = parentPath + SLOW_WAV;
      File test = new File(slowAudioRef);
      boolean exists = test.exists();
      if (!exists) {
        // logger.info("3 no audio at " + test.getAbsolutePath());

        test = new File(installPath, slowAudioRef);
        exists = test.exists();
      }
      if (exists) {
        //       imported.addAudio(new AudioAttribute(ensureForwardSlashes(slowAudioRef), defaultUser).markSlow());
        long duration = (long) (audioCheck.getDurationInSeconds(test) * 1000d);
        try {

          slowAudioRef = "bestAudio" + slowAudioRef.split("bestAudio")[1];

          addAudio(connection, (int) defaultUser.getId(), ensureForwardSlashes(slowAudioRef), imported.getID(), test.lastModified(), SLOW, duration, imported.getForeignLanguage());
          total++;
        } catch (Exception e) {
          logger.error("got " + e, e);
        }
      } else {
//        logger.info("4 no audio at " + test.getAbsolutePath());

      }
    }
    return total;
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  /**
   * Assumes audio index field looks like : 11109 8723 8722 8721
   *
   * @param refAudioIndex
   * @return
   */
  private String findBest(String refAudioIndex) {
    String[] split = refAudioIndex.split("\\s+");
    return (split.length == 0) ? "" : split[0];
  }

  /**
   * TODO : Seems really expensive - avoid doing this if we can.
   *
   * @return
   * @seex ExerciseDAO#setAudioDAO
   * @see AudioExport#writeFolderContents
   * @see AudioExport#writeFolderContentsContextOnly
   * @see DatabaseImpl#attachAllAudio
   */
  public Map<String, List<AudioAttribute>> getExToAudio() {
    long then = System.currentTimeMillis();
    Map<String, List<AudioAttribute>> exToAudio = new HashMap<>();
    Map<String, Set<String>> idToPaths = new HashMap<>();
    Collection<AudioAttribute> audioAttributes1 = getAudioAttributes();
    for (AudioAttribute audio : audioAttributes1) {
      String exid = audio.getExid();
      List<AudioAttribute> audioAttributes = exToAudio.get(exid);
      Set<String> paths = idToPaths.get(exid);
      if (audioAttributes == null) {
        exToAudio.put(exid, audioAttributes = new ArrayList<>());
        idToPaths.put(exid, paths = new HashSet<>());
      }
      String audioRef = audio.getAudioRef();
      if (!paths.contains(audioRef)) {
        audioAttributes.add(audio);
        paths.add(audioRef);
      }
      //    else {
      //logger.warn("skipping " +audioRef + " on " + exid);
      //  }
    }
    long now = System.currentTimeMillis();
    logger.info("getExToAudio (" + database.getLanguage() +
        ") took " + (now - then) + " millis to get  " + audioAttributes1.size() + " audio entries");
//    logger.debug("map size is " + exToAudio.size());
    return exToAudio;
  }

  /**
   * Go back and mark transcripts on audio cuts that were not marked properly initially.
   *
   * @see DatabaseImpl#makeDAO(String, String, String)
   */
  void markTranscripts() {
    List<AudioAttribute> toUpdate = new ArrayList<>();

    Collection<AudioAttribute> audioAttributes1 = getAudioAttributes();
    for (AudioAttribute audio : audioAttributes1) {
      CommonShell exercise = exerciseDAO.getExercise(audio.getExid());
      if (exercise != null) {
        String english = exercise.getEnglish();
        String fl = exercise.getForeignLanguage();
        String transcript = audio.getTranscript();
        if (audio.isContextAudio()) {
          String context = exercise.getContext();
          if ((transcript == null ||
              transcript.isEmpty() ||
              transcript.equals(english) ||
              transcript.equals(fl)) &&
              (!context.isEmpty() && !context.equals(transcript))
              ) {
            audio.setTranscript(context);
            toUpdate.add(audio);
            //  logger.info("context update " + exercise.getID() + "/" +audio.getUniqueID()+" to " + context);
          }
        } else {
          if ((transcript == null ||
              transcript.isEmpty() ||
              transcript.equals(english)) &&
              (!fl.isEmpty() && !fl.equals(transcript))
              ) {
            audio.setTranscript(fl);
//            logger.info("update " + exercise.getID() + "/" +audio.getUniqueID()+" to " + fl + " from " + exercise.getEnglish());

            toUpdate.add(audio);
          }
        }
      }
    }
    updateTranscript(toUpdate);
  }

  /**
   * @param audio
   * @return
   * @see #markTranscripts()
   */
  private int updateTranscript(Collection<AudioAttribute> audio) {
    int c = 0;

    long then = System.currentTimeMillis();
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO + " " +
          "SET " + TRANSCRIPT + "=? " +
          "WHERE " +
          ID + "=?";

      PreparedStatement statement = connection.prepareStatement(sql);

      for (AudioAttribute audioAttribute : audio) {
        int ii = 1;
        statement.setString(ii++, audioAttribute.getTranscript());
        statement.setInt(ii++, audioAttribute.getUniqueID());
        int i = statement.executeUpdate();
        if (i > 0) c++;
      }

      finish(connection, statement);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    long now = System.currentTimeMillis();

    if (c > 0) {
      logger.info("updateTranscript did " + c + "/" + audio.size() + " in " + (now - then) + " millis");
    }
    return c;
  }

  /**
   * @param exerciseDAO
   * @see DatabaseImpl#makeDAO(String, String, String)
   */
  void setExerciseDAO(ExerciseDAO<?> exerciseDAO) {
    this.exerciseDAO = exerciseDAO;
  }

  public int numRows() {
    return getCount(AUDIO);
  }

  /**
   * Pulls the list of audio recordings out of the database.
   *
   * @return
   * @see #getExToAudio
   * @see Report#getReport
   */
  public Collection<AudioAttribute> getAudioAttributes() {
    try {
      String sql = SELECT_ALL + " WHERE " + DEFECT + "=false";
      return getResultsSQL(sql);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<>();
  }


  /**
   * @param firstExercise
   * @param installPath
   * @param relativeConfigDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#attachAudio(mitll.langtest.shared.exercise.CommonExercise)
   * @see DatabaseImpl#attachAudio(CommonExercise)
   * @see DatabaseImpl#writeUserListAudio(OutputStream, long, PathHelper, AudioExport.AudioExportOptions)
   */
  public int attachAudio(CommonExercise firstExercise, String installPath, String relativeConfigDir) {
    Collection<AudioAttribute> audioAttributes = getAudioAttributes(firstExercise.getID());

/*    if (DEBUG) {
      logger.debug("\attachAudio : found " + audioAttributes.size() + " for " + firstExercise.getID());
      for (AudioAttribute attribute : audioAttributes) {
        logger.debug("\t\attachAudio : exid " + firstExercise.getID() + " audio " + attribute);
      }

      for (AudioAttribute attribute : firstExercise.getAudioAttributes()) {
        logger.debug("\t\tbefore attachAudio on ex : exid " + firstExercise.getID() + " audio " + attribute);
      }
    }*/

    boolean attachedAll = attachAudio(firstExercise, installPath, relativeConfigDir, audioAttributes);

    if (!attachedAll)
      logger.info("didn't attach all audio to " + firstExercise.getID() + " " + firstExercise.getForeignLanguage());
/*    if (DEBUG) {
      for (AudioAttribute attribute : firstExercise.getAudioAttributes()) {
        logger.debug("\t\tafter attachAudio : after on ex exid " + firstExercise.getID() + " audio " + attribute);
      }
    }*/

    return audioAttributes.size();
  }

  /**
   * TODO : rewrite this so it's not insane -adding and removing attributes??
   * <p>
   * Complicated, but separates old school "Default Speaker" audio into a second pile.
   * If we've already added an audio attribute with the path for a default speaker, then we remove it.
   * <p>
   * TODO : why would we want to skip adding audio from the initial path set?
   *
   * @param firstExercise
   * @param installPath
   * @param relativeConfigDir
   * @param audioAttributes
   * @see mitll.langtest.server.database.AudioExport#writeFolderContents
   * @see #attachAudio
   * @see mitll.langtest.server.json.JsonExport#getJsonArray
   * @see
   */
  public boolean attachAudio(CommonExercise firstExercise, String installPath, String relativeConfigDir,
                             Collection<AudioAttribute> audioAttributes) {
    AudioConversion audioConversion = new AudioConversion(database.getServerProps());

    List<AudioAttribute> defaultAudio = new ArrayList<>();
    Set<String> audioPaths = new HashSet<>();
    //Set<String> initialPaths = new HashSet<String>();

    // get all the audio on the exercise initially
    //for (AudioAttribute initial : firstExercise.getAudioAttributes()) {
    // logger.debug("predef audio " +initial + " for " + firstExercise.getID());
    //   initialPaths.add(initial.getAudioRef());
    // }

    boolean allSucceeded = true;

    for (AudioAttribute attr : audioAttributes) {
      //if (initialPaths.contains(attr.getAudioRef())) {
      //  logger.debug("skipping " + attr + " on " +firstExercise);
      //}
      //else {
      if (attr.getUser().isUnknownDefault()) {
        defaultAudio.add(attr);
      } else {
        audioPaths.add(attr.getAudioRef());
        boolean didIt = attachAudioAndFixPath(firstExercise, installPath, relativeConfigDir, audioConversion, attr);
        if (!didIt) {
          if (DEBUG_ATTACH && allSucceeded) {
            String foreignLanguage = attr.isContextAudio() ? firstExercise.getContext() : firstExercise.getForeignLanguage();
            logger.info("not attaching audio\t" + attr.getUniqueID() + " to\t" + firstExercise.getID() +
                "\tsince transcript has changed : old '" +
                attr.getTranscript() +
                "' vs new '" + foreignLanguage +
                "'");
          }
          allSucceeded = false;
        }
        // logger.debug("\tadding path '" + attr.getAudioRef() + "' " + attr + " to " + firstExercise.getID());
      }
      //}
    }

    for (AudioAttribute attr : defaultAudio) {
      if (!audioPaths.contains(attr.getAudioRef())) {
        boolean didIt = attachAudioAndFixPath(firstExercise, installPath, relativeConfigDir, audioConversion, attr);
        if (!didIt) {
          if (DEBUG_ATTACH && allSucceeded) {
            logger.info("not attaching audio\t" + attr.getUniqueID() + " to\t" + firstExercise.getID() +
                "\tsince transcript has changed : old '" + attr.getTranscript() +
                "' vs new '" + firstExercise.getForeignLanguage() +
                "'");
          }
          allSucceeded = false;
        }
      }
    }

    List<AudioAttribute> toRemove = new ArrayList<>();
    for (AudioAttribute attr : firstExercise.getAudioAttributes()) {
      // logger.debug("\treviewing " + attr + " : is default? " + attr.getUser().isUnknownDefault());
      if (attr.getUser().isUnknownDefault() && audioPaths.contains(attr.getAudioRef())) {
        toRemove.add(attr);
      }
    }

    //if (!toRemove.isEmpty()) {
    //  logger.debug("\tremoving  " + toRemove.size());
    //}

    MutableAudioExercise mutable = firstExercise.getMutableAudio();
    for (AudioAttribute attr : toRemove) {
      if (!mutable.removeAudio(attr)) logger.warn("huh? didn't remove " + attr);
      //else {
      //   logger.debug("\tremoving " +attr);
      //  }
    }
    return allSucceeded;
  }

  /**
   * @param firstExercise
   * @param installPath
   * @param relativeConfigDir
   * @param audioConversion
   * @param attr
   * @return
   * @see #attachAudio(CommonExercise, String, String)
   */
  private boolean attachAudioAndFixPath(CommonExercise firstExercise,
                                        String installPath,
                                        String relativeConfigDir,
                                        AudioConversion audioConversion,
                                        AudioAttribute attr) {
    String against = attr.isContextAudio() ? firstExercise.getContext() : firstExercise.getForeignLanguage();
//    String noAccents = Normalizer.normalize(against, Normalizer.Form.NFD);
//
//    logger.info("attachAudioAndFixPath before '" +against+
//        "' after '" + noAccents+
//        "'");

    String noAccents = StringUtils.stripAccents(against);
    String transcript = attr.getTranscript();
    String noAccentsTranscript = transcript == null ? null : StringUtils.stripAccents(transcript);
//    boolean foundAlt = false;
//    if (!before.equals(noAccents)) {
//      if (firstExercise.getID().equals("3277")) {
//        logger.info("attachAudio before '" + before +
//            "' after '" + noAccents +
//            "'");
//      }
//      foundAlt = true;
//    } else {
//      if (firstExercise.getID().equals("3277")) {
//        logger.info("attachAudio before '" + before +
//            "' after '" + noAccents +
//            "'");
//      }
//    }

    if (attr.matchTranscript(against, transcript) || attr.matchTranscript(noAccents, noAccentsTranscript)) {
      firstExercise.getMutableAudio().addAudio(attr);

      if (attr.getAudioRef() == null)
        logger.error("attachAudioAndFixPath huh? no audio ref for " + attr + " under " + firstExercise);
      else if (!audioConversion.exists(attr.getAudioRef(), installPath)) {
        if (audioConversion.exists(attr.getAudioRef(), relativeConfigDir)) {
          logger.debug("\tattachAudioAndFixPath was '" + attr.getAudioRef() + "'");
          attr.setAudioRef(relativeConfigDir + File.separator + attr.getAudioRef());
          logger.debug("\tattachAudioAndFixPath now '" + attr.getAudioRef() + "'");
        } else {
//          logger.debug("\tattachAudio couldn't find audio file at '" + attr.getAudioRef() + "'");
        }
      }
      return true;
    } else {

      return false;
    }
  }

  /**
   * Defensively protect against duplicate entries for same audio file.
   *
   * @param exid
   * @return
   * @see #attachAudio
   */
  public Collection<AudioAttribute> getAudioAttributes(String exid) {
    try {
      String sql = SELECT_ALL + " WHERE " + Database.EXID + "='" + exid + "' AND " + DEFECT + "=false";
      Collection<AudioAttribute> resultsSQL = getResultsSQL(sql);
      Set<String> paths = new HashSet<>();

      List<AudioAttribute> ret = new ArrayList<>();

      for (AudioAttribute audioAttribute : resultsSQL) {
        String audioRef = audioAttribute.getAudioRef();
        String key = audioRef + "_" + audioAttribute.getTranscript();
        if (!paths.contains(key)) {
          ret.add(audioAttribute);
          paths.add(key);
        }
        //  else {
        //logger.info("skipping duplicate audio attr " + audioAttribute + " for " + exid);
        //  }
      }
      if (DEBUG) {
        logger.debug("sql for " + exid + " = " + sql + " returned " + ret);
      }
      return ret;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new ArrayList<>();
  }

  /**
   * Get back the ids of exercises recorded by people who are the same gender as the userid.
   *
   * @param userid only used to determine the gender we should show
   * @return ids with both regular and slow speed recordings
   * @see mitll.langtest.server.LangTestDatabaseImpl#filterByUnrecorded
   */
  public Set<String> getRecordedBy(long userid) {
    Map<Long, User> userMap = getUserMap(userid);
    //logger.debug("found " + (isMale ? " male " : " female ") + " users : " + userMap.keySet());
    // find set of users of same gender
    Set<String> validAudioAtReg = getAudioForGender(userMap, REGULAR);
    //logger.debug(" regular speed for " + userMap.keySet() + " " + validAudioAtReg.size());
    Set<String> validAudioAtSlow = getAudioForGender(userMap, SLOW);
//    logger.debug(" slow speed for " + userMap.keySet() + " " + validAudioAtSlow.size());

    boolean b = validAudioAtReg.retainAll(validAudioAtSlow);
    //  logger.debug("retain all " + b + " " + validAudioAtReg.size());
    return validAudioAtReg;
  }

  private Map<Long, User> getUserMap(long userid) {
    User user = userDAO.getUserMap().get(userid);
    boolean isMale = (user != null && user.isMale());
    return userDAO.getUserMap(isMale);
  }

  public Set<String> getWithContext(long userid) {
    return getWithContext(getUserMap(userid));
  }

  private Set<String> getWithContext(Map<Long, User> userMap) {
    return getAudioForGender(userMap.keySet(), CONTEXT_REGULAR);
  }

  /**
   * select count(distinct exid) from audio where audiotype='regular';
   *
   * @param userMap
   * @param audioSpeed
   * @return
   * @see #getRecordedBy
   */
  private Set<String> getAudioForGender(Map<Long, User> userMap, String audioSpeed) {
    Set<Long> userIDs = userMap.keySet();
    return getAudioForGender(userIDs, audioSpeed);
  }

  private Set<String> getAudioForGender(Set<Long> userIDs, String audioSpeed) {
    Set<String> results = new HashSet<>();
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String s = getInClause(userIDs);
      if (!s.isEmpty()) s = s.substring(0, s.length() - 1);
      String sql = "SELECT distinct " + Database.EXID +
          " FROM " + AUDIO +
          " WHERE " +
          (s.isEmpty() ? "" : USERID + " IN (" + s + ") AND ") +
          DEFECT + "<>true " +
          " AND " + AUDIO_TYPE + "='" +
          audioSpeed +
          "' AND length(" + Database.EXID +
          ") > 0 ";
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String trim = rs.getString(1).trim();
        if (trim.isEmpty()) logger.warn("huh? got empty exid");
        results.add(trim);
      }
      //    logger.debug("for " + audioSpeed + " " + sql + " yielded " + results.size());
      finish(connection, statement, rs);

    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return results;
  }

/*  public Map<String, Integer> getExToCount() {
    String sql = "select exid, count(exid) from (select distinct exid,userid from audio where defect=false and audiotype='regular')  group by exid\n";

    Map<String, Integer> results = new HashMap<>();
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String trim = rs.getString(1).trim();
        results.put(trim, rs.getInt(2));
      }
      //    logger.debug("for " + audioSpeed + " " + sql + " yielded " + results.size());
      finish(connection, statement, rs);
      logger.debug("results returned " + results.size());

    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return results;
  }*/

  /**
   * @param total
   * @return
   * @paramx userMapMales
   * @paramx userMapFemales
   * @paramx uniqueIDs
   * @see DatabaseImpl#getMaleFemaleProgress
   */
/*  Map<String, Float> getRecordedReport(Map<Long, User> userMapMales,
                                       Map<Long, User> userMapFemales,
                                       float total,
                                       Set<String> uniqueIDs,
                                       float totalContext) {
    Set<Long> maleIDs = userMapMales.keySet();
    maleIDs = new HashSet<>(maleIDs);
    maleIDs.add((long) UserDAO.DEFAULT_MALE_ID);

    float maleFast = getCountForGender(maleIDs, REGULAR, uniqueIDs);
    float maleSlow = getCountForGender(maleIDs, SLOW, uniqueIDs);
    float male = getCountBothSpeeds(maleIDs, uniqueIDs);

    Set<Long> femaleIDs = userMapFemales.keySet();
    femaleIDs = new HashSet<>(femaleIDs);
    femaleIDs.add((long) UserDAO.DEFAULT_FEMALE_ID);

    float femaleFast = getCountForGender(femaleIDs, REGULAR, uniqueIDs);
    float femaleSlow = getCountForGender(femaleIDs, SLOW, uniqueIDs);
    float female = getCountBothSpeeds(femaleIDs, uniqueIDs);

    float cmale = getCountForGender(maleIDs, CONTEXT_REGULAR, uniqueIDs);
    float cfemale = getCountForGender(femaleIDs, CONTEXT_REGULAR, uniqueIDs);

    Map<String, Float> report = new HashMap<>();
    report.put(TOTAL, total);
    report.put(TOTAL_CONTEXT, totalContext);
    report.put(MALE, male);
    report.put(FEMALE, female);
    report.put(MALE_FAST, maleFast);
    report.put(MALE_SLOW, maleSlow);
    report.put(FEMALE_FAST, femaleFast);
    report.put(FEMALE_SLOW, femaleSlow);
    report.put(MALE_CONTEXT, cmale);
    report.put(FEMALE_CONTEXT, cfemale);
    return report;
  }*/

  /**
   * So here, instead of asking the database for which items have been recorded,
   * we ask the exercises directly for what has been attached to them.
   *
   * This accounts for more complicated logic in attach audio that tries to look for audio entries that
   * have matching transcripts for items without audio.
   *
   * @param total
   * @param totalContext
   * @param exercises
   * @return
   */
  Map<String, Float> getRecordedReportFromExercises(
      float total,
      float totalContext,
      Collection<CommonExercise> exercises) {

    float maleFast = getCountForGenderEx(true, REGULAR, exercises);
    float maleSlow = getCountForGenderEx(true, SLOW, exercises);
    List<String> audioTypes = Arrays.asList(REGULAR, SLOW);
    float male = getCountForGenderExIn(true, audioTypes, exercises);

    float femaleFast = getCountForGenderEx(false, REGULAR, exercises);
    float femaleSlow = getCountForGenderEx(false, SLOW, exercises);
    float female = getCountForGenderExIn(false, audioTypes, exercises);

    float cmale = getCountForGenderEx(true, CONTEXT_REGULAR, exercises);
    float cfemale = getCountForGenderEx(false, CONTEXT_REGULAR, exercises);

    Map<String, Float> report = new HashMap<>();
    report.put(TOTAL, total);
    report.put(TOTAL_CONTEXT, totalContext);
    report.put(MALE, male);
    report.put(FEMALE, female);
    report.put(MALE_FAST, maleFast);
    report.put(MALE_SLOW, maleSlow);
    report.put(FEMALE_FAST, femaleFast);
    report.put(FEMALE_SLOW, femaleSlow);
    report.put(MALE_CONTEXT, cmale);
    report.put(FEMALE_CONTEXT, cfemale);
    return report;
  }

  private int getCountForGenderEx(boolean isMale,
                                  String audioType,
                                  Collection<CommonExercise> exercises) {
    int n = 0;
    for (CommonExercise ex : exercises) {
      Collection<AudioAttribute> audioAttributes = ex.getAudioAttributes();
      for (AudioAttribute audioAttribute : audioAttributes) {
        if (audioAttribute.isMale() == isMale && audioAttribute.getAudioType().equals(audioType)) {
          n++;
          break;
        }
      }

    }
    return n;
  }

  private int getCountForGenderExIn(boolean isMale,
                                    Collection<String> audioTypes,
                                    Collection<CommonExercise> exercises) {
    int n = 0;
    Set<String> copy;

    for (CommonExercise ex : exercises) {
      copy = new HashSet<>(audioTypes);
      Collection<AudioAttribute> audioAttributes = ex.getAudioAttributes();
      for (AudioAttribute audioAttribute : audioAttributes) {
        String audioType = audioAttribute.getAudioType();

        if (audioAttribute.isMale() == isMale) {
          if (copy.contains(audioType)) {
            copy.remove(audioType);
            if (copy.isEmpty()) {
              n++;
              break;
            }
          }
        }
      }
    }
    return n;
  }

  /**
   * select count(*) from (select count(*) from (select DISTINCT exid, audiotype from audio where length(exid) > 0 and audiotype='regular' OR audiotype='slow' and defect<>true) where length(exid) > 0 group by exid)
   *
   * @return
   * @paramx userIds
   * @paramx audioSpeed
   * @seex #getRecordedReport(Map, Map, float, Set)
   */
/*  private int getCountForGender(Set<Long> userIds,
                                String audioSpeed,
                                Set<String> uniqueIDs) {
    Set<String> idsOfRecordedExercises = new HashSet<>();

    Set<String> idsOfStaleExercises = new HashSet<>();

    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String s = getInClause(userIds);
      // logger.info("checking speed " + audioSpeed + " on " + userIds.size() + " users and " + uniqueIDs.size() + " ex ids");
      if (!s.isEmpty()) s = s.substring(0, s.length() - 1);
      String sql = "select " +
          "distinct " + Database.EXID +
          " from " + AUDIO +
          " WHERE " +
          (s.isEmpty() ? "" : USERID + " IN (" + s + ") AND ") +
          DEFECT + "<>true " +
          " AND " + AUDIO_TYPE + "='" + audioSpeed + "' ";
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String exid = rs.getString(1);
        if (uniqueIDs.contains(exid)) {
          idsOfRecordedExercises.add(exid);
        } else {
          idsOfStaleExercises.add(exid);
          //        logger.debug("getCountForGender skipping stale exid " + exid);
        }
      }
      finish(connection, statement, rs);
*//*      logger.debug("getCountForGender : for " + audioSpeed + "\n\t" + sql + "\n\tgot " + idsOfRecordedExercises.size() +
          " and stale " +idsOfStaleExercises);*//*
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return idsOfRecordedExercises.size();
  }*/

  /*private int getCountBothSpeeds(Set<Long> userIds,
                                 Set<String> uniqueIDs) {
    Set<String> results = new HashSet<>();

    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String s = getInClause(userIds);
      if (!s.isEmpty()) s = s.substring(0, s.length() - 1);
//      String sql2 =
//          "select count(count1) from " +
//              " (select count(*) as count1 from " +
//              " (select DISTINCT exid, audiotype from " +
//              AUDIO +
//              " where length(exid) > 0 and audiotype='regular' OR audiotype='slow' and defect<>true " +
//              (s.isEmpty() ? "" : "AND " + USERID + " IN (" + s + ") ") +
//              ") where length(exid) > 0 group by exid) where count1 = 2";
////        ;

      String sql = "select exid from (select exid,count(*) as count1 from " +
          "(select DISTINCT exid, audiotype from audio " +
          "where length(exid) > 0 and audiotype='regular' OR audiotype='slow' and defect<>true " +
          (s.isEmpty() ? "" : "AND " + USERID + " IN (" + s + ") ") +
          ") " +
          "where length(exid) > 0 group by exid) where count1 = 2";

      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String id = rs.getString(1);
        if (uniqueIDs.contains(id)) {
          results.add(id);
        }
      }
      finish(connection, statement, rs);

    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    //logger.debug("both speeds " + results.size());
    return results.size();
  }*/
  private String getInClause(Set<Long> longs) {
    StringBuilder buffer = new StringBuilder();
    for (long id : longs) {
      buffer.append(id).append(",");
    }
    return buffer.toString();
  }


  /**
   * Items that are recorded must have both regular and slow speed audio.
   *
   * @param userid
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#markRecordedState
   */
  public Set<String> getRecordedForUser(long userid) {
    try {
      Set<String> validAudioAtReg = getValidAudioOfType(userid, REGULAR);
      Set<String> validAudioAtSlow = getValidAudioOfType(userid, SLOW);
      /*boolean b =*/
      validAudioAtReg.retainAll(validAudioAtSlow);
      return validAudioAtReg;
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<>();
  }

  /**
   * TODO make this a like "context=%"
   *
   * @param userid
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#markRecordedState(int, String, java.util.Collection, boolean)
   */
  public Set<String> getRecordedExampleForUser(long userid) {
    try {
      return getValidAudioOfType(userid, AUDIO_TYPE1);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<>();
  }

  private Set<String> getValidAudioOfType(long userid, String audioType) throws SQLException {
    String sql = "SELECT " + Database.EXID +
        " FROM " + AUDIO + " WHERE " + USERID + "=" + userid +
        " AND " + DEFECT + "<>true " +
        " AND " + AUDIO_TYPE + "='" + audioType + "'";

    // logger.debug("sql " + sql);
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    return getExidResultsForQuery(connection, statement);
  }

  /**
   * @param sql
   * @return
   * @throws SQLException
   * @see #getAudioAttributes()
   * @see #getAudioAttributes(String)
   */
  private List<AudioAttribute> getResultsSQL(String sql) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);
    return getResultsForQuery(connection, statement);
  }

  private int c = 0;

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
    List<AudioAttribute> results = new ArrayList<>();
    Map<Long, MiniUser> miniUsers = userDAO.getMiniUsers();

    while (rs.next()) {
      int uniqueID = rs.getInt(ID);
      long userID = rs.getLong(USERID);
      String exid = rs.getString(Database.EXID);
      Timestamp timestamp = rs.getTimestamp(Database.TIME);
      String audioRef = rs.getString(AUDIO_REF);

      String type = rs.getString(AUDIO_TYPE);
      int dur = rs.getInt(DURATION);
      String transcript = rs.getString(TRANSCRIPT);
      Float dnr = rs.getFloat(DNR);

      MiniUser user = miniUsers.get(userID);
      user = checkDefaultUser(userID, user);

      AudioAttribute audioAttr = new AudioAttribute(uniqueID, userID, //id
          exid, // id
          audioRef, // answer
          timestamp.getTime(),
          dur, type,
          user,
          transcript,
          dnr);

      if (user == null) {
        if (c++ < 20) {
          logger.warn("can't find user " + userID + " for " + audioAttr + " in " + miniUsers.keySet());
        }
      }
      results.add(audioAttr);
    }
    //   logger.debug("found " + results.size() + " audio attributes");

    finish(connection, statement, rs);

    return results;
  }

  private MiniUser checkDefaultUser(long userID, MiniUser user) {
    if (userID == UserDAO.DEFAULT_USER_ID) {
      user = UserDAO.DEFAULT_USER;
    } else if (userID == UserDAO.DEFAULT_MALE_ID) {
      user = UserDAO.DEFAULT_MALE;
    } else if (userID == UserDAO.DEFAULT_FEMALE_ID) {
      user = UserDAO.DEFAULT_FEMALE;
    }
    return user;
  }

  private Set<String> getExidResultsForQuery(Connection connection, PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    Set<String> results = new HashSet<>();
    while (rs.next()) {
      results.add(rs.getString(Database.EXID));
    }
    finish(connection, statement, rs);

    return results;
  }

  /**
   * @see mitll.langtest.server.database.ImportCourseExamples#copyAudio
   */
  public long add(Result result, int userid, String path) {
    try {
      long then = System.currentTimeMillis();
      //  logger.debug("path was " + result.answer + " now '" + audioRef + "'");
      if (userid < 1) {
        logger.error("huh? userid is " + userid);
        new Exception().printStackTrace();
      }

      long newid = add(connection, result, userid, path, "unknownTranscript");
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
   * Go back and mark gender on really old audio that had no user info on it.
   *
   * @param userid
   * @param attr
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#markGender(mitll.langtest.shared.exercise.AudioAttribute, boolean)
   */
  public void addOrUpdateUser(int userid, AudioAttribute attr) {
    long timestamp = attr.getTimestamp();
    if (timestamp == 0) timestamp = System.currentTimeMillis();
    addOrUpdateUser(userid, attr.getAudioRef(), attr.getExid(), timestamp, attr.getAudioType(),
        (int) attr.getDurationInMillis(), UNKNOWN);
  }

  /**
   * @param userid
   * @param audioRef
   * @param exerciseID
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#editItem(mitll.langtest.shared.custom.UserExercise)
   * @see mitll.langtest.server.database.custom.UserExerciseDAO#addMissingAudio(mitll.langtest.shared.custom.UserExercise, String, String)
   */
/*  public long add(int userid, String audioRef, String exerciseID, long timestamp, String audioType, long durationInMillis) {
    try {
      if (isBadUser(userid)) {
        logger.error("huh? userid is " + userid);
        new Exception().printStackTrace();
      }

      logger.debug("added exerciseID " + exerciseID + " ref '" + audioRef + "' " +audioType + " for " + userid);
      long newid = addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis);
      database.closeConnection(connection);
      return newid;
    } catch (Exception ee) {
      logger.error("addAnswer got " + ee, ee);
    }
    return -1;
  }*/

  /**
   * Add a row to the table.
   * Each insert is marked with a timestamp.
   * This allows us to determine user completion rate.
   * <p>
   * <p>
   * "id IDENTITY, " +
   * "userid INT, " +
   * Database.EXID + " VARCHAR, " +
   * Database.TIME + " TIMESTAMP, " +// " AS CURRENT_TIMESTAMP," +
   * "audioRef CLOB," +
   * AUDIO_TYPE + " VARCHAR," +
   * DURATION + " INT" +
   *
   * @param connection
   * @param transcript
   * @throws java.sql.SQLException
   * @see #add(mitll.langtest.shared.Result, int, String)
   */
  private long add(Connection connection, Result result, int userid, String audioRef, String transcript) throws SQLException {
    String exerciseID = result.getExerciseID();
    long timestamp = result.getTimestamp();
    String audioType = result.getAudioType();
    int durationInMillis = result.getDurationInMillis();
    logger.debug("add result - " + result.getID() + " for " + userid + " ref " + audioRef);
    return addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis, transcript);
  }

  /**
   * @param existing
   * @param newTranscript
   * @see DatabaseImpl#editItem(CommonExercise, boolean)
   */
  void copyWithNewTranscript(AudioAttribute existing, String newTranscript) {
    String exerciseID = existing.getExid();
    long timestamp = existing.getTimestamp();
    String audioType = existing.getAudioType();
    long durationInMillis = existing.getDurationInMillis();
    logger.debug("copyWithNewTranscript existing - " + existing);
    try {
      addAudio(connection, (int) existing.getUserid(), existing.getAudioRef(), exerciseID, timestamp, audioType, durationInMillis, newTranscript);
    } catch (SQLException e) {
      logger.error("Got " + e, e);
    }
  }

  /**
   * Why does this have to be so schizo? add or update -- should just choose
   *
   * @param userid           part of unique id
   * @param audioRef
   * @param exerciseID       part of unique id
   * @param timestamp
   * @param audioType        part of unique id
   * @param durationInMillis
   * @param transcript
   * @return AudioAttribute that represents the audio that has been added to the exercise
   * @see mitll.langtest.server.LangTestDatabaseImpl#addToAudioTable
   * @see #addOrUpdateUser(int, AudioAttribute)
   */
  private void addOrUpdateUser(int userid, String audioRef, String exerciseID, long timestamp, String audioType,
                               int durationInMillis, String transcript) {
    if (isBadUser(userid)) {
      logger.error("huh? userid is " + userid);
      new Exception().printStackTrace();
    }
    try {
      logger.debug("addOrUpdate userid = " + userid + " audio ref " + audioRef + " ex " + exerciseID + " at " +
          new Date(timestamp) + " type " + audioType + " dur " + durationInMillis);
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO + " " +
          "SET " + USERID + "=? " +
          "WHERE " +
          Database.EXID + "=?" + " AND " +
          AUDIO_TYPE + "=? AND " +
          DEFECT + "=FALSE AND " +
          AUDIO_REF + "=?";

      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setInt(ii++, userid);
      statement.setString(ii++, exerciseID);
      statement.setString(ii++, audioType);
      statement.setString(ii++, audioRef);

      int i = statement.executeUpdate();

      if (i == 0) { // so we didn't update, so we need to add it
        logger.debug("\taddOrUpdate adding entry for  " + userid + " " + audioRef + " ex " + exerciseID +
            " at " + new Date(timestamp) + " type " + audioType + " dur " + durationInMillis);

        long l = addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis, transcript);
      }

      finish(connection, statement);

    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * TODO : Why does this have to be so schizo? add or update -- should just choose?
   * <p>
   * This guarantees that there will only be one row in the audio table for the key "user-exid-speed",
   * e.g. user 20 exid 5 and speed "regular" will only appear once if it's not defective.
   *
   * @param userid           part of unique id
   * @param exerciseID       part of unique id
   * @param audioType        part of unique id
   * @param audioRef
   * @param timestamp
   * @param durationInMillis
   * @param transcript
   * @param dnr
   * @return AudioAttribute that represents the audio that has been added to the exercise
   * @see mitll.langtest.server.LangTestDatabaseImpl#addToAudioTable
   */
  public AudioAttribute addOrUpdate(int userid, String exerciseID, String audioType, String audioRef, long timestamp,
                                    long durationInMillis, String transcript, float dnr) {
    if (isBadUser(userid)) {
      logger.error("huh? userid is " + userid, new Exception("huh? userid is " + userid));
    }
    try {
      //logger.debug("addOrUpdate " + userid + " " + audioRef + " ex " + exerciseID + " at " + new Date(timestamp) + " type " + audioType + " dur " + durationInMillis);
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO +
          " SET " +
          AUDIO_REF + "=?," +
          Database.TIME + "=?," +
          ResultDAO.DURATION + "=?, " +
          TRANSCRIPT + "=? " +

          "WHERE " +
          Database.EXID + "=? AND " +
          USERID + "=? AND " +
          AUDIO_TYPE + "=? AND " +
          DEFECT + "=FALSE";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setString(ii++, audioRef);
      statement.setTimestamp(ii++, new Timestamp(timestamp));
      statement.setInt(ii++, (int) durationInMillis);
      statement.setString(ii++, transcript);

      statement.setString(ii++, exerciseID);
      statement.setInt(ii++, userid);
      statement.setString(ii++, audioType);

      int i = statement.executeUpdate();

      AudioAttribute audioAttr;
      if (i == 0) {
        logger.debug("\taddOrUpdate *adding* entry for  " + userid + " " + audioRef + " ex " + exerciseID +// " at " + new Date(timestamp) +
            " type " + audioType + " dur " + durationInMillis);

        long l = addAudio(connection, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis, transcript);
        audioAttr = getAudioAttribute((int) l, userid, audioRef, exerciseID, timestamp, audioType, durationInMillis, transcript, dnr);
      } else {
        logger.debug("\taddOrUpdate updating entry for  " + userid + " " + audioRef + " ex " + exerciseID +
            " type " + audioType + " dur " + durationInMillis);
        audioAttr = getAudioAttribute(userid, exerciseID, audioType);
      }
      //  logger.debug("returning " + audioAttr);
      finish(connection, statement);

      return audioAttr;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return null;
  }

  private AudioAttribute getAudioAttribute(int userid, String exerciseID, String audioType) {
    AudioAttribute audioAttr = null;
    Collection<AudioAttribute> audioAttributes = getAudioAttributes(exerciseID);
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
    return audioAttr;
  }

  /**
   * @param i
   * @param userid
   * @param audioRef
   * @param exerciseID
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @param transcript
   * @return
   * @see #addOrUpdate
   */
  private AudioAttribute getAudioAttribute(int i,
                                           int userid, String audioRef, String exerciseID, long timestamp,
                                           String audioType, long durationInMillis, String transcript, float dnr) {
    return new AudioAttribute(i, userid,
        exerciseID, // id
        audioRef, // answer
        timestamp,
        durationInMillis, audioType,
        userDAO.getMiniUser(userid), transcript, dnr);
  }

  public void updateExerciseID(int uniqueID, String exerciseID) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO +
          " SET " +
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

      finish(connection, statement);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  public void updateDNR(int uniqueID, float dnr) {
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      String sql = "UPDATE " + AUDIO +
          " SET " +
          DNR +
          "=? " +
          "WHERE " +
          ID + "=?";
      PreparedStatement statement = connection.prepareStatement(sql);

      int ii = 1;

      statement.setFloat(ii++, dnr);
      statement.setInt(ii++, uniqueID);

      int i = statement.executeUpdate();

      if (i == 0) {
        logger.error("huh? couldn't update audio " + uniqueID + " to " + dnr);
      }

      finish(connection, statement);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * @param attribute
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getAndMarkDefects
   * @see DatabaseImpl#markAudioDefect(AudioAttribute)
   */
  public int markDefect(AudioAttribute attribute) {
    return markDefect((int) attribute.getUserid(), attribute.getExid(), attribute.getAudioType());
  }

  /**
   * An audio cut is uniquely identified by by exercise id, speed (reg/slow), and who recorded it.
   *
   * @param userid     recorded by this user
   * @param exerciseID on this exercise
   * @param audioType  at this speed
   * @return > 0 if audio was marked defective
   * @see mitll.langtest.server.database.DatabaseImpl#editItem
   * @see mitll.langtest.client.custom.dialog.EditableExerciseDialog#postEditItem
   */
  private int markDefect(int userid, String exerciseID, String audioType) {
    try {
      if (audioType.equals(AudioAttribute.REGULAR_AND_SLOW)) {
        audioType = Result.AUDIO_TYPE_FAST_AND_SLOW;
      }
      Connection connection = database.getConnection(this.getClass().toString());
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
        logger.error("huh? couldn't find audio by " + userid + " for ex " + exerciseID + " and " + audioType);
      } else {
        logger.debug("Num modified = " + i + ", marked audio defect by " + userid + " ex " + exerciseID + " speed " + audioType);
      }

      finish(connection, statement);
      return i;
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
    return -1;
  }

  /**
   * @param connection
   * @param userid
   * @param audioRef
   * @param exerciseID
   * @param timestamp
   * @param audioType
   * @param durationInMillis
   * @param transcript
   * @return key from insert
   * @throws SQLException
   * @see #add(Connection, Result, int, String, String)
   */
  private long addAudio(Connection connection,
                        int userid,
                        String audioRef,
                        String exerciseID,
                        long timestamp,
                        String audioType,
                        long durationInMillis,
                        String transcript) throws SQLException {
    if (isBadUser(userid)) {
      logger.error("huh? userid is " + userid);
      new Exception().printStackTrace();
    }
    // logger.debug("addAudio : by " + userid + " for ex " + exerciseID + " type " + audioType + " ref " + audioRef);
    int before = 0;//DEBUG ? getCount(AUDIO) : 0;

    PreparedStatement statement = connection.prepareStatement("INSERT INTO " + AUDIO +
        "(" +
        USERID + "," +
        Database.EXID + "," +
        Database.TIME + "," +
        AUDIO_REF + "," +
        ResultDAO.AUDIO_TYPE + "," +
        ResultDAO.DURATION + "," +
        TRANSCRIPT + "," +
        DEFECT +
        ") VALUES(?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

    int i = 1;

    statement.setInt(i++, userid);
    statement.setString(i++, exerciseID);
    statement.setTimestamp(i++, new Timestamp(timestamp));
    statement.setString(i++, audioRef);
    statement.setString(i++, audioType);
    statement.setLong(i++, durationInMillis);
    statement.setString(i++, transcript);
    statement.setBoolean(i++, false);

    statement.executeUpdate();

    long newID = getGeneratedKey(statement);
    if (newID == -1) {
      logger.error("addAudio : huh? no key was generated?");
    } else {
      //logger.debug("key was " + newID);
    }

    statement.close();
    connection.commit();

    int after = DEBUG ? getCount(AUDIO) : 1;
    if (DEBUG && before == after) {
      logger.error("huh? after adding " + after + " but before " + before);
    }
    return newID;
  }

  private boolean isBadUser(int userid) {
    return userid < UserDAO.DEFAULT_FEMALE_ID;
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
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
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
        DEFECT + " BOOLEAN DEFAULT FALSE, " +
        TRANSCRIPT + " VARCHAR" +
        ")");
    statement.execute();
    statement.close();
    index(database);
  }

  private void index(Database database) throws SQLException {
    createIndex(database, Database.EXID, AUDIO);
  }
}