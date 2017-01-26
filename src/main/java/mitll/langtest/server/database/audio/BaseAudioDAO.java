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

import com.google.common.base.CharMatcher;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.audio.AudioExport;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.user.MiniUser;
import mitll.langtest.shared.user.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

public abstract class BaseAudioDAO extends DAO {
  private static final Logger logger = LogManager.getLogger(BaseAudioDAO.class);

  public static final String UNKNOWN = "unknown";
  private static final String TOTAL = "total";
  private static final String TOTAL_CONTEXT = "totalContext";
  public static final String MALE = "male";
  public static final String FEMALE = "female";
  public static final String MALE_FAST = "maleFast";
  public static final String MALE_SLOW = "maleSlow";
  public static final String FEMALE_FAST = "femaleFast";
  public static final String FEMALE_SLOW = "femaleSlow";
  public static final String MALE_CONTEXT = "maleContext";
  public static final String FEMALE_CONTEXT = "femaleContext";
  protected static final String REGULAR = "regular";
  protected static final String SLOW = "slow";
  private static final String AUDIO_TYPE1 = "context=" + REGULAR;
  private static final String CONTEXT_REGULAR = AUDIO_TYPE1;
  private static final String TRANSLITERATION = "transliteration";
  private static final boolean DEBUG_ATTACH = false;
  private static final int WARN_DURATION = 30;
  // public static final String BEST_AUDIO = "bestAudio";

  protected final IUserDAO userDAO;
  private final int netProfDurLength;

  BaseAudioDAO(Database database, IUserDAO userDAO) {
    super(database);
    this.userDAO = userDAO;
    netProfDurLength = database.getServerProps().getAudioBaseDir().length();
  }

