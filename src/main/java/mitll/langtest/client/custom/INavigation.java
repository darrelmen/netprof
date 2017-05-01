package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Widget;

/**
 * Created by go22670 on 4/10/17.
 */
public interface INavigation {
  void showInitialState();
/*
  void showLearn();

  void showDrill();

  void showProgress();

  void showLists();

  void showRecord();

  void showRecordExample();*/

  void showView(VIEWS view);

  Widget getNavigation();

  void onResize();

  void showPreviousState();

  void clearCurrent();

  public enum VIEWS {
    NONE,
    LISTS,
    PROGRESS,
    LEARN,
    DRILL,
    ITEMS,
    CONTEXT,
    DEFECTS,
    FIX;

    public String toString() {
      return name().substring(0,1) + name().substring(1).toLowerCase();
    }
  }
}
