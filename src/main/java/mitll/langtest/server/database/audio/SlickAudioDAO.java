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

package mitll.langtest.server.database.audio;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickAudio;
import mitll.npdata.dao.audio.AudioDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Tuple2;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

public class SlickAudioDAO extends BaseAudioDAO implements IAudioDAO {
  private static final Logger logger = LogManager.getLogger(SlickAudioDAO.class);
  private static final String BEST_AUDIO = "bestAudio";

  private final AudioDAOWrapper dao;
  private final long now = System.currentTimeMillis();
  private final long before = now - (24 * 60 * 60 * 1000);
  private final boolean doCheckOnStartup;
  private int defaultResult;
  private final ServerProperties serverProps;

  public SlickAudioDAO(Database database, DBConnection dbConnection, IUserDAO userDAO) {
    super(database, userDAO);
    dao = new AudioDAOWrapper(dbConnection);
    serverProps = database.getServerProps();
    doCheckOnStartup = serverProps.doAudioCheckOnStartup();
  }

  public Map<String,Integer> getPairs(int projid) {
    return dao.getPairs(projid);
  }
  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * @param projid
   * @return
   * @see BaseAudioDAO#getExToAudio
   */
  @Override
  public Collection<AudioAttribute> getAudioAttributesByProjectThatHaveBeenChecked(int projid) {
    List<SlickAudio> all = dao.getAll(projid);
    logger.info("getAudioAttributesByProjectThatHaveBeenChecked " + projid + " found " + all.size() + " that has been checked to exist.");
    return toAudioAttribute(all);
  }

  @Override
  public AudioAttribute addOrUpdate(AudioInfo info) {
    MiniUser miniUser = userDAO.getMiniUser(info.getUserid());
    return toAudioAttribute(
        dao.addOrUpdate(
            info.getUserid(),
            info.getExerciseID(),
            info.getProjid(),
            info.getAudioType().toString(),
            info.getAudioRef(),
            info.getTimestamp(),
            info.getDurationInMillis(),
            info.getTranscript(),
            info.getDnr(),
            info.getResultID()),
        miniUser);
  }

  /**
   * Update the user if the audio is already there.
   *
   * @paramx userid
   * @paramx exerciseID
   * @paramx projid
   * @paramx audioType
   * @paramx audioRef
   * @paramx timestamp
   * @paramx durationInMillis
   * @paramx transcript
   * @paramx dnr
   */
  @Override
  public void addOrUpdateUser(
      AudioInfo info) {
    dao.addOrUpdateUser(info.getUserid(), info.getExerciseID(), info.getProjid(), info.getAudioType().toString(), info.getAudioRef(), info.getTimestamp(), info.getDurationInMillis(), info.getTranscript(), info.getDnr(), info.getResultID());
  }

  /**
   * @param uniqueID
   * @param exerciseID
   * @param actualPath
   */
  @Override
  public void updateExerciseID(int uniqueID, int exerciseID, String actualPath) {
    dao.updateExerciseID(uniqueID, exerciseID, actualPath);
  }

  /**
   * @param projid
   * @param installPath
   * @param language
   * @param checkAll
   * @see #makeSureAudioIsThere
   */
  private void validateFileExists(int projid, String installPath, String language, boolean checkAll) {
    long pastTime = doCheckOnStartup || checkAll ? now : before;
    logger.debug("validateFileExists before " + new Date(pastTime));
    dao.validateFileExists(projid, pastTime, installPath, language.toLowerCase());
  }

  @Override
  public void updateDNR(int uniqueID, float dnr) {
    logger.info("add impl for updateDNR...");
  }

  public boolean didFindAnyAudioFiles(int projectid) {
    return dao.getCountExists(projectid) > 0;
  }

