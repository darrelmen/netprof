package mitll.langtest.client.services;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.FilterResponse;

@RemoteServiceRelativePath("dialog-manager")
public interface DialogService extends RemoteService {
  FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException;

  ExerciseListWrapper<IDialog> getDialogs(ExerciseListRequest request) throws DominoSessionException;

  IDialog getDialog(int id) throws DominoSessionException;
}
