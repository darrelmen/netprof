package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.constants.Alignment;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.github.gwtbootstrap.client.ui.constants.NavbarPosition;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import mitll.langtest.client.download.DownloadEvent;
import mitll.langtest.client.download.ShowEvent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.ShowChoices;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.banner.NewContentChooser.VIEWS;

/**
 * Created by go22670 on 4/10/17.
 */
public class NewBanner extends ResponsiveNavbar implements IBanner, ValueChangeHandler<String> {
  public static final String SHOW_PHONES = "showPhones";
  public static final String NETPROF_MANUAL = "langtest/NetProF_Manual.pdf";
  public static final String MAILTO_SUBJECT = "Question%20about%20netprof";
  public static final IconType CHECK = IconType.CHECK;
  private final Logger logger = Logger.getLogger("NewBanner");

  public static final String SHOW = "showStorage";
  //  private static final String LEARN = "Learn";
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
 // private Dropdown viewMenu;

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
    this.lifecycle = lifecycle;

//    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
//      public void execute() {
//        addWidgets(userManager, userMenu, breadcrumbs);
//      }
//    });

    addWidgets(userManager, userMenu, breadcrumbs);
  }

  private void addWidgets(UserManager userManager, UserMenu userMenu, Breadcrumbs breadcrumbs) {
    add(getImage());
    add(getBrand());
    add(breadcrumbs);

    styleBreadcrumbs(breadcrumbs);

    NavCollapse navCollapse = new NavCollapse();
    navCollapse.addStyleName("topFiveMargin");
    navCollapse.getElement().setId("navCollapse1");

    add(navCollapse);

    navCollapse.add(this.lnav = getLNav());
    addChoicesForUser(lnav);
    Nav recnav = getRecNav();
    this.recnav = recnav;
    navCollapse.add(recnav);
    recordMenuVisible();

    Nav defectnav = getDefectNav();
    this.defectnav = defectnav;
    navCollapse.add(defectnav);
    defectMenuVisible();

    navCollapse.add(getRightSideChoices(userManager, userMenu));

    setCogVisible(userManager.hasUser());


    addHistoryListener();
  }

  @NotNull
  private Nav getRecNav() {
    Nav recnav = new Nav();
    recnav.getElement().setId("recnav");
    recnav.addStyleName("inlineFlex");
    recnav.getElement().getStyle().setMarginLeft(0, Style.Unit.PX);
    recnav.getElement().getStyle().setMarginRight(0, Style.Unit.PX);

    getChoice(recnav, VIEWS.ITEMS.toString());
    getChoice(recnav, VIEWS.CONTEXT.toString());
    return recnav;
  }

  @NotNull
  private Nav getDefectNav() {
    Nav recnav = new Nav();
    recnav.getElement().setId("defectnav");
    recnav.addStyleName("inlineFlex");
    recnav.getElement().getStyle().setMarginLeft(0, Style.Unit.PX);
    recnav.getElement().getStyle().setMarginRight(0, Style.Unit.PX);

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
    lnav.getElement().getStyle().setMarginLeft(0, Style.Unit.PX);
    lnav.getElement().getStyle().setMarginRight(0, Style.Unit.PX);

    return lnav;
  }


/*
  @NotNull
  private ShowChoices getChoices() {
    ShowChoices choices = ShowChoices.FL;
    String show = controller.getStorage().getValue(SHOW);
    if (show != null) {
      try {
        choices = ShowChoices.valueOf(show);
      } catch (IllegalArgumentException ee) {
      }
    }
    return choices;
  }
*/

/*  @NotNull
  private PhonesChoices getPhonesDisplay() {
    PhonesChoices choices = PhonesChoices.SHOW;
    String show = controller.getStorage().getValue(SHOW_PHONES);
    if (show != null && !show.isEmpty()) {
      try {
        choices = PhonesChoices.valueOf(show);
     //   logger.info("getPhonesDisplay got " + choices);
      } catch (IllegalArgumentException ee) {
        logger.warning("getPhonesDisplay got " + ee);
      }
    }
    return choices;
  }*/

  private Nav lnav;
  private Dropdown cog;

  @NotNull
  private Nav getRightSideChoices(UserManager userManager, UserMenu userMenu) {
    Nav rnav = new Nav();
    rnav.setAlignment(Alignment.RIGHT);
    addSubtitle(rnav);

    addUserMenu(userManager, userMenu, rnav);

    //rnav.add(viewMenu = getRealViewMenu());

    cog = new Dropdown("");
    cog.setIcon(IconType.COG);
    userMenu.getCogMenuChoices2().forEach(lt -> cog.add(lt.getLink()));
    rnav.add(cog);


    getInfoMenu(userMenu, rnav);
    return rnav;
  }

