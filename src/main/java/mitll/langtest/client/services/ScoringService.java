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

package mitll.langtest.client.services;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.scoring.AlignmentAndScore;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * So this could either be hosted on hydra1 or hydra2 or possibly more.
 */
@RemoteServiceRelativePath("scoring-manager")
public interface ScoringService extends RemoteService {
  /**
   * @param resultID
   * @return
   * @see mitll.langtest.client.scoring.ReviewScoringPanel#scoreAudio
   */
  PretestScore getResultASRInfo(int resultID, ImageOptions imageOptions) throws DominoSessionException, RestrictedOperationException;


  Map<Integer, AlignmentAndScore> getAlignments(int projid, Set<Integer> audioIDs) throws DominoSessionException;
  AlignmentAndScore getStudentAlignment(int projid, int resultID) throws DominoSessionException;

  /**
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param usePhonemeMap
   * @return
   * @see ASRScoringAudioPanel#scoreAudio
   */
  PretestScore getASRScoreForAudio(int reqid,
                                   long resultID,
                                   String testAudioFile,
                                   String sentence,
                                   String transliteration,

                                   ImageOptions imageOptions,
                                   int exerciseID,
                                   boolean usePhonemeMap) throws DominoSessionException, RestrictedOperationException;
  /**
   *
   * @param projid
   * @param resultid
   * @param roundTrip
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#addRT
   */
  void addRoundTrip(int projid, int resultid, int roundTrip);

  boolean isHydraRunning(int projid) throws DominoSessionException, RestrictedOperationException;

  /**
   * @param foreign
   * @return
   * @seex mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase
   */
  Collection<String> isValidForeignPhrase(int projID, String foreign, String transliteration);

  /**
   * @param projid
   * @see mitll.langtest.client.project.ProjectChoices#recalcProject
   */
  void recalcAlignments(int projid) throws DominoSessionException, RestrictedOperationException;

  /**
   * @see mitll.langtest.client.LangTest#tellHydraServerToRefreshProject
   * @param projID
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   */
  void configureAndRefresh(int projID) throws DominoSessionException, RestrictedOperationException;

  void ensureAudio(int resultID) throws DominoSessionException;
}
