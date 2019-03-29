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

package mitll.langtest.shared.exercise;

import mitll.langtest.shared.user.MiniUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AudioRefExercise {
  boolean hasRefAudio();

  /**
   * @param vocab
   * @return
   * @see mitll.langtest.client.scoring.DialogExercisePanel#hasAudio
   */
  boolean hasAudioNonContext(boolean vocab);

  /**
   * @see mitll.langtest.client.scoring.DialogExercisePanel#hasAudio
   * @return
   */
  boolean hasContextAudio();

  String getRefAudio();

  String getSlowAudioRef();

  /**
   * @return
   * @see mitll.langtest.client.scoring.DialogExercisePanel#getRegularSpeedIfAvailable
   */
  AudioAttribute getRegularSpeed();

  AudioAttribute getFirst();
  Collection<AudioAttribute> getAudioAttributes();
  Collection<AudioAttribute> getContextAudio();

  Collection<String> getAudioPaths();

  AudioAttribute getRecordingsBy(long userID, boolean regularSpeed);

  /**
   * TODO :  Sorted by user age. Gotta choose something... ???
   *
   * @param malesMap
   * @return
   */
  List<MiniUser> getSortedUsers(Map<MiniUser, List<AudioAttribute>> malesMap);

  Map<MiniUser, List<AudioAttribute>> getUserMap(boolean isMale, boolean includeContext);

  /**
   * CLIENT
   *
   * @param isMale
   * @return
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#getContextPlay
   */
  AudioAttribute getAudioAttrPrefGender(boolean isMale);

  /**
   * CLIENT
   *
   * @param isMale
   * @param preferredUsers
   * @param includeContext
   * @return
   * @see mitll.langtest.client.scoring.ChoicePlayAudioPanel#addChoices
   */
  Map<MiniUser, List<AudioAttribute>> getMostRecentAudio(boolean isMale, Collection<Integer> preferredUsers, boolean includeContext);

  List<AudioAttribute> getMostRecentAudioEasy(boolean isMale, boolean includeContext);
}
