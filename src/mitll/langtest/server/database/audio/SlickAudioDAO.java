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
import mitll.langtest.shared.AudioType;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.npdata.dao.SlickAudio;
import mitll.npdata.dao.audio.AudioDAOWrapper;
import mitll.npdata.dao.DBConnection;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.*;

public class SlickAudioDAO extends BaseAudioDAO implements IAudioDAO {
  private static final Logger logger = Logger.getLogger(SlickAudioDAO.class);

  private final AudioDAOWrapper dao;

  public SlickAudioDAO(Database database, DBConnection dbConnection, IUserDAO userDAO) {
    super(database, userDAO);
    dao = new AudioDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  @Override
  public Collection<AudioAttribute> getAudioAttributes() {
    return toAudioAttribute(dao.getAll(), userDAO.getMiniUsers());
  }

  @Override
  public AudioAttribute addOrUpdate(int userid, String exerciseID, AudioType audioType, String audioRef,
                                    long timestamp, long durationInMillis, String transcript) {
    MiniUser miniUser = userDAO.getMiniUser(userid);
    Map<Integer, MiniUser> mini = new HashMap<>();
    mini.put(userid, miniUser);
    return toAudioAttribute(
        dao.addOrUpdate(userid, exerciseID, audioType.toString(), audioRef, timestamp, durationInMillis, transcript),
        mini);
  }

  /**
   * Update the user if the audio is already there.
   *
   * @param userid
   * @param exerciseID
   * @param audioType
   * @param audioRef
   * @param timestamp
   * @param durationInMillis
   * @param transcript
   */
  @Override
  public void addOrUpdateUser(int userid, String exerciseID, AudioType audioType, String audioRef, long timestamp,
                              int durationInMillis, String transcript) {
    dao.addOrUpdate(userid, exerciseID, audioType.toString(), audioRef, timestamp, durationInMillis, transcript);
  }

  @Override
  public void updateExerciseID(int uniqueID, String exerciseID) {
    dao.updateExerciseID(uniqueID, exerciseID);
  }

  @Override
  Collection<AudioAttribute> getAudioAttributes(String exid) {
    return toAudioAttributes(dao.getByExerciseID(exid));
  }

  @Override
  int getCountForGender(Set<Integer> userIds, String audioSpeed, Set<String> uniqueIDs) {
    return dao.getCountForGender(userIds, audioSpeed, uniqueIDs);
  }

  @Override
  Set<String> getValidAudioOfType(int userid, String audioType) {
    return dao.getExerciseIDsOfValidAudioOfType(userid, audioType);
  }

  /**
   * Items that are recorded must have both regular and slow speed audio.
   *
   * @param userid
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#markRecordedState
   */
  public Collection<String> getRecordedForUser(int userid) {
    try {
      return dao.getRecordedForUser(userid);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
    }
    return new HashSet<>();
  }

  @Override
  int markDefect(int userid, String exerciseID, AudioType audioType) {
    return dao.markDefect(userid, exerciseID, audioType.toString());
  }

  public Collection<String> getRecordedBy(int userid) {
    Collection<Integer> userIDs = getUserIDs(userid);
    return dao.getAudioForGenderBothSpeeds(userIDs);
  }

  @Override
  Set<String> getAudioExercisesForGender(Collection<Integer> userIDs, String audioSpeed) {
    return dao.getAudioForGender(userIDs, audioSpeed);
  }

  @Override
  int getCountBothSpeeds(Set<Integer> userIds, Set<String> uniqueIDs) {
    return dao.getCountBothSpeeds(userIds, uniqueIDs);
  }

  private List<AudioAttribute> toAudioAttributes(Collection<SlickAudio> all) {
    List<AudioAttribute> copy = new ArrayList<>();
    Map<Integer, MiniUser> idToMini = userDAO.getMiniUsers();
    for (SlickAudio s : all)
      copy.add(toAudioAttribute(s, idToMini));
    return copy;
  }

  int spew = 0;

  private AudioAttribute toAudioAttribute(SlickAudio s, Map<Integer, MiniUser> idToMini) {
    MiniUser miniUser = idToMini.get(s.userid());

    if (miniUser == null && spew++ < 20) logger.error("no user for " + s.userid());

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
        s.transcript());
  }

  public SlickAudio getSlickAudio(AudioAttribute orig, Map<Integer, Integer> oldToNewUser) {
    AudioType audioType = orig.getAudioType();
    if (audioType == null) {
      audioType = AudioType.REGULAR;
      logger.error("getSlickAudio found missing audio type " + orig);
    }
    Integer userid = oldToNewUser.get(orig.getUserid());
    if (userid == null) {
      logger.error("huh? no user id for " + orig.getUserid() + " for " + orig + " in " + oldToNewUser.size());
      return null;
    } else {
      return new SlickAudio(
          orig.getUniqueID(),
          userid,
          orig.getExid(),
          new Timestamp(orig.getTimestamp()),
          orig.getAudioRef(),
          audioType.toString(),
          orig.getDurationInMillis(),
          false,
          orig.getTranscript());
    }
  }

  private List<AudioAttribute> toAudioAttribute(List<SlickAudio> all, Map<Integer, MiniUser> idToMini) {
    List<AudioAttribute> copy = new ArrayList<>();
//    logger.info("table has " + dao.getNumRows());
    for (SlickAudio s : all) {
//      logger.info("got " + s);
      copy.add(toAudioAttribute(s, idToMini));
    }
    return copy;
  }

  public void addBulk(List<SlickAudio> bulk) {
    dao.addBulk(bulk);
  }  public void addBulk2(List<SlickAudio> bulk) {
    dao.addBulk2(bulk);
  }

  public int getNumRows() {
    return dao.getNumRows();
  }
}
