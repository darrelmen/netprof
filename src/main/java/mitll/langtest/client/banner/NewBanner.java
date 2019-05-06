/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.*;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.client.list.ViewParser;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.user.ActiveUser;
import mitll.langtest.shared.user.Permission;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.banner.NewContentChooser.VIEWS;
import static mitll.langtest.client.custom.INavigation.VIEWS.*;
import static mitll.langtest.shared.project.ProjectType.DIALOG;

/**
 * Holds nav links at top of page...
 * Created by go22670 on 4/10/17.
 */
public class NewBanner extends ResponsiveNavbar implements IBanner {
  private final Logger logger = Logger.getLogger("NewBanner");

  private static final String DIALOG_PRACTICE = "Practice";

  private static final String RECORD = "Record";
  private static final String QC = "QC";

  private static final List<VIEWS> STANDARD_VIEWS = Arrays.asList(LEARN, VIEWS.PRACTICE, QUIZ, PROGRESS, LISTS);

  private static final List<VIEWS> DIALOG_VIEWS_IN_DROPDOWN =
      Arrays.asList(
          STUDY,
          LISTEN,
          REHEARSE,
          CORE_REHEARSE,
          PERFORM_PRESS_AND_HOLD,
          PERFORM);

  private static final List<VIEWS> DIALOG_VIEWS =
      Arrays.asList(
          VIEWS.DIALOG,
          STUDY,
          LISTEN,
          REHEARSE,
          CORE_REHEARSE,
          PERFORM_PRESS_AND_HOLD,
          PERFORM,
          SCORES);

  /**
   * @see #hideOrShowByMode
   */
  private static final List<VIEWS> BOTH = new ArrayList<>(STANDARD_VIEWS);

  static {
    BOTH.addAll(DIALOG_VIEWS);
  }

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
  /**
   *
   */
  private ComplexWidget recnav, defectnav, dialognav;
  private Dropdown dialogPracticeNav;

  /**
   *
   */
  private Nav lnav;
  /**
   * @see #setCogVisible(boolean)
   */
  private Dropdown cog;

  private INavigation navigation;
  private final Collection<NavLink> navLinks = new ArrayList<>();
  private Dropdown userDrop;
  /**
   * @see #addChoicesForUser(ComplexWidget)
   */
  private final Map<VIEWS, NavLink> viewToLink = new HashMap<>();

  private final ExerciseController<?> controller;
  private Label subtitle;

  private ViewParser viewParser = new ViewParser();
  private final List<LinkAndTitle> teacherReq = new ArrayList<>();

  /**
   * @see #setCogVisible
   */
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

//    NavCollapse navCollapse = new NavCollapse();
    DivWidget navCollapse = new DivWidget();
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
      ComplexWidget interpreterNav = getDialogNav();
      this.dialognav = interpreterNav;
      navCollapse.add(interpreterNav);
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

    navCollapse.add(getRightSideChoices(userMenu));

