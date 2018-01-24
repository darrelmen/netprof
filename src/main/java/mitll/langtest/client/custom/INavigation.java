package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static mitll.langtest.shared.user.User.Permission.*;

/**
 * Closely related to {@link mitll.langtest.shared.user.User.Permission}
 * Created by go22670 on 4/10/17.
 */
public interface INavigation {
  enum VIEWS {
    NONE,
    LISTS,
    PROGRESS,
    LEARN,
    DRILL,
    RECORD(Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    CONTEXT(Arrays.asList(RECORD_AUDIO, QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    DEFECTS(Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN)),
    FIX(Arrays.asList(QUALITY_CONTROL, DEVELOP_CONTENT, PROJECT_ADMIN));

    private List<User.Permission> perms;

    VIEWS(List<User.Permission> perms) {
      this.perms = perms;
    }

    VIEWS() {
      this.perms = Collections.emptyList();
    }

    public List<User.Permission> getPerms() {
      return perms;
    }

    public String toString() {
      return name().substring(0, 1) + name().substring(1).toLowerCase();
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
