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
    /**
     *
     */
    PROGRESS("Progress", VOCABULARY),
    LEARN("Learn", VOCABULARY),
    LEARN_SENTENCES("Learn Sentences", VOCABULARY, true),
    PRACTICE("Practice", VOCABULARY),
    PRACTICE_SENTENCES("Practice Sentences", VOCABULARY, true),
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
    /**
     * @see mitll.langtest.client.banner.NewContentChooser#showScores
     */
    SCORES("Scores", ProjectMode.DIALOG),

    RECORD_ENTRIES("Record Entries", Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    RECORD_CONTEXT("Record Context", Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    QC("QC", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN), true, false, false),
    FIX("Fix", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN), false, true, false),
    QC_SENTENCES("QC Sentences", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN), true, false, true),
    FIX_SENTENCES("Fix Sentences", Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN), false, true, true);

    private final List<User.Permission> perms;
    private final ProjectMode mode;
    private boolean isQC;
    private boolean isFix;
    private boolean isContext;

    final String display;

    VIEWS(String display, List<User.Permission> perms) {
      this.display = display;
      this.perms = perms;
      this.mode = EITHER;
    }

    VIEWS(String display, List<User.Permission> perms, boolean isQC, boolean isFix, boolean isContext) {
      this.display = display;
      this.perms = perms;
      this.mode = EITHER;
      this.isQC = isQC;
      this.isFix = isFix;
      this.isContext = isContext;
    }

    VIEWS(String display, ProjectMode mode) {
      this.display = display;
      this.perms = Collections.emptyList();
      this.mode = mode;
    }

    VIEWS(String display, ProjectMode mode, boolean isContext) {
      this.display = display;
      this.perms = Collections.emptyList();
      this.mode = mode;
      this.isContext = isContext;
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

    public boolean isQC() {
      return isQC;
    }


    public boolean isFix() {
      return isFix;
    }

    public boolean isContext() {
      return isContext;
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
