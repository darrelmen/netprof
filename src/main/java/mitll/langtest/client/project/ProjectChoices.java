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

package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.FileUpload;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.hlt.domino.client.common.DecoratedFields;
import mitll.hlt.domino.shared.Constants;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.common.MessageHelper;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.domino.common.UploadViewBase;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.initial.LifecycleSupport;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.client.scoring.UnitChapterItemHelper;
import mitll.langtest.client.services.OpenUserServiceAsync;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.client.user.UserNotification;
import mitll.langtest.client.user.UserState;
import mitll.langtest.shared.exercise.DominoUpdateResponse;
import mitll.langtest.shared.project.*;
import mitll.langtest.shared.user.Permission;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.shared.project.ProjectProperty.MODEL_TYPE;
import static mitll.langtest.shared.user.Permission.*;

/**
 * Created by go22670 on 1/12/17.
 */
public class ProjectChoices extends ThumbnailChoices {
  public static final int THUMB_WIDTH = 181;
  private final Logger logger = Logger.getLogger("ProjectChoices");


  private static final String EDIT_PROJECT = "Edit project.";
  /**
   *
   */
  private static final String MODES = "Interpreter and Vocab";

  private static final String GVIDAVER = "gvidaver";

  public static final String PLEASE_WAIT = "Please wait...";

  /**
   * @see #getImportButton
   */
  private static final String SYNCHRONIZE_CONTENT_WITH_DOMINO = "Synchronize content with domino.";
  private static final String START_TO_DELETE_THIS_PROJECT = "Start to delete this project.";
  private static final String DELETE_PROJECT = "delete project";

  /**
   * @see #getImportButton(SlimProject)
   */
  private static final boolean ALLOW_SYNC_WITH_DOMINO = true;

  private static final int DIALOG_HEIGHT = 598;
  private static final String COURSE1 = " course";
  /**
   * @see #getLabel
   */
  private static final String COURSES = COURSE1 + "s";


  private static final String RECALC_REF = "Recalc Ref";
  private static final String ALL_PROJECTS_COMPLETE = "All projects complete.";

  private static final String COURSE = "Course";

  /**
   * @see #showDeleteDialog
   */
  private static final String DELETING_PLEASE_WAIT = "Deleting... please wait.";
  private static final String CHECK_AUDIO = "Check Audio";

  /**
   * @see #showNewProjectDialog
   */
  private static final String CREATE_NEW_PROJECT = "Create New Project";

  /**
   * @see #getImageAnchor
   */
  private static final int MIN_HEIGHT = 125;
  /**
   * ? why 91?
   */
  private static final int NORMAL_MIN_HEIGHT = 91;

  /**
   * @see #getCreateNewButton
   */
  private static final String NEW_PROJECT = "New Project";

  private static final int LANGUAGE_SIZE = 3;

  /**
   * @see #showProjectChoices
   */
  private static final String PLEASE_SELECT_A_LANGUAGE = "Select a language";
  private static final String PLEASE_SELECT_A_COURSE = "Select a course";
  /**
   * @see #getPromptText
   */
  private static final String PLEASE_SELECT_A_MODE = "Select a mode";
  /**
   * @see #getHeader(List, int)
   */
  private static final String NO_LANGUAGES_LOADED_YET = "No languages loaded yet.";

  private final UILifecycle uiLifecycle;

  private final LifecycleSupport lifecycleSupport;
  private final ExerciseController<?> controller;
  private final UserNotification userNotification;

