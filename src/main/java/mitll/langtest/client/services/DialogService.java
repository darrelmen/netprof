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
import mitll.langtest.client.dialog.RehearseViewHelper;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.DialogSession;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.dialog.IDialogSession;
import mitll.langtest.shared.exercise.*;

import java.util.List;
import java.util.Map;

@RemoteServiceRelativePath("dialog-manager")
public interface DialogService extends RemoteService {
  /**
   * @see mitll.langtest.client.dialog.DialogExerciseList#getTypeToValues(Map, int)
   * @param request
   * @return
   * @throws DominoSessionException
   */
  FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException;

  /**
   * @see mitll.langtest.client.dialog.DialogExerciseList#getExerciseIDs(Map, String, int, ExerciseListRequest)
   * @param request
   * @return
   * @throws DominoSessionException
   */
  ExerciseListWrapper<IDialog> getDialogs(ExerciseListRequest request) throws DominoSessionException;

  void update(IDialog dialog) throws DominoSessionException;
  IDialog addEmptyExercises(int dialogID, int afterExid, boolean isLeftSpeaker) throws DominoSessionException;

  IDialog addDialog(IDialog dialog) throws DominoSessionException;
  /**
   *
   *
   * @param projid
   * @param id
   * @throws DominoSessionException
   */
  boolean delete(int projid, int id) throws DominoSessionException;

  /**
   * @see mitll.langtest.client.dialog.ListenViewHelper#showContent
   * @param id
   * @return
   * @throws DominoSessionException
   */
  IDialog getDialog(int id) throws DominoSessionException;


  /**
   * @see RehearseViewHelper#setSession
   * @param dialogSession
   * @return
   * @throws DominoSessionException
   */
  int addSession(DialogSession dialogSession) throws DominoSessionException;

  /**
   * @see mitll.langtest.client.analysis.SessionAnalysis#SessionAnalysis
   * @param userid
   * @param dialogid
   * @return
   * @throws DominoSessionException
   */
  List<IDialogSession> getDialogSessions(int userid, int dialogid) throws DominoSessionException;

  List<Integer> deleteATurnOrPair(int projid, int dialogID, int exid) throws DominoSessionException;
}
