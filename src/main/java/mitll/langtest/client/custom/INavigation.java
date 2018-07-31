package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.shared.user.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static mitll.langtest.shared.user.User.Permission.*;

/**
 * Closely related to {@link mitll.langtest.shared.user.User.Permission}
 * Created by go22670 on 4/10/17.
 */
public interface INavigation extends IViewContaner {
  enum VIEWS {
    NONE(""),
    LISTS("Lists"),
    PROGRESS("Progress"),
    LEARN("Learn"),
    DRILL("Drill"),
    QUIZ("Quiz"),
    RECORD_ENTRIES("Record Entries", Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    RECORD_CONTEXT("Record Context", Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    QC("QC", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    FIX("Fix", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN));

    private List<User.Permission> perms;
    String display;

    VIEWS(String display, List<User.Permission> perms) {
      this.display = display;
      this.perms = perms;
    }

    VIEWS(String display) {
      this.display = display;
      this.perms = Collections.emptyList();
    }

    public List<User.Permission> getPerms() {
      return perms;
    }

    public String toString() {
      return display;
    }
  }

  void showView(VIEWS view);

  void showView(VIEWS view, boolean isFirstTime, boolean fromClick);

  void showInitialState();

  void showListIn(int listid, VIEWS view);

  ShowTab getShowTab();

  Widget getNavigation();

  void setBannerVisible(boolean visible);

  void onResize();

  void showPreviousState();

  void clearCurrent();
}
