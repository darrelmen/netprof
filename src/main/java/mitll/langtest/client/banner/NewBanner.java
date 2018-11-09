package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.constants.Alignment;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.github.gwtbootstrap.client.ui.constants.NavbarPosition;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.banner.NewContentChooser.VIEWS;
import static mitll.langtest.shared.project.ProjectType.DIALOG;

/**
 * Holds nav links at top of page...
 * Created by go22670 on 4/10/17.
 */
public class NewBanner extends ResponsiveNavbar implements IBanner {
  private final Logger logger = Logger.getLogger("NewBanner");

  private static final String RECORD = "Record";
  private static final String QC = "QC";

  private static final List<VIEWS> STANDARD_VIEWS =
      Arrays.asList(VIEWS.LEARN, VIEWS.PRACTICE, VIEWS.QUIZ, VIEWS.PROGRESS, VIEWS.LISTS);

  private static final List<VIEWS> DIALOG_VIEWS =
      Arrays.asList(VIEWS.DIALOG, VIEWS.STUDY, VIEWS.LISTEN, VIEWS.REHEARSE, VIEWS.PERFORM, VIEWS.SCORES);

  private static final List<VIEWS> BOTH = new ArrayList<>(STANDARD_VIEWS);

  static {
    BOTH.addAll(DIALOG_VIEWS);
  }

  private static final List<VIEWS> POLY_VIEWS = Arrays.asList(VIEWS.LEARN, VIEWS.PRACTICE, VIEWS.PROGRESS);

  private static final String appNameToUse = "netprof";
  private static final String NETPROF = appNameToUse + (PropertyHandler.IS_BETA ? "BETA" : "");

  private static final String IS_YOUR_MICROPHONE_ACTIVE = "Is your microphone active?";

  private static final String NETPROF_MANUAL = "langtest/NetProF_Manual.pdf";
  /**
   * @see #getImage
   */
  private static final String NEW_PRO_F1_PNG = "NewProF1_48x48.png";

  private static final String MAILTO_SUBJECT = "Question%20about%20netprof";

  @NotNull
  private String getMailTo() {
    return "mailto:" + controller.getProps().getHelpEmail() + "?" + "Subject=" + MAILTO_SUBJECT;
  }

  private static final String NEED_HELP_QUESTIONS_CONTACT_US = "Contact us";
  private static final String DOCUMENTATION = "User Manual";

  private final UILifecycle lifecycle;
  private ComplexWidget recnav, defectnav, dialognav;//, learnNav, drillNav;

  private Nav lnav;
  private Dropdown cog;

  private INavigation navigation;
  private final Collection<NavLink> navLinks = new ArrayList<>();
  private Dropdown userDrop;
  /**
   * @see #addChoicesForUser(ComplexWidget)
   */
  private final Map<VIEWS, NavLink> viewToLink = new HashMap<>();

  private final ExerciseController controller;
  private Label subtitle;

  private static final boolean DEBUG = false;

  /**
   * @param userManager
   * @param lifecycle
   * @see InitialUI#InitialUI
   */
  public NewBanner(UserManager userManager,
                   UILifecycle lifecycle,
                   UserMenu userMenu,
                   Breadcrumbs breadcrumbs,
                   ExerciseController controller) {
    setPosition(NavbarPosition.TOP);
    this.controller = controller;
    this.lifecycle = lifecycle;
    //  logger.info("--- addWidgets ---");
    addWidgets(userManager, userMenu, breadcrumbs);
  }

  private void addWidgets(UserManager userManager, UserMenu userMenu, Breadcrumbs breadcrumbs) {
    add(getImage());
    add(getBrand());
    add(breadcrumbs);

    NavCollapse navCollapse = new NavCollapse();
    navCollapse.addStyleName("topFiveMargin");
    navCollapse.getElement().setId("navCollapse1");

    add(navCollapse);

    // left nav
    {
      navCollapse.add(this.lnav = getLNav());
      addChoicesForUser(lnav);
    }

    // dialog nav
    {
      navCollapse.add(dialognav = getDialogNav());
    }

    // rev nav
    {
      ComplexWidget recnav = getRecNav();
      this.recnav = recnav;
      navCollapse.add(recnav);
    }
    recordMenuVisible();

    // qc nav
    {
      ComplexWidget defectnav = getDefectNav();
      this.defectnav = defectnav;
      navCollapse.add(defectnav);
    }
    defectMenuVisible();

    navCollapse.add(getRightSideChoices(userManager, userMenu));

    setCogVisible(userManager.hasUser());
  }

