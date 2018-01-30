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
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.client.LangTest;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.image.ImageResponse;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.ImageOptions;

/**
 * Might actually live on hydra1 or hydra2 - chosen in the client.
 */
@RemoteServiceRelativePath("audio-manager")
public interface AudioService extends RemoteService {
  /**
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#postAudioFile(String)
   * @param base64EncodedString encoded audio bytes
   * @param audioContext
   * @param recordedWithFlash
   * @param deviceType
   * @param device
   * @param doFlashcard
   * @param recordInResults
   * @param addToAudioTable
   * @param allowAlternates
   * @return
   */
  AudioAnswer writeAudioFile(String base64EncodedString,
                             AudioContext audioContext,
                             boolean recordedWithFlash,
                             String deviceType,
                             String device,
                             boolean doFlashcard,
                             boolean recordInResults,
                             boolean addToAudioTable,
                             boolean allowAlternates) throws DominoSessionException;

  /**
   * TODO : why exerciseID a String
   * @see LangTest#getImage
   * @param reqid
   * @param audioFile
   * @param imageType
   * @param imageOptions
   * @param exerciseID
   * @return
   */
  ImageResponse getImageForAudioFile(int reqid,
                                     String audioFile, String imageType, ImageOptions imageOptions,
                                     String exerciseID,
                                     String language) throws DominoSessionException;

  /**
   * @see mitll.langtest.client.project.ProjectEditForm#getCheckAudio(ProjectInfo)
   * @param projectid
   */
  void checkAudio(int projectid) throws DominoSessionException, RestrictedOperationException;
  void recalcRefAudio(int projectid) throws DominoSessionException, RestrictedOperationException;
}
