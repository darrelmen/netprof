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

import mitll.langtest.server.database.IDAO;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.AudioAttributeExercise;
import mitll.langtest.shared.exercise.CommonExercise;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IAudioDAO extends IDAO {
  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#addToAudioTable
   * @param info
   * @return
   */
  AudioAttribute addOrUpdate(AudioInfo info);

  Map<Integer, List<AudioAttribute>> getExToAudio(int projectid);

  Collection<AudioAttribute> getAudioAttributesByProject(int projid);

  int attachAudioToExercise(CommonExercise firstExercise, String language);

  boolean attachAudio(CommonExercise firstExercise,
                      Collection<AudioAttribute> audioAttributes,
                      String language);

  /**
   * @see mitll.langtest.server.services.ExerciseServiceImpl#filterByUnrecorded
   * @param userid
   * @param exToTranscript
   * @param projid
   * @return
   */
  Collection<Integer> getRecordedBySameGender(int userid, Map<Integer, String> exToTranscript, int projid);

  Set<Integer> getWithContext(int userid, Map<Integer, String> exToContext, int projid);

  Map<String, Float> getRecordedReport(int projid,
                                       float total,
                                       float totalContext,
                                       Set<Integer> exerciseIDs,
                                       Map<Integer, String> exToTranscript,
                                       Map<Integer, String> exToContextTranscript);

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

  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#reallyCreateNewItem
   * @param uniqueID
   * @param exerciseID
   */
  void updateExerciseID(int uniqueID, int exerciseID, String actualPath);

  void validateFileExists(int projid, String installPath, String language);

  void updateDNR(int uniqueID, float dnr);

  boolean didFindAnyAudioFiles(int projectid);
}