  @NotNull
  private Nav getDialogNav() {
    Nav recnav = new Nav();
    recnav.setVisible(false);
    recnav.getElement().setId("dialogNav");

    recnav.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    recnav.getElement().getStyle().setMarginRight(0, Style.Unit.PX);

    DIALOG_VIEWS.forEach(views -> rememberViewAndLink(recnav, views));
    return recnav;
  }


  /**
   * @return
   */
  @NotNull
  private Nav getRecNav() {
    Nav recnav = new Nav();
    recnav.setVisible(false);
    recnav.getElement().setId("recnav");
    styleNav(recnav);

    Dropdown nav = new Dropdown(RECORD);
    rememberViewAndLink(nav, VIEWS.RECORD_ENTRIES);
    rememberViewAndLink(nav, VIEWS.RECORD_SENTENCES);

    recnav.add(nav);
    return recnav;
  }

  private void styleNav(ComplexWidget recnav) {
    //  recnav.addStyleName("inlineFlex");
    zeroLeftRightMargins(recnav);
  }

  private void zeroLeftRightMargins(ComplexWidget recnav) {
    recnav.getElement().getStyle().setMarginLeft(0, Style.Unit.PX);
    recnav.getElement().getStyle().setMarginRight(0, Style.Unit.PX);
  }

/*  @NotNull
  private ComplexWidget getLearnNav() {
    Nav rnav = new Nav();
    zeroLeftRightMargins(rnav);

    Dropdown nav = new Dropdown(QC);
    rnav.add(nav);
    rememberViewAndLink(nav, VIEWS.QC);
    rememberViewAndLink(nav, VIEWS.FIX_ENTRIES);

    rememberViewAndLink(nav, VIEWS.QC_SENTENCES);
    rememberViewAndLink(nav, VIEWS.FIX_SENTENCES);

    return rnav;
  }*/

  /**
   * @return
   * @see #addWidgets
   */
  @NotNull
  private ComplexWidget getDefectNav() {
    Nav rnav = new Nav();
    zeroLeftRightMargins(rnav);

    Dropdown nav = new Dropdown(QC);
    rnav.add(nav);
    rememberViewAndLink(nav, VIEWS.QC_ENTRIES);
    rememberViewAndLink(nav, VIEWS.FIX_ENTRIES);

    rememberViewAndLink(nav, VIEWS.QC_SENTENCES);
    rememberViewAndLink(nav, VIEWS.FIX_SENTENCES);

    return rnav;
  }

  private void rememberViewAndLink(ComplexWidget recnav, VIEWS record) {
    viewToLink.put(record, getChoice(recnav, record));
  }

  @NotNull
  private Brand getBrand() {
    Brand netprof = new Brand(NETPROF);
    netprof.addStyleName("topFiveMargin");
    addHomeClick(netprof);
    netprof.addStyleName("handCursor");
    return netprof;
  }

  @NotNull
  private Nav getLNav() {
    Nav lnav = new Nav();
    lnav.getElement().setId("lnav");
    styleNav(lnav);
    return lnav;
  }

  /**
   * @param userManager
   * @param userMenu
   * @return
   * @see #addWidgets
   */
  @NotNull
  private Nav getRightSideChoices(UserManager userManager, UserMenu userMenu) {
    Nav rnav = new Nav();
    rnav.setAlignment(Alignment.RIGHT);
    addSubtitle(rnav);
    rnav.getElement().getStyle().setMarginRight(-10, Style.Unit.PX);

    addUserMenu(userManager, userMenu, rnav);

    {
      cog = new Dropdown("");
      cog.setIcon(IconType.COG);

      userMenu.getCogMenuChoicesForAdmin().forEach(lt -> cog.add(lt.makeNewLink()));

      hasProjectChoices = userMenu.getProjectSpecificChoices();
      hasProjectChoices.forEach(lt -> cog.add(lt.makeNewLink()));

      rnav.add(cog);
    }

    getInfoMenu(userMenu, rnav);
    return rnav;
  }

