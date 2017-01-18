package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.*;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.client.services.UserService;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.user.SlimProject;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/12/17.
 */
public class ProjectChoices {
  private final Logger logger = Logger.getLogger("ProjectChoices");

  /**
   * @see #showProjectChoices(List, int)
   */
  private static final String PLEASE_SELECT_A_LANGUAGE = "Please select a language";
  private static final String PLEASE_SELECT_A_COURSE = "Please select a course";
  private static final String NO_LANGUAGES_LOADED_YET = "No languages loaded yet. Please wait.";

  /**
   * Tamas doesn't like scrolling -- try to prevent it on laptops
   */
  private static final int ITEMS_IN_ROW = 5;
  protected static final String LOGIN = "Login";
  private static final int NO_USER_INITIAL = -2;
  private final UILifecycle uiLifecycle;

  /**
   * @seex #configureUIGivenUser
   * @seex #gotUser
   * @seex #lastUser
   * @seex #resetState
   * @seex #showUserPermissions
   */
  protected long lastUser = NO_USER_INITIAL;

  private final LifecycleSupport lifecycleSupport;
  protected final ExerciseController controller;
  private final UserNotification userNotification;
  protected final PropertyHandler props;

  protected final LangTestDatabaseAsync service = GWT.create(LangTestDatabase.class);
  private final UserServiceAsync userService = GWT.create(UserService.class);
  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);

  private Panel contentRow;
  //private static final boolean DEBUG = false;

  public ProjectChoices(LangTest langTest, UILifecycle uiLifecycle) {
    this.lifecycleSupport = langTest;
    this.props = langTest.getProps();
    this.controller = langTest;
    //userFeedback = langTest;
    this.userNotification = langTest;
    this.uiLifecycle = uiLifecycle;
  }

  public void showProjectChoices(int level, SlimProject parent) {
    List<SlimProject> projects = parent == null ? lifecycleSupport.getStartupInfo().getProjects() : parent.getChildren();
//    logger.info("addProjectChoices found " + projects.size() + " initial projects, nest " + level);
    showProjectChoices(getVisibleProjects(projects), level);
  }

  private List<SlimProject> getVisibleProjects(List<SlimProject> projects) {
    List<SlimProject> filtered = new ArrayList<>();
    Collection<User.Permission> permissions = controller.getPermissions();
    boolean canRecord = permissions.contains(User.Permission.RECORD_AUDIO) ||
        permissions.contains(User.Permission.QUALITY_CONTROL) || permissions.contains(User.Permission.DEVELOP_CONTENT);

    for (SlimProject project : projects) {
      if (project.getStatus() != ProjectStatus.PRODUCTION) {
        if (canRecord) {
          filtered.add(project);
        }
      } else filtered.add(project);
    }
    return filtered;
  }

  public void showProject(SlimProject project) {
    showProjectChoices(project.getChildren(), 1);
  }

  /**
   * @param result
   * @param nest
   * @see InitialUI#addProjectChoices
   */
  public void showProjectChoices(List<SlimProject> result, int nest) {
//    logger.info("showProjectChoices " + result.size() + " : " + nest);

    final Section section = new Section("section");
    contentRow.add(section);

    section.add(getHeader(result, nest));

    final Container flags = new Container();
   // removeWidth(flags);

    section.add(flags);

    addFlags(result, nest, flags);
  }

  private void addFlags(List<SlimProject> result, int nest, Container flags) {
    Panel current = new Thumbnails();
    flags.add(current);

    List<SlimProject> languages = new ArrayList<>(result);

//    logger.info("addProjectChoices " + languages.size() + " languages");

    sortLanguages(nest, languages);

    int size = languages.size();
  //  logger.info("addProjectChoices " + size + "-------- nest " + nest);
    int total = 0;
    for (int i = 0; i < size; i += ITEMS_IN_ROW) {
      int max = i + ITEMS_IN_ROW;
      if (max > size) max = size;
      for (int j = i; j < max; j++) {
        SlimProject project = languages.get(j);
        Panel langIcon = getLangIcon(project.getLanguage(), project, nest);
        current.add(langIcon);
        total++;
      }

      if (total < size) {
        current = new Thumbnails();
        flags.add(current);
      }
    }
  }

  private void sortLanguages(final int nest, List<SlimProject> languages) {
    Collections.sort(languages, new Comparator<SlimProject>() {
      @Override
      public int compare(SlimProject o1, SlimProject o2) {
        if (nest == 0) {

          return o1.getLanguage().toLowerCase().compareTo(o2.getLanguage().toLowerCase());
        } else {
          int i = Integer.valueOf(o1.getDisplayOrder()).compareTo(o2.getDisplayOrder());
          return i == 0 ? o1.getName().compareTo(o2.getName()) : i;
        }
      }
    });
  }

  /**
   * TODO : there's excess horizontal space - the container is somehow set to a width of 724???
   * @param result
   * @param nest
   * @return
   */
  @NotNull
  private DivWidget getHeader(List<SlimProject> result, int nest) {
    DivWidget header = new DivWidget();
    header.addStyleName("container");
   // removeWidth(header);
    String text = PLEASE_SELECT_A_LANGUAGE;
    if (nest == 1) {
      text = PLEASE_SELECT_A_COURSE;
    }

    if (result.isEmpty()) {
      text = NO_LANGUAGES_LOADED_YET;
    }

    Heading child = new Heading(3, text);
    child.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);

    header.add(child);
    return header;
  }

  private void removeWidth(Widget header) {
    header.getElement().removeAttribute("width");
  }

  /**
   * @param lang
   * @param projectForLang
   * @param nest
   * @return
   * @see #showProjectChoices
   */
  private Panel getLangIcon(String lang, SlimProject projectForLang, int nest) {
    String lang1 = nest == 0 ? lang : projectForLang.getName();
    return getImageAnchor(lang1, projectForLang);
  }

  /**
   * TODO : Consider arbitrarily deep nesting...
   *
   * @param name
   * @param projectForLang
   * @return
   * @see #getLangIcon
   */
  private Panel getImageAnchor(String name, SlimProject projectForLang) {
    int nest = 1;

    Thumbnail widgets = new Thumbnail();
    widgets.setSize(2);
    final int projid = projectForLang.getProjectid();

    PushButton button = new PushButton(getFlag(projectForLang.getCountryCode()));
    button.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        NavLink projectCrumb = uiLifecycle.makeBreadcrumb(name);
        List<SlimProject> children = projectForLang.getChildren();
        // logger.info("project " + projid + " has " + children);
        if (children.size() < 2) {
          //logger.info("onClick select leaf project " + projid + " current user " + userManager.getUser() + " : " + userManager.getUserID());
          setProjectForUser(projid);
        } else {
          logger.info("onClick select parent project " + projid + " and " + children.size() + " children ");
          projectCrumb.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
              uiLifecycle.clickOnParentCrumb(projectForLang);
            }
          });
          uiLifecycle.clearContent();
          showProjectChoices(children, nest);
        }
      }
    });
    widgets.add(button);
    Heading label = new Heading(5, name);

    if (projectForLang.getStatus() != ProjectStatus.PRODUCTION) {
      label.setSubtext(projectForLang.getStatus().name());
    }
    widgets.add(label);

    return widgets;
  }

  @NotNull
  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    return new com.google.gwt.user.client.ui.Image("langtest/cc/" + cc + ".png");
  }

  /**
   * @param projectid
   * @see #getImageAnchor(String, SlimProject)
   */
  private void setProjectForUser(final int projectid) {
    projectServiceAsync.exists(projectid, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          reallySetTheProject(projectid);
        } else {
          lifecycleSupport.getStartupInfo();
        }
      }
    });
  }

  private void reallySetTheProject(int projectid) {
    logger.info("setProjectForUser set project for " + projectid);

    uiLifecycle.clearContent();
    userService.setProject(projectid, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable throwable) {

      }

      @Override
      public void onSuccess(User aUser) {
        if (aUser == null) {
          logger.warning("huh? no current user? ");
        } else {
          userNotification.setProjectStartupInfo(aUser);
          logger.info("setProjectForUser set project for " + aUser + " show initial state " + lifecycleSupport.getProjectStartupInfo());
          uiLifecycle.showInitialState();
        }
      }
    });
  }

  public void setContentRow(Panel contentRow) {
    this.contentRow = contentRow;
  }
}