  private final OpenUserServiceAsync userService;
  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);
  private final MessageHelper messageHelper;
  /**
   * @see InitialUI#populateRootPanel
   */
  private DivWidget contentRow;
  private int sessionUser = -1;
  private boolean isSuperUser = false;


  private static final boolean DEBUG = false;
  private static final boolean DEBUG_CLICK = false;

  /**
   * @param langTest
   * @param uiLifecycle
   * @see InitialUI#InitialUI
   */
  public ProjectChoices(LangTest langTest, UILifecycle uiLifecycle) {
    this.lifecycleSupport = langTest;
    this.sessionUser = langTest.getUser();

    {
      String userID = langTest.getUserManager().getUserID();
      if (userID != null) isSuperUser = userID.equalsIgnoreCase(GVIDAVER);
    }

    this.controller = langTest;
    messageHelper = langTest.getMessageHelper();
    this.userNotification = langTest;
    this.uiLifecycle = uiLifecycle;
    userService = langTest.getOpenUserService();
  }

  @Override
  protected int getChoiceWidth() {
    return THUMB_WIDTH;
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
      if (DEBUG) logger.info("showProjectChoices show initial " + level);
      showInitialChoices(level);
    } else {
      if (DEBUG) logger.info("showProjectChoices show choice for parent " + parent.getName() + " " + level);
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
   * Polyglot users can only see polyglot sites.
   * <p>
   * TODO: rationalize this - check factor out
   *
   * @param projects
   * @return
   * @see #getNumVisible
   * @see #addProjectChoices
   */
  private List<SlimProject> getVisibleProjects(List<SlimProject> projects) {
    List<SlimProject> filtered = new ArrayList<>();
    Collection<Permission> permissions = controller.getPermissions();
    boolean canRecord = isCanRecord(permissions);
    //boolean isPoly = permissions.contains(POLYGLOT);
//    logger.info("isPoly " + isPoly + " startup " + projectStartupInfo);

    /*    logger.info("getVisibleProjects : Examining  " + projects.size() + " projects," +
        "\n\tpoly " + isPoly +
        "\n\tcan record = " + canRecord +
        "\n\tpermissions " + permissions);*/
    boolean admin = controller.getUserManager().isAdmin();

    for (SlimProject project : projects) {
      ProjectStatus status = project.getStatus();

      if (status == ProjectStatus.PRODUCTION) {
        filtered.add(project);
      } else {
        if (status.shouldShowOnlyToAdmins()) { // retired are only visible to admins
          if (admin) {
            filtered.add(project);
          }
        } else if (canRecord) {
          filtered.add(project);
        }
      }
    }

//    List<SlimProject> filtered2 = new ArrayList<>();
//    if (isPoly) {
//      for (SlimProject project : filtered) {
//        if (isPolyglot(project) || project.hasChildren()) {
//          filtered2.add(project);
//        }
//      }
//    } else {
//      filtered2 = filtered;
//    }
    return filtered;
  }

/*
  private boolean isPolyglot(SlimProject project) {
    return project.getProjectType() == ProjectType.POLYGLOT;
  }
*/

  private boolean isCanRecord(Collection<Permission> permissions) {
    return permissions.contains(RECORD_AUDIO) ||
        permissions.contains(QUALITY_CONTROL) ||
        permissions.contains(DEVELOP_CONTENT);
  }

  /**
   * @param project
   * @see InitialUI#resetLanguageSelection
   */
  public void showProject(SlimProject project) {
    int widgetCount = contentRow.getWidgetCount();
    if (widgetCount == 2) {
      logger.warning("showProject has " + widgetCount);
    }
    contentRow.add(showProjectChoices(getVisibleProjects(project.getChildren()), 1));
  }

  /**
   * @param result
   * @param nest
   * @see InitialUI#addProjectChoices
   * @see #gotClickOnFlag
   * @see #showProject(SlimProject)
   */
  private Section showProjectChoices(List<SlimProject> result, int nest) {
    if (DEBUG) {
      logger.info("showProjectChoices choices # = " + result.size() + " : nest level " + nest);
    }

    final Section section = getScrollingSection();
    section.add(getHeader(result, nest));

    {
      final Container flags = new Container();
      flags.add(addFlags(result, nest));
      section.add(flags);
    }

    return section;
  }

  /**
   * skip parents that have no visible children -- e.g. with polyglot users.
   * getLangIcon will be null in those cases.
   *
   * @param result
   * @param nest
   * @see #showProjectChoices
   */
  private Thumbnails addFlags(List<SlimProject> result, int nest) {
    Thumbnails current = new Thumbnails();
    current.getElement().getStyle().setMarginBottom(70, Style.Unit.PX);
    getSorted(result, nest)
        .forEach(project -> {
          Panel langIcon = getLangIcon(getDisplayLang(project), project, nest);
          if (langIcon != null) {
            current.add(langIcon);
          }
        });

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
    return language.equalsIgnoreCase("msa") ? "MSA" : language.substring(0, 1).toUpperCase() + language.substring(1);
  }

  /**
   * Sort by display name not enum name.
   *
   * @param nest
   * @param languages
   */
  private void sortLanguages(final int nest, List<SlimProject> languages) {
    languages.sort((o1, o2) -> {
      if (nest == 0) {
        return getDisplayLang(o1).compareTo(getDisplayLang(o2));
      } else {
        int i = Integer.compare(o1.getDisplayOrder(), o2.getDisplayOrder());
        return i == 0 ? o1.getName().compareTo(o2.getName()) : i;
      }
    });
  }

  private String getDisplayLang(SlimProject project) {
    return project.getLanguage().toDisplay();
  }

  /**
   * TODO : there's excess horizontal space - the container is somehow set to a width of 724???
   *
   * @param result
   * @param nest
   * @return
   * @see #showProjectChoices(List, int)
   */
  @NotNull
  private DivWidget getHeader(List<SlimProject> result, int nest) {
    DivWidget header = new DivWidget();
    header.addStyleName("container");

    //   logger.info("getHeader " + result.size() + " : " + nest);
    {
      DivWidget left = new DivWidget();
      left.addStyleName("floatLeftAndClear");

      String promptText = (result.isEmpty()) ? NO_LANGUAGES_LOADED_YET : getPromptText(result, nest);
      Heading child = new Heading(3, promptText);
      child.getElement().getStyle().setMarginLeft(10, Style.Unit.PX);
      left.add(child);
      header.add(left);
    }

    if (nest == 0) {
      DivWidget topBottom = new DivWidget();
      topBottom.addStyleName("floatRight");
      topBottom.getElement().getStyle().setMarginBottom(18, Style.Unit.PX);
      header.add(topBottom);

      {
        DivWidget right = new DivWidget();
        right.addStyleName("floatRight");
        right.addStyleName("inlineFlex");
        right.addStyleName("topMargin");

        topBottom.add(right);
        if (isQC()) {
          right.add(getCreateNewButton());
        }

        if (controller.getUserState().isAdmin()) {
          topBottom.add(addAdminControls(right));
        }
      }
    }

    return header;
  }

  /**
   * @param result
   * @param nest
   * @return
   * @see #getHeader
   */
  @NotNull
  private String getPromptText(List<SlimProject> result, int nest) {
    List<SlimProject> dialogProjects = getDialogProjects(result);
    //  logger.info("getHeader " + result.size() + " nest  " + nest);

    return dialogProjects.size() == result.size() ?
        PLEASE_SELECT_A_MODE : (nest == 1) ? PLEASE_SELECT_A_COURSE : PLEASE_SELECT_A_LANGUAGE;
  }

  private List<SlimProject> getDialogProjects(List<SlimProject> projects) {
    return projects
        .stream()
        .filter(slimProject -> slimProject.getProjectType() == ProjectType.DIALOG && !slimProject.hasChildren())
        .collect(Collectors.toList());
  }

  /**
   * For right now recalc all alignments only should be done by really prepared admin.
   *
   * @param right
   * @paramx topBottom
   */
  private HTML addAdminControls(DivWidget right) {
    HTML status = new HTML();
    status.setHeight("15px");
    status.addStyleName("leftFiveMargin");

    right.add(getEnsureAllAudioButton(status));

    if (isSuperUser) {
      right.add(getRecalcRefAudioButton(status));
    }

    return status;
  }

  /**
   * @return
   * @see #getHeader
   */
  private DivWidget getCreateNewButton() {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button(NEW_PROJECT);

    DivWidget right = new DivWidget();
    right.add(w);

    w.addStyleName("floatLeft");

    w.setIcon(IconType.PLUS);
    w.setSize(ButtonSize.LARGE);
    w.setType(ButtonType.WARNING);
    w.addClickHandler(event -> showNewProjectDialog());

    return right;
  }

  /**
   * @param status
   * @see #getHeader(List, int)
   */
  private DivWidget getEnsureAllAudioButton(HTML status) {
    com.github.gwtbootstrap.client.ui.Button checkAudio = new com.github.gwtbootstrap.client.ui.Button(CHECK_AUDIO);

    DivWidget right = new DivWidget();
    right.add(checkAudio);
    checkAudio.addStyleName("leftFiveMargin");

    checkAudio.addStyleName("floatLeft");

    checkAudio.setIcon(IconType.CHECK);
    checkAudio.setSize(ButtonSize.LARGE);
    checkAudio.setType(ButtonType.SUCCESS);
    checkAudio.addClickHandler(event -> checkAudio(controller.getAllProjects(), status));

    return right;
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

              controller.getExerciseService().refreshAllAudio(remove.getID(), new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {

                }

                @Override
                public void onSuccess(Void result) {
                  logger.info("refreshAllAudio complete");
                }
              });

              checkAudio(projects, status);
            }
          });
    }
  }

  private DivWidget getRecalcRefAudioButton(HTML status) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button(RECALC_REF);

    DivWidget right = new DivWidget();
    right.add(w);
    w.addStyleName("leftFiveMargin");

    w.addStyleName("floatLeft");

    w.setIcon(IconType.MEDKIT);
    w.setSize(ButtonSize.LARGE);
    w.setType(ButtonType.SUCCESS);
    w.addClickHandler(event -> recalcProject(controller.getAllProjects(), status));

    return right;
  }

  /**
   * @param projects
   * @param status
   * @see mitll.langtest.server.services.ScoringServiceImpl#recalcAlignments
   */
  private void recalcProject(List<SlimProject> projects, HTML status) {
    if (projects.isEmpty()) {
      status.setText(ALL_PROJECTS_COMPLETE);
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

    new DialogHelper(true)
        .show(
            CREATE_NEW_PROJECT,
            projectEditForm.getForm(new ProjectInfo(), true),
            new DialogHelper.CloseListener() {
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

              @Override
              public void gotHidden() {

              }
            },
            DIALOG_HEIGHT, -1);
  }

  /**
   * @param lang
   * @param projectForLang
   * @param nest
   * @return null if the project is a parent and has no visible children (polyglot filter)
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
    int numVisibleChildren = getNumVisible(projectForLang);
    if (projectForLang.hasChildren() && numVisibleChildren == 0) {
      return null;
    } else {
      Thumbnail thumbnail = getThumbnail();

      boolean isQC = isQC();
      {
        String countryCode = projectForLang.getCountryCode();
        //    logger.info("for " + name +" cc " + countryCode);
        PushButton button = new PushButton(getFlag(countryCode));
        final int projid = projectForLang.getID();
        button.addClickHandler(clickEvent -> gotClickOnFlag(name, projectForLang, projid, 1));
        thumbnail.add(button);

        boolean hasChildren = projectForLang.hasChildren();
        if (isQC) {
          if (!hasChildren) {
            addPopover(button, projectForLang);
          }
        } else {
          if (projectForLang.getCourse().isEmpty()) {
            // addPopover(button, projectForLang);
          } else {
            addPopoverUsual(button, projectForLang);
          }
        }
      }

      DivWidget horiz = new DivWidget();
      horiz.getElement().getStyle().setProperty("minHeight", (isQC ? MIN_HEIGHT : NORMAL_MIN_HEIGHT) + "px"); // so they wrap nicely
      thumbnail.add(horiz);

      horiz.add(getContainerWithButtons(name, projectForLang, isQC, numVisibleChildren));

      return thumbnail;
    }
  }

  /**
   * @param name
   * @param projectForLang
   * @param isQC
   * @param numVisibleChildren
   * @return
   * @see #getImageAnchor
   */
  @NotNull
  private DivWidget getContainerWithButtons(String name, SlimProject projectForLang, boolean isQC, int numVisibleChildren) {
    boolean hasChildren = projectForLang.hasChildren();
    boolean allDialog = areAllChildrenDialogChoices(projectForLang, numVisibleChildren);

    DivWidget container = new DivWidget();
    Heading label;

    container.add(label = getLabel(truncate(name, 23), projectForLang, numVisibleChildren, allDialog));
    container.setWidth("100%");
    container.addStyleName("floatLeft");

    ProjectType projectType = projectForLang.getProjectType();
    //logger.info("getContainerWithButtons project " + projectForLang.getID() + " " + projectForLang.getName() + " " + projectType);

    if (isQC &&
        ((hasChildren && allDialog) ||
            (!hasChildren && projectType != ProjectType.DIALOG))) {
      // logger.info("getContainerWithButtons add qc buttons " + isQC);
      container.add(getQCButtons(projectForLang, label));
    }
    return container;
  }

  private void addPopoverUsual(FocusWidget button, SlimProject projectForLang) {
    // logger.info("addPopoverUsual " + projectForLang);
    Set<String> typeOrder = new HashSet<>(Collections.singletonList(COURSE));
    UnitChapterItemHelper<?> ClientExerciseUnitChapterItemHelper = new UnitChapterItemHelper<>(typeOrder);
    button.addMouseOverHandler(event -> showPopoverUsual(projectForLang, button, typeOrder, ClientExerciseUnitChapterItemHelper));
  }

  private void showPopoverUsual(SlimProject projectForLang,
                                Widget button,
                                Set<String> typeOrder,
                                UnitChapterItemHelper<?> ClientExerciseUnitChapterItemHelper) {
    Map<String, String> value = new HashMap<>();
    value.put(COURSE, projectForLang.getCourse());
    showPopover(value, button, typeOrder, ClientExerciseUnitChapterItemHelper, Placement.RIGHT);
  }

  /**
   * @param button
   * @param projectForLang
   * @see #getImageAnchor
   */
  private void addPopover(FocusWidget button, SlimProject projectForLang) {
    Map<String, String> props = getProps(projectForLang);

    props.remove(MODEL_TYPE.toString());

    //logger.info("addPopover " + projectForLang + " : " + props);
    addPopover(button, props, Placement.RIGHT);
  }

  private Map<String, String> getProps(SlimProject projectForLang) {
    return projectForLang.getPropertyValue();
  }

  /**
   * @param name
   * @param projectForLang
   * @param numVisibleChildren
   * @param allDialog
   * @return
   * @paramx hasChildren
   * @see #getContainerWithButtons(String, SlimProject, boolean, int)
   */
  @NotNull
  private Heading getLabel(String name, SlimProject projectForLang, int numVisibleChildren, boolean allDialog) {
    ProjectStatus status = projectForLang.getStatus();
    String statusText = status == ProjectStatus.PRODUCTION ? "" : status.name();
    return getLabel(name, projectForLang.hasChildren(), numVisibleChildren, statusText, allDialog);
  }

  private boolean areAllChildrenDialogChoices(SlimProject projectForLang, int numVisibleChildren) {
    List<SlimProject> collect = getDialogProjects(projectForLang);
    return (collect.size() == numVisibleChildren) && collect.size() == 2;
  }

  private List<SlimProject> getDialogProjects(SlimProject projectForLang) {
    return projectForLang.getChildren().stream().filter(slimProject -> slimProject.getProjectType() == ProjectType.DIALOG).collect(Collectors.toList());
  }

  @NotNull
  private Heading getLabel(String name, boolean hasChildren, int numVisibleChildren,
                           String statusText, boolean alldialog) {
    Heading label = getChoiceLabel(LANGUAGE_SIZE, name, true);

    String subtext = alldialog ? MODES : hasChildren ?
        (numVisibleChildren + ((numVisibleChildren == 1) ? COURSE1 : COURSES)) : statusText;

    label.setSubtext(subtext);

    return label;
  }

  private int getNumVisible(SlimProject projectForLang) {
    return getVisibleProjects(projectForLang.getChildren()).size();
  }

  private void showProjectStatus(ProjectStatus status, Heading label) {
    if (status == ProjectStatus.PRODUCTION) {
      label.setSubtext("");
    } else {
      label.setSubtext(status.name());
    }
  }

  /**
   * @param projectForLang
   * @param label
   * @return
   * @see #getContainerWithButtons
   */
  private DivWidget getQCButtons(SlimProject projectForLang, Heading label) {
    DivWidget horiz2 = new DivWidget();
    horiz2.addStyleName("inlineFlex");
    horiz2.add(getEditButtonContainer(projectForLang, label));

    {
      DivWidget importButtonContainer = getImportButtonContainer(projectForLang);
      importButtonContainer.addStyleName("leftFiveMargin");
      horiz2.add(importButtonContainer);
      importButtonContainer.setVisible(projectForLang.getDominoID() > 0);
    }

    {
      boolean allowedToDelete = isAllowedToDelete(projectForLang);
      if (allowedToDelete) {
        Button deleteButton = getDeleteButton(projectForLang, label);
        deleteButton.addStyleName("leftFiveMargin");
        horiz2.add(getButtonContainer(deleteButton));
      }

      if (isOwnerOrAdmin(projectForLang)) {
        Button deleteButton = getUploadButton(projectForLang.getID());
        deleteButton.addStyleName("leftFiveMargin");
        horiz2.add(getButtonContainer(deleteButton));
      }
    }

    return horiz2;
  }

  /**
   * You can only delete a project if it's not in production, or it's yours or you're an admin.
   *
   * @param projectForLang
   * @return
   */
  private boolean isAllowedToDelete(SlimProject projectForLang) {
    return (projectForLang.getStatus() != ProjectStatus.PRODUCTION) && isOwnerOrAdmin(projectForLang);
  }

  private boolean isOwnerOrAdmin(SlimProject projectForLang) {
    return projectForLang.isMine(sessionUser) || controller.getUserManager().isAdmin();
  }

  @NotNull
  private DivWidget getEditButtonContainer(SlimProject projectForLang, Heading label) {
    return getButtonContainer(getEditButton(projectForLang, label));
  }

  /**
   * @param projectForLang
   * @return
   * @see #getQCButtons
   */
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

  /**
   * @param projectForLang
   * @param label
   * @return
   * @see #getEditButtonContainer(SlimProject, Heading)
   */
  @NotNull
  private com.github.gwtbootstrap.client.ui.Button getEditButton(SlimProject projectForLang, Heading label) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button();
    w.setIcon(IconType.PENCIL);
    addTooltip(w, EDIT_PROJECT);

    w.addClickHandler(event -> showEditDialog(projectForLang, label));
    return w;
  }

  /**
   * Set button disabled for now - until we know all sync operations are good (1/17).
   *
   * @param projectForLang
   * @return
   * @see #getImportButtonContainer
   */
  @NotNull
  private com.github.gwtbootstrap.client.ui.Button getImportButton(SlimProject projectForLang) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button();

    w.setIcon(IconType.EXCHANGE);
    w.setEnabled(ALLOW_SYNC_WITH_DOMINO);
    addTooltip(w, SYNCHRONIZE_CONTENT_WITH_DOMINO);

    w.addClickHandler(event -> {
      w.setEnabled(false);
      showImportDialog(projectForLang, w, false);
    });
    return w;
  }

  @NotNull
  private com.github.gwtbootstrap.client.ui.Button getDeleteButton(SlimProject projectForLang, Heading label) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button();
    w.setIcon(IconType.ERASER);
    w.setType(ButtonType.DANGER);
    addTooltip(w, START_TO_DELETE_THIS_PROJECT);
    w.addClickHandler(event -> showDeleteDialog(projectForLang, label));

    return w;
  }

  @NotNull
  private com.github.gwtbootstrap.client.ui.Button getUploadButton(int projid) {
    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button();

    w.setIcon(IconType.UPLOAD);
    w.setType(ButtonType.WARNING);

    addTooltip(w, "Upload excel into domino.");

    if (w != null) {
      w.addClickHandler(event -> {
        UploadViewBase widgets = new UploadViewBase(projid);
        widgets.showModal();
      });
    }


//    FileUpload importFileBox = new FileUpload();
//    importFileBox.setName("bulk-filename");
//
//    setAcceptOnInput(importFileBox.getElement());
//      importFileFields = new DecoratedFields(IMPORT_BULK_AUDIO, importFileBox, getImportFileTip(), null);
//      fields.add(importFileFields.getCtrlGroup());


    return w;
  }

  private void setAcceptOnInput(Element element) {
    element.setAttribute("accept", "application/vnd.ms-excel");
  }

  private void addTooltip(Widget w, String tip) {
    new TooltipHelper().createAddTooltip(w, tip, Placement.TOP);
  }

  private void showEditDialog(SlimProject projectForLang, Heading label) {
    ProjectEditForm projectEditForm = new ProjectEditForm(lifecycleSupport, controller);
    DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
      @Override
      public boolean gotYes() {
        projectEditForm.updateProject();
        showProjectStatus(projectForLang.getStatus(), label);
        return true;
      }

      @Override
      public void gotNo() {
      }

      @Override
      public void gotHidden() {

      }
    };
    new DialogHelper(true).show(
        "Edit " + projectForLang.getName(),
        projectEditForm.getForm(projectForLang, false),
        listener,
        DIALOG_HEIGHT, -1);
  }

  /**
   * TODO : fix number of items
   *
   * @param projectForLang
   * @param button
   * @param doChange
   */
  private void showImportDialog(SlimProject projectForLang, Button button, boolean doChange) {
    //  logger.info("showImport " + doChange);
    //   String s = getProps(projectForLang).get(NUM_ITEMS);
    //   logger.info("showImportDialog # items = " + s);
    final Object waitToken = messageHelper.startWaiting(PLEASE_WAIT);

    int id = projectForLang.getID();
    projectServiceAsync.addPending(id, doChange, new AsyncCallback<DominoUpdateResponse>() {
      @Override
      public void onFailure(Throwable caught) {
        messageHelper.stopWaiting(waitToken);
        button.setEnabled(true);

        controller.handleNonFatalError("add pending exercises to project", caught);
      }

      @Override
      public void onSuccess(DominoUpdateResponse result) {
        messageHelper.stopWaiting(waitToken);
        button.setEnabled(true);

        DominoUpdateResponse.UPLOAD_STATUS status = result.getStatus();
        //     logger.info("showImport got " + status);
        if (status == DominoUpdateResponse.UPLOAD_STATUS.SUCCESS && !doChange) {
          //     logger.info("showImport show " + status);
          getProps(projectForLang).putAll(result.getProps());
          showResponseReport(projectForLang, button, result);
        } else {
          //   logger.info("showImport 2 show " + status);
          showStatus(result, status);

          /**
           * Make sure the other servers internally know that the project has changed and should go look at the database again.
           */
          controller.tellOtherServerToRefreshProject(id);
        }
      }
    });
  }

  private void showStatus(DominoUpdateResponse result, DominoUpdateResponse.UPLOAD_STATUS status) {
    String title = "";
    String message = "";

    switch (status) {
      case SUCCESS:
        title = "Success";
        message = "Sync with domino complete!";
        break;
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
    new ModalInfoDialog(title, message);
  }

  private void showResponseReport(SlimProject projectForLang, Button button, DominoUpdateResponse result) {
    new ResponseModal(result, new DialogHelper.CloseListener() {
      @Override
      public boolean gotYes() {
        showImportDialog(projectForLang, button, true);
        return true;
      }

      @Override
      public void gotNo() {

      }

      @Override
      public void gotHidden() {

      }
    }, controller
    ).prepareContentWidget();
  }

  private void showDeleteDialog(SlimProject projectForLang, Heading label) {
    DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
      @Override
      public boolean gotYes() {
        label.setSubtext(DELETING_PLEASE_WAIT);
        projectServiceAsync.delete(projectForLang.getID(), new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError(DELETE_PROJECT, caught);
          }

          @Override
          public void onSuccess(Boolean result) {
            if (result) {
              uiLifecycle.startOver();
            } else {
              new ModalInfoDialog("Not allowed to delete", "You are not the creator of this project.");
            }
          }
        });
        return true;
      }

      @Override
      public void gotNo() {
      }

      @Override
      public void gotHidden() {
      }
    };

    new DialogHelper(true).show(
        "Delete " + projectForLang.getName() + " forever?",
        new Heading(2, "Are you sure?"),
        listener,
        DIALOG_HEIGHT, -1);
  }


  private boolean isQC() {
    UserState userState = controller.getUserState();
    return userState.hasPermission(QUALITY_CONTROL) || userState.isAdmin();
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
    if (DEBUG_CLICK) logger.info("gotClickOnFlag project " + projid + " has " + children);
    NavLink breadcrumb = makeBreadcrumb(name);
    if (children.size() < 2) {
      if (DEBUG_CLICK) logger.info("gotClickOnFlag onClick select leaf project " + projid +
          " " + projectForLang.getMode() +
          "\n\tcurrent user " + controller.getUser() + " : " + controller.getUserManager().getUserID());

      setProjectForUser(projid, projectForLang.getMode());
    } else { // at this point, the breadcrumb should be empty?
      if (DEBUG_CLICK)
        logger.info("gotClickOnFlag onClick select parent project " + projid + " and " + children.size() + " children ");
      breadcrumb.addClickHandler(clickEvent -> {
//        SlimProject projectForLang1 = projectForLang;
//        logger.info("gotClickOnFlag Click on crumb " + projectForLang1.getName() + " nest " + nest);
        uiLifecycle.clickOnParentCrumb(projectForLang);
      });

      uiLifecycle.clearContent();
      addProjectChoices(nest, children);
    }
  }

  @NotNull
  private NavLink makeBreadcrumb(String name) {
    return uiLifecycle.makeBreadcrumb(name);
  }

  /**
   * @param nest
   * @param children
   */
  private void addProjectChoices(int nest, List<SlimProject> children) {
    // int widgetCount = contentRow.getWidgetCount();
    // logger.info("addProjectChoices " + widgetCount);
    if (contentRow.getWidgetCount() == 0) {
      contentRow.add(showProjectChoices(getVisibleProjects(children), nest));
    } else {
      if (DEBUG || true) logger.info("addProjectChoices not adding project choices again...");
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
   * @param mode
   * @see #gotClickOnFlag
   */
  private void setProjectForUser(int projectid, ProjectMode mode) {
    //   logger.info("setProjectForUser set project for " + projectid + " mode " + mode);
    uiLifecycle.clearContent();
    userService.setProject(projectid, new AsyncCallback<User>() {
      @Override
      public void onFailure(Throwable throwable) {
        controller.handleNonFatalError("setting project", throwable);
      }

      @Override
      public void onSuccess(User aUser) {
        if (aUser == null) {
          logger.warning("setProjectForUser : no current user? ");
          uiLifecycle.logout();
        } else {
          if (aUser.getStartupInfo() == null) { // no project with that project id
            lifecycleSupport.getStartupInfo();
          } else {
            uiLifecycle.getNavigation().storeViewForMode(mode);

            userNotification.setProjectStartupInfo(aUser);
            //     logger.info("setProjectForUser set project for " + aUser + " show initial state " + lifecycleSupport.getProjectStartupInfo());
            uiLifecycle.showInitialState();
          }
        }
      }
    });
  }

  /**
   * @param contentRow
   * @see InitialUI#populateRootPanel
   */
  public void setContentRow(DivWidget contentRow) {
    this.contentRow = contentRow;
  }
}
