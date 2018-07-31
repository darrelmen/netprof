package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.event.dom.client.ClickHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 4/10/17.
 */
class LinkAndTitle {
  private final ClickHandler clickHandler;
  private final String title;
  private String linkURL;
  private NavLink myLink;

  /**
   * @param title
   * @param click
   * @see UserMenu#getChangePassword
   */
  LinkAndTitle(String title, ClickHandler click) {
    this.title = title;
    this.clickHandler = click;
    this.linkURL = null;
  }

  /**
   * @param title
   * @param linkURL
   * @paramx isAdminChoice
   * @see UserMenu#getCogMenuChoicesForAdmin
   */
  LinkAndTitle(String title, String linkURL) {
    this.title = title;
    this.linkURL = linkURL;
    this.clickHandler = null;
  }

/*
  public NavLink add(Dropdown dropdown) {
    NavLink monitoringC = makeNewLink();
    dropdown.add(monitoringC);
    return monitoringC;
  }
*/

  @NotNull
  NavLink makeNewLink() {
    NavLink monitoringC = new NavLink(title);
    if (linkURL != null) {
      monitoringC.setHref(linkURL);
      monitoringC.setTarget("_blank");
    } else {
      monitoringC.addClickHandler(clickHandler);
    }

    return myLink = monitoringC;
  }

  NavLink getMyLink() {
    return myLink;
  }
}