  /**
   * @see #attachAudioToExercise
   * @param exid
   * @return
   */
  @Override
  Collection<AudioAttribute> getAudioAttributesForExercise(int exid) {
    long then = System.currentTimeMillis();
    List<SlickAudio> byExerciseID = dao.getByExerciseID(exid);
    long now = System.currentTimeMillis();
    if (now - then > 20)
      logger.warn("getAudioAttributesForExercise took " + (now - then) + " to get " + byExerciseID.size() + " attr for " + exid);
    return toAudioAttributes(byExerciseID);
  }

  /**
   * @see BaseAudioDAO#attachAudioToExercises(Collection, String)
   * @param exids
   * @return
   */
  Map<Integer, List<AudioAttribute>> getAudioAttributesForExercises(Set<Integer> exids) {
    long then = System.currentTimeMillis();
    Map<Integer, List<SlickAudio>> byExerciseID = dao.getByExerciseIDs(exids);
    long now = System.currentTimeMillis();

    if (now - then > 30) {
      logger.warn("getAudioAttributesForExercise took " + (now - then) + " to get " + byExerciseID.size() + " attr for " + exids.size());
    }

    Map<Integer, List<AudioAttribute>> copy = new HashMap<>(byExerciseID.size());
    for (Map.Entry<Integer, List<SlickAudio>> pair : byExerciseID.entrySet()) {
      copy.put(pair.getKey(), toAudioAttributes(pair.getValue()));
    }
    if (copy.size() != exids.size()) {
      logger.info("getAudioAttributesForExercises asked for " + exids.size() + " exercises, but only found " + copy.size());
    }
    return copy;
  }

  /**
   * Do this differently
   *
   * @param projid
   * @param audioSpeed
   * @param uniqueIDs
   * @param exToTranscript
   * @param idsOfRecordedExercisesForFemales
   * @return
   * @see BaseAudioDAO#getRecordedReport
   */
  @Override
  void getCountForGender(int projid, AudioType audioSpeed,
                         Set<Integer> uniqueIDs,
                         Map<Integer, String> exToTranscript,
                         Set<Integer> idsOfRecordedExercisesForMales,
                         Set<Integer> idsOfRecordedExercisesForFemales) {


    Map<Integer, Collection<Tuple2<Integer, Integer>>> countForGender4 =
        dao.getCountForGender(audioSpeed.toString(), uniqueIDs, exToTranscript, true, projid);


    for (Integer key : countForGender4.keySet()) {
      boolean male = userDAO.isMale(key);
      Collection<Tuple2<Integer, Integer>> tuple2s = countForGender4.get(key);

      Iterator<Tuple2<Integer, Integer>> iterator1 = tuple2s.iterator();
      //   scala.collection.Iterator<Tuple2<Object, Object>> iterator = tuple2s.iterator();

      Set<Integer> exIDs = getExIDs(iterator1);

      if (male) {
        idsOfRecordedExercisesForMales.addAll(exIDs);
      } else {
        idsOfRecordedExercisesForFemales.addAll(exIDs);
      }
    }
  }

  @Override
  Set<Integer> getValidAudioOfType(int userid, AudioType audioType) {
    return dao.getExerciseIDsOfValidAudioOfType(userid, audioType.toString());
  }

