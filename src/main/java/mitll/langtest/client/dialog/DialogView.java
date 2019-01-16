package mitll.langtest.client.dialog;

import mitll.langtest.client.custom.INavigation;
import org.jetbrains.annotations.NotNull;

abstract class DialogView {
  @NotNull
  protected abstract INavigation.VIEWS getPrevView();

  @NotNull
  protected abstract INavigation.VIEWS getNextView();
}
