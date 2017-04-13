package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Widget;

/**
 * Created by go22670 on 4/10/17.
 */
public interface INavigation {
  void showInitialState();

  void showLearn();
  void showDrill();
  void showProgress();
  void showLists();

  Widget getNavigation();

  void onResize();

  void showPreviousState();

  void clearCurrent();
}
