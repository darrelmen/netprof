package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.user.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static mitll.langtest.shared.project.ProjectMode.EITHER;
import static mitll.langtest.shared.project.ProjectMode.VOCABULARY;
import static mitll.langtest.shared.user.User.Permission.*;

/**
 * Closely related to {@link mitll.langtest.shared.user.User.Permission}
 * Created by go22670 on 4/10/17.
 */
public interface INavigation extends IViewContaner {
  enum VIEWS {
    NONE("", EITHER),

    LISTS("Lists", VOCABULARY),
    PROGRESS("Progress", VOCABULARY),
    LEARN("Learn", VOCABULARY),
    DRILL("Drill", VOCABULARY),
    QUIZ("Quiz", VOCABULARY),

    DIALOG("Dialog", ProjectMode.DIALOG),
    /**
     * @see mitll.langtest.client.banner.DialogExerciseList#gotClickOnDialog
     * @see mitll.langtest.client.banner.NewContentChooser#showView
     */
    STUDY("Study", ProjectMode.DIALOG),
    LISTEN("Listen", ProjectMode.DIALOG),
    REHEARSE("Rehearse", ProjectMode.DIALOG),
    PERFORM("Perform", ProjectMode.DIALOG),
    SCORES("Scores", ProjectMode.DIALOG),

    RECORD_ENTRIES("Record Entries", Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    RECORD_CONTEXT("Record Context", Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    QC("QC", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    FIX("Fix", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN));

    private List<User.Permission> perms;
    private ProjectMode mode;

    String display;

    VIEWS(String display, List<User.Permission> perms) {
      this.display = display;
      this.perms = perms;
      this.mode = EITHER;
    }

    VIEWS(String display, ProjectMode mode) {
      this.display = display;
      this.perms = Collections.emptyList();
      this.mode = mode;
    }

    public List<User.Permission> getPerms() {
      return perms;
    }

    public ProjectMode getMode() {
      return mode;
    }

    public String toString() {
      return display;
    }
  }

  void storeViewForMode(ProjectMode mode);

  void show(VIEWS views);

  void showView(VIEWS view);

  void showView(VIEWS view, boolean isFirstTime, boolean fromClick);

  void showInitialState();

  void showListIn(int listid, VIEWS view);

  void showDialogIn(int dialogid, VIEWS view);

  /**
   *
   * @param views
   * @return
   */
  ShowTab getShowTab(VIEWS views);

  Widget getNavigation();

  void setBannerVisible(boolean visible);

  void onResize();

  void showPreviousState();

  void clearCurrent();
}
