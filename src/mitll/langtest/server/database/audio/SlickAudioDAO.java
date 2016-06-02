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
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickAudio;
import mitll.npdata.dao.audio.AudioDAOWrapper;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SlickAudioDAO extends BaseAudioDAO implements IAudioDAO {
  private final AudioDAOWrapper dao;

  public SlickAudioDAO(Database database, DBConnection dbConnection, IUserDAO userDAO) {
    super(database, userDAO);
    dao = new AudioDAOWrapper(dbConnection);
  }

  @Override
  public Collection<AudioAttribute> getAudioAttributes() {
    return toAudioAttribute(dao.getAll());
  }

   @Override
  public AudioAttribute addOrUpdate(int userid, String exerciseID, String audioType, String audioRef, long timestamp, long durationInMillis, String transcript) {
    return null;
  }

  @Override
  public void updateExerciseID(int uniqueID, String exerciseID) {

  }

  @Override
  Collection<AudioAttribute> getAudioAttributes(String exid) {
    return toAudioAttributes(dao.getByExerciseID(exid));
  }

  @Override
  int getCountForGender(Set<Integer> userIds, String audioSpeed, Set<String> uniqueIDs) {
    return dao.getCountForGender(userIds,audioSpeed,uniqueIDs);
  }

  @Override
  Set<String> getValidAudioOfType(long userid, String audioType) {
    return null;
  }

  @Override
  void addOrUpdateUser(int userid, String audioRef, String exerciseID, long timestamp, String audioType, int durationInMillis, String transcript) {

  }

  @Override
  int markDefect(int userid, String exerciseID, String audioType) {
    return 0;
  }

  @Override
  Set<String> getAudioExercisesForGender(Set<Integer> userIDs, String audioSpeed) {
    return dao.getAudioForGender(userIDs,audioSpeed);
  }

  @Override
  int getCountBothSpeeds(Set<Integer> userIds, Set<String> uniqueIDs) {
    return 0;
  }

  private List<AudioAttribute> toAudioAttributes(Collection<SlickAudio> all) {
    List<AudioAttribute> copy = new ArrayList<>();
    for (SlickAudio s : all)
      copy.add(toAudioAttribute(s));
    return copy;
  }

  private AudioAttribute toAudioAttribute(SlickAudio s) {
    MiniUser miniUser = userDAO.getMiniUser(s.userid());
    return new AudioAttribute(
        s.id(),
        s.userid(),
        s.exid(),
        s.audioref(),
        s.modified().getTime(),
        s.duration(),
        s.audiotype(),
        miniUser,
        s.transcript());

  }
  private List<AudioAttribute> toAudioAttribute(List<SlickAudio> all) {
    List<AudioAttribute> copy = new ArrayList<>();
    for (SlickAudio s : all)
      copy.add(toAudioAttribute(s));
    return copy;
  }
}
