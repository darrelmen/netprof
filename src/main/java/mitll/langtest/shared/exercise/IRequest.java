package mitll.langtest.shared.exercise;

import mitll.langtest.shared.project.ProjectMode;

public interface IRequest {
  int getReqID();

  String getPrefix();

  int getLimit();

  ProjectMode getMode();
}
