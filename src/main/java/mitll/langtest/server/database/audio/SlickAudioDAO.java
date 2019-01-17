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
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.domino.AudioCopy;
import mitll.langtest.shared.UserTimeBase;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.Language;
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

import static mitll.langtest.shared.user.MiniUser.Gender.Male;

public class SlickAudioDAO extends BaseAudioDAO implements IAudioDAO {
  private static final Logger logger = LogManager.getLogger(SlickAudioDAO.class);
  private static final String BEST_AUDIO = "bestAudio";
  private static final boolean DEBUG = false;

  private final AudioDAOWrapper dao;
  private final long now = System.currentTimeMillis();
  private final long before = now - (24 * 60 * 60 * 1000);
  private final boolean doCheckOnStartup;
  private final ServerProperties serverProps;
  private final boolean hasMediaDir;

  private int defaultResult;

  private static final boolean DEBUG_AUDIO_REPORT = false;

  public SlickAudioDAO(Database database, DBConnection dbConnection, IUserDAO userDAO) {
    super(database, userDAO);
    dao = new AudioDAOWrapper(dbConnection);
    serverProps = database.getServerProps();
    doCheckOnStartup = serverProps.doAudioCheckOnStartup();
    String mediaDir = serverProps.getMediaDir();
    hasMediaDir = new File(mediaDir).exists();
    if (!hasMediaDir) {
      logger.info("no media dir at " + mediaDir + " - this is OK on netprof host.");
    }
  }

  public boolean updateProject(int old, int newprojid) {
    return dao.updateProject(old, newprojid) > 0;
  }

  public boolean updateProjectIn(int old, int newprojid, List<Integer> exids) {
    return dao.updateProjectIn(old, newprojid, exids) > 0;
  }

