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

package mitll.langtest.client.services;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.image.ImageResponse;
import mitll.langtest.shared.project.StartupInfo;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.DecoderOptions;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.RecalcRefResponse;

import java.util.Set;

public interface AudioServiceAsync {
  /**
   * @param base64EncodedString
   * @param audioContext
   * @param deviceType
   * @param device
   * @param decoderOptions
   * @param async
   * @see mitll.langtest.client.recorder.RecordButtonPanel#postAudioFile
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#postAudioFile
   */
  void writeAudioFile(String base64EncodedString,
                      AudioContext audioContext,

                      String deviceType,
                      String device,
                      DecoderOptions decoderOptions,

                      AsyncCallback<AudioAnswer> async);

  /**
   * @param reqid
   * @param audioFile
   * @param imageType
   * @param imageOptions
   * @param exerciseID
   * @param language
   * @param async
   * @see mitll.langtest.client.LangTest#getImage(int, String, String, String, int, int, String, String, AsyncCallback)
   */
  void getImageForAudioFile(int reqid, String audioFile, String imageType, ImageOptions imageOptions,
                            String exerciseID,
                            String language,
                            AsyncCallback<ImageResponse> async);

  /**
   * @param projectid
   * @param async
   * @see mitll.langtest.client.project.ProjectEditForm#checkAudio
   */
  void checkAudio(int projectid, AsyncCallback<Void> async);

  /**
   * @param id
   * @param asyncCallback
   * @see mitll.langtest.client.project.ProjectEditForm#recalcRefAudio
   */
  void recalcRefAudio(int id, AsyncCallback<RecalcRefResponse> asyncCallback);

  void logMessage(String subject, String message, boolean sendEmail, AsyncCallback<Void> async);

  void getStartupInfo(AsyncCallback<StartupInfo> async);

  /**
   * @param userExercise
   * @param keepAudio
   * @param async
   */
  void editItem(ClientExercise userExercise, boolean keepAudio, AsyncCallback<Void> async);

  void refreshExercises(int projid, Set<Integer> exids, AsyncCallback<Void> async);

  void getTranscriptMatch(int projID, int exid, int audioID, boolean isContext, String transcript, AsyncCallback<AudioAttribute> async);
}
