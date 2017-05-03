package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Breadcrumbs;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.shared.user.User;

import java.util.Collection;

/**
 * Created by go22670 on 4/10/17.
 */
public interface IBanner {
  Panel getBanner();
  Panel getBanner2();

  void setNavigation(INavigation navigation);

  void setSubtitle();

  void reflectPermissions(Collection<User.Permission> permissions);

  void setCogVisible(boolean val);

  void setBrowserInfo(String v);

  void setVisibleAdmin(boolean visibleAdmin);

  /**
   * @see mitll.langtest.client.InitialUI#gotUser
   * @param name
   */
  void setUserName(String name);

  void onResize();

  void checkProjectSelected();

  void showLearn();
}
