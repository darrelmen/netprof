package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Alignment;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.github.gwtbootstrap.client.ui.constants.NavbarPosition;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.InitialUI;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.UILifecycle;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.banner.NewContentChooser.*;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewBanner extends ResponsiveNavbar implements IBanner, ValueChangeHandler<String> {
  private final Logger logger = Logger.getLogger("NewBanner");


  private static final String LEARN = "Learn";
//  private static final String DRILL = "Drill";
//  private static final String PROGRESS = "Progress";
//  private static final String LISTS = "Lists";
  private static final String NEW_PRO_F1_PNG = "NewProF1_48x48.png";
  private static final String NETPROF_HELP_LL_MIT_EDU = "netprof-help@dliflc.edu";
  private static final String NEED_HELP_QUESTIONS_CONTACT_US = "Contact us";
  private static final String DOCUMENTATION = "User Manual";
  private static final String RECORDING_DISABLED = "RECORDING DISABLED";

  private final UILifecycle lifecycle;
  private ComplexWidget recnav, defectnav;
  private INavigation navigation;
  private HandlerRegistration handlerRegistration;
  private final Map<String, NavLink> nameToLink = new HashMap<>();
  private Dropdown userDrop;

  private final List<Widget> choices = new ArrayList<>();

  private final ExerciseController controller;

  /**
   * @param userManager
   * @param lifecycle
   * @see InitialUI#InitialUI(LangTest, UserManager)
   */
  public NewBanner(UserManager userManager, UILifecycle lifecycle, UserMenu userMenu, Breadcrumbs breadcrumbs,
                   ExerciseController controller) {
    setPosition(NavbarPosition.TOP);

    setInverse(true);

    this.controller = controller;

    add(getImage());
    add(getBrand());
    add(breadcrumbs);

    styleBreadcrumbs(breadcrumbs);

    NavCollapse navCollapse = new NavCollapse();
    navCollapse.addStyleName("topFiveMargin");
    navCollapse.getElement().setId("navCollapse1");

    //DivWidget upperLower = new DivWidget();

//    upperLower.add(navCollapse);
    add(navCollapse);
  //  add(upperLower);

//    NavCollapse navCollapse2 = new NavCollapse();
 //   navCollapse2.addStyleName("topFiveMargin");
   // navCollapse2.getElement().setId("navCollapse2");

//    add(navCollapse2);

    navCollapse.add(this.lnav = getLNav());
    addChoicesForUser(lnav);
/*
    lnav.add(recnav =new Dropdown("Record"));
    */

    Nav recnav = getRecNav();
    this.recnav = recnav;
    navCollapse.add(recnav);
//    upperLower.add(navCollapse2);

    recordMenuVisible();


    Nav defectnav = getDefectNav();
    this.defectnav = defectnav;
    navCollapse.add(defectnav);
    defectMenuVisible();

    navCollapse.add(getRightSideChoices(userManager, userMenu));

    setCogVisible(userManager.hasUser());

    this.lifecycle = lifecycle;

    addHistoryListener();
  }

  @NotNull
  private Nav getRecNav() {
    Nav recnav = new Nav();
    recnav.getElement().setId("recnav");
    recnav.addStyleName("inlineFlex");
    recnav.getElement().getStyle().setMarginLeft(0, Style.Unit.PX  );
    recnav.getElement().getStyle().setMarginRight(0, Style.Unit.PX  );

    getChoice(recnav, VIEWS.ITEMS.toString());
    getChoice(recnav, VIEWS.CONTEXT.toString());
    return recnav;
  }

  @NotNull
  private Nav getDefectNav() {
    Nav recnav = new Nav();
    recnav.getElement().setId("defectnav");
    recnav.addStyleName("inlineFlex");
    recnav.getElement().getStyle().setMarginLeft(0, Style.Unit.PX  );
    recnav.getElement().getStyle().setMarginRight(0, Style.Unit.PX  );

    getChoice(recnav, VIEWS.DEFECTS.toString());
    getChoice(recnav, VIEWS.FIX.toString());
    return recnav;
  }

  @NotNull
  private Brand getBrand() {
    Brand netprof = new Brand("netprof");
    netprof.addStyleName("topFiveMargin");
    addHomeClick(netprof);
    netprof.addStyleName("handCursor");
    return netprof;
  }

  private void styleBreadcrumbs(Breadcrumbs breadcrumbs) {
    breadcrumbs.addStyleName("floatLeft");
    Style style = breadcrumbs.getElement().getStyle();
    style.setMarginTop(7, Style.Unit.PX);
    breadcrumbs.addStyleName("rightTwentyMargin");
  }

  @NotNull
  private Nav getLNav() {
    Nav lnav = new Nav();
    lnav.getElement().setId("lnav");
    lnav.addStyleName("inlineFlex");
    lnav.getElement().getStyle().setMarginLeft(0, Style.Unit.PX  );
    lnav.getElement().getStyle().setMarginRight(0, Style.Unit.PX  );

    return lnav;
  }

  private  Nav lnav;
  private Dropdown cog;

  @NotNull
  private Nav getRightSideChoices(UserManager userManager, UserMenu userMenu) {
    Nav rnav = new Nav();
    rnav.setAlignment(Alignment.RIGHT);
    rnav.add(subtitle = new Label());

    subtitle.addStyleName("floatLeft");
    subtitle.setType(LabelType.WARNING);
    subtitle.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
    new TooltipHelper().addTooltip(subtitle, "Is your microphone active?");

    //recordMenu = getRecordMenu(rnav);
    addUserMenu(userManager, userMenu, rnav);

    subtitle.setVisible(!controller.isRecordingEnabled());

    cog = new Dropdown("");
    cog.setIcon(IconType.COG);
    userMenu.getCogMenuChoices2().forEach(lt -> cog.add(lt.getLink()));
    rnav.add(cog);

    getInfoMenu(userMenu, rnav);
    return rnav;
  }

  private void addUserMenu(UserManager userManager, UserMenu userMenu, Nav rnav) {
    userDrop = new Dropdown(userManager.getUserID());
    userDrop.setIcon(IconType.USER);
    rnav.add(userDrop);
    userMenu.getUserMenuChoices().forEach(lt -> userDrop.add(lt.getLink()));
  }

  private Label subtitle;

  @Override
  public void setSubtitle() {
    this.subtitle.setText(RECORDING_DISABLED);
    subtitle.removeStyleName("subtitleForeground");
    subtitle.addStyleName("subtitleNoRecordingForeground");
  }

  private void addHistoryListener() {
    if (handlerRegistration == null) {
      handlerRegistration = History.addValueChangeHandler(this);
    }
  }

  public void onValueChange(ValueChangeEvent<String> event) {
    String token = event.getValue();
    SelectionState selectionState = new SelectionState(token, false);
    String instance1 = selectionState.getInstance();

    logger.info("onValueChange got '" + token + "' instance '" + instance1 + "'");

    showSection(instance1);
  }

  private void showSection(String instance1) {
    VIEWS choices = VIEWS.NONE;
    try {
      choices = VIEWS.valueOf(instance1.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.info("can't parse " + instance1);

    }
    navigation.showView(choices);
  }

  private void getInfoMenu(UserMenu userMenu, Nav rnav) {
    Dropdown info = new Dropdown();
    info.setIcon(IconType.INFO);
    rnav.add(info);
    info.add(userMenu.getAbout());
    info.add(getManual());
    info.add(getContactUs());
  }

  NavLink firstChoice = null;
  /**
   * @param nav
   */
  private void addChoicesForUser(ComplexWidget nav) {
    boolean first = true;
    boolean hasProject = hasProjectChoice();

    logger.info("addChoicesForUser has project " + hasProject);
    for (String choice : Arrays.asList(VIEWS.LEARN.toString(), VIEWS.DRILL.toString(), VIEWS.PROGRESS.toString(), VIEWS.LISTS.toString())) {
      NavLink choice1 = getChoice(nav, choice);
      if (first) {
        choice1.addStyleName("leftTenMargin");
        firstChoice = choice1;
      }
      first = false;
      choices.add(choice1);
    }
  }

  @NotNull
  private NavLink getChoice(ComplexWidget nav, String instanceName) {
    String historyToken = SelectionState.SECTION_SEPARATOR + SelectionState.INSTANCE + "=" + instanceName;

    NavLink learn = getLink(nav, instanceName);
    learn.addClickHandler(event -> {
      logger.info("getChoice got click on " + instanceName + " = " + historyToken);
      gotClickOnChoice(instanceName, learn);
      // setHistoryItem(historyToken);
    });
    return learn;
  }

  private void gotClickOnChoice(String instanceName, NavLink learn) {
    showSection(instanceName);
    showActive(learn);
  }

  public void showLearn() {
    gotClickOnChoice(LEARN, firstChoice);
  }

/*  private void setHistoryItem(String historyToken) {
    if (true) logger.info("NewBanner.setHistoryItem '" + historyToken + "' -------------- ");
    History.newItem(historyToken);
  }*/

  @NotNull
  private NavLink getLink(ComplexWidget nav, String learn1) {
    NavLink learn = new NavLink(learn1);
    nameToLink.put(learn1, learn);
    nav.add(learn);
    return learn;
  }

  private void showActive(NavLink learn) {
    for (NavLink link : nameToLink.values()) link.setActive(false);
    learn.setActive(true);
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
/*
  private String getGreeting() {
    return userManager.getUserID() == null ? "" : ("" + userManager.getUserID());
  }
*/
  @Override
  public Panel getBanner() {
    return this;
  }
  @Override
  public Panel getBanner2() {

    return null;
/*    ResponsiveNavbar responsiveNavbar = new ResponsiveNavbar();
    NavCollapse navCollapse = new NavCollapse();
    navCollapse.addStyleName("topFiveMargin");
    navCollapse.getElement().setId("navCollapseBelow");
    responsiveNavbar.add(navCollapse);

    Nav recnav = getRecNav();
    this.recnav = recnav;
    navCollapse.add(recnav);

    recordMenuVisible();

    Nav defectnav = getDefectNav();
    this.defectnav = defectnav;
    navCollapse.add(defectnav);
    defectMenuVisible();

    return responsiveNavbar;*/
  }

  @Override
  public void setNavigation(INavigation navigation) {
    this.navigation = navigation;
  }


  @Override
  public void reflectPermissions(Collection<User.Permission> permissions) {
    recordMenuVisible();
    defectMenuVisible();
  }

  @Override
  public void setCogVisible(boolean val) {
    userDrop.setVisible(val);
    for (Widget choice : choices) choice.setVisible(val);

    cog.setVisible(isAdmin());
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
    userDrop.setText(name);

    recordMenuVisible();
  }

  private void recordMenuVisible() {
    if (recnav != null) {
      recnav.setVisible(isPermittedToRecord() && hasProjectChoice());
    }
  }

  private boolean hasProjectChoice() {
    return controller.getProjectStartupInfo() != null;
  }

  private void defectMenuVisible() {
    defectnav.setVisible(isQC() && hasProjectChoice());
  }

  private boolean isPermittedToRecord() {
    return controller.getUserState().hasPermission(User.Permission.RECORD_AUDIO) || isAdmin();
  }

  private boolean isAdmin() {
    return controller.getUserState().isAdmin();
  }

  private boolean isQC() {
  //  logger.info("isQC " + controller.getUserState().getPermissions() + " is admin " + isAdmin());
    return controller.getUserState().hasPermission(User.Permission.QUALITY_CONTROL) || isAdmin();
  }

  @Override
  public void onResize() {
  }

  @Override
  public void checkProjectSelected() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    // Collection<NavLink> values = nameToLink.values();
//    logger.info("project info " + projectStartupInfo);
//    logger.info("setting " + values.size() + " links");

    boolean show = projectStartupInfo != null;
//    for (NavLink link : values) {
//      link.setDisabled(!show);
//    }

    lnav.setVisible(show);

    showActive(firstChoice);

    recordMenuVisible();
  }

  private NavLink getContactUs() {
    return getAnchor(NEED_HELP_QUESTIONS_CONTACT_US, getMailTo());
  }

  @NotNull
  private String getMailTo() {
    return "mailto:" +
        NETPROF_HELP_LL_MIT_EDU + "?" +
        //   "cc=" + LTEA_DLIFLC_EDU + "&" +
        "Subject=Question%20about%20NetProF";
  }

  /**
   * Make sure this points to the manual.
   *
   * @return
   */
  private NavLink getManual() {
    NavLink anchor = getAnchor(DOCUMENTATION, "langtest/NetProF_Manual.pdf");
    anchor.getElement().getStyle().setColor("#5bb75b");
    return anchor;
  }

  private NavLink getAnchor(String title, String href) {
    NavLink emailAnchor = new NavLink(title, href);

    emailAnchor.getElement().setId("emailAnchor");
    emailAnchor.addStyleName("bold");
    emailAnchor.addStyleName("rightTwentyMargin");
    emailAnchor.getElement().setAttribute("download", "");
    emailAnchor.getElement().getStyle().setColor("#90B3CF");
    return emailAnchor;
  }
}
