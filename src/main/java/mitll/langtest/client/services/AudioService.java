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
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.image.ImageResponse;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.OOVInfo;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.StartupInfo;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.RecalcRefResponse;

import java.util.Set;

/**
 * Might actually live on hydra1 or hydra2 - chosen in the client.
 */
@RemoteServiceRelativePath("audio-manager")
public interface AudioService extends RemoteService {
  /**
   * @param base64EncodedString encoded audio bytes
   * @param audioContext
   * @param deviceType
   * @param device
   * @return
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#postAudioFile
   * @see mitll.langtest.client.recorder.RecordButtonPanel#postAudioFile(Panel, String)
   */
  AudioAnswer writeAudioFile(String base64EncodedString,
                             AudioContext audioContext,
                             String deviceType,
                             String device,
                             DecoderOptions decoderOptions) throws DominoSessionException;

  /**
   * TODO : why exerciseID a String
   *
   * @param reqid
   * @param audioFile
   * @param imageType
   * @param imageOptions
   * @param exerciseID
   * @param language
   * @return
   * @see LangTest#getImage
   */
  ImageResponse getImageForAudioFile(int reqid,
                                     String audioFile, String imageType, ImageOptions imageOptions,
                                     String exerciseID,
                                     Language language) throws DominoSessionException;

  /**
   * @param projectid
   * @see mitll.langtest.client.project.ProjectEditForm#getCheckAudio(ProjectInfo)
   */
  void checkAudio(int projectid) throws DominoSessionException, RestrictedOperationException;

  RecalcRefResponse recalcRefAudio(int projectid) throws DominoSessionException, RestrictedOperationException;

  void logMessage(String subject, String message, boolean sendEmail);

  AudioAttribute getTranscriptMatch(int projID, int exid, int audioID, boolean isContext, String transcript) throws DominoSessionException;

  /**
   * @return
   */
  StartupInfo getStartupInfo();

  /**
   * @param userExercise
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#editItem
   */
  void editItem(ClientExercise userExercise, boolean keepAudio) throws DominoSessionException, RestrictedOperationException;

  void refreshExercises(int projid, Set<Integer> exids) throws DominoSessionException;

  OOVInfo checkOOV(int id) throws DominoSessionException;

}
