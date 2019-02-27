package mitll.langtest.client.banner;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.user.User;

import java.util.Collection;

/**
 * Created by go22670 on 4/10/17.
 */
public interface IBanner {
  Panel getBanner();

  void setNavigation(INavigation navigation);

  /**
   * @param subtitle
   * @see mitll.langtest.client.initial.InitialUI#setSplash
   */
  void setSubtitle(String subtitle);

  void setVisible(boolean visible);

  void reflectPermissions(Collection<User.Permission> permissions);

  void setVisibleChoices(boolean visible);

  void setVisibleChoicesByMode(ProjectMode mode);

  void setCogVisible(boolean val);

  void reset();

  /**
   * @param name
   * @see UILifecycle#gotUser
   */
  void setUserName(String name);

  void checkProjectSelected();

  void show(INavigation.VIEWS views);
  void show(INavigation.VIEWS views, boolean fromClick);
}
