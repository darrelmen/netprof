package mitll.langtest.client.banner;

import mitll.langtest.client.custom.INavigation;
import org.jetbrains.annotations.NotNull;

public abstract class DialogView {
  @NotNull
  protected abstract INavigation.VIEWS getPrevView();

  @NotNull
  protected abstract INavigation.VIEWS getNextView();
}
