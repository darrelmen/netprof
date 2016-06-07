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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.database.AudioExport;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.user.BaseUserDAO;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.User;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.MutableAudioExercise;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.OutputStream;
import java.util.*;

public abstract class BaseAudioDAO extends DAO {
  private static final Logger logger = Logger.getLogger(BaseAudioDAO.class);

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
  protected static final String CONTEXT_REGULAR = AUDIO_TYPE1;
  private static final boolean DEBUG_ATTACH = false;

  protected final IUserDAO userDAO;

  BaseAudioDAO(Database database, IUserDAO userDAO) {
    super(database);
    this.userDAO = userDAO;
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
        ") took " + (now - then) + " millis to get  " + audioAttributes1.size() + " audio entries " + this);
//    logger.debug("map size is " + exToAudio.size());
    return exToAudio;
  }

  abstract Collection<AudioAttribute> getAudioAttributes();

  /**
   * @param firstExercise
   * @param installPath
   * @param relativeConfigDir
   * @see mitll.langtest.server.LangTestDatabaseImpl#attachAudio(mitll.langtest.shared.exercise.CommonExercise)
   * @see DatabaseImpl#attachAudio(CommonExercise)
   * @see DatabaseImpl#writeZip(OutputStream, long, PathHelper)
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
   * @see AudioExport#writeFolderContents
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
      MiniUser user = attr.getUser();
      if (user.isUnknownDefault()) {
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

  private boolean attachAudioAndFixPath(CommonExercise firstExercise,
                                        String installPath,
                                        String relativeConfigDir,
                                        AudioConversion audioConversion,
                                        AudioAttribute attr) {
    Collection<CommonExercise> directlyRelated = firstExercise.getDirectlyRelated();
    String against = attr.isContextAudio() && !directlyRelated.isEmpty() ?
        directlyRelated.iterator().next().getForeignLanguage() : firstExercise.getForeignLanguage();
    if (attr.hasMatchingTranscript(against)) {
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
/*
      logger.info("not attaching audio " + attr.getUniqueID() + " to " + firstExercise.getID() + " since transcript has changed. Audio '" +
          attr.getTranscript()+
          "' vs exercise '" +foreignLanguage+
          "'");
*/

      return false;
    }
  }

  abstract Collection<AudioAttribute> getAudioAttributes(String exid);

  /**
   * Get back the ids of exercises recorded by people who are the same gender as the userid.
   *
   * @param userid only used to determine the gender we should show
   * @return ids with both regular and slow speed recordings
   * @see mitll.langtest.server.LangTestDatabaseImpl#filterByUnrecorded
   */
  public Collection<String> getRecordedBy(int userid) {
    //Map<Integer, User> userMap = getUserMap(userid);
    Collection<Integer> userIDs = getUserIDs(userid);
    //logger.debug("found " + (isMale ? " male " : " female ") + " users : " + userMap.keySet());
    // find set of users of same gender
    Set<String> validAudioAtReg = getAudioExercisesForGender(userIDs, REGULAR);
    //logger.debug(" regular speed for " + userMap.keySet() + " " + validAudioAtReg.size());

    Set<String> validAudioAtSlow = getAudioExercisesForGender(userIDs, SLOW);
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
   * @see DatabaseImpl#getMaleFemaleProgress()
   */
  public Map<String, Float> getRecordedReport(Map<Integer, User> userMapMales,
                                              Map<Integer, User> userMapFemales,
                                              float total,
                                              Set<String> uniqueIDs,
                                              float totalContext) {
    Set<Integer> maleIDs = userMapMales.keySet();
    maleIDs = new HashSet<>(maleIDs);
    maleIDs.add(BaseUserDAO.DEFAULT_MALE_ID);

    float maleFast = getCountForGender(maleIDs, REGULAR, uniqueIDs);
    float maleSlow = getCountForGender(maleIDs, SLOW, uniqueIDs);
    float male = getCountBothSpeeds(maleIDs, uniqueIDs);

    Set<Integer> femaleIDs = userMapFemales.keySet();
    femaleIDs = new HashSet<>(femaleIDs);
    femaleIDs.add(BaseUserDAO.DEFAULT_FEMALE_ID);

    float femaleFast = getCountForGender(femaleIDs, REGULAR, uniqueIDs);
    float femaleSlow = getCountForGender(femaleIDs, SLOW, uniqueIDs);
    float female = getCountBothSpeeds(femaleIDs, uniqueIDs);

    float cmale = getCountForGender(maleIDs, CONTEXT_REGULAR, uniqueIDs);
    float cfemale = getCountForGender(femaleIDs, CONTEXT_REGULAR, uniqueIDs);

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

  abstract int getCountForGender(Set<Integer> userIds, String audioSpeed, Set<String> uniqueIDs);

  /**
   * Items that are recorded must have both regular and slow speed audio.
   *
   * @param userid
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#markRecordedState
   */
  public Collection<String> getRecordedForUser(int userid) {
    try {
      Set<String> validAudioAtReg  = getValidAudioOfType(userid, REGULAR);
      Set<String> validAudioAtSlow = getValidAudioOfType(userid, SLOW);
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
   * @see mitll.langtest.server.LangTestDatabaseImpl#markRecordedState(int, String, Collection, boolean)
   */
  public Collection<String> getRecordedExampleForUser(int userid) {
    try {
      return getValidAudioOfType(userid, AUDIO_TYPE1);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<>();
  }

  abstract Set<String> getValidAudioOfType(int userid, String audioType);

  MiniUser checkDefaultUser(long userID, MiniUser user) {
    if (userID == BaseUserDAO.DEFAULT_USER_ID) {
      user = BaseUserDAO.DEFAULT_USER;
    } else if (userID == BaseUserDAO.DEFAULT_MALE_ID) {
      user = BaseUserDAO.DEFAULT_MALE;
    } else if (userID == BaseUserDAO.DEFAULT_FEMALE_ID) {
      user = BaseUserDAO.DEFAULT_FEMALE;
    }
    return user;
  }

  protected AudioAttribute getAudioAttribute(int userid, String exerciseID, String audioType) {
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
  protected AudioAttribute getAudioAttribute(int i,
                                             int userid, String audioRef, String exerciseID, long timestamp,
                                             String audioType, long durationInMillis, String transcript) {
    MiniUser miniUser = userDAO.getMiniUser(userid);

    return new AudioAttribute(i, userid,
        exerciseID, // id
        audioRef, // answer
        timestamp,
        durationInMillis, audioType,
        miniUser, transcript);
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
   * Go back and mark gender on really old audio that had no user info on it.
   *
   * @param userid
   * @param attr
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#markGender(AudioAttribute, boolean)
   */
  public void addOrUpdateUser(int userid, AudioAttribute attr) {
    long timestamp = attr.getTimestamp();
    if (timestamp == 0) timestamp = System.currentTimeMillis();
    addOrUpdateUser(userid, attr.getExid(), attr.getAudioType(), attr.getAudioRef(), timestamp,
        (int) attr.getDurationInMillis(), BaseAudioDAO.UNKNOWN);
  }

  abstract void addOrUpdateUser(int userid, String exerciseID, String audioType, String audioRef, long timestamp,
                                int durationInMillis, String transcript);

  abstract int markDefect(int userid, String exerciseID, String audioType);

  boolean isBadUser(int userid) {
    return userid < BaseUserDAO.DEFAULT_FEMALE_ID;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#filterByUnrecorded(ExerciseListRequest, Collection)
   * @param userid
   * @return
   */
  public Set<String> getWithContext(int userid) { return getWithContext(getUserMap(userid));  }

  private Set<String> getWithContext(Map<Integer, User> userMap) {
    return getAudioExercisesForGender(userMap.keySet(), CONTEXT_REGULAR);
  }

  abstract Set<String> getAudioExercisesForGender(Collection<Integer> userIDs, String audioSpeed);

  abstract int getCountBothSpeeds(Set<Integer> userIds,
                                  Set<String> uniqueIDs);
}
