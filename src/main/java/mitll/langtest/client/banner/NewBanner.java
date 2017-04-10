package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.Alignment;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.UILifecycle;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.user.User;
import org.apache.commons.collections.ArrayStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewBanner extends ResponsiveNavbar implements IBanner {
  private final Logger logger = Logger.getLogger("NewBanner");
  private static final String NEW_PRO_F1_PNG = "NewProF1_48x48.png";

  private UILifecycle lifecycle;
  private UserManager userManager;
  private INavigation navigation;

  /**
   * @param userManager
   * @param lifecycle
   * @see InitialUI#InitialUI(LangTest, UserManager)
   */
  public NewBanner(UserManager userManager, UILifecycle lifecycle, UserMenu userMenu) {
    Brand netprof = new Brand("netprof");
    addHomeClick(netprof);
    netprof.addStyleName("handCursor");

    add(getImage());
    add(netprof);
    NavCollapse navCollapse = new NavCollapse();
    add(navCollapse);

    // TODO: override marginbottom
    Nav nav = new Nav();

    navCollapse.add(nav);

    //if (hasUser) {
    addChoicesForUser(nav);
    //}

    Nav rnav = new Nav();
    rnav.setAlignment(Alignment.RIGHT);
    navCollapse.add(rnav);


    userDrop = new Dropdown(userManager.getUserID());
    userDrop.setIcon(IconType.USER);
//    userDrop.addClickHandler(new ClickHandler() {
//      @Override
//      public void onClick(ClickEvent event) {
//
//      }
//    });

    rnav.add(userDrop);
    // userDrop.setVisible(hasUser);

    List<LinkAndTitle> userMenuChoices = userMenu.getUserMenuChoices();
    logger.info("adding " + userMenuChoices.size());

    for (LinkAndTitle linkAndTitle : userMenuChoices) {
      userDrop.add(linkAndTitle.getLink());
    }

    // NavLink info = new NavLink();
    Dropdown info = new Dropdown();
    info.setIcon(IconType.INFO);
    rnav.add(info);
    info.add(userMenu.getAbout());

    setCogVisible(userManager.hasUser());
    this.userManager = userManager;
    this.lifecycle = lifecycle;
  }

  private void addChoicesForUser(Nav nav) {
    choices.add(addLearn(nav));
    choices.add(addDrill(nav));
  }

  Dropdown userDrop;

  List<Widget> choices = new ArrayList();


  private NavLink addLearn(Nav nav) {
    NavLink learn = new NavLink("Learn");
    nav.add(learn);
    learn.addClickHandler(event -> navigation.showLearn());
    return learn;
  }

  private Widget addDrill(Nav nav) {
    NavLink learn = new NavLink("Drill");
    nav.add(learn);
    learn.addClickHandler(event -> navigation.showDrill());

    return learn;
  }

  private Image getImage() {
    Image flashcardImage = new Image(LangTest.LANGTEST_IMAGES + NEW_PRO_F1_PNG);
    flashcardImage.addStyleName("floatLeftAndClear");
    flashcardImage.addStyleName("rightFiveMargin");
    flashcardImage.getElement().getStyle().setCursor(Style.Cursor.POINTER);
    addHomeClick(flashcardImage);
    return flashcardImage;
  }

  private void addHomeClick(HasClickHandlers flashcardImage) {
    flashcardImage.addClickHandler(event -> lifecycle.chooseProjectAgain());
  }

  /**
   * @return
   * @seez #gotUser
   * @seez #makeHeaderRow()
   */
  private String getGreeting() {
    return userManager.getUserID() == null ? "" : ("" + userManager.getUserID());
  }

  @Override
  public Panel getBanner() {
    return this;
  }

  @Override
  public void setNavigation(INavigation navigation) {
    this.navigation = navigation;
  }

  @Override
  public void setSubtitle() {

  }

  @Override
  public void reflectPermissions(Collection<User.Permission> permissions) {

  }

  @Override
  public void setCogVisible(boolean val) {
    userDrop.setVisible(val);
    for (Widget choice : choices) choice.setVisible(val);
  }

  @Override
  public void setBrowserInfo(String v) {

  }

  @Override
  public void setVisibleAdmin(boolean visibleAdmin) {

  }

  @Override
  public void setUserName(String name) {
    userDrop.setTitle(name);
  }

  @Override
  public void onResize() {

  }
}