  public Map<String, Integer> getPairs(int projid) {
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
   * @param hasProjectSpecificAudio
   * @return
   * @see BaseAudioDAO#getExToAudio
   */
  @Override
  public Collection<AudioAttribute> getAudioAttributesByProjectThatHaveBeenChecked(int projid, boolean hasProjectSpecificAudio) {
    List<SlickAudio> all = getAll(projid);
    logger.info("getAudioAttributesByProjectThatHaveBeenChecked " + projid + " found " + all.size() + " that has been checked to exist.");
    return toAudioAttribute(all, hasProjectSpecificAudio);
  }

  public List<SlickAudio> getAll(int projid) {
    return dao.getAll(projid);
  }

  public List<SlickAudio> getAllNoExistsCheck(int projid) {
    return dao.getAllNoCheck(projid);
  }

  public Collection<UserTimeBase> getAudioForReport(int projid) {
    List<UserTimeBase> report = new ArrayList<>();
    dao
        .getAllNoCheck(projid)
        .forEach(slickAudio -> report.add(new UserTimeBaseImpl(slickAudio.userid(), slickAudio.modified().getTime())));
    return report;
  }

  private static class UserTimeBaseImpl implements UserTimeBase {
    final int userid;
    final long timestamp;

    UserTimeBaseImpl(int userid, long timestamp) {
      this.userid = userid;
      this.timestamp = timestamp;
    }

    @Override
    public int getUserid() {
      return userid;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }
  }

  /**
   * @param info
   * @return
   * @see mitll.langtest.server.services.AudioServiceImpl#addToAudioTable
   */
  @Override
  public AudioAttribute addOrUpdate(AudioInfo info) {
    MiniUser miniUser = userDAO.getMiniUser(info.getUserid());

    AudioAttribute audioAttribute = toAudioAttribute(
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
            info.getResultID(),
            miniUser.getRealGenderInt()),
        miniUser,
        info.isHasProjectSpecificAudio());

//    logger.info("addOrUpdate Add or update after  " + audioAttribute);

    //  Collection<SlickAudio> byID = dao.getByID(audioAttribute.getUniqueID());

//    byID.forEach(slickAudio -> logger.info("addOrUpdate Got " + slickAudio));
//    logger.info("Add or update exists " + audioAttribute);
    return audioAttribute;
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
  public void addOrUpdateUser(AudioInfo info) {
    dao.addOrUpdateUser(info.getUserid(), info.getExerciseID(), info.getProjid(),
        info.getAudioType().toString(), info.getAudioRef(), info.getTimestamp(),
        info.getDurationInMillis(), info.getTranscript(), info.getDnr(), info.getResultID(),
        info.getGender());
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
    logger.info("validateFileExists before " + new Date(pastTime));
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
   * @param exid
   * @return
   * @see #attachAudioToExercise
   */
  @Override
  Collection<AudioAttribute> getAudioAttributesForExercise(int exid, Map<Integer, MiniUser> idToMini) {
    long then = System.currentTimeMillis();
    List<SlickAudio> byExerciseID = getAudioForExercise(exid);
    long now = System.currentTimeMillis();
    if (now - then > 20)
      logger.warn("getAudioAttributesForExercise took " + (now - then) + " to get " + byExerciseID.size() + " attr for " + exid);
    return toAudioAttributes(byExerciseID, idToMini);
  }

  @Override
  public boolean hasAudio(int exid) {
    return !getAudioForExercise(exid).isEmpty();
  }

  private List<SlickAudio> getAudioForExercise(int exid) {
    return dao.getByExerciseID(exid);
  }

  /**
   * So if there is no audio associated with the exercise, look for an existing audio cut with the same transcript.
   * If there is one, go ahead and associate it with the exercise.
   * If the transcript changes, remove the old audio and replace it
   * if the transcript is the same as the current audio, just return the current audio attribute.
   * if there is no transcript, remove any current audio
   *
   * @param projID
   * @param exid
   * @param audioID
   * @param isContext
   * @param transcript
   * @param audioCopy
   * @return
   * @see mitll.langtest.server.services.AudioServiceImpl#getTranscriptMatch
   */
  public AudioAttribute getTranscriptMatch(int projID, int exid, int audioID, boolean isContext,
                                           String transcript, AudioCopy audioCopy) {

    logger.info("getTranscriptMatch projID " + projID +
        "\n\texid    " + exid +
        "\n\taudio   " + audioID +
        "\n\tcontext " + isContext +
        "\n\ttext    '" + transcript + "'"
    );

    // cheesy thing where we might have an audio cut with an empty transcript...
    boolean hasText = !transcript.isEmpty();
    SlickAudio first = audioID > 0 && hasText ? getFirst(audioID) : null;
    if (first != null && first.transcript().equals(transcript)) {
      logger.info("getTranscriptMatch no change - match to transcript '" + first.transcript() +
          "'");
      AudioAttribute audioAttribute = getAudioAttribute(first, new HashMap<>(), false);
      fixAudioRef(projID, audioAttribute);
      return audioAttribute;
    } else if (hasText) {
      List<SlickAudio> transcriptMatch = dao.getTranscriptMatch(projID, transcript);
      if (!transcriptMatch.isEmpty()) {
        if (transcriptMatch.size() > 1)
          logger.info("getTranscriptMatch found " + transcriptMatch.size() + " matches -- choosing the first one.");

        AudioAttribute audioAttribute = getAudioAttribute(transcriptMatch.get(0), new HashMap<>(), false);

        logger.info("getTranscriptMatch Match! '" + transcript + "' = " +
            "\n\tattr" + audioAttribute);

        copyOne(audioCopy, audioAttribute.getUniqueID(), exid, isContext);

        // required!
        fixAudioRef(projID, audioAttribute);

        logger.info("\tgetTranscriptMatch after = " + audioAttribute.getAudioRef());

        return audioAttribute;
      } else {
        logger.info("getTranscriptMatch no match for " + transcript);
        /*   if (audioID > 0) {  // no matter whether there is a match or not, invalidate any current audio.
          logger.info("getTranscriptMatch MARK DEFECT projID " + projID + "\n\texid " + exid + "\n\taudio " + audioID +
              "\n\tcontext " + isContext);
          markDefect(audioID);
        }*/

        return null;
      }
    } else { // can't match empty
      return null;
    }
  }

  private void fixAudioRef(int projID, AudioAttribute audioAttribute) {
    fixAudioRef(null, mediaDir, audioAttribute, database.getProject(projID).getLanguageEnum());

    String audioRef = audioAttribute.getAudioRef();
    if (audioRef.endsWith(".wav")) {
      audioAttribute.setAudioRef(audioRef.replaceAll(".wav", ".mp3"));
    }
  }


  /**
   * @param projID
   * @param exids
   * @return
   * @see BaseAudioDAO#attachAudioToExercises
   */
  Map<Integer, List<AudioAttribute>> getAudioAttributesForExercises(int projID, Set<Integer> exids, Map<Integer, MiniUser> idToMini) {
    long then = System.currentTimeMillis();
    Map<Integer, List<SlickAudio>> byExerciseID = dao.getByExerciseIDsThatExist(projID, exids);
    long now = System.currentTimeMillis();

    if (now - then > 30) {
      logger.info("getAudioAttributesForExercise took " + (now - then) +
          "\n\tto get " + byExerciseID.size() + " attr" +
          "\n\tfor    " + exids.size());
    }

    Map<Integer, List<AudioAttribute>> copy = new HashMap<>(byExerciseID.size());
    byExerciseID.forEach((k, v) -> copy.put(k, toAudioAttributes(v, idToMini)));

    if (DEBUG) copy.forEach((k, v) -> logger.info(" getAudioAttributesForExercises " + k + " - " + v));

    if (copy.size() != exids.size()) {
      String suffix = copy.size() < 10 ? " : " + copy.keySet() : "";
      logger.info("getAudioAttributesForExercises asked for " + exids.size() + " exercises, but only found " +
          copy.size() + suffix);
    }
    return copy;
  }

  public Set<Integer> getExercisesThatHaveAudio(int projID, Collection<Integer> exids) {
    return dao.getExercisesThatHaveAudio(projID, exids);
  }

  /**
   * Do this differently
   * <p>
   * TODO : the audio is already marked with a gender - we don't have to ask about the users.
   *
   * @param projid
   * @param audioSpeed
   * @param uniqueIDs
   * @param exToTranscript
   * @param idsOfRecordedExercisesForMales
   * @param idsOfRecordedExercisesForFemales
   * @return
   * @see BaseAudioDAO#getRecordedReport
   */
  @Override
  void getCountForGender(int projid,
                         AudioType audioSpeed,
                         Set<Integer> uniqueIDs,
                         Map<Integer, String> exToTranscript,
                         Set<Integer> idsOfRecordedExercisesForMales,
                         Set<Integer> idsOfRecordedExercisesForFemales) {

/*    List<Integer> max = new ArrayList<>();

    for (Map.Entry<Integer, String> pair : exToTranscript.entrySet()) {
      max.add(pair.getKey());

      if (max.size() == 2) break;

      logger.info("getCountForGender " + speed + " " + pair.getKey() + " " + pair.getValue());
    }*/
    Map<Integer, Collection<Tuple2<Integer, Integer>>> countForGender4 =
        dao.getCountForGender(audioSpeed.toString(), uniqueIDs, exToTranscript, true, projid);

    if (DEBUG) {
      String speed = audioSpeed.toString();
      logger.info("getCountForGender" +
          "\n\tfor       '" + speed + "'" +
          "\n\tgiven ids " + uniqueIDs.size() +
          "\n\tex->trans " + exToTranscript.size() +
          "\n\tgot       " + countForGender4.size());
    }

//    countForGender4.forEach((k,v)->{});

    for (Integer userID : countForGender4.keySet()) {
      Collection<Tuple2<Integer, Integer>> tuple2s = countForGender4.get(userID);
      Set<Integer> exIDs = getExIDs(tuple2s.iterator());

      if (userDAO.isMale(userID)) {
        if (DEBUG) logger.info("getCountForGender male   user " + userID + " = " + exIDs.size());
        idsOfRecordedExercisesForMales.addAll(exIDs);
      } else {
        if (DEBUG) logger.info("getCountForGender female user " + userID + " = " + exIDs.size());
        idsOfRecordedExercisesForFemales.addAll(exIDs);
      }
    }

    if (DEBUG) {
      String speed = audioSpeed.toString();
      logger.info("getCountForGender " +
          "\n\tfor     '" + speed + "'" +
          "\n\tmales   " + idsOfRecordedExercisesForMales.size() +
          "\n\tfemales " + idsOfRecordedExercisesForFemales.size());
    }
  }

  @Override
  int markDefect(int userid, int exerciseID, AudioType audioType) {
    return dao.markDefect(userid, exerciseID, audioType.toString());
  }

  @Override
  public int markDefect(int id) {
    return dao.markDefect(id);
  }

  /**
   * TODO TODO :
   * <p>
   * only include ids for audio where the audio transcript matches the exercise text
   *
   * @param audioSpeed
   * @param projid
   * @return modifiable set - what's returned from scala isn't
   * @paramx userIDs
   * @see #getRecordedBySameGenderContext
   */
  @Override
  Set<Integer> getAudioExercisesForGender(boolean isMale,
                                          String audioSpeed,
                                          int projid) {
    Map<Integer, Collection<Tuple2<Integer, Integer>>> audioForGender1 = dao.getAudioForGender(audioSpeed, projid, isMale ? 0 : 1);
    Set<Pair> genderMatch = getGenderMatch(audioForGender1);
    return getExercises(genderMatch);
  }

  /**
   * @param projid
   * @param isMale
   * @param exToTranscript
   * @return
   */
  @Override
  Set<Integer> getAudioExercisesForGenderBothSpeeds(int projid,
                                                    boolean isMale,
                                                    Map<Integer, String> exToTranscript) {
    Set<Integer> maleReg = new HashSet<>();
    Set<Integer> femaleReg = new HashSet<>();
    Set<Integer> uniqueIDs = exToTranscript.keySet();
    if (DEBUG_AUDIO_REPORT)
      logger.info("getAudioExercisesForGenderBothSpeeds : check " + uniqueIDs.size() + " exercises");
    getCountForGender(projid, AudioType.REGULAR, uniqueIDs, exToTranscript, maleReg, femaleReg);

    float maleFast = (float) maleReg.size();
    if (DEBUG_AUDIO_REPORT) logger.info("getRecordedReport male fast " + maleFast);
    float femaleFast = (float) femaleReg.size();

    Set<Integer> maleSlowSpeed = new HashSet<>();
    Set<Integer> femaleSlowSpeed = new HashSet<>();
    getCountForGender(projid, AudioType.SLOW, uniqueIDs, exToTranscript, maleSlowSpeed, femaleSlowSpeed);
    float maleSlow = (float) maleSlowSpeed.size();
    float femaleSlow = (float) femaleSlowSpeed.size();

    if (DEBUG_AUDIO_REPORT) logger.info("getRecordedReport male slow " + maleSlow);

    maleReg.retainAll(maleSlowSpeed);
    //  float male = maleReg.size();
    //  if (DEBUG_AUDIO_REPORT) logger.info("male total " + male);

//    Set<Integer> femaleIDs = userMapFemales.keySet();
    //   femaleIDs = new HashSet<>(femaleIDs);
    //  femaleIDs.add(BaseUserDAO.DEFAULT_FEMALE_ID);
//    float femaleFast = getCountForGender(projid, AudioType.REGULAR, uniqueIDs, exToTranscript, femaleReg, , false);
//    float femaleSlow = getCountForGender(projid, AudioType.SLOW, uniqueIDs, exToTranscript, femaleSlowSpeed, , false);

    if (DEBUG_AUDIO_REPORT) logger.info("female fast " + femaleFast + " slow " + femaleSlow);

    // overlap
    femaleReg.retainAll(femaleSlowSpeed);
//    float female = femaleReg.size();
    return isMale ? maleReg : femaleReg;
  }

  /**
   * Never going to record slow speec context audio, at least per DLI's request.
   *
   * @param projid
   * @param isMale
   * @param exToContextTranscript
   * @return
   */
  @Override
  Set<Integer> getContextAudioExercises(int projid,
                                        boolean isMale,
                                        Map<Integer, String> exToContextTranscript) {
    logger.info("getContextAudioExercises  : checking " + exToContextTranscript.size() + " context exerises");
    Set<Integer> conMaleReg = new HashSet<>();
    Set<Integer> conFemaleReg = new HashSet<>();

    getCountForGender(projid, AudioType.CONTEXT_REGULAR, exToContextTranscript.keySet(), exToContextTranscript, conMaleReg, conFemaleReg);

    /*Set<Integer> conMaleSlow = new HashSet<>();
    Set<Integer> conFemaleSlow = new HashSet<>();

    // TODO : add male/female fast/slow for context
    getCountForGender(projid, AudioType.CONTEXT_SLOW, exToContextTranscript.keySet(), exToContextTranscript, conMaleSlow, conFemaleSlow);
*/
  /*  conMaleReg.retainAll(conMaleSlow);
    conFemaleReg.retainAll(conFemaleSlow);
*/
    return isMale ? conMaleReg : conFemaleReg;
  }

  @NotNull
  private Set<Integer> getExercises(Set<Pair> regSpeedGenderMatch) {
    Set<Integer> regExids = new HashSet<>(regSpeedGenderMatch.size());
    for (Pair p : regSpeedGenderMatch) regExids.add(p.getExid());
    return regExids;
  }

  private Set<Pair> getGenderMatch(Map<Integer, Collection<Tuple2<Integer, Integer>>> regSpeedPairs) {
    Set<Pair> regSpeedGenderMatch = new HashSet<>();
    for (Integer key : regSpeedPairs.keySet()) {
      Iterator<Tuple2<Integer, Integer>> iterator = regSpeedPairs.get(key).iterator();
      regSpeedGenderMatch.addAll(getPairs(iterator));
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
    while (iterator.hasNext()) {
      Tuple2<Integer, Integer> next = iterator.next();
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

  private static final boolean reportBadDNRAudio = false;

  /**
   * Skips over audio that has negative infinity DNR - usually a bad sign.
   * <p>
   * TODO : consider cache for mini users
   *
   * @param all
   * @return
   */
  private List<AudioAttribute> toAudioAttributes(Collection<SlickAudio> all, Map<Integer, MiniUser> idToMini) {
    List<AudioAttribute> copy = new ArrayList<>(all.size());
    boolean hasProjectSpecificAudio = false;

    if (!all.isEmpty()) {
      int projid = all.iterator().next().projid();
      Project project = database.getProject(projid);
      if (project != null) {
        hasProjectSpecificAudio = project.hasProjectSpecificAudio();
      }
    }

    for (SlickAudio slickAudio : all) {
      AudioAttribute audioAttribute = getAudioAttribute(slickAudio, idToMini, hasProjectSpecificAudio);
      if (audioAttribute.getDnr() == Float.NEGATIVE_INFINITY) {
        if (reportBadDNRAudio) {
          logger.info("toAudioAttributes : Skip audio with bad dnr : " + audioAttribute);
        }
      } else {
        copy.add(audioAttribute);
      }
    }
    return copy;
  }

  private int spew = 0;

  /**
   * Actual audio path is where we find it on the server... potentially different from where it was originally recorded...
   *
   * @param s
   * @param miniUser
   * @param isAudioPerProject
   * @return
   * @see #toAudioAttribute
   */
  private AudioAttribute toAudioAttribute(SlickAudio s, MiniUser miniUser, boolean isAudioPerProject) {
    if (miniUser == null && spew++ < 20) {
      logger.error("toAudioAttribute : no user for " + s.userid());
    }

    String audiotype = s.audiotype();

    if (audiotype.contains("=")) {
      audiotype = audiotype.replaceAll("=", "_");
    }
    int gender = s.gender();
    MiniUser.Gender realGender = gender == 0 ? Male : gender == 1 ? MiniUser.Gender.Female : MiniUser.Gender.Unspecified;
    String audioref = s.audioref();

    if (isAudioPerProject) {
      audioref = "project_" + s.projid() + File.separator + audioref;
    }
    AudioType type = AudioType.valueOf(audiotype.toUpperCase());

//    logger.info("")
    return new AudioAttribute(
        s.id(),
        s.userid(),
        s.exid(),
        audioref,
        s.modified().getTime(),
        s.duration(),
        type,
        miniUser,
        s.transcript(),
        s.actualpath(),
        s.dnr(),
        s.resultid(),
        realGender);
  }

  private int c = 0;
  private final Set<MiniUser> warned = new HashSet<>();

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

      MiniUser user = orig.getUser();
      MiniUser.Gender realGender = user == null ? Male : user.getRealGender();
      int gender = realGender == Male ? 0 : realGender == MiniUser.Gender.Female ? 1 : 2;

      if (gender == 2) {
        //if (!warned.contains(user)) {
        logger.warn("getSlickAudio user " + user);
        logger.warn("gender " + realGender);
        logger.warn("gender " + gender);
        warned.add(user);
        // }
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
          resultid,
          gender); // unspecified is 2
    }
  }

  /**
   * @param audioRef
   * @return
   * @see #getSlickAudio
   */
  @NotNull
  private static String removeConfigPrefix(String audioRef) {
    int bestAudio = audioRef.indexOf(BEST_AUDIO);
    if (bestAudio == -1) {
      logger.warn("removeConfigPrefix huh? expecting bestaudio in " + audioRef);
    } else {
      audioRef = audioRef.substring(bestAudio);
    }
    return audioRef;
  }

  /**
   * @param all
   * @param hasProjectSpecificAudio
   * @return
   * @see BaseAudioDAO#getAudioAttributesByProjectThatHaveBeenChecked(int, boolean)
   */
  private List<AudioAttribute> toAudioAttribute(Collection<SlickAudio> all, boolean hasProjectSpecificAudio) {
    List<AudioAttribute> copy = new ArrayList<>(all.size());
    if (all.isEmpty()) {
      logger.warn("toAudioAttribute table has " + dao.getNumRows() + " rows but no audio?");
    }
//    logger.info("toAudioAttribute " + all.size());
    all.forEach(slickAudio -> copy.add(getAudioAttribute(slickAudio, idToMini, hasProjectSpecificAudio)));
    return copy;
  }

  /**
   * Avoid asking domino for the same user over and over.
   *
   * @param s
   * @param idToMini
   * @param hasProjectSpecificAudio
   * @return
   */
  private AudioAttribute getAudioAttribute(SlickAudio s, Map<Integer, MiniUser> idToMini, boolean hasProjectSpecificAudio) {
    int userid = s.userid();

//    int before = idToMini.size();
    MiniUser miniUser = idToMini.computeIfAbsent(userid, k -> userDAO.getMiniUser(userid));
//    int after = idToMini.size();
//    if (after > before) logger.info("getAudioAttribute id->mini now " + after + " req for " + userid);

    return toAudioAttribute(s, miniUser, hasProjectSpecificAudio);
  }

  public void addBulk(List<SlickAudio> bulk) {
    dao.addBulk(bulk);
  }

  @Override
  public void updateUser(int old, int newUser) {
    dao.updateUser(old, newUser);
  }

  public void setDefaultResult(int defaultResult) {
    this.defaultResult = defaultResult;
  }

  /**
   * TODO : also check that the compressed versions are there too!
   *
   * @param projectID
   * @param language
   * @param validateAll
   * @see mitll.langtest.server.services.AudioServiceImpl#checkAudio
   * @see mitll.langtest.server.database.exercise.BaseExerciseDAO#setAudioDAO
   */
  public void makeSureAudioIsThere(int projectID, String language, boolean validateAll) {
    if (hasMediaDir) {
      logger.info("makeSureAudioIsThere " + projectID + " " + language + " validate all = " + validateAll);
      //logger.debug("makeSureAudioIsThere media dir " + file + " exists ");
      String mediaDir = serverProps.getMediaDir();
      File file = new File(mediaDir);
      if (file.isDirectory()) {
        String[] list = file.list();
        if (list == null) {
          logger.error("setAudioDAO configuration error - can't get files from media directory " + mediaDir);
        } else if (list.length > 0) { // only on pnetprof (behind firewall), znetprof has no audio, might have a directory.
          //logger.debug("setAudioDAO validating files under " + file.getAbsolutePath());
          boolean foundFiles = didFindAnyAudioFiles(projectID);
          if (validateAll ||
              (serverProps.doAudioChecksInProduction() &&
                  (serverProps.doAudioFileExistsCheck() || !foundFiles))) {
            logger.info("makeSureAudioIsThere validateFileExists ");
            validateFileExists(projectID, mediaDir, language, validateAll);
          }
        }
      } else {
        logger.error("configuration error - (" + projectID + " " + language +
            ") expecting media directory " + mediaDir + " to be directory.");
      }
    } /*else {
      logger.warn("makeSureAudioIsThere : (" + projectID + " " + language +
          ") configuration error? - expecting a media directory " + mediaDir);
    }*/
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
   * @param language
   * @param idToMini
   * @return
   * @see Database#getNativeAudio
   */
  @Nullable
  public String getNativeAudio(Map<Integer, MiniUser.Gender> userToGender,
                               int userid,
                               CommonExercise exercise,
                               Language language,
                               Map<Integer, MiniUser> idToMini) {
    //String nativeAudio = null;
    if (exercise != null) {
      MiniUser.Gender genderOfUser = getGender(userToGender, userid);
      if (genderOfUser == null) {
        logger.error("getNativeAudio can't find user " + userid);
        return null; // no user with this id?
      }

      if (!exercise.hasRefAudio()) {
        //      logger.info("Attach audio to " + exercise.getID());
        attachAudioToExercise(exercise, language, idToMini);
      }

      AudioAttribute audioAttributePrefGender = exercise.getAudioAttributePrefGender(genderOfUser == Male, true);

      if (audioAttributePrefGender == null) {
//        logger.warn("no audio for " + exercise.getID());
        return null;
      } else {
        return audioAttributePrefGender.getAudioRef();
      }

    } else {
      return null;
    }
  }

  @Nullable
  private MiniUser.Gender getGender(Map<Integer, MiniUser.Gender> userToGender, int userid) {
    MiniUser.Gender genderForUser = userToGender.get(userid);
    if (genderForUser == null) {
      User byID = userDAO.getByID(userid);
      if (byID == null) {
        logger.error("getGender huh? can't find " + userid);
      } else {
        userToGender.put(userid, genderForUser = byID.getRealGender());

      }
    }
    return genderForUser;
  }

  public AudioAttribute getByID(int audioID, boolean hasProjectSpecificAudio) {
    Collection<SlickAudio> byID = getSlickAudios(audioID);
    return byID.isEmpty() ? null : toAudioAttribute(byID, hasProjectSpecificAudio).iterator().next();
  }

  private Collection<SlickAudio> getSlickAudios(int audioID) {
    return dao.getByID(audioID);
  }

  @Override
  public void copyOne(AudioCopy audioCopy, int audioID, int exid, boolean isContext) {
    SlickAudio next = getFirst(audioID);
    addBulk(Collections.singletonList(audioCopy.getCopiedAudio(exid, isContext, next)));
  }

  private SlickAudio getFirst(int audioID) {
    Collection<SlickAudio> slickAudios = getSlickAudios(audioID);
    return slickAudios.isEmpty() ? null : slickAudios.iterator().next();
  }

  @Override
  public void deleteForProject(int projID) {
    dao.deleteForProject(projID);
  }

}
