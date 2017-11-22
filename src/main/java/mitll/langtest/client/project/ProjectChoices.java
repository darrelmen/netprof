package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.LifecycleSupport;
import mitll.langtest.client.initial.PropertyHandler;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.client.scoring.UnitChapterItemHelper;
import mitll.langtest.client.services.*;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.client.user.UserState;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.DominoUpdateResponse;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.SlimProject;
import mitll.langtest.shared.project.StartupInfo;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.server.database.project.ProjectManagement.NUM_ITEMS;

/**
 * Created by go22670 on 1/12/17.
 */
public class ProjectChoices {
  private final Logger logger = Logger.getLogger("ProjectChoices");

  private static final String COURSE = "Course";

  /**
   * @see #showDeleteDialog
   */
  private static final String DELETING_PLEASE_WAIT = "Deleting... please wait.";

  private static final String CHECK_AUDIO = "Check Audio";

  /**
   *
   */
  private static final String IMPORT_DATA_INTO = "Import data into ";

  /**
   * @see #showNewProjectDialog
   */
  private static final String CREATE_NEW_PROJECT = "Create New Project";

  private static final int CHOICE_WIDTH = 170;//180;//190;//195;
  /**
   * @see #getImageAnchor
   */
  private static final int MIN_HEIGHT = 125;//100;// 110;//115;//125;
  private static final int NORMAL_MIN_HEIGHT = 67;

  private static final String NEW_PROJECT = "New Project";

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

  protected static final String LOGIN = "Login";
  private final UILifecycle uiLifecycle;

  private final LifecycleSupport lifecycleSupport;
  protected final ExerciseController controller;
  private final UserNotification userNotification;
  protected final PropertyHandler props;

