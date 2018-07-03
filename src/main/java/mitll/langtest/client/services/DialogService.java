package mitll.langtest.client.services;

import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.FilterResponse;

public interface DialogService {
  FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException;

}
