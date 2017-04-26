package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.*;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.client.services.UserService;
import mitll.langtest.client.services.UserServiceAsync;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.shared.StartupInfo;
import mitll.langtest.shared.project.ProjectInfo;
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
  public static final String NEW_PROJECT = "New Project";
  private final Logger logger = Logger.getLogger("ProjectChoices");

  private static final int LANGUAGE_SIZE = 3;

  /**
   * @see #showProjectChoices(List, int)
   */
  private static final String PLEASE_SELECT_A_LANGUAGE = "Select a language";
  private static final String PLEASE_SELECT_A_COURSE = "Select a course";
  /**
   * @see #getHeader(List, int)
   */
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
    this.userNotification = langTest;
    this.uiLifecycle = uiLifecycle;
  }

  /**
   * Overkill?
   *
   * @param level
   * @param parent
   * @see InitialUI#addProjectChoices
   */
  public void showProjectChoices(int level, SlimProject parent) {
    if (parent != null) {
      contentRow.add(showProjectChoices(getVisibleProjects(parent.getChildren()), level));
    } else {
      long then = System.currentTimeMillis();
      service.getStartupInfo(new AsyncCallback<StartupInfo>() {
        public void onFailure(Throwable caught) {
          lifecycleSupport.onFailure(caught, then);
        }

        public void onSuccess(StartupInfo startupInfo) {
//          logger.info("got " +startupInfo);
          contentRow.add(showProjectChoices(getVisibleProjects(startupInfo.getProjects()), level));
        }
      });
    }
  }

  /**
   * Students and teachers can only see production sites.
   * Admins can see retired sites.
   * Developers can see development sites.
   *
   * @param projects
   * @return
   */
  private List<SlimProject> getVisibleProjects(List<SlimProject> projects) {
    List<SlimProject> filtered = new ArrayList<>();
    Collection<User.Permission> permissions = controller.getPermissions();
    boolean canRecord = permissions.contains(User.Permission.RECORD_AUDIO) ||
        permissions.contains(User.Permission.QUALITY_CONTROL) || permissions.contains(User.Permission.DEVELOP_CONTENT);

    for (SlimProject project : projects) {
      if (project.getStatus() != ProjectStatus.PRODUCTION) {
        if (project.getStatus() == ProjectStatus.RETIRED) {
          boolean admin = controller.getUserManager().isAdmin();
          if (admin) {
            filtered.add(project);
          }
        } else if (canRecord) {
          filtered.add(project);
        }
      } else filtered.add(project);
    }
    return filtered;
  }

  public void showProject(SlimProject project) {
    contentRow.add(showProjectChoices(project.getChildren(), 1));
  }

  /**
   * @param result
   * @param nest
   * @see InitialUI#addProjectChoices
   */
  private Section showProjectChoices(List<SlimProject> result, int nest) {
    logger.info("showProjectChoices choices # = " + result.size() + " : nest level " + nest);
    final Section section = new Section("section");
    section.add(getHeader(result, nest));

    final Container flags = new Container();
    section.add(flags);
    addFlags(result, nest, flags);
    return section;
  }

  /**
   * @param result
   * @param nest
   * @param flags
   * @see #showProjectChoices(List, int)
   */
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
        String language = project.getLanguage();
        language = language.substring(0, 1).toUpperCase() + language.substring(1);
        current.add(getLangIcon(language, project, nest));
        total++;
      }

      if (total < size) {
        flags.add(current = new Thumbnails());
      }
    }
  }

  private void sortLanguages(final int nest, List<SlimProject> languages) {
    Collections.sort(languages, (o1, o2) -> {
      if (nest == 0) {
        return o1.getLanguage().toLowerCase().compareTo(o2.getLanguage().toLowerCase());
      } else {
        int i = Integer.valueOf(o1.getDisplayOrder()).compareTo(o2.getDisplayOrder());
        return i == 0 ? o1.getName().compareTo(o2.getName()) : i;
      }
    });
  }

  /**
   * TODO : there's excess horizontal space - the container is somehow set to a width of 724???
   *
   * @param result
   * @param nest
   * @return
   * @see #showProjectChoices
   */
  @NotNull
  private DivWidget getHeader(List<SlimProject> result, int nest) {
    DivWidget header = new DivWidget();
    header.addStyleName("container");
//    header.addStyleName("inlineFlex");
    String text = PLEASE_SELECT_A_LANGUAGE;
    if (nest == 1) {
      text = PLEASE_SELECT_A_COURSE;
    }
    if (result.isEmpty()) {
      text = NO_LANGUAGES_LOADED_YET;
    }

    Heading child = new Heading(3, text);
    child.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);
    //header.add(child);

    DivWidget left = new DivWidget();
    left.addStyleName("floatLeftAndClear");
    left.add(child);
    //left.addStyleName("clear");
    header.add(left);

    if (isQC()) {
      getCreateNewButton(header);
    }

    return header;
  }

  private void getCreateNewButton(DivWidget header) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button(NEW_PROJECT);

    DivWidget right = new DivWidget();
    right.addStyleName("floatRight");
    right.add(w);

    w.addStyleName("floatLeft");
    header.add(right);

    w.setIcon(IconType.PLUS);
    w.setSize(ButtonSize.LARGE);
    w.setType(ButtonType.WARNING);
    w.addClickHandler(event -> {
      showNewProjectDialog();
    });
  }

  private void showNewProjectDialog() {
    ProjectEditForm projectEditForm = new ProjectEditForm(lifecycleSupport, controller);
    DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
        projectEditForm.newProject();
      }

      @Override
      public void gotNo() {

      }
    };

    new DialogHelper(true).show(
        "Create New Project",
        projectEditForm.getForm(new ProjectInfo(), true),
        listener,
        550);
  }

  /**
   * @param lang
   * @param projectForLang
   * @param nest
   * @return
   * @see #showProjectChoices
   */
  private Panel getLangIcon(String lang, SlimProject projectForLang, int nest) {
//    logger.info("project " + projectForLang);
//    logger.info("lang " + lang);
//    logger.info("nest " + nest);
    String lang1 =
        nest == 0 &&
            //   lang.equalsIgnoreCase(projectForLang.getName()) &&
            projectForLang.hasChildren() ? lang : projectForLang.getName();
    return getImageAnchor(lang1, projectForLang);
  }

  /**
   * Has three parts - flag, label, and optional edit icon.
   * TODO : Consider arbitrarily deep nesting...
   *
   * @param name
   * @param projectForLang
   * @return
   * @see #getLangIcon
   */
  private Panel getImageAnchor(String name, SlimProject projectForLang) {
    int nest = 1;
    Thumbnail thumbnail = new Thumbnail();
    // widgets.setWidth("100%");
    thumbnail.setWidth("195px");
    thumbnail.setSize(2);
    final int projid = projectForLang.getID();

    {
      PushButton button = new PushButton(getFlag(projectForLang.getCountryCode()));
      button.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent clickEvent) {
          gotClickOnFlag(name, projectForLang, projid, nest);
        }
      });
      thumbnail.add(button);
    }

    DivWidget horiz = new DivWidget();
    horiz.addStyleName("inlineFlex");
    horiz.setWidth("100%");
    thumbnail.add(horiz);
    {
      Heading label = new Heading(LANGUAGE_SIZE, name);
      label.setWidth("100%");
      label.getElement().getStyle().setLineHeight(25, Style.Unit.PX);
      if (projectForLang.getStatus() != ProjectStatus.PRODUCTION) {
        label.setSubtext(projectForLang.getStatus().name());
      } else if (projectForLang.hasChildren()) {
        label.setSubtext(projectForLang.getChildren().size() + " courses");
      }
      label.addStyleName("floatLeft");

      DivWidget container = new DivWidget();
      container.add(label);
      container.setWidth("100%");
      container.addStyleName("floatLeft");


      if (isQC() && !projectForLang.hasChildren()) {
        DivWidget horiz2 = new DivWidget();
        horiz2.addStyleName("inlineFlex");
        horiz2.add(getEditButtonContainer(projectForLang));
        DivWidget importButtonContainer = getImportButtonContainer(projectForLang);
        importButtonContainer.addStyleName("leftFiveMargin");
        horiz2.add(importButtonContainer);
        horiz2.add(getButtonContainer(getDeleteButton(projectForLang)));
        container.add(horiz2);
      }

      horiz.add(container);
    }