/*  @NotNull
  private Dropdown getRealViewMenu() {
    Dropdown view = new Dropdown("View");
    view.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        phoneChoice.setText(getPhoneMenuTitle());
      }
    });
    view.setIcon(IconType.REORDER);
    NavLink download = new NavLink("Download");
    download.setIcon(IconType.DOWNLOAD_ALT);
    view.add(download);

    download.addClickHandler(event -> LangTest.EVENT_BUS.fireEvent(new DownloadEvent()));

    DropdownSubmenu showChoices = getViewMenu();
    view.add(showChoices);
    return view;
  }*/

  /*private NavLink phoneChoice;

  @NotNull
  private DropdownSubmenu getViewMenu() {
    DropdownSubmenu showChoices = new DropdownSubmenu("Show");
    flTextChoices(showChoices);

    phoneChoice = new NavLink(getPhoneMenuTitle());

    Scheduler.get().scheduleDeferred(() -> {
    //  logger.info("show phones " + getPhonesDisplay());
      phoneChoice.setText(getPhoneMenuTitle());
    });

    showChoices.add(phoneChoice);
    phoneChoice.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getPhonesDisplay() == PhonesChoices.SHOW) {
         // phoneChoice.setText("Hide Sounds");
          storePhoneChoices(PhonesChoices.HIDE.toString());
          phoneChoice.setIcon(null);
        } else {
         // phoneChoice.setText("Show Sounds");
          phoneChoice.setIcon(CHECK);
          storePhoneChoices(PhonesChoices.SHOW.toString());
        }
        fireShowEvent();
      }
    });

    return showChoices;
  }

  @NotNull
  private String getPhoneMenuTitle() {
    return "Show Sounds";//(getPhonesDisplay() == PhonesChoices.SHOW ? "Hide" : "Show") + " Sounds";
  }

  private void flTextChoices(DropdownSubmenu showChoices) {
    NavLink altflChoice = new NavLink("Alternate text");
    NavLink primary = new NavLink("Primary text");
    NavLink both = new NavLink("Both Primary and Alternate");

    ShowChoices choices = getChoices();
     switch (choices) {
      case BOTH:
        both.setIcon(CHECK);
        break;
      case FL:
        primary.setIcon(CHECK);
        break;
      case ALTFL:
        altflChoice.setIcon(CHECK);
        break;
    }
    altflChoice.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        storeShowChoices(ShowChoices.ALTFL.toString());
        fireShowEvent();
        altflChoice.setIcon(CHECK);
        both.setIcon(null);
        primary.setIcon(null);
      }
    });
    showChoices.add(primary);
    showChoices.add(altflChoice);
    primary.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        storeShowChoices(ShowChoices.FL.toString());
        fireShowEvent();
        altflChoice.setIcon(null);
        both.setIcon(null);
        primary.setIcon(CHECK);
      }
    });
    showChoices.add(both);
    both.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        storeShowChoices(ShowChoices.BOTH.toString());
        fireShowEvent();
        altflChoice.setIcon(null);
        both.setIcon(CHECK);
        primary.setIcon(null);
      }
    });
  }

  private void storeShowChoices(String toStore) {
    controller.getStorage().storeValue(SHOW, toStore);
  }

  private void storePhoneChoices(String toStore) {
    controller.getStorage().storeValue(SHOW_PHONES, toStore);
  }

  private void fireShowEvent() {
    LangTest.EVENT_BUS.fireEvent(new ShowEvent());
  }*/

  private void addSubtitle(Nav rnav) {
    rnav.add(subtitle = new Label());

    subtitle.addStyleName("floatLeft");
    subtitle.setType(LabelType.WARNING);
    subtitle.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
    new TooltipHelper().addTooltip(subtitle, "Is your microphone active?");
    subtitle.setVisible(!controller.isRecordingEnabled());
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
    //showSection(instance1);
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

  private Map<VIEWS, NavLink> viewToLink = new HashMap<>();

  /**
   * @param nav
   */
  private void addChoicesForUser(ComplexWidget nav) {
    boolean first = true;
    boolean hasProject = hasProjectChoice();

    logger.info("addChoicesForUser has project " + hasProject);
    for (VIEWS choice : Arrays.asList(VIEWS.LEARN, VIEWS.DRILL, VIEWS.PROGRESS, VIEWS.LISTS)) {
      NavLink choice1 = getChoice(nav, choice.toString());
      if (first) {
        choice1.addStyleName("leftTenMargin");
      }
      first = false;
      choices.add(choice1);

      viewToLink.put(choice, choice1);
    }
  }

  @NotNull
  private NavLink getChoice(ComplexWidget nav, String instanceName) {
    String historyToken = SelectionState.SECTION_SEPARATOR + SelectionState.INSTANCE + "=" + instanceName;

    NavLink learn = getLink(nav, instanceName);
    learn.addClickHandler(event -> {
    //  logger.info("getChoice got click on " + instanceName + " = " + historyToken);
      gotClickOnChoice(instanceName, learn);
      // setHistoryItem(historyToken);
    });
    return learn;
  }

  public void showLearn() {
    gotClickOnChoice(VIEWS.LEARN.toString(), viewToLink.get(VIEWS.LEARN));
  }

  public void showDrill() {
    gotClickOnChoice(VIEWS.DRILL.toString(), viewToLink.get(VIEWS.DRILL));
  }

  private void gotClickOnChoice(String instanceName, NavLink learn) {
    showSection(instanceName);
    showActive(learn);
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

  @Override
  public Panel getBanner() {
    return this;
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
    setVisibleChoices(controller.getProjectStartupInfo() != null);
    showActive(viewToLink.get(VIEWS.LEARN));
    recordMenuVisible();
  }

  private void setVisibleChoices(boolean show) {
    lnav.setVisible(show);
    //viewMenu.setVisible(show);
  }

  private NavLink getContactUs() {
    return getAnchor(NEED_HELP_QUESTIONS_CONTACT_US, getMailTo());
  }

  @NotNull
  private String getMailTo() {
    return "mailto:" + NETPROF_HELP_LL_MIT_EDU + "?" + "Subject=" + MAILTO_SUBJECT;
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