  /**
   * Items that are recorded must have both regular and slow speed audio.
   *
   * @param userid
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#markRecordedState
   */
  public Collection<Integer> getRecordedExForUser(int userid) {
    try {
      return dao.getRecordedForUser(userid);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<>();
  }

  @Override
  int markDefect(int userid, int exerciseID, AudioType audioType) {
    return dao.markDefect(userid, exerciseID, audioType.toString());
  }

/*  public Collection<Integer> getRecordedBySameGender(int userid) {
    Collection<Integer> userIDs = getUserIDs(userid);
    return dao.getAudioForGenderBothSpeeds(userIDs);
  }*/

  /**
   * TODO TODO :
   * <p>
   * only include ids for audio where the audio transcript matches the exercise text
   *
   * @param audioSpeed
   * @param exToTranscript
   * @param projid
   * @return modifiable set - what's returned from scala isn't
   * @paramx userIDs
   * @see #getWithContext(boolean, Map, int)
   */
  @Override
  Set<Integer> getAudioExercisesForGender(boolean isMale,
                                          String audioSpeed,
                                          Map<Integer, String> exToTranscript,
                                          int projid) {
    if (database.getServerProps().shouldCheckAudioTranscript()) {
      logger.warn("getAudioExercisesForGender should do check audio transcript - ?");
    }

    Map<Integer, Collection<Tuple2<Integer, Integer>>> audioForGender1 = dao.getAudioForGender(audioSpeed, projid);
    Set<Pair> genderMatch = getGenderMatch(isMale, audioForGender1);
    return getExercises(genderMatch);

//    Seq<Tuple2<Object, Object>> audioForGender1 = (Seq<Tuple2<Object, Object>>) audioForGender;
//
//    scala.collection.Iterator<Tuple2<Object, Object>> iterator = audioForGender1.iterator();
//
//    HashSet<Pair> pairs = new HashSet<>();
//    while (iterator.hasNext()
//        ) {
//      Tuple2<Object, Object> next = iterator.next();
//      Object o = next._1();
//      Integer exid = (Integer) o;
//      Integer userid = (Integer) next._2();
//      Pair pair = new Pair(exid, userid);
//      pairs.add(pair);
//    }
//    return pairs;
  }

  /**
   * @param isMale
   * @param regSpeed
   * @param slowSpeed
   * @param exToTranscript
   * @param projid
   * @return
   * @see #getRecordedBySameGender(int, Map, int)
   */
  @Override
  Set<Integer> getAudioExercisesForGenderBothSpeeds(boolean isMale,
                                                    String regSpeed,
                                                    String slowSpeed,
                                                    Map<Integer, String> exToTranscript,
                                                    int projid) {
    if (database.getServerProps().shouldCheckAudioTranscript()) {
      logger.warn("getAudioExercisesForGender should do check audio transcript - ?");
    }

    Tuple2<Map<Integer, Collection<Tuple2<Integer, Integer>>>,
        Map<Integer, Collection<Tuple2<Integer, Integer>>>> audioForGenderBothRecorded = dao.getAudioForGenderBothRecorded(regSpeed, slowSpeed, projid);


    Map<Integer, Collection<Tuple2<Integer, Integer>>> regSpeedPairs = audioForGenderBothRecorded._1();
    Map<Integer, Collection<Tuple2<Integer, Integer>>> slowSpeedPairs = audioForGenderBothRecorded._2();

    Set<Pair> regSpeedGenderMatch = getGenderMatch(isMale, regSpeedPairs);
    Set<Pair> slowSpeedGenderMatch = getGenderMatch(isMale, slowSpeedPairs);

    // now find overlap

    Set<Integer> regExids = getExercises(regSpeedGenderMatch);
    Set<Integer> slowExids = getExercises(slowSpeedGenderMatch);

    regExids.retainAll(slowExids);

    return regExids;
  }

  @NotNull
  private Set<Integer> getExercises(Set<Pair> regSpeedGenderMatch) {
    Set<Integer> regExids = new HashSet<>();
    for (Pair p : regSpeedGenderMatch) regExids.add(p.getExid());
    return regExids;
  }

  private Set<Pair> getGenderMatch(boolean isMale, Map<Integer, Collection<Tuple2<Integer, Integer>>> regSpeedPairs) {
    Set<Pair> regSpeedGenderMatch = new HashSet<>();
    for (Integer key : regSpeedPairs.keySet()) {
      boolean male = userDAO.isMale(key);
      if (male == isMale) {
        Iterator<Tuple2<Integer, Integer>> iterator = regSpeedPairs.get(key).iterator();
        regSpeedGenderMatch.addAll(getPairs(iterator));
      }
    }
    return regSpeedGenderMatch;
  }

  private Set<Pair> getPairs(Iterator<Tuple2<Integer, Integer>> iterator) {
    Set<Pair> pairs = new HashSet<>();
    while (iterator.hasNext()) {
      Tuple2<Integer, Integer> next = iterator.next();
      Integer exid = next._1();
      Integer userid = next._2();
      Pair pair = new Pair(exid, userid);
      pairs.add(pair);
    }
    return pairs;
  }


  /**
   * TODO : why do we return a tuple when we don't use the second one?
   *
   * @param iterator
   * @return
   */
  private Set<Integer> getExIDs(Iterator<Tuple2<Integer, Integer>> iterator) {
    Set<Integer> pairs = new HashSet<>();
    while (iterator.hasNext()
        ) {
      Tuple2<Integer, Integer> next = iterator.next();
      //  Integer o = next._1();
      //Integer userid = (Integer) next._2();
      pairs.add(next._1());
    }
    return pairs;
  }

  public static class Pair {
    private final int exid;
    private final int userid;

    public Pair(int exid, int userid) {
      this.exid = exid;
      this.userid = userid;
    }

    public int getExid() {
      return exid;
    }

 /*   public int getUserid() {
      return userid;
    }*/

    @Override
    public boolean equals(Object obj) {
      Pair po = (Pair) obj;
      return Integer.valueOf(exid).equals(po.exid) && Integer.valueOf(userid).equals(po.userid);
    }

    @Override
    public int hashCode() {
      return exid + userid;
    }
  }

  /**
   * TODO : consider cache for mini users
   *
   * @param all
   * @return
   */
  private List<AudioAttribute> toAudioAttributes(Collection<SlickAudio> all) {
    List<AudioAttribute> copy = new ArrayList<>();
    Map<Integer, MiniUser> idToMini = new HashMap<>();

    for (SlickAudio s : all) {
      copy.add(getAudioAttribute(s, idToMini));
    }
    return copy;
  }

  private int spew = 0;

  /**
   * Actual audio path is where we find it on the server... potentially different from where it was originally recorded...
   *
   * @param s
   * @param miniUser
   * @return
   * @see #toAudioAttribute
   */
  private AudioAttribute toAudioAttribute(SlickAudio s, MiniUser miniUser) {
    if (miniUser == null && spew++ < 20) {
      logger.error("toAudioAttribute : no user for " + s.userid());
    }

    String audiotype = s.audiotype();

    if (audiotype.contains("=")) {
      audiotype = audiotype.replaceAll("=", "_");
    }
    return new AudioAttribute(
        s.id(),
        s.userid(),
        s.exid(),
        s.audioref(),
        s.modified().getTime(),
        s.duration(),
        AudioType.valueOf(audiotype.toUpperCase()),
        miniUser,
        s.transcript(),
        s.actualpath(),
        s.dnr(),
        s.resultid());
  }

  private int c = 0;

  /**
   * @param orig
   * @param oldToNewUser
   * @param projid
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyAudio
   */
  public SlickAudio getSlickAudio(AudioAttribute orig, Map<Integer, Integer> oldToNewUser, int projid) {
    AudioType audioType = orig.getAudioType();
    if (audioType == null) {
      audioType = AudioType.REGULAR;
      logger.error("getSlickAudio found missing audio type " + orig);
    }
    Integer userid = oldToNewUser.get(orig.getUserid());
    if (userid == null) {
      if (c++ < 100) {
        logger.error("getSlickAudio huh? no user id for " + orig.getUserid() + " for " + orig + " in " + oldToNewUser.size());
      }
      return null;
    } else {
      long timestamp = orig.getTimestamp();

      int resultid = orig.getResultid();

      if (resultid == -1) {
        resultid = defaultResult;
      }
      String audioRef = orig.getAudioRef();
      if (audioRef.startsWith("config")) {
        audioRef = removeConfigPrefix(audioRef);
      }
      return new SlickAudio(
          orig.getUniqueID(),
          userid,
          orig.getExid(),
          new Timestamp(timestamp),
          audioRef,
          audioType.toString(),
          orig.getDurationInMillis(),
          false,
          orig.getTranscript(),
          projid,
          false,
          0,
          "",
          orig.getDnr(),
          resultid);
    }
  }

  @NotNull
  private static String removeConfigPrefix(String audioRef) {
    int bestAudio = audioRef.indexOf(BEST_AUDIO);
    if (bestAudio == -1) logger.error("huh? expecting bestaudio in "+audioRef);
    else {
      audioRef = audioRef.substring(bestAudio);
    }
    return audioRef;
  }

  /**
   * @param all
   * @return
   * @paramx idToMini
   * @see #getAudioAttributesByProjectThatHaveBeenChecked(int)
   */
  private List<AudioAttribute> toAudioAttribute(Collection<SlickAudio> all) {
    List<AudioAttribute> copy = new ArrayList<>();
    if (all.isEmpty()) {
      logger.warn("toAudioAttribute table has " + dao.getNumRows() + " rows but no audio?");
    }
    Map<Integer, MiniUser> idToMini = new HashMap<>();
    for (SlickAudio s : all) {
      copy.add(getAudioAttribute(s, idToMini));
    }
    return copy;
  }

  private AudioAttribute getAudioAttribute(SlickAudio s, Map<Integer, MiniUser> idToMini) {
    MiniUser miniUser = idToMini.computeIfAbsent(s.userid(), k -> userDAO.getMiniUser(s.userid()));
    //      logger.info("got " + s);
    return toAudioAttribute(s, miniUser);
  }

  public void addBulk(List<SlickAudio> bulk)  { dao.addBulk(bulk);  }
/*  public void addBulk2(List<SlickAudio> bulk) {
    dao.addBulk2(bulk);
  }*/
/*  public int getNumRows() {
    return dao.getNumRows();
  }*/

/*  public int getDefaultResult() {
    // logger.info("\n\n\tdefault id is  " + defaultResult);
    return defaultResult;
  }*/

  public void setDefaultResult(int defaultResult) {
    this.defaultResult = defaultResult;
    // logger.info("\n\n\tdefault id is  " + defaultResult);
  }

  /**
   * TODO : also check that the compressed versions are there too!
   * @param projectID
   * @param language
   * @param validateAll
   * @see mitll.langtest.server.services.AudioServiceImpl#checkAudio
   */
  public void makeSureAudioIsThere(int projectID, String language, boolean validateAll) {
    boolean foundFiles = didFindAnyAudioFiles(projectID);
    String mediaDir = serverProps.getMediaDir();
    File file = new File(mediaDir);
    //logger.info("makeSureAudioIsThere " + projectID + " " + language + " " + validateAll);
    if (file.exists()) {
      //logger.debug("makeSureAudioIsThere media dir " + file + " exists ");
      if (file.isDirectory()) {
        String[] list = file.list();
        if (list == null) {
          logger.error("setAudioDAO configuration error - can't get files from media directory " + mediaDir);
        } else if (list.length > 0) { // only on pnetprof (behind firewall), znetprof has no audio, might have a directory.
          logger.debug("setAudioDAO validating files under " + file.getAbsolutePath());
          if (validateAll ||
              (serverProps.doAudioChecksInProduction() &&
                  (serverProps.doAudioFileExistsCheck() || !foundFiles))) {
            logger.debug("makeSureAudioIsThere validateFileExists ");
            validateFileExists(projectID, mediaDir, language, validateAll);
          }
        }
      } else {
        logger.error("configuration error - expecting media directory " + mediaDir + " to be directory.");
      }
    } else {
      logger.warn("makeSureAudioIsThere : configuration error? - expecting a media directory " + mediaDir);
    }
  }

  /**
   * For now, default to regular speed audio.
   * maybe somehow later choose slow?
   * <p>
   * Eventually consider concept of preferred voices??
   *
   * @param userToGender
   * @param userid
   * @param exercise
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getNativeAudio
   */
  @Nullable
  public String getNativeAudio(Map<Integer, MiniUser.Gender> userToGender, int userid, CommonExercise exercise, String language) {
    //String nativeAudio = null;
    if (exercise != null) {
      MiniUser.Gender orDefault = getGender(userToGender, userid);
      if (orDefault == null) {
        logger.error("getNativeAudio can't find user " + userid);
        return null; // no user with this id?
      }

      if (!exercise.hasRefAudio()) {
        logger.info("Attach audio to " + exercise.getID());
        attachAudioToExercise(exercise,language);
      }

      AudioAttribute audioAttributePrefGender = exercise.getAudioAttributePrefGender(orDefault == MiniUser.Gender.Male, true);

      if (audioAttributePrefGender == null) {
        logger.warn("no audio for " + exercise.getID());
        return null;
      }
      else {
        return audioAttributePrefGender.getAudioRef();
      }

/*
      Map<MiniUser, List<AudioAttribute>> userMap = exercise.getUserMap(orDefault == MiniUser.Gender.Male,false);
      Collection<List<AudioAttribute>> audioByMatchingGender = userMap.values();

      if (audioByMatchingGender.isEmpty()) { // ok, no audio with matching gender, fall back to other audio
        userMap = exercise.getUserMap(orDefault != MiniUser.Gender.Male,false);
        audioByMatchingGender = userMap.values();
      }

      if (audioByMatchingGender.isEmpty()) {
        // logger.warn("no audio for " + userid + " and " + exercise.getID());
        //missing++;
      } else {
        nativeAudio = getRegularSpeedFromARecorder(audioByMatchingGender);

        if (nativeAudio == null) {
          nativeAudio = getSlowSpeedFromARecorder(audioByMatchingGender);
        }
      }*/
    }
    else {
      return null;
    }
  }

/*  @Nullable
  private String getRegularSpeedFromARecorder(Collection<List<AudioAttribute>> audioByMatchingGender) {
    String nativeAudio = null;
    for (List<AudioAttribute> audioByRecorder : audioByMatchingGender) {
      if (audioByRecorder.isEmpty()) {
        logger.debug("huh? no audio on " + audioByRecorder);
      } else {
        nativeAudio = getRegularSpeed(audioByRecorder);
        if (nativeAudio != null) break;
      }
    }
    return nativeAudio;
  }*/

/*  private String getRegularSpeed(List<AudioAttribute> audioByRecorder) {
    String nativeAudio = null;
    for (AudioAttribute audioAttribute : audioByRecorder) {
      if (audioAttribute.isRegularSpeed()) {
        nativeAudio = audioAttribute.getAudioRef();
        break;
      }
    }
    return nativeAudio;
  }*/
/*
  @Nullable
  private String getSlowSpeedFromARecorder(Collection<List<AudioAttribute>> audioByMatchingGender) {
    String nativeAudio = null;
    for (List<AudioAttribute> audioByRecorder : audioByMatchingGender) {
      if (audioByRecorder.isEmpty()) {
        logger.debug("huh? no audio on " + audioByRecorder);
      } else {
        nativeAudio = getSlowSpeed(audioByRecorder);
        if (nativeAudio != null) break;
      }
    }
    return nativeAudio;
  }*/

/*  private String getSlowSpeed(List<AudioAttribute> audioByRecorder) {
    String nativeAudio = null;
    for (AudioAttribute audioAttribute : audioByRecorder) {
      if (audioAttribute.isRegularSpeed()) {
        nativeAudio = audioAttribute.getAudioRef();
        break;
      }
    }
    return nativeAudio;
  }*/

  @Nullable
  private MiniUser.Gender getGender(Map<Integer, MiniUser.Gender> userToGender, int userid) {
    MiniUser.Gender orDefault = userToGender.get(userid);
    if (orDefault == null) {
      User byID = userDAO.getByID(userid);
      if (byID == null) {
        logger.warn("getNativeAudio huh? can't find " + userid);
      } else {
        userToGender.put(userid, byID.getRealGender());
      }
    }
    return orDefault;
  }

  public AudioAttribute getByID(int audioID) {
    Collection<SlickAudio> byID = dao.getByID(audioID);
    return byID.isEmpty()?null:toAudioAttribute(byID).iterator().next();
  }
}