  /**
   * TODO : Seems really expensive - avoid doing this if we can.
   *
   * @param projid
   * @return
   * @seex ExerciseDAO#setAudioDAO
   * @see AudioExport#writeFolderContents
   * @see DatabaseImpl#attachAllAudio
   * @see mitll.langtest.server.database.exercise.BaseExerciseDAO#attachAudio()
   */
  public Map<Integer, List<AudioAttribute>> getExToAudio(int projid) {
    long then = System.currentTimeMillis();
    Map<Integer, List<AudioAttribute>> exToAudio = new HashMap<>();
    Map<Integer, Set<String>> idToPaths = new HashMap<>();

//    logger.info("getExToAudio - for " +projid);
    Collection<AudioAttribute> attributesByProject = getAudioAttributesByProject(projid);
    //  logger.info("getExToAudio - for " +projid + " got " +attributesByProject.size());

    for (AudioAttribute audio : attributesByProject) {
      Integer exid = audio.getExid();
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
    logger.info("getExToAudio " +
        " project " + +projid +
        "(" + database.getLanguage() +
        ") took " + (now - then) + " millis to get  " + attributesByProject.size() + " audio entries " + this);
//    logger.debug("map size is " + exToAudio.size());
    return exToAudio;
  }

  abstract Collection<AudioAttribute> getAudioAttributesByProject(int projid);

  /**
   * @param firstExercise
   * @param language
   * @see mitll.langtest.server.services.ExerciseServiceImpl#attachAudio
   * @see DatabaseImpl#attachAudio(CommonExercise)
   * @see DatabaseImpl#writeZip
   */
  public int attachAudioToExercise(CommonExercise firstExercise, String language) {
    long then = System.currentTimeMillis();
    Collection<AudioAttribute> audioAttributes = getAudioAttributesForExercise(firstExercise.getID());
    long now = System.currentTimeMillis();

    if (now - then > WARN_DURATION)
      logger.warn("attachAudioToExercise took " + (now - then) + " to get " + audioAttributes.size() + " attributes");
/*    if (DEBUG) {
      logger.debug("\attachAudio : found " + audioAttributes.size() + " for " + firstExercise.getOldID());
      for (AudioAttribute attribute : audioAttributes) {
        logger.debug("\t\attachAudio : exid " + firstExercise.getOldID() + " audio " + attribute);
      }

      for (AudioAttribute attribute : firstExercise.getAudioAttributes()) {
        logger.debug("\t\tbefore attachAudio on ex : exid " + firstExercise.getOldID() + " audio " + attribute);
      }
    }*/

    then = now;
    boolean attachedAll = attachAudio(firstExercise, audioAttributes, language);
    now = System.currentTimeMillis();

    if (now - then > WARN_DURATION)
      logger.warn("attachAudioToExercise took " + (now - then) + " to attach audio to " + firstExercise.getID());

    if (!attachedAll)
      logger.info("didn't attach all audio to " + firstExercise.getID() + " " + firstExercise.getForeignLanguage());
/*    if (DEBUG) {
      for (AudioAttribute attribute : firstExercise.getAudioAttributes()) {
        logger.debug("\t\tafter attachAudio : after on ex exid " + firstExercise.getOldID() + " audio " + attribute);
      }
    }*/

    return audioAttributes.size();
  }

  /**
   * TODO : deal with possibility of audio being in either bestAudio or in answers...
   * <p>
   * TODO : rewrite this so it's not insane -adding and removing attributes??
   * <p>
   * Complicated, but separates old school "Default Speaker" audio into a second pile.
   * If we've already added an audio attribute with the path for a default speaker, then we remove it.
   * <p>
   * TODO : why would we want to skip adding audio from the initial path set?
   *
   * @param firstExercise
   * @param audioAttributes
   * @param language
   * @see AudioExport#writeFolderContents
   * @see #attachAudioToExercise
   * @see mitll.langtest.server.json.JsonExport#getJsonArray
   * @see
   */
  public boolean attachAudio(CommonExercise firstExercise,
                             Collection<AudioAttribute> audioAttributes,
                             String language) {
    String installPath = database.getServerProps().getMediaDir();

    List<AudioAttribute> defaultAudio = new ArrayList<>();
    Set<String> audioPaths = new HashSet<>();
    boolean allSucceeded = true;

    for (AudioAttribute attr : audioAttributes) {
      MiniUser user = attr.getUser();
      if (user.isUnknownDefault()) {
        defaultAudio.add(attr);
      } else {
        audioPaths.add(attr.getAudioRef());
        boolean didIt = attachAudioAndFixPath(firstExercise, installPath, attr, language);
        if (!didIt) {
          if (DEBUG_ATTACH && allSucceeded) {
            String foreignLanguage = attr.isContextAudio() && firstExercise.hasContext() ? firstExercise.getContext() : firstExercise.getForeignLanguage();
            logger.info("not attaching audio\t" + attr.getUniqueID() + " to\t" + firstExercise.getID() +
                "\tsince transcript has changed : old '" +
                attr.getTranscript() +
                "' vs new '" + foreignLanguage +
                "'");
          }
          allSucceeded = false;
        }
        // logger.debug("\tadding path '" + attr.getAudioRef() + "' " + attr + " to " + firstExercise.getOldID());
      }
      //}
    }

    for (AudioAttribute attr : defaultAudio) {
      if (!audioPaths.contains(attr.getAudioRef())) {
        boolean didIt = attachAudioAndFixPath(firstExercise, installPath, attr, language);
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
   * So this happens on the znetProf side where we don't have access to the actual file
   * We have confidence the file is there b/c we check it's existence every 24 hours, or on every startup of the
   * pnetProf instance.
   * <p>
   * TODO : not sure what to do if we have multiple context sentences...
   *
   * @param firstExercise
   * @param installPath
   * @param attr
   * @param language
   * @return
   * @see #attachAudio
   */
  private boolean attachAudioAndFixPath(CommonExercise firstExercise,
                                        String installPath,
                                        AudioAttribute attr,
                                        String language) {
    Collection<CommonExercise> directlyRelated = firstExercise.getDirectlyRelated();
    String against = attr.isContextAudio() && !directlyRelated.isEmpty() ?
        directlyRelated.iterator().next().getForeignLanguage() :
        firstExercise.getForeignLanguage();
    if (attr.hasMatchingTranscript(against)) {
      firstExercise.getMutableAudio().addAudio(attr);
      String audioRef = attr.getAudioRef();
      if (audioRef == null)
        logger.error("attachAudioAndFixPath huh? no audio ref for " + attr + " under " + firstExercise);
      else {
        // so a path to the file on disk will now look like /opt/netProf/bestAudio/spanish/bestAudio/123/regular_XXX.wav
        // in the database we store just bestAudio/123/regular_XXX.wav

        // or if we store bestAudio/spanish/123/regular_YYY.wav ...? e.g. for newly recorded audio

        String s = language.toLowerCase();
        String prefix = installPath + File.separator + s;
        String relPrefix = prefix.substring(netProfDurLength);
        if (!audioRef.contains(s)) {
          if (DEBUG_ATTACH) logger.info("audioref " + audioRef + " does not contain '" + prefix +
              "' before " + attr.getAudioRef());
          attr.setAudioRef(relPrefix + File.separator + audioRef);
          if (DEBUG_ATTACH) logger.info("after " + attr.getAudioRef());
        }
        if (DEBUG_ATTACH) logger.debug("\tattachAudioAndFixPath now '" + attr.getAudioRef() + "'");
      }
      return true;
    } else {
/*
      logger.info("not attaching audio " + attr.getUniqueID() + " to " + firstExercise.getOldID() + " since transcript has changed. Audio '" +
          attr.getTranscript()+
          "' vs exercise '" +foreignLanguage+
          "'");
*/
      return false;
    }
  }


  abstract Collection<AudioAttribute> getAudioAttributesForExercise(int exid);

  /**
   * Get back the ids of exercises recorded by people who are the same gender as the userid.
   *
   * TODO : don't do this - won't scale with users
   *
   * @param userid only used to determine the gender we should show
   * @return ids with both regular and slow speed recordings
   * @see mitll.langtest.server.services.ExerciseServiceImpl#filterByUnrecorded
   */
  public Collection<Integer> getRecordedBy(int userid, Map<Integer, String> exToTranscript) {
    //Map<Integer, User> userMap = getUserMap(userid);
    Collection<Integer> userIDs = getUserIDs(userid);
    //logger.debug("found " + (isMale ? " male " : " female ") + " users : " + userMap.keySet());
    // find set of users of same gender
    Set<Integer> validAudioAtReg = getAudioExercisesForGender(userIDs, REGULAR, exToTranscript);
    //logger.debug(" regular speed for " + userMap.keySet() + " " + validAudioAtReg.size());
    Set<Integer> validAudioAtSlow = getAudioExercisesForGender(userIDs, SLOW, exToTranscript);
//    logger.debug(" slow speed for " + userMap.keySet() + " " + validAudioAtSlow.size());
    boolean b = validAudioAtReg.retainAll(validAudioAtSlow);
    //  logger.debug("retain all " + b + " " + validAudioAtReg.size());
    return validAudioAtReg;
  }

  protected Map<Integer, User> getUserMap(int userid) {
    User user = userDAO.getUserWhere(userid);
    boolean isMale = (user != null && user.isMale());
    return userDAO.getUserMap(isMale);
  }

  /**
   * @param userid
   * @return
   * @see #getRecordedBy
   */
  Collection<Integer> getUserIDs(int userid) {
    User user = userDAO.getUserWhere(userid);
    boolean isMale = (user != null && user.isMale());
    return userDAO.getUserIDs(isMale);
  }

  /**
   * @param userMapMales
   * @param userMapFemales
   * @param total
   * @param uniqueIDs
   * @return
   * @see DatabaseImpl#getMaleFemaleProgress(int)
   */
  public Map<String, Float> getRecordedReport(Map<Integer, User> userMapMales,
                                              Map<Integer, User> userMapFemales,
                                              float total,
                                              Set<Integer> uniqueIDs,
                                              Map<Integer, String> exToTranscript,
                                              Map<Integer, String> exToContextTranscript,
                                              float totalContext) {
    Set<Integer> maleIDs = userMapMales.keySet();
    maleIDs = new HashSet<>(maleIDs);
    maleIDs.add(BaseUserDAO.DEFAULT_MALE_ID);

    Set<Integer> maleReg = new HashSet<>();
    Set<Integer> maleSlowSpeed = new HashSet<>();

    float maleFast = getCountForGender(maleIDs, REGULAR, uniqueIDs, exToTranscript, maleReg);
    logger.info("male fast " + maleFast);
    float maleSlow = getCountForGender(maleIDs, SLOW, uniqueIDs, exToTranscript, maleSlowSpeed);
    logger.info("male slow " + maleSlow);

    maleReg.retainAll(maleSlowSpeed);
    float male = maleReg.size();
    logger.info("male total " + male);

    Set<Integer> femaleIDs = userMapFemales.keySet();
    femaleIDs = new HashSet<>(femaleIDs);
    femaleIDs.add(BaseUserDAO.DEFAULT_FEMALE_ID);

    Set<Integer> femaleReg = new HashSet<>();
    Set<Integer> femaleSlowSpeed = new HashSet<>();
    float femaleFast = getCountForGender(femaleIDs, REGULAR, uniqueIDs, exToTranscript, femaleReg);
    float femaleSlow = getCountForGender(femaleIDs, SLOW, uniqueIDs, exToTranscript, femaleSlowSpeed);
    logger.info("female fast " + femaleFast);
    logger.info("female slow " + femaleSlow);

    femaleReg.retainAll(femaleSlowSpeed);
    float female = femaleReg.size();
    logger.info("female total " + female);

    Set<Integer> conReg = new HashSet<>();
    Set<Integer> conSlow = new HashSet<>();

    float cmale = getCountForGender(maleIDs, CONTEXT_REGULAR, uniqueIDs, exToContextTranscript, conReg);
    float cfemale = getCountForGender(femaleIDs, CONTEXT_REGULAR, uniqueIDs, exToContextTranscript, conSlow);
    logger.info("cmale fast " + cmale);
    logger.info("cfemale fast " + cfemale);
    Map<String, Float> report = new HashMap<>();
    report.put(BaseAudioDAO.TOTAL, total);
    report.put(BaseAudioDAO.TOTAL_CONTEXT, totalContext);
    report.put(BaseAudioDAO.MALE, male);
    report.put(BaseAudioDAO.FEMALE, female);
    report.put(BaseAudioDAO.MALE_FAST, maleFast);
    report.put(BaseAudioDAO.MALE_SLOW, maleSlow);
    report.put(BaseAudioDAO.FEMALE_FAST, femaleFast);
    report.put(BaseAudioDAO.FEMALE_SLOW, femaleSlow);
    report.put(BaseAudioDAO.MALE_CONTEXT, cmale);
    report.put(BaseAudioDAO.FEMALE_CONTEXT, cfemale);
    return report;
  }

  abstract int getCountForGender(Set<Integer> userIds,
                                 String audioSpeed,
                                 Set<Integer> uniqueIDs,
                                 Map<Integer, String> exToTranscript,
                                 Set<Integer> idsOfRecordedExercises);

  /**
   * Items that are recorded must have both regular and slow speed audio.
   *
   * @param userid
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#markRecordedState
   */
  public Collection<Integer> getRecordedExForUser(int userid) {
    try {
      Set<Integer> validAudioAtReg = getValidAudioOfType(userid, REGULAR);
      Set<Integer> validAudioAtSlow = getValidAudioOfType(userid, SLOW);
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
   * @see mitll.langtest.server.services.ExerciseServiceImpl#markRecordedState
   */
  public Collection<Integer> getRecordedExampleForUser(int userid) {
    try {
      return getValidAudioOfType(userid, AUDIO_TYPE1);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<>();
  }

  abstract Set<Integer> getValidAudioOfType(int userid, String audioType);

  /**
   * @param userid
   * @param exerciseID
   * @param audioType
   * @return
   * @see IAudioDAO#addOrUpdate(int, int, int, AudioType, String, long, long, String, float)
   */
  protected AudioAttribute getAudioAttribute(int userid, int exerciseID, AudioType audioType) {
    AudioAttribute audioAttr = null;
    Collection<AudioAttribute> audioAttributes = getAudioAttributesForExercise(exerciseID);
    //logger.debug("for  " +exerciseID + " found " + audioAttributes);

    for (AudioAttribute audioAttribute : audioAttributes) {
      AudioType audioType1 = audioAttribute.getAudioType();
      //logger.debug("\tfor  " +audioAttribute + " against " + userid + "/" + audioType  + " audio type " + audioType1);
      if (audioAttribute.getUserid() == userid && audioType1 == audioType) {
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
   * @param dnr
   * @return
   * @see IAudioDAO#addOrUpdate
   * @deprecated
   */
  protected AudioAttribute getAudioAttribute(int i,
                                             int userid, String audioRef, int exerciseID, long timestamp,
                                             AudioType audioType, long durationInMillis, String transcript, float dnr) {
    MiniUser miniUser = userDAO.getMiniUser(userid);

    return new AudioAttribute(i, userid,
        exerciseID, // id
        audioRef, // answer
        timestamp,
        durationInMillis, audioType,
        miniUser, transcript,
        audioRef,
        dnr,
        -1);
  }

  public Set<AudioAttribute> getAndMarkDefects(AudioAttributeExercise userExercise,
                                               Map<String, ExerciseAnnotation> fieldToAnnotation) {
    Set<AudioAttribute> defects = new HashSet<AudioAttribute>();

    for (Map.Entry<String, ExerciseAnnotation> fieldAnno : fieldToAnnotation.entrySet()) {
      if (!fieldAnno.getValue().isCorrect()) {  // i.e. defect
        AudioAttribute audioAttribute = userExercise.getAudioRefToAttr().get(fieldAnno.getKey());
        if (audioAttribute != null) {
          logger.debug("getAndMarkDefects : found defect " + audioAttribute +
              " anno : " + fieldAnno.getValue() +
              " field  " + fieldAnno.getKey());
          // logger.debug("\tmarking defect on audio");
          defects.add(audioAttribute);
          markDefect(audioAttribute);
        } else if (!fieldAnno.getKey().equals(TRANSLITERATION)) {
          logger.warn("\tcan't mark defect on audio : looking for field '" + fieldAnno.getKey() +
              "' in " + userExercise.getAudioRefToAttr().keySet());
        }
      }
    }

    return defects;
  }

  /**
   * @param attribute
   * @return
   * @see DatabaseImpl#markAudioDefect(AudioAttribute)
   */
  public int markDefect(AudioAttribute attribute) {
    return markDefect(attribute.getUserid(), attribute.getExid(), attribute.getAudioType());
  }

  /**
   * TODO : confirm this works...
   * <p>
   * Go back and mark gender on really old audio that had no user info on it.
   *
   * @param userid
   * @param projid
   * @param attr
   * @return
   * @see mitll.langtest.server.services.QCServiceImpl#markGender
   */
  public void addOrUpdateUser(int userid, int projid, AudioAttribute attr) {
    long timestamp = attr.getTimestamp();
    if (timestamp == 0) timestamp = System.currentTimeMillis();
    float dnr = new AudioCheck(database.getServerProps()).getDNR(new File(attr.getActualPath()));
    addOrUpdateUser(new AudioInfo(userid, attr.getExid(), projid, attr.getAudioType(), attr.getAudioRef(), timestamp,
        (int) attr.getDurationInMillis(), BaseAudioDAO.UNKNOWN, dnr, attr.getResultid())
    );
  }

  abstract void addOrUpdateUser(/*int userid, int exerciseID, int projid, AudioType audioType, String audioRef, long timestamp,
                                int durationInMillis, String transcript, float dnr*/
                                AudioInfo info
  );

  abstract int markDefect(int userid, int exerciseID, AudioType audioType);

  boolean isBadUser(int userid) {
    return userid < BaseUserDAO.DEFAULT_FEMALE_ID;
  }

  /**
   * @param userid
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#filterByUnrecorded
   */
  public Set<Integer> getWithContext(int userid, Map<Integer, String> exToContext) {
    return getWithContext(getUserMap(userid), exToContext);
  }

  private Set<Integer> getWithContext(Map<Integer, User> userMap, Map<Integer, String> exToContext) {
    return getAudioExercisesForGender(userMap.keySet(), CONTEXT_REGULAR, exToContext);
  }

  abstract Set<Integer> getAudioExercisesForGender(Collection<Integer> userIDs,
                                                   String audioSpeed,
                                                   Map<Integer, String> exToTranscript);

  abstract int getCountBothSpeeds(Set<Integer> userIds, Set<Integer> uniqueIDs);


  protected boolean isNoAccentMatch(String transcript, String exerciseFL) {
    if (exerciseFL == null) return false;
    String before = trimWhitespace(exerciseFL);
    String trimmed = trimWhitespace(transcript);
    String noAccents = StringUtils.stripAccents(before);
    //String transcript = audio.getTranscript();
    String noAccentsTranscript = trimmed == null ? null : StringUtils.stripAccents(trimmed);

    return matchTranscript(before, trimmed) ||
        matchTranscript(noAccents, noAccentsTranscript);
  }

  private String trimWhitespace(String against) {
    return CharMatcher.WHITESPACE.trimFrom(against);
  }

  private boolean matchTranscript(String foreignLanguage, String transcript) {
    return transcript == null ||
        foreignLanguage.isEmpty() ||
        transcript.isEmpty() ||
        removePunct(transcript).toLowerCase().equals(removePunct(foreignLanguage).toLowerCase());
  }

  private String removePunct(String t) {
    return t.replaceAll("\\p{P}", "").replaceAll("\\s++", "");
  }
}
