package mitll.langtest.client.initial;

import mitll.langtest.shared.project.SlimProject;

public interface BreadcrumbPartner {
  void resetLanguageSelection(int levelToRemove, SlimProject project);

  void chooseProjectAgain();
}
