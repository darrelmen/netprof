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

package mitll.langtest.shared.exercise;

import mitll.langtest.server.audio.AudioExport;
import mitll.langtest.shared.user.MiniUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/5/16.
 */
public interface AudioAttributeExercise extends AudioRefExercise {
  String getRefAudioWithPrefs(Collection<Long> preferredVoices);

  /**
   * @see mitll.langtest.server.json.JsonExport#addContextAudioRefs
   * @see AudioExport#copyContextAudioBothGenders
   * @return
   */
  AudioAttribute getLatestContext(boolean isMale);

  /**
   * @see AudioExport#getAudioAttribute(MiniUser, CommonExercise, boolean, String)
   * @param userID
   * @param speed
   * @return
   */
  AudioAttribute getRecordingsBy(long userID, String speed);

  /**
   * @see AudioExport#getAudioAttribute
   * @param isMale
   * @return
   */
  Collection<AudioAttribute> getByGender(boolean isMale);

  /**
   * @see mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel#getAfterPlayWidget()
   * @return
   */
  Collection<AudioAttribute> getDefaultUserAudio();

  Map<String, AudioAttribute> getAudioRefToAttr();

  /**
   * @see mitll.langtest.client.scoring.FastAndSlowASRScoringAudioPanel#getAfterPlayWidget()
   * @param isMale
   * @return
   */
  Map<MiniUser, List<AudioAttribute>> getMostRecentAudio(boolean isMale, Collection<Long> preferredUsers);
}