  private final OpenUserServiceAsync userService;
  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);

  /**
   * @see InitialUI#populateRootPanel
   */
  private DivWidget contentRow;
  //private static final boolean DEBUG = false;

  /**
   * @param langTest
   * @param uiLifecycle
   * @see InitialUI#InitialUI
   */
  public ProjectChoices(LangTest langTest, UILifecycle uiLifecycle) {
    this.lifecycleSupport = langTest;
    this.props = langTest.getProps();
    this.controller = langTest;
    this.userNotification = langTest;
    this.uiLifecycle = uiLifecycle;
    userService = langTest.getOpenUserService();
  }

  /**
   * Overkill?
   *
   * @param parent
   * @param level
   * @see InitialUI#addProjectChoices
   */
  public void showProjectChoices(SlimProject parent, int level) {
    if (parent == null) {
      showInitialChoices(level);
    } else {
      addProjectChoices(level, parent.getChildren());
    }
  }

  private void showInitialChoices(int level) {
    final long then = System.currentTimeMillis();

    controller.getService().getStartupInfo(new AsyncCallback<StartupInfo>() {
      public void onFailure(Throwable caught) {
        lifecycleSupport.onFailure(caught, then);
      }

      public void onSuccess(StartupInfo startupInfo) {
        addProjectChoices(level, startupInfo.getProjects());
      }
    });
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
    boolean canRecord =
        permissions.contains(User.Permission.RECORD_AUDIO) ||
            permissions.contains(User.Permission.QUALITY_CONTROL) ||
            permissions.contains(User.Permission.DEVELOP_CONTENT);
    //  logger.info("Examining  " + projects.size() + " projects, can record = " + canRecord + " permissions " + permissions);

    for (SlimProject project : projects) {
      if (project.getStatus() == ProjectStatus.PRODUCTION) {
        filtered.add(project);
      } else {
        if (project.getStatus() == ProjectStatus.RETIRED) {
          boolean admin = controller.getUserManager().isAdmin();
          if (admin) {
            filtered.add(project);
          }
        } else if (canRecord) {
          filtered.add(project);
        }
      }
    }
    return filtered;
  }

  /**
   * @param project
   * @see InitialUI#getLangBreadcrumb
   */
  public void showProject(SlimProject project) {
    int widgetCount = contentRow.getWidgetCount();
    if (widgetCount == 2) {
      logger.warning("showProject has " + widgetCount);
    }
    contentRow.add(showProjectChoices(project.getChildren(), 1));
  }

  /**
   * @param result
   * @param nest
   * @see InitialUI#addProjectChoices
   * @see #gotClickOnFlag(String, SlimProject, int, int)
   * @see #showProject(SlimProject)
   */
  private Section showProjectChoices(List<SlimProject> result, int nest) {
    // logger.info("showProjectChoices choices # = " + result.size() + " : nest level " + nest);
    final Section section = new Section("section");
    section.add(getHeader(result, nest));

    final Container flags = new Container();
    section.add(flags);
    flags.add(addFlags(result, nest));

    return section;
  }

  /**
   * @param result
   * @param nest
   * @see #showProjectChoices(List, int)
   */
  private Thumbnails addFlags(List<SlimProject> result, int nest) {
    Thumbnails current = new Thumbnails();

    getSorted(result, nest)
        .forEach(project -> current.add(getLangIcon(capitalize(project.getLanguage()), project, nest)));

    return current;
  }

  @NotNull
  private List<SlimProject> getSorted(List<SlimProject> result, int nest) {
    List<SlimProject> sortedProjects = new ArrayList<>(result);
    sortLanguages(nest, sortedProjects);
    return sortedProjects;
  }

  @NotNull
  private String capitalize(String language) {
    return language.substring(0, 1).toUpperCase() + language.substring(1);
  }

  private void sortLanguages(final int nest, List<SlimProject> languages) {
    languages.sort((o1, o2) -> {
      if (nest == 0) {
        return o1.getLanguage().toLowerCase().compareTo(o2.getLanguage().toLowerCase());
      } else {
        int i = Integer.compare(o1.getDisplayOrder(), o2.getDisplayOrder());
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
    String text = (nest == 1) ? PLEASE_SELECT_A_COURSE : PLEASE_SELECT_A_LANGUAGE;

    if (result.isEmpty()) {
      text = NO_LANGUAGES_LOADED_YET;
    }

    {
      DivWidget left = new DivWidget();
      left.addStyleName("floatLeftAndClear");
      Heading child = new Heading(3, text);
      child.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);
      left.add(child);
      header.add(left);
    }

    {
      DivWidget topBottom = new DivWidget();
      topBottom.addStyleName("floatRight");
      header.add(topBottom);

      DivWidget right = new DivWidget();
      right.addStyleName("floatRight");
      right.addStyleName("inlineFlex");

      topBottom.add(right);
      if (isQC()) {
        getCreateNewButton(right);
      }

      if (controller.getUserState().isAdmin()) {
        addAdminControls(topBottom, right);
      }
    }

    return header;
  }

  private void addAdminControls(DivWidget topBottom, DivWidget right) {
    HTML status = new HTML();
    status.setHeight("15px");
    status.addStyleName("leftFiveMargin");
    getEnsureAllAudioButton(right, status);
    right.addStyleName("topFiveMargin");
    getRecalcRefAudioButton(right, status);
    topBottom.add(status);
  }

  private void getCreateNewButton(DivWidget header) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button(NEW_PROJECT);

    DivWidget right = new DivWidget();
    right.add(w);

    w.addStyleName("floatLeft");
    header.add(right);

    w.setIcon(IconType.PLUS);
    w.setSize(ButtonSize.LARGE);
    w.setType(ButtonType.WARNING);
    w.addClickHandler(event -> showNewProjectDialog());
  }

  /**
   * @param header
   * @see #getHeader(List, int)
   */
  private void getEnsureAllAudioButton(DivWidget header, HTML status) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button(CHECK_AUDIO);

    DivWidget right = new DivWidget();
    right.add(w);
    // w.addStyleName("topFiveMargin");
    w.addStyleName("leftFiveMargin");

    w.addStyleName("floatLeft");
    header.add(right);

    w.setIcon(IconType.CHECK);
    w.setSize(ButtonSize.LARGE);
    w.setType(ButtonType.SUCCESS);
    w.addClickHandler(event -> checkAudio(controller.getAllProjects(), status));
  }

  private void checkAudio(List<SlimProject> projects, HTML status) {
    if (projects.isEmpty()) {
      status.setText("All projects complete.");
    } else {
      ProjectInfo remove = projects.remove(0);
      status.setText("Checking " + remove.getName() + "...");
      controller.getAudioServiceAsyncForHost(remove.getHost())
          .checkAudio(remove.getID(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              status.setText("ERROR - couldn't check audio for " + remove.getName());
              controller.handleNonFatalError("checking audio for project", caught);
            }

            @Override
            public void onSuccess(Void result) {
              status.setText(remove.getName() + " checked...");
              checkAudio(projects, status);
            }
          });
    }
  }

  private void getRecalcRefAudioButton(DivWidget header, HTML status) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button("Recalc Ref");

    DivWidget right = new DivWidget();
    right.add(w);
    w.addStyleName("leftFiveMargin");

    w.addStyleName("floatLeft");
    header.add(right);

    w.setIcon(IconType.MEDKIT);
    w.setSize(ButtonSize.LARGE);
    w.setType(ButtonType.SUCCESS);
    w.addClickHandler(event -> recalcProject(controller.getAllProjects(), status));
  }

  private void recalcProject(List<SlimProject> projects, HTML status) {
    if (projects.isEmpty()) {
      status.setText("All projects complete.");
    } else {
      ProjectInfo remove = projects.remove(0);
      status.setText("Recalculating alignments for " + remove.getName() + "...");
      controller
          .getScoringServiceAsyncForHost(remove.getHost())
          .recalcAlignments(remove.getID(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              status.setText("ERROR - couldn't recalc audio for " + remove.getName());
              controller.handleNonFatalError("recald alignments for audio in project", caught);
            }

            @Override
            public void onSuccess(Void result) {
              status.setText(remove.getName() + " complete...");
              recalcProject(projects, status);
            }
          });
    }
  }

  /**
   * Do some validity checking...
   */
  private void showNewProjectDialog() {
    ProjectEditForm projectEditForm = new ProjectEditForm(lifecycleSupport, controller);
    DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
      @Override
      public boolean gotYes() {
        if (projectEditForm.isValid()) {
          projectEditForm.newProject();
          return true;
        } else {
          return false;
        }
      }

      @Override
      public void gotNo() {
      }
    };

    new DialogHelper(true).show(
        CREATE_NEW_PROJECT,
        projectEditForm.getForm(new ProjectInfo(), true),
        listener,
        550);
  }

  /**
   * @param lang
   * @param projectForLang
   * @param nest
   * @return
   * @see #addFlags
   */
  private Panel getLangIcon(String lang, SlimProject projectForLang, int nest) {
    String lang1 =
        nest == 0 &&
            projectForLang.hasChildren() ? lang : projectForLang.getName();
    return getImageAnchor(capitalize(lang1), projectForLang);
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
  private Panel getImageAnchor(final String name, SlimProject projectForLang) {
    Thumbnail thumbnail = new Thumbnail();
    thumbnail.setWidth(CHOICE_WIDTH + "px");
    thumbnail.setSize(2);

    boolean isQC = isQC();
    {
      PushButton button = new PushButton(getFlag(projectForLang.getCountryCode()));
      final int projid = projectForLang.getID();
      button.addClickHandler(clickEvent -> gotClickOnFlag(name, projectForLang, projid, 1));
      thumbnail.add(button);

      if (isQC) {
        addPopover(projectForLang, button);
      } else {
        if (!projectForLang.getCourse().isEmpty()) {
          addPopoverUsual(projectForLang, button);
        }
      }
    }

    DivWidget horiz = new DivWidget();
    horiz.getElement().getStyle().setProperty("minHeight", (isQC ? MIN_HEIGHT : NORMAL_MIN_HEIGHT) + "px"); // so they wrap nicely
    thumbnail.add(horiz);
    {
      boolean hasChildren = projectForLang.hasChildren();

      DivWidget container = new DivWidget();
      Heading label;

      container.add(label = getLabel(truncate(name, 23), projectForLang, hasChildren));
      container.setWidth("100%");
      container.addStyleName("floatLeft");

      if (isQC && !hasChildren) {
        container.add(getQCButtons(projectForLang, label));
      }

      horiz.add(container);
    }

    return thumbnail;
  }

  private void addPopover(SlimProject projectForLang, FocusWidget button) {
    Set<String> typeOrder = projectForLang.getProps().keySet();
    UnitChapterItemHelper<CommonExercise> commonExerciseUnitChapterItemHelper =
        new UnitChapterItemHelper<>(typeOrder);
    button.addMouseOverHandler(event -> showPopover(projectForLang, button, typeOrder, commonExerciseUnitChapterItemHelper));
  }

  private void addPopoverUsual(SlimProject projectForLang, FocusWidget button) {
    Set<String> typeOrder = new HashSet<>(Arrays.asList(COURSE));
    UnitChapterItemHelper<CommonExercise> commonExerciseUnitChapterItemHelper =
        new UnitChapterItemHelper<>(typeOrder);
    button.addMouseOverHandler(event -> showPopoverUsual(projectForLang, button, typeOrder, commonExerciseUnitChapterItemHelper));
  }

  private void showPopoverUsual(SlimProject projectForLang,
                                Widget button,
                                Set<String> typeOrder,
                                UnitChapterItemHelper<CommonExercise> commonExerciseUnitChapterItemHelper) {
    Map<String, String> value = new HashMap<>();
    value.put(COURSE, projectForLang.getCourse());

    new BasicDialog().showPopover(
        button,
        null,
        commonExerciseUnitChapterItemHelper.getTypeToValue(typeOrder, value),
        Placement.RIGHT);
  }

  private void showPopover(SlimProject projectForLang,
                           Widget button,
                           Set<String> typeOrder,
                           UnitChapterItemHelper<CommonExercise> commonExerciseUnitChapterItemHelper) {
    new BasicDialog().showPopover(
        button,
        null,
        commonExerciseUnitChapterItemHelper.getTypeToValue(typeOrder, projectForLang.getProps()),
        Placement.RIGHT);
  }

  @NotNull
  protected String truncate(String columnText, int maxLengthId) {
    if (columnText.length() > maxLengthId) columnText = columnText.substring(0, maxLengthId - 3) + "...";
    return columnText;
  }


  /**
   * @param name
   * @param projectForLang
   * @param hasChildren
   * @return
   * @see #getImageAnchor
   */
  @NotNull
  private Heading getLabel(String name, SlimProject projectForLang, boolean hasChildren) {
    Heading label = new Heading(LANGUAGE_SIZE, name);
    label.addStyleName("floatLeft");
    label.setWidth("100%");
    label.getElement().getStyle().setLineHeight(25, Style.Unit.PX);

    {
      Widget subtitle = label.getWidget(0);
      subtitle.addStyleName("floatLeft");
      subtitle.setWidth("100%");
      subtitle.addStyleName("topFiveMargin");
    }

    if (hasChildren) {
      List<SlimProject> visibleProjects = getVisibleProjects(projectForLang.getChildren());
      String suffix = (visibleProjects.size() == 1) ? " course" : " courses";
      label.setSubtext(visibleProjects.size() + suffix);
    } else {
      showProjectStatus(projectForLang, label);
    }

    label.addStyleName("floatLeft");
    return label;
  }

  private void showProjectStatus(SlimProject projectForLang, Heading label) {
    if (projectForLang.getStatus() == ProjectStatus.PRODUCTION) {
      label.setSubtext("");
    } else {
      label.setSubtext(projectForLang.getStatus().name());
    }
  }

  private DivWidget getQCButtons(SlimProject projectForLang, Heading label) {
    DivWidget horiz2 = new DivWidget();
    horiz2.addStyleName("inlineFlex");
    horiz2.add(getEditButtonContainer(projectForLang, label));

    {
      DivWidget importButtonContainer = getImportButtonContainer(projectForLang);
      importButtonContainer.addStyleName("leftFiveMargin");
      horiz2.add(importButtonContainer);
    }

    {
      if (projectForLang.getStatus() != ProjectStatus.PRODUCTION) {
        Button deleteButton = getDeleteButton(projectForLang, label);
        deleteButton.addStyleName("leftFiveMargin");
        horiz2.add(getButtonContainer(deleteButton));
      }
    }

    return horiz2;
  }

  @NotNull
  private DivWidget getEditButtonContainer(SlimProject projectForLang, Heading label) {
    return getButtonContainer(getEditButton(projectForLang, label));
  }

  @NotNull
  private DivWidget getImportButtonContainer(SlimProject projectForLang) {
    return getButtonContainer(getImportButton(projectForLang));
  }

  @NotNull
  private DivWidget getButtonContainer(Button editButton) {
    DivWidget buttonContainer = new DivWidget();
    buttonContainer.addStyleName("floatLeft");
    editButton.addStyleName("floatRight");
    buttonContainer.setWidth("100%");
    buttonContainer.addStyleName("topFiveMargin");
    buttonContainer.add(editButton);
    return buttonContainer;
  }

  @NotNull
  private com.github.gwtbootstrap.client.ui.Button getEditButton(SlimProject projectForLang, Heading label) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button();
    w.setIcon(IconType.PENCIL);
    w.addClickHandler(event -> showEditDialog(projectForLang, label));
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
  private com.github.gwtbootstrap.client.ui.Button getDeleteButton(SlimProject projectForLang, Heading label) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button();
    w.setIcon(IconType.ERASER);
    w.setType(ButtonType.DANGER);
    w.addClickHandler(event -> showDeleteDialog(projectForLang, label));
    return w;
  }

  private void showEditDialog(SlimProject projectForLang, Heading label) {
    ProjectEditForm projectEditForm = new ProjectEditForm(lifecycleSupport, controller);
    DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
      @Override
      public boolean gotYes() {

        projectEditForm.updateProject();
        showProjectStatus(projectForLang, label);
        return true;
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
    DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
      @Override
      public boolean gotYes() {
        projectServiceAsync.addPending(projectForLang.getID(), new AsyncCallback<DominoUpdateResponse>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("add pending exercises to project", caught);
          }

          @Override
          public void onSuccess(DominoUpdateResponse result) {
            DominoUpdateResponse.UPLOAD_STATUS status = result.getStatus();
            if (status == DominoUpdateResponse.UPLOAD_STATUS.SUCCESS) {
              projectForLang.getProps().putAll(result.getProps());
            } else {
              String title = "";
              String message = "";

              switch (status) {
                case FAIL:
                  title = "Import failed";
                  message = "Server error importing items - please report.";
                  break;
                case WRONG_PROJECT:
                  title = "Wrong domino project";
                  message = "Upload data is from domino project #" + result.getDominoID() +
                      " but this project is for #" + result.getCurrentDominoID() +
                      ".<br/>You probably want to make a new NetProF project and add it to there.";
                  break;
                case ANOTHER_PROJECT:
                  title = "Another domino project";
                  message = "Upload data is from domino project #" + result.getDominoID() +
                      ", which is already associated with the " + result.getMessage() + " project." +
                      "<br/>You probably want to add it to there.";
                  break;
              }
              new ModalInfoDialog(title,
                  message);
            }
          }
        });
        return true;
      }

      @Override
      public void gotNo() {
      }
    };

    new DialogHelper(true).show(
        IMPORT_DATA_INTO + projectForLang.getName(),
        new FileUploader().getForm(projectForLang.getID()),
        listener,
        550);
  }

  private void showDeleteDialog(SlimProject projectForLang, Heading label) {
    DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
      @Override
      public boolean gotYes() {
        label.setSubtext(DELETING_PLEASE_WAIT);
        projectServiceAsync.delete(projectForLang.getID(), new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("delete project", caught);
          }

          @Override
          public void onSuccess(Boolean result) {
            uiLifecycle.startOver();
          }
        });
        return true;
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
    UserState userState = controller.getUserState();
    return userState.hasPermission(User.Permission.QUALITY_CONTROL) || userState.isAdmin();
  }

  /**
   * @param name
   * @param projectForLang
   * @param projid
   * @param nest
   * @see #getImageAnchor
   */
  private void gotClickOnFlag(String name, SlimProject projectForLang, int projid, int nest) {
    List<SlimProject> children = projectForLang.getChildren();
//    logger.info("gotClickOnFlag project " + projid + " has " + children);
    if (children.size() < 2) {
/*
      logger.info("gotClickOnFlag onClick select leaf project " + projid +
          " current user " + controller.getUser() + " : " + controller.getUserManager().getUserID());
          */
      uiLifecycle.makeBreadcrumb(name);
      setProjectForUser(projid);
    } else { // at this point, the breadcrumb should be empty?
      // logger.info("gotClickOnFlag onClick select parent project " + projid + " and " + children.size() + " children ");
      NavLink projectCrumb = uiLifecycle.makeBreadcrumb(name);
      projectCrumb.addClickHandler(clickEvent -> uiLifecycle.clickOnParentCrumb(projectForLang));

      uiLifecycle.clearContent();
      addProjectChoices(nest, children);
    }
  }

  private void addProjectChoices(int nest, List<SlimProject> children) {
    int widgetCount = contentRow.getWidgetCount();
    // logger.info("addProjectChoices " + widgetCount);

    if (widgetCount == 1) {
      contentRow.add(showProjectChoices(getVisibleProjects(children), nest));
    } else {
      logger.warning("not adding project choices again...");
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
        controller.handleNonFatalError("project exists?", caught);

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
   // logger.info("setProjectForUser set project for " + projectid);
    uiLifecycle.clearContent();
    userService.setProject(projectid, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable throwable) {
//        Window.alert("Can't contact server.");
        controller.handleNonFatalError("setting project", throwable);
      }

      @Override
      public void onSuccess(User aUser) {
        if (aUser == null) {
          logger.warning("huh? no current user? ");
        } else {
          userNotification.setProjectStartupInfo(aUser);
     //     logger.info("setProjectForUser set project for " + aUser + " show initial state " + lifecycleSupport.getProjectStartupInfo());
          uiLifecycle.showInitialState();
        }
      }
    });
  }

  public void setContentRow(DivWidget contentRow) {
    this.contentRow = contentRow;
  }
}
