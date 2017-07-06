package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Widget;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 4/10/17.
 */
public interface INavigation {
  enum VIEWS {
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

  @NotNull
  VIEWS getCurrentView();

  void showView(VIEWS view);


  void showInitialState();

  void showLearnList(int listid);

  void showDrillList(int listid);

  Widget getNavigation();

  void onResize();

  void showPreviousState();

  void clearCurrent();
}