  /**
   * @see #setCogVisible
   */
  private List<LinkAndTitle> hasProjectChoices;

  private void addSubtitle(Nav rnav) {
    rnav.add(subtitle = new Label());

    subtitle.addStyleName("floatLeft");
    subtitle.setType(LabelType.WARNING);
    subtitle.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
    new TooltipHelper().addTooltip(subtitle, IS_YOUR_MICROPHONE_ACTIVE);
    subtitle.setVisible(!controller.isRecordingEnabled());
  }

  List<LinkAndTitle> teacherReq = new ArrayList<>();

  /**
   * @param userManager
   * @param userMenu
   * @param rnav
   * @see #getRightSideChoices
   */
  private void addUserMenu(UserManager userManager, UserMenu userMenu, Nav rnav) {
    userDrop = new Dropdown(userManager.getUserID());
    userDrop.setIcon(IconType.USER);
    rnav.add(userDrop);

    teacherReq.clear();
    userDrop.addClickHandler(event -> {
      userDrop.clear();
      userMenu.getStandardUserMenuChoices(teacherReq).forEach(linkAndTitle -> userDrop.add(linkAndTitle.makeNewLink()));
      hasProjectChoices.addAll(teacherReq);
    });
  }

  /**
   * Tell them we can't record.
   *
   * @param subtitle
   * @see UILifecycle#setSplash
   */
  @Override
  public void setSubtitle(String subtitle) {
    this.subtitle.setText(subtitle);
    this.subtitle.removeStyleName("subtitleForeground");
    this.subtitle.addStyleName("subtitleNoRecordingForeground");
  }

  private void getInfoMenu(UserMenu userMenu, Nav rnav) {
    Dropdown info = new Dropdown();
    info.setIcon(IconType.INFO);
    rnav.add(info);
    info.add(userMenu.getAbout());
    info.add(getManual());
    info.add(getContactUs());
  }

  /**
   * @param nav
   * @see #addWidgets
   */
  private void addChoicesForUser(ComplexWidget nav) {
    boolean first = true;
    boolean isPoly = controller.getPermissions().size() == 1 && controller.getPermissions().iterator().next() == User.Permission.POLYGLOT;

    boolean isDialog = controller.getProjectStartupInfo() != null && controller.getProjectStartupInfo().getProjectType() == DIALOG;
    List<VIEWS> toShow = isPoly ?
        POLY_VIEWS :
        (isDialog ?
            DIALOG_VIEWS :
            STANDARD_VIEWS);

    if (DEBUG) logger.info("addChoicesForUser " + toShow.size());

    for (VIEWS choice : toShow) {
      NavLink choice1 = getChoice(nav, choice);
      //   choice1.getElement().setId("Link_" + choice.name());
      if (first) {
        choice1.addStyleName("leftTenMargin");
        first = false;
      }
      viewToLink.put(choice, choice1);
    }
  }

  @NotNull
  private NavLink getChoice(ComplexWidget nav, VIEWS views) {
    String viewName = views.toString();

    NavLink learn = getLink(nav, viewName);
    learn.addClickHandler(event -> {
      //  logger.info("getChoice got click on " + viewName + " = " + historyToken);
      controller.logEvent(viewName, "NavLink", "N/A", "click on view");
      gotClickOnChoice(viewName, learn, true);
      // setHistoryItem(historyToken);
    });
    return learn;
  }

  public void show(VIEWS views) {
    show(views, true);
  }

  public void show(VIEWS views, boolean fromClick) {
    NavLink learn = viewToLink.get(views);
    if (learn == null) {
      logger.warning("no view for " + views + " yet...");
    } else {
      gotClickOnChoice(views.toString(), learn, fromClick);
    }
  }

  /**
   * @param instanceName
   * @param learn
   * @param fromClick
   */
  private void gotClickOnChoice(String instanceName, NavLink learn, boolean fromClick) {
    showSection(instanceName, fromClick);
    showActive(learn);  // how can this be null?
  }

