package mitll.langtest.client.initial;

import com.github.gwtbootstrap.client.ui.Breadcrumbs;
import com.github.gwtbootstrap.client.ui.NavLink;
import org.jetbrains.annotations.NotNull;

public interface IBreadcrumbHelper {
  void setVisible(boolean visible);

  Breadcrumbs getBreadcrumbs();

  void addCrumbs(boolean showOnlyHomeLink);

  void clearBreadcrumbs();

  @NotNull
  NavLink makeBreadcrumb(String name);

  void removeLastCrumb();
}
