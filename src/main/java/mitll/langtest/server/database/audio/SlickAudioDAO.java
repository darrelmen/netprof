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

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.user.MiniUser;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickAudio;
import mitll.npdata.dao.audio.AudioDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.*;

public class SlickAudioDAO extends BaseAudioDAO implements IAudioDAO {
  private static final Logger logger = LogManager.getLogger(SlickAudioDAO.class);

  private final AudioDAOWrapper dao;
  private final long now = System.currentTimeMillis();
  private final long before = now - (24 * 60 * 60 * 1000);
  private final boolean doCheckOnStartup;

  public SlickAudioDAO(Database database, DBConnection dbConnection, IUserDAO userDAO) {
    super(database, userDAO);
    dao = new AudioDAOWrapper(dbConnection);
    doCheckOnStartup = database.getServerProps().doAudioCheckOnStartup();
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
   */
  @Override
  public Collection<AudioAttribute> getAudioAttributesByProject(int projid) {
    List<SlickAudio> all = dao.getAll(projid);
    logger.info("Getting mini users");
   // Map<Integer, MiniUser> miniUsers = userDAO.getMiniUsers();
    logger.info("getAudioAttributesByProject " + projid +
        " " + all.size()
        //+        " users " + miniUsers.size()
    );
    return toAudioAttribute(all);
  }

  @Override
  public AudioAttribute addOrUpdate(int userid, int exerciseID, int projid, AudioType audioType, String audioRef,
                                    long timestamp, long durationInMillis, String transcript, float dnr) {
    MiniUser miniUser = userDAO.getMiniUser(userid);
//    Map<Integer, MiniUser> mini = new HashMap<>();
//    mini.put(userid, miniUser);
    return toAudioAttribute(
        dao.addOrUpdate(userid, exerciseID, projid, audioType.toString(), audioRef, timestamp, durationInMillis, transcript, dnr),
        miniUser);
  }

  /**
   * Update the user if the audio is already there.
   *
   * @param userid
   * @param exerciseID
   * @param projid
   * @param audioType
   * @param audioRef
   * @param timestamp
   * @param durationInMillis
   * @param transcript
   * @param dnr
   */
  @Override
  public void addOrUpdateUser(int userid, int exerciseID, int projid, AudioType audioType, String audioRef, long timestamp,
                              int durationInMillis, String transcript, float dnr) {
    dao.addOrUpdateUser(userid, exerciseID, projid, audioType.toString(), audioRef, timestamp, durationInMillis, transcript, dnr);
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
   * @see mitll.langtest.server.database.exercise.BaseExerciseDAO#setAudioDAO
   */
  @Override
  public void validateFileExists(int projid, String installPath, String language) {
    long pastTime = doCheckOnStartup ? now : before;
    dao.validateFileExists(projid, pastTime, installPath, language.toLowerCase());
  }

  @Override
  public void updateDNR(int uniqueID, float dnr) {
    logger.info("add impl for updateDNR...");
  }

  public boolean didFindAnyAudioFiles(int projectid) {
    return dao.getCountExists(projectid) > 0;
  }

  @Override
  Collection<AudioAttribute> getAudioAttributesForExercise(int exid) {
    long then = System.currentTimeMillis();
    List<SlickAudio> byExerciseID = dao.getByExerciseID(exid);
    long now = System.currentTimeMillis();
    if (now - then > 20)
      logger.warn("getAudioAttributesForExercise took " + (now - then) + " to get " + byExerciseID.size() + " attr for " + exid);
    return toAudioAttributes(byExerciseID);
  }

  @Override
  int getCountForGender(Set<Integer> userIds,
                        String audioSpeed,
                        Set<Integer> uniqueIDs,
                        Map<Integer, String> exToTranscript,
                        Set<Integer> idsOfRecordedExercises) {
    Set<Integer> countForGender1 = dao.getCountForGender(userIds, audioSpeed, uniqueIDs,exToTranscript,true);
    Set<Integer> countForGender = countForGender1;
    idsOfRecordedExercises.addAll(countForGender);
    return countForGender.size();
  }

  @Override
  Set<Integer> getValidAudioOfType(int userid, String audioType) {
    return dao.getExerciseIDsOfValidAudioOfType(userid, audioType);
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

  public Collection<Integer> getRecordedBy(int userid) {
    Collection<Integer> userIDs = getUserIDs(userid);
    return dao.getAudioForGenderBothSpeeds(userIDs);
  }

  /**
   * TODO TODO :
   * <p>
   * only include ids for audio where the audio transcript matches the exercise text
   *
   * @param userIDs
   * @param audioSpeed
   * @param exToTranscript
   * @return modifiable set - what's returned from scala isn't
   */
  @Override
  Set<Integer> getAudioExercisesForGender(Collection<Integer> userIDs,
                                          String audioSpeed,
                                          Map<Integer, String> exToTranscript) {
    boolean checkAudioTranscript = database.getServerProps().shouldCheckAudioTranscript();

    if (checkAudioTranscript) {
      logger.warn("getAudioExercisesForGender should do check audio transcript -");
    }

    Set<Integer> audioForGender = dao.getAudioForGender(userIDs, audioSpeed);
    return new HashSet<>(audioForGender);
  }

  @Override
  int getCountBothSpeeds(Set<Integer> userIds, Set<Integer> uniqueIDs) {
    return dao.getCountBothSpeeds(userIds, uniqueIDs);
  }

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
   * @see #toAudioAttribute(List)
   */
  private AudioAttribute toAudioAttribute(SlickAudio s, MiniUser miniUser){//Map<Integer, MiniUser> idToMini2) {
   // MiniUser miniUser = idToMini.get(s.userid());

    if (miniUser == null && spew++ < 20) {
      logger.error("toAudioAttribute : no user for " + s.userid());
    }

    String audiotype = s.audiotype();

    // logger.info("got slick " + s);
    // for the enum!
    if (audiotype.contains("=")) {
      // logger.info("Was " + audiotype);
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
        s.dnr());
  }

  int c = 0;

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

      return new SlickAudio(
          orig.getUniqueID(),
          userid,
          orig.getExid(),
          new Timestamp(timestamp),
          orig.getAudioRef(),
          audioType.toString(),
          orig.getDurationInMillis(),
          false,
          orig.getTranscript(),
          projid,
          false,
          0,
          "",
          orig.getDnr());
    }
  }

  /**
   * @paramx idToMini
   * @param all
   * @return
   * @see #getAudioAttributesByProject(int)
   */
  private List<AudioAttribute> toAudioAttribute(List<SlickAudio> all) {
    List<AudioAttribute> copy = new ArrayList<>();
    if (all.isEmpty()) {
      logger.warn("toAudioAttribute table has " + dao.getNumRows());
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

  public void addBulk(List<SlickAudio> bulk) {
    dao.addBulk(bulk);
  }

  public void addBulk2(List<SlickAudio> bulk) {
    dao.addBulk2(bulk);
  }

  public int getNumRows() {
    return dao.getNumRows();
  }
}
