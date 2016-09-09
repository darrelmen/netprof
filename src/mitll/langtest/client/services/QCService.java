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

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.client.custom.dialog.ReviewEditableExercise;
import mitll.langtest.client.custom.tabs.RememberTabAndContent;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.STATE;
import mitll.langtest.shared.user.MiniUser;

import java.util.List;

@RemoteServiceRelativePath("qc-manager")
public interface QCService extends RemoteService {
  /**
   * @see mitll.langtest.client.flashcard.FlashcardPanel#addAnnotation(String, String, String)
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#addAnnotation(String, String, String)
   * @param exerciseID
   * @param field
   * @param status
   * @param comment
   * @param userID
   */
  void addAnnotation(int exerciseID, String field, String status, String comment, int userID);

  // QC State changes

  /**
   * @see mitll.langtest.client.custom.dialog.ReviewEditableExercise#getDeleteButton
   * @param audioAttribute
   * @param exid
   */
  void markAudioDefect(AudioAttribute audioAttribute, HasID exid);

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#markGender(MiniUser, Button, AudioAttribute, RememberTabAndContent, List, Button, boolean)
   * @param attr
   * @param isMale
   */
  void markGender(AudioAttribute attr, boolean isMale);

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#markReviewed(HasID)
   * @param exid
   * @param isCorrect
   * @param creatorID
   */
  void markReviewed(int exid, boolean isCorrect, int creatorID);

  /**
   * @see mitll.langtest.client.qc.QCNPFExercise#markAttentionLL(ListInterface, HasID)
   * @param exid
   * @param state
   * @param creatorID
   */
  void markState(int exid, STATE state, int creatorID);

  /**
   * @see ReviewEditableExercise#confirmThenDeleteItem()
   * @param exid
   * @return
   */
  boolean deleteItem(int exid);

}
