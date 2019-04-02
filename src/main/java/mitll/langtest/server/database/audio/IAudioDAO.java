/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.audio;

import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.server.domino.AudioCopy;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.shared.UserTimeBase;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.user.MiniUser;
import mitll.npdata.dao.SlickAudio;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IAudioDAO extends IDAO {

  /**
   * @param info
   * @return
   * @see mitll.langtest.server.services.AudioServiceImpl#addToAudioTable
   */
  AudioAttribute addOrUpdate(AudioInfo info);

  /**
   * @param projectid
   * @param hasProjectSpecificAudio
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#getAllAudioIDs
   */
  Map<Integer, List<AudioAttribute>> getExToAudio(int projectid, boolean hasProjectSpecificAudio);

  Collection<AudioAttribute> getAudioAttributesByProjectThatHaveBeenChecked(int projid, boolean hasProjectSpecificAudio);

  /**
   * @param projid
   * @return
   * @see SlickAudioDAO#getAudioAttributesByProjectThatHaveBeenChecked
   */
  List<SlickAudio> getAll(int projid);

  /**
   * @param projid
   * @return
   * @see mitll.langtest.server.domino.AudioCopy#getTranscriptToAudio
   */
  List<SlickAudio> getAllNoExistsCheck(int projid);

  Set<Integer> getExercisesThatHaveAudio(int projID, Collection<Integer> exids);

  /**
   * @param firstExercise
   * @param language
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#attachAudio
   */
  int attachAudioToExercise(ClientExercise firstExercise, Language language, Map<Integer, MiniUser> idToMini, SmallVocabDecoder smallVocabDecoder);

  /**
   * @param exercises
   * @param language
   * @param projID
   * @see mitll.langtest.server.database.DatabaseImpl#attachAllAudio
   * @see mitll.langtest.server.database.DatabaseImpl#writeZip
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getFullExercises
   */
  <T extends ClientExercise> void attachAudioToExercises(Collection<T> exercises, Language language, int projID);

  /**
   * @param userid
   * @param projid
   * @param exToTranscript
   * @return
   * @see mitll.langtest.server.database.exercise.FilterResponseHelper#getRecordedByMatchingGender
   */
  Collection<Integer> getRecordedBySameGender(int userid, int projid, Map<Integer, String> exToTranscript, boolean filterOnBothSpeeds);

  Set<Integer> getRecordedBySameGenderContext(int userid, int projid, Map<Integer, String> exToTranscript);

  boolean hasAudio(int exid);

  AudioAttribute getTranscriptMatch(int projID, int exid, int audioID, boolean isContext, String transcript, AudioCopy audioCopy);

  /**
   * @deprecated
   * @see mitll.langtest.server.services.QCServiceImpl#markGender
   * @param userid
   * @param projid
   * @param attr
   */
  void addOrUpdateUser(int userid, int projid, AudioAttribute attr);

  int markDefect(AudioAttribute attribute);

  Set<AudioAttribute> getAndMarkDefects(AudioAttributeExercise userExercise, Map<String, ExerciseAnnotation> fieldToAnnotation);

  void updateDNR(int uniqueID, float dnr);

  boolean didFindAnyAudioFiles(int projectid);

  void updateUser(int old, int newUser);

  void makeSureAudioIsThere(int projectID, String language, boolean validateAll);

  /**
   * @param userToGender
   * @param userid
   * @param exercise
   * @param language
   * @param idToMini
   * @param smallVocabDecoder
   * @return
   * @see Database#getNativeAudio(Map, int, int, Project, Map)
   */
  String getNativeAudio(Map<Integer, MiniUser.Gender> userToGender, int userid, CommonExercise exercise,
                        Language language, Map<Integer, MiniUser> idToMini, SmallVocabDecoder smallVocabDecoder);

  @NotNull
  List<Integer> getAllAudioIDs(int projectID, boolean hasProjectSpecificAudio);

  Map<String, Integer> getPairs(int projid);

  /**
   * @param audioID
   * @param hasProjectSpecificAudio
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#recalcRefAudioWithHelper(int, Integer, AudioFileHelper, int)
   */
  AudioAttribute getByID(int audioID, boolean hasProjectSpecificAudio);

  int markDefect(int id);

  /**
   * @see mitll.langtest.server.domino.AudioCopy#addCopiesToDatabase
   * @param copies
   */
  void addBulk(List<SlickAudio> copies);

  Collection<UserTimeBase> getAudioForReport(int projid);

  Map<String, Float> getMaleFemaleProgress(int projectid, Collection<CommonExercise> exercises);

  void copyOne(AudioCopy audioCopy, int audioID, int exid, boolean isContext);

  void deleteForProject(int projID);
}
