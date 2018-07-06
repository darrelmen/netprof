package mitll.langtest.client.services;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.FilterResponse;

@RemoteServiceRelativePath("dialog-manager")
public interface DialogService extends RemoteService {
  FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException;
}
