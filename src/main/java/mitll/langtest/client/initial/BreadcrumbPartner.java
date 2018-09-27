package mitll.langtest.client.initial;

import mitll.langtest.client.custom.INavigation;
import mitll.langtest.shared.project.SlimProject;

interface BreadcrumbPartner {
  void resetLanguageSelection(int levelToRemove, SlimProject project);

  void chooseProjectAgain();

  INavigation getNavigation();
}