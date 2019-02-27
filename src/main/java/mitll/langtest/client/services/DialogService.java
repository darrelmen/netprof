package mitll.langtest.client.services;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.DialogSession;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.dialog.IDialogSession;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.FilterResponse;

import java.util.List;

@RemoteServiceRelativePath("dialog-manager")
public interface DialogService extends RemoteService {
  FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException;

  ExerciseListWrapper<IDialog> getDialogs(ExerciseListRequest request) throws DominoSessionException;

  /**
   * @see mitll.langtest.client.dialog.ListenViewHelper#showContent
   * @param id
   * @return
   * @throws DominoSessionException
   */
  IDialog getDialog(int id) throws DominoSessionException;

  int addSession(DialogSession dialogSession) throws DominoSessionException;

  /**
   * JUST FOR NOW. to show we can get one session worth of analysis data
   * @param dialogid
   * @return
   * @throws DominoSessionException
   */
  int getLatestDialogSessionID(int dialogid) throws DominoSessionException;

  /**
   * @see mitll.langtest.client.analysis.SessionAnalysis#SessionAnalysis
   * @param userid
   * @param dialogid
   * @return
   * @throws DominoSessionException
   */
  List<IDialogSession> getDialogSessions(int userid, int dialogid) throws DominoSessionException;
}