//    if (isQC() && !projectForLang.hasChildren()) {
//      horiz.add(getEditButtonContainer(projectForLang));
//      horiz.add(getImportButtonContainer(projectForLang));
//    }

    return thumbnail;
  }

  @NotNull
  private DivWidget getEditButtonContainer(SlimProject projectForLang) {
    com.github.gwtbootstrap.client.ui.Button editButton = getEditButton(projectForLang);
    return getButtonContainer(editButton);
  }

  @NotNull
  private DivWidget getImportButtonContainer(SlimProject projectForLang) {
    com.github.gwtbootstrap.client.ui.Button editButton = getImportButton(projectForLang);
    return getButtonContainer(editButton);
  }

  @NotNull
  private DivWidget getButtonContainer(Button editButton) {
    DivWidget buttonContainer = new DivWidget();
    buttonContainer.addStyleName("floatLeft");
    editButton.addStyleName("floatRight");
    buttonContainer.setWidth("100%");
    buttonContainer.addStyleName("topFiveMargin");
//    buttonContainer.addStyleName("topTwentyMargin");
    buttonContainer.add(editButton);
    return buttonContainer;
  }

  @NotNull
  private com.github.gwtbootstrap.client.ui.Button getEditButton(SlimProject projectForLang) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button();
    w.setIcon(IconType.PENCIL);
    w.addClickHandler(event -> showEditDialog(projectForLang));
    return w;
  }

  @NotNull
  private com.github.gwtbootstrap.client.ui.Button getImportButton(SlimProject projectForLang) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button();
    w.setIcon(IconType.UPLOAD);
    w.addClickHandler(event -> showImportDialog(projectForLang));
    return w;
  }

  @NotNull
  private com.github.gwtbootstrap.client.ui.Button getDeleteButton(SlimProject projectForLang) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button();
    w.setIcon(IconType.ERASER);
    w.setType(ButtonType.DANGER);
    w.addClickHandler(event -> showDeleteDialog(projectForLang));
    return w;
  }

  private void showEditDialog(SlimProject projectForLang) {
    logger.info("projectForLang " + projectForLang);
    ProjectEditForm projectEditForm = new ProjectEditForm(lifecycleSupport, controller);

    DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
        projectEditForm.updateProject();
      }

      @Override
      public void gotNo() {
      }
    };
    new DialogHelper(true).show(
        "Edit " + projectForLang.getName(),
        projectEditForm.getForm(projectForLang, false),
        listener,
        550);
  }

  private void showImportDialog(SlimProject projectForLang) {
    // logger.info("projectForLang " + projectForLang);
    //new FileUploader().getForm(projectForLang.getID());
    DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
        projectServiceAsync.addPending(projectForLang.getID(), new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
          }
        });
      }

      @Override
      public void gotNo() {
      }
    };

    new DialogHelper(true).show(
        "Import data into " + projectForLang.getName(),
        new FileUploader().getForm(projectForLang.getID()),
        listener,
        550);
  }

  private void showDeleteDialog(SlimProject projectForLang) {
    // logger.info("projectForLang " + projectForLang);
    //new FileUploader().getForm(projectForLang.getID());
    DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
        logger.info("delete project!");
        projectServiceAsync.delete(projectForLang.getID(), new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(Boolean result) {
            uiLifecycle.startOver();
          }
        });
        // projectEditForm.updateProject();
      }

      @Override
      public void gotNo() {
      }
    };
    Heading contents = new Heading(2, "Are you sure?");
    new DialogHelper(true).show(
        "Delete " + projectForLang.getName() + " forever?",
        contents,
        listener,
        550);
  }

  private boolean isQC() {
    return controller.getUserState().hasPermission(User.Permission.QUALITY_CONTROL) || controller.getUserState().isAdmin();
  }

  private void gotClickOnFlag(String name, SlimProject projectForLang, int projid, int nest) {
    NavLink projectCrumb = uiLifecycle.makeBreadcrumb(name);
    List<SlimProject> children = projectForLang.getChildren();

//    logger.info("gotClickOnFlag project " + projid + " has " + children);

    if (children.size() < 2) {
      logger.info("onClick select leaf project " + projid +
          " current user " + controller.getUser() + " : " + controller.getUserManager().getUserID());
      setProjectForUser(projid);
    } else {
      logger.info("onClick select parent project " + projid + " and " + children.size() + " children ");

      projectCrumb.addClickHandler(clickEvent -> uiLifecycle.clickOnParentCrumb(projectForLang));
      uiLifecycle.clearContent();
      contentRow.add(showProjectChoices(children, nest));
    }
  }

  /**
   * @param cc
   * @return
   * @see #getImageAnchor
   */
  @NotNull
  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    return new com.google.gwt.user.client.ui.Image("langtest/cc/" + cc + ".png");
  }

  /**
   * @param projectid
   * @see #getImageAnchor
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

  /**
   * @param projectid
   * @see #setProjectForUser
   */
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
