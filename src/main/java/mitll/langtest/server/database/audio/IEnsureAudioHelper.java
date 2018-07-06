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

import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.User;

import java.util.Collection;
import java.util.Map;

public interface IEnsureAudioHelper {
  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#checkAudio
   * @see mitll.langtest.client.project.ProjectEditForm#getCheckAudio
   * @param projectid
   */
  void ensureAudio(int projectid);

  /**
   *
   * @param exercises
   * @param language
   */
  void ensureCompressedAudio(Collection<CommonExercise> exercises, String language);

  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#writeAudioFile
   * @param user
   * @param commonShell
   * @param path
   * @param audioType
   * @param language
   * @param idToUser
   * @return
   */
  String ensureCompressedAudio(int user,
                               ClientExercise commonShell,
                               String path,
                               AudioType audioType,
                               String language,
                               Map<Integer, User> idToUser);

  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#getImageForAudioFile
   * @param audioFile
   * @param language
   * @return
   */
  String getWavAudioFile(String audioFile, String language);
}
