package mitll.langtest.client.banner;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.shared.user.User;

import java.util.Collection;

/**
 * Created by go22670 on 4/10/17.
 */
public interface IBanner {
  Panel getBanner();

  void setNavigation(INavigation navigation);

  void setSubtitle();

  void reflectPermissions(Collection<User.Permission> permissions);

  void setVisibleChoices(boolean visible);
  void setCogVisible(boolean val);

  void reset();

  /**
   * @see UILifecycle#gotUser
   * @param name
   */
  void setUserName(String name);

  void checkProjectSelected();

  void showLearn();
  void showDrill();
}