  private void showSection(String instance1, boolean fromClick) {
    navigation.showView(getViews(instance1), false, fromClick);
  }

  @NotNull
  private VIEWS getViews(String instance1) {
    VIEWS choices = VIEWS.NONE;
    try {
      String name = instance1.toUpperCase();
      name = name.replaceAll(" ", "_");
      if (name.equalsIgnoreCase("Drill")) name = "Practice".toUpperCase();
      //  if (name.equalsIgnoreCase("Practice")) name = "Drill".toUpperCase();

      //    logger.info("name " + name);
      choices = VIEWS.valueOf(name);
    } catch (IllegalArgumentException e) {
      logger.warning("showSection can't parse " + instance1);
    }
    return choices;
  }

  public void selectView(VIEWS views) {
    NavLink learn = viewToLink.get(views);
    if (learn != null) showActive(learn);
  }

  /**
   * @param learn
   * @see #checkProjectSelected
   * @see #gotClickOnChoice
   */
  private void showActive(NavLink learn) {
    navLinks.forEach(link -> link.setActive(false));
    learn.setActive(true);
  }

  @NotNull
  private NavLink getLink(ComplexWidget nav, String learn1) {
    NavLink learn = new NavLink(learn1);
    navLinks.add(learn);
    nav.add(learn);
    return learn;
  }

  private Image getImage() {
    Image npImage = new Image(LangTest.LANGTEST_IMAGES + NEW_PRO_F1_PNG);
    npImage.addStyleName("floatLeftAndClear");
    npImage.addStyleName("rightFiveMargin");
    npImage.getElement().getStyle().setCursor(Style.Cursor.POINTER);
    addHomeClick(npImage);
    return npImage;
  }

  private void addHomeClick(HasClickHandlers npImage) {
    npImage.addClickHandler(event -> {
      controller.logEvent("HomeIcon", "Image", "N/A", "click on home icon");
      lifecycle.chooseProjectAgain();
    });
  }

  @Override
  public Panel getBanner() {
    return this;
  }

  @Override
  public void setNavigation(INavigation navigation) {
    this.navigation = navigation;
  }

  /**
   * @param permissions
   * @see #setVisibleChoices(boolean)
   * @see InitialUI#showUserPermissions
   */
  @Override
  public void reflectPermissions(Collection<User.Permission> permissions) {
    recordMenuVisible();
    defectMenuVisible();
    boolean visible = hasProjectChoice();
    dialognav.setVisible(visible);

    //  logger.info("reflectPerm " + visible);
    // setLearnAndDrill(visible);
  }

/*  private void setLearnAndDrill(boolean visible) {
    learnNav.setVisible(visible);
    drillNav.setVisible(visible);
  }*/

  @Override
  public void setCogVisible(boolean val) {
    userDrop.setVisible(val);

    viewToLink.values().forEach(choice -> choice.setVisible(val));

    if (val && navigation != null) {
      setVisibleChoicesByMode(navigation.getCurrentView().getMode());
    }

    cog.setVisible(isAdmin());
    //  setLearnAndDrill(val);
    boolean hasProject = controller.getProjectStartupInfo() != null;
    //  if (DEBUG) logger.info("setCogVisible " + val + " has project " + hasProject + " for " + hasProjectChoices.size());

    hasProjectChoices.forEach(linkAndTitle -> {
      linkAndTitle.getMyLink().setVisible(hasProject);
      // logger.info("setCogVisible " + hasProject + " for " + linkAndTitle);
    });
  }

  /**
   * @see InitialUI#logout
   */
  @Override
  public void reset() {
    setCogVisible(false);
    setRecNavVisible(false);
    setDefectNavVisible(false);
    dialognav.setVisible(false);

//    setLearnAndDrill(false);
  }

  @Override
  public void setUserName(String name) {
    userDrop.setTitle(name);
    userDrop.setText(name);
    recordMenuVisible();
  }

  private boolean hasProjectChoice() {
    return controller.getProjectStartupInfo() != null;
  }

