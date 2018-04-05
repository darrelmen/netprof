package mitll.langtest.client.custom;

import org.jetbrains.annotations.NotNull;

public interface IViewContaner {
  @NotNull
  INavigation.VIEWS getCurrentView();
}
