package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.ShowTab;
import mitll.langtest.shared.dialog.IDialog;
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
    NONE(EITHER),

    LISTS(VOCABULARY),
    PROGRESS(VOCABULARY),
    LEARN(VOCABULARY),
    DRILL(VOCABULARY),
    QUIZ(VOCABULARY),

    DIALOG(ProjectMode.DIALOG),
    /**
     * @see mitll.langtest.client.banner.DialogExerciseList#gotClickOnDialog
     * @see mitll.langtest.client.banner.NewContentChooser#showView
     */
    LISTEN(ProjectMode.DIALOG),
    REHEARSE(ProjectMode.DIALOG),

    RECORD(Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    CONTEXT(Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    DEFECTS(Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    FIX(Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN));

    private List<User.Permission> perms;
    private ProjectMode mode;

    VIEWS(List<User.Permission> perms) {
      this.perms = perms;
      this.mode = EITHER;
    }

    VIEWS(ProjectMode mode) {
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
      return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
  }

 // String getCurrentStoredView();

  void storeViewForMode(ProjectMode mode);

  void show(VIEWS views);

  void showView(VIEWS view);

  void showView(VIEWS view, boolean isFirstTime, boolean fromClick);

  void showInitialState();

  void showListIn(int listid, VIEWS view);

  void showDialogIn(int dialogid, VIEWS view);

  ShowTab getShowTab();

  Widget getNavigation();

  void setBannerVisible(boolean visible);

  void onResize();

  void showPreviousState();

  void clearCurrent();
}