    setCogVisible(userManager.hasUser());
  }

  @NotNull
  private Nav getDialogNav() {
    Nav recnav = new Nav();
    recnav.setVisible(false);
    recnav.getElement().setId("dialogNav");

    recnav.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    recnav.getElement().getStyle().setMarginRight(0, Style.Unit.PX);

    rememberViewAndLink(recnav, VIEWS.DIALOG);

    dialogPracticeNav = new Dropdown(DIALOG_PRACTICE);
    recnav.add(dialogPracticeNav);

    DIALOG_VIEWS_IN_DROPDOWN.forEach(views -> rememberViewAndLink(dialogPracticeNav, views));

    rememberViewAndLink(recnav, SCORES);

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
    rememberViewAndLink(nav, RECORD_ENTRIES);
    rememberViewAndLink(nav, RECORD_SENTENCES);
    rememberViewAndLink(nav, OOV_EDITOR);

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
    rememberViewAndLink(nav, QC_ENTRIES);
    rememberViewAndLink(nav, FIX_ENTRIES);

    rememberViewAndLink(nav, QC_SENTENCES);
    rememberViewAndLink(nav, FIX_SENTENCES);

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

  private UserMenu userMenu;
  private List<LinkAndTitle> allChoices = new ArrayList<>();

  /**
   * @param userMenu
   * @return
   * @see #addWidgets
   */
  @NotNull
  private Nav getRightSideChoices(UserMenu userMenu) {
    this.userMenu = userMenu;

    Nav rnav = new Nav();
    rnav.setAlignment(Alignment.RIGHT);
    addSubtitle(rnav);
    rnav.getElement().getStyle().setMarginRight(-10, Style.Unit.PX);

    addUserMenu(userMenu, rnav);

    {
      cog = new Dropdown("");
      cog.setIcon(IconType.COG);

      allChoices.clear();

      List<LinkAndTitle> cogMenuChoicesForAdmin = userMenu.getCogMenuChoicesForAdmin();
      cogMenuChoicesForAdmin.forEach(lt -> cog.add(lt.makeNewLink()));
      allChoices.addAll(cogMenuChoicesForAdmin);

      List<LinkAndTitle> hasProjectChoices = userMenu.getProjectSpecificChoices();
      hasProjectChoices.forEach(lt -> cog.add(lt.makeNewLink()));
      allChoices.addAll(hasProjectChoices);

      rnav.add(cog);
    }

    getInfoMenu(userMenu, rnav);

    return rnav;
  }

  /**
   * @see #setUserName(String)
   */
  public void setCogTitle() {
//    logger.info("setCogTitle  project " + controller.getProjectID());
    if (controller.getProjectID() > -1) {
      controller.getUserService().getPendingUsers(controller.getProjectID(), new AsyncCallback<List<ActiveUser>>() {
        @Override
        public void onFailure(Throwable caught) {

        }

        @Override
        public void onSuccess(List<ActiveUser> result) {
          cog.setText(result.isEmpty() ? "" : "(" + result.size() + ")");
          userMenu.setPendingTitle(result.size());
        }
      });
    } else {
      logger.info("setCogTitle : no project");
      setChoicesVisibility(isAdmin(), isTeacher());
      setVisibleChoices(false);
    }
  }


  private void addSubtitle(Nav rnav) {
    rnav.add(subtitle = new Label());

    subtitle.addStyleName("floatLeft");
    subtitle.setType(LabelType.WARNING);
    subtitle.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
    new TooltipHelper().addTooltip(subtitle, IS_YOUR_MICROPHONE_ACTIVE);
    subtitle.setVisible(!controller.isRecordingEnabled());
  }


  /**
   * @param userMenu
   * @param rnav
   * @see #getRightSideChoices
   */
  private void addUserMenu(UserMenu userMenu, Nav rnav) {
    userDrop = new Dropdown("");
    userDrop.setIcon(IconType.USER);
    rnav.add(userDrop);

    teacherReq.clear();
    userDrop.addClickHandler(event -> {
      userDrop.clear();
      userMenu.getStandardUserMenuChoices(teacherReq).forEach(linkAndTitle -> userDrop.add(linkAndTitle.makeNewLink()));
      // hasProjectChoices.addAll(teacherReq);
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
    //boolean isPoly = controller.getPermissions().size() == 1 && controller.getPermissions().iterator().next() == Permission.POLYGLOT;
    boolean isDialog = controller.getProjectStartupInfo() != null && controller.getProjectStartupInfo().getProjectType() == DIALOG;
    List<VIEWS> toShow = (isDialog ? DIALOG_VIEWS : STANDARD_VIEWS);

    if (DEBUG) {
      logger.info("addChoicesForUser " + toShow.size() + " : startup : " + controller.getProjectStartupInfo() + " : " + toShow);
    }

    boolean first = true;
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
      //   logger.info("getChoice got click on " + viewName);
      controller.logEvent(viewName, "NavLink", "N/A", "click on view");
      gotClickOnChoice(viewName, learn, true);
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
    //  logger.info("gotClickOn " + instanceName + " " + learn + " from click " + fromClick);
//    showSectionAfterClick(instanceName, fromClick);
    navigation.showView(getViews(instanceName), false, fromClick);
    showActive(learn);  // how can this be null?
  }

//  private void showSectionAfterClick(String instance1, boolean fromClick) {
//    navigation.showView(getViews(instance1), false, fromClick);
//  }

  @NotNull
  private VIEWS getViews(String instance1) {
    VIEWS view = viewParser.getView(instance1);
    //  logger.info("getViews " + instance1 + " = " + view);
    return view;
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
  public void reflectPermissions(Collection<Permission> permissions) {
    recordMenuVisible();
    defectMenuVisible();

    boolean isDialog = controller.getProjectStartupInfo() != null && controller.getProjectStartupInfo().getProjectType() == DIALOG;
    boolean visible = hasProjectChoice() && isDialog;
    setDialogNavVisible(visible);
    //   logger.info("reflectPermissions : " + permissions);
  }

  private void setDialogNavVisible(boolean visible) {
    dialognav.setVisible(visible && controller.getMode() == ProjectMode.DIALOG);
  }

  /**
   * @see #addWidgets(UserManager, UserMenu, Breadcrumbs)
   * @see #reset
   * @param val
   */
  @Override
  public void setCogVisible(boolean val) {
    userDrop.setVisible(val);

    viewToLink.values().forEach(choice -> choice.setVisible(val));

    if (val && navigation != null) {
      // VIEWS currentView = navigation.getCurrentView();
      //logger.info("setCogVisible current view " + currentView);
      setVisibleChoicesByMode(navigation.getCurrentView().getMode());
    }

    boolean admin = isAdmin();
    boolean teacher = isTeacher();
    cog.setVisible(admin || teacher);

    setChoicesVisibility(admin, teacher);
  }

  private void setChoicesVisibility(boolean admin, boolean teacher) {
    boolean hasProject = controller.getProjectStartupInfo() != null;
    allChoices.forEach(linkAndTitle -> linkAndTitle.setVisible(admin, teacher, hasProject));
  }

  private boolean isTeacher() {
    return controller.getUserState().hasPermission(Permission.TEACHER_PERM);
  }

  /**
   * @see InitialUI#logout
   */
  @Override
  public void reset() {
    setCogVisible(false);

    setRecNavVisible(false);
    setDefectNavVisible(false);
    setDialogNavVisible(false);
  }

  /**
   * @param name
   * @see InitialUI#gotUser
   */
  @Override
  public void setUserName(String name) {
    userDrop.setText(name);
    User current = controller.getUserManager().getCurrent();

    if (current != null) {
      String tip = current.getFullName() + " (" + current.getUserID() + ") " + getUserSuffix(current);
      userDrop.setTitle(tip);
//      logger.info("setUserName suffix " + suffix);
      new TooltipHelper().createAddTooltip(userDrop, tip, Placement.LEFT);
    }

    recordMenuVisible();

    setCogTitle();
  }

  @NotNull
  private String getUserSuffix(User current) {
    List<String> perms = new ArrayList<>();
//      logger.info("setUserName " + name + " kind " + current.getUserKind());
//      logger.info("setUserName perms " + current.getPermissions());
    String suffix = "";//current.getUserKind() != Kind.STUDENT ? "( " + current.getUserKind().getName() + ")" : "";

    current.getPermissions().forEach(permission -> perms.add(permission.getKind().getName()));
    if (!perms.isEmpty()) {
      suffix += " " + perms;
    }
    return suffix;
  }

  private boolean hasProjectChoice() {
    return controller.getProjectStartupInfo() != null;
  }

  private void defectMenuVisible() {
    setDefectNavVisible(isQC() && hasProjectChoice());
  }

  private void setDefectNavVisible(boolean visible) {
    defectnav.setVisible(visible);
  }

  private boolean isPermittedToRecord() {
    return controller.getUserState().hasPermission(Permission.RECORD_AUDIO) || isAdmin();
  }

  private boolean isAdmin() {
    return controller.getUserState().isAdmin();
  }

  private boolean isQC() {
    // logger.info("isQC " + controller.getUserState().getPermissions() + " is admin " + isAdmin());
    boolean admin = isAdmin();
    // logger.info("is admin " + admin);
    boolean canDoQC = controller.getUserState().hasPermission(Permission.QUALITY_CONTROL);
    // logger.info("is canDoQC " + canDoQC);
    return canDoQC || admin;
  }

  /**
   * @see InitialUI#showInitialState
   */
  @Override
  public void checkProjectSelected() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();

 //   logger.info("checkProjectSelected " + projectStartupInfo);

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
        linkToShow = viewToLink.get(LEARN);
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
    if (DEBUG) logger.info("setVisibleChoices " + show);
    reflectPermissions(controller.getPermissions());
  }

  /**
   * @param mode
   * @see NewBanner#setCogVisible
   */
  @Override
  public void setVisibleChoicesByMode(ProjectMode mode) {
    if (DEBUG) logger.info("setVisibleChoicesByMode set visible choices " + mode);
    boolean isDialogMode = mode == ProjectMode.DIALOG;
    hideOrShowByMode(isDialogMode ? DIALOG_VIEWS : STANDARD_VIEWS);
    if (DEBUG)
      logger.info("setVisibleChoicesByMode dialognav " + dialognav.getElement().getId() + " is " + isDialogMode);

    setDialogNavVisible(isDialogMode);
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
      setRecNavVisible(isPermittedToRecord() && hasProjectChoice());
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
