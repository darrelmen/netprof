package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.event.dom.client.ClickHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 4/10/17.
 */
public class LinkAndTitle {
  private final ClickHandler clickHandler;
  private final String title;
  private final boolean isAdminChoice;
  private String linkURL = null;

  public LinkAndTitle(String title, ClickHandler click, boolean isAdminChoice) {
    this.title = title;
    this.clickHandler = click;
    this.isAdminChoice = isAdminChoice;
    this.linkURL = null;
  }

  public LinkAndTitle(String title, String linkURL, boolean isAdminChoice) {
    this.title = title;
    this.linkURL = linkURL;
    this.isAdminChoice = isAdminChoice;
    this.clickHandler = null;
  }

  public NavLink add(Dropdown dropdown) {
    NavLink monitoringC = getLink();

    dropdown.add(monitoringC);
    return monitoringC;
  }

  @NotNull
  public NavLink getLink() {
    NavLink monitoringC = new NavLink(title);
    if (linkURL != null) {
      monitoringC.setHref(linkURL);
      monitoringC.setTarget("_blank");
    } else {
      monitoringC.addClickHandler(clickHandler);
    }
    return monitoringC;
  }

/*  public String getLinkURL() {
    return linkURL;
  }*/

/*
  public void setLinkURL(String linkURL) {
    this.linkURL = linkURL;
  }
*/

  public boolean isAdminChoice() {
    return isAdminChoice;
  }
}