  private void defectMenuVisible() {
    boolean qc = isQC();
    boolean b = hasProjectChoice();
//    logger.info("is QC " + controller.getUser() + " : " + qc + " has choice " + b);
    boolean visible = qc && b;
    setDefectNavVisible(visible);
  }

  private void setDefectNavVisible(boolean visible) {
    defectnav.setVisible(visible);
  }

  private boolean isPermittedToRecord() {
    return controller.getUserState().hasPermission(User.Permission.RECORD_AUDIO) || isAdmin();
  }

  private boolean isAdmin() {
    return controller.getUserState().isAdmin();
  }

  private boolean isQC() {
    // logger.info("isQC " + controller.getUserState().getPermissions() + " is admin " + isAdmin());
    boolean admin = isAdmin();
    // logger.info("is admin " + admin);
    boolean canDoQC = controller.getUserState().hasPermission(User.Permission.QUALITY_CONTROL);
    // logger.info("is canDoQC " + canDoQC);
    return canDoQC || admin;
  }

  /**
   * @see UILifecycle#showInitialState
   */
  @Override
  public void checkProjectSelected() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    setVisibleChoices(projectStartupInfo != null);

    VIEWS currentView = navigation.getCurrentView();
    NavLink linkToShow = viewToLink.get(currentView);

    if (linkToShow == null) {
      logger.warning("checkProjectSelected Current view is " + currentView);
      //  logger.warning("checkProjectSelected Current view link is null");
      logger.warning("checkProjectSelected huh? keys are " + viewToLink.keySet());

      if (projectStartupInfo != null && projectStartupInfo.getProjectType() == DIALOG) {
        if (DEBUG) logger.info("choosing dialog view... " + projectStartupInfo.getProjectType());
        linkToShow = viewToLink.get(VIEWS.DIALOG);
      } else {
        if (DEBUG) logger.info("choosing learn view..." + projectStartupInfo);
        linkToShow = viewToLink.get(VIEWS.LEARN);
      }
    }
    showActive(linkToShow);
    recordMenuVisible();
  }

  /**
   * @param show
   * @see #checkProjectSelected
   */
  public void setVisibleChoices(boolean show) {
    lnav.setVisible(show);
    reflectPermissions(controller.getPermissions());
    //   setLearnAndDrill(show);
  }

  /**
   * @param mode
   * @see NewBanner#setCogVisible
   */
  @Override
  public void setVisibleChoicesByMode(ProjectMode mode) {
    if (DEBUG) logger.info("setVisibleChoicesByMode set visible choices " + mode);
    hideOrShowByMode((mode == ProjectMode.DIALOG) ? DIALOG_VIEWS : STANDARD_VIEWS);
  }

  private void hideOrShowByMode(List<VIEWS> standardViews) {
    BOTH.forEach(views -> {
      NavLink widgets = viewToLink.get(views);
      if (widgets == null) {
        logger.warning("no widget for " + views);
      } else {
        boolean contains = standardViews.contains(views);
//        logger.info("\tlink " + views + " " +widgets.getElement().getId()+
//            " " + widgets.isVisible() + " '" + widgets.getTitle() + "'" +
//            " '" + widgets.getName() + "' display " + widgets.getElement().getStyle().getDisplay() +
//            " now " + contains);
        widgets.setVisible(contains);
      }
    });
  }

  private void recordMenuVisible() {
    if (recnav != null) {
      boolean visible = isPermittedToRecord() && hasProjectChoice();

//      boolean learnVisible = viewToLink.get(VIEWS.LEARN).isVisible();
      //    logger.info("recordMenuVisible learn vis " + learnVisible);
      //    visible &= learnVisible;
      setRecNavVisible(visible);
    }
  }

  private void setRecNavVisible(boolean visible) {
    recnav.setVisible(visible);
  }

  private NavLink getContactUs() {
    return getAnchor(NEED_HELP_QUESTIONS_CONTACT_US, getMailTo());
  }

  /**
   * Make sure this points to the manual.
   *
   * @return
   */
  private NavLink getManual() {
    NavLink anchor = getAnchor(DOCUMENTATION, NETPROF_MANUAL);
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
