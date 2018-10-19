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

import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.UserTimeBase;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.user.MiniUser;
import mitll.npdata.dao.SlickAudio;

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

  List<SlickAudio> getAll(int projid);

  List<SlickAudio> getAllNoExistsCheck(int projid);

  /**
   * @param firstExercise
   * @param language
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#attachAudio
   */
  int attachAudioToExercise(ClientExercise firstExercise, Language language, Map<Integer, MiniUser> idToMini);

  /**
   * @param exercises
   * @param language
   * @see mitll.langtest.server.database.DatabaseImpl#attachAllAudio
   * @see mitll.langtest.server.database.DatabaseImpl#writeZip
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getFullExercises
   */
 <T extends ClientExercise> void attachAudioToExercises(Collection<T> exercises, Language language);

  /**
   * @param firstExercise
   * @param audioAttributes
   * @param language
   * @return
   * @see
   */
/*
  boolean attachAudio(CommonExercise firstExercise,
                      Collection<AudioAttribute> audioAttributes,
                      String language, boolean debug);
*/

  /**
   * @param userid
   * @param projid
   * @param exToTranscript
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#filterByUnrecorded
   */
  Collection<Integer> getRecordedBySameGender(int userid, int projid, Map<Integer, String> exToTranscript);

  Set<Integer> getRecordedBySameGenderContext(int userid, int projid, Map<Integer, String> exToTranscript);


  boolean hasAudio(int exid);

  /**
   * @param userid
   * @return
   * @see mitll.langtest.server.services.ExerciseServiceImpl#markRecordedState
   */
  Collection<Integer> getRecordedExForUser(int userid);

  Collection<Integer> getRecordedExampleForUser(int userid);

  void addOrUpdateUser(int userid, int projid, AudioAttribute attr);

  int markDefect(AudioAttribute attribute);

  Set<AudioAttribute> getAndMarkDefects(AudioAttributeExercise userExercise,
                                        Map<String, ExerciseAnnotation> fieldToAnnotation);

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
   * @return
   * @see Database#getNativeAudio(Map, int, int, Project, Map)
   */
  String getNativeAudio(Map<Integer, MiniUser.Gender> userToGender, int userid, CommonExercise exercise,
                        Language language, Map<Integer, MiniUser> idToMini);

  Map<String, Integer> getPairs(int projid);

  /**
   * @param audioID
   * @param hasProjectSpecificAudio
   * @return
   * @see mitll.langtest.server.services.ScoringServiceImpl#recalcRefAudioWithHelper(int, Integer, AudioFileHelper, int)
   */
  AudioAttribute getByID(int audioID, boolean hasProjectSpecificAudio);

  void addBulk(List<SlickAudio> copies);

  Collection<UserTimeBase> getAudioForReport(int projid);

  Map<String, Float> getMaleFemaleProgress(int projectid, Collection<CommonExercise> exercises);

  void deleteForProject(int projID);
}
