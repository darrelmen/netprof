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

package mitll.langtest.server.database.refaudio;

import mitll.langtest.server.audio.DecodeAlignOutput;
import mitll.langtest.server.database.IDAO;
import mitll.langtest.server.database.result.ISlimResult;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.userexercise.ExercisePhoneInfo;
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.langtest.shared.exercise.CommonExercise;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IRefResultDAO extends IDAO {
  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#getRefAudioAnswerDecoding
   * @param userID
   * @param projid
   * @param exid
   * @param audioID
   * @param durationInMillis
   * @param correct
   * @param alignOutput
   * @param decodeOutput
   * @param alignOutputOld
   * @param decodeOutputOld
   * @param isMale
   * @param speed
   * @param model
   * @return
   */
  long addAnswer(int userID,
                 int projid,
                 int exid,
                 int audioID,
                 long durationInMillis,
                 boolean correct,

                 DecodeAlignOutput alignOutput,
                 DecodeAlignOutput decodeOutput,

                 DecodeAlignOutput alignOutputOld,
                 DecodeAlignOutput decodeOutputOld,

                 boolean isMale, String speed, String model);

  boolean removeByAudioID(int audioID);

  /**
   * @see RefResultDecoder#getDecodedFiles
   * @return
   */
  List<Result> getResults();

  Map<Integer, ExercisePhoneInfo> getExerciseToPhoneForProject(int projid);

  /**
   * @see mitll.langtest.server.services.ScoringServiceImpl#getPretestScore
   * @return
   */
  Collection<ISlimResult> getAllSlimForProject(int projid);
  Collection<ISlimResult> getAllSlimForProjectIn(int projid, Set<Integer> audioIDs);
  ISlimResult getResult(int audioid);

  /**
   * @see mitll.langtest.server.decoder.RefResultDecoder#ensure
   * @return
   */
  int getNumResults();

  List<Integer> getAllAudioIDsForProject(int projid);

  void deleteForProject(int projid);
}
