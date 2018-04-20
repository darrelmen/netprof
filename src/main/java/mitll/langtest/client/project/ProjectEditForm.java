/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.common.MessageHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.Services;
import mitll.langtest.client.initial.LifecycleSupport;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.client.user.FormField;
import mitll.langtest.client.user.UserDialog;
import mitll.langtest.shared.project.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * Created by go22670 on 1/17/17.
 */
public class ProjectEditForm extends UserDialog {
  public static final String PROJECT_TYPE = "Project Type";
  public static final boolean SHOW_PROJECT_TYPE = false;
  private final Logger logger = Logger.getLogger("ProjectEditForm");

  public static final String LANGUAGE = "Language";
  public static final String LIFECYCLE = "Lifecycle";

  private static final int LEFT_MARGIN_FOR_DOMINO = 320;

  private static final String PLEASE_ENTER_A_PROJECT_NAME = "Please enter a project name.";
  private static final String SHOW_ON_I_OS = "Show On iOS";
  private static final String STATUS_BOX = "Status_Box";

  private static final String DOMINO_PROJECT = "Domino";
  private static final String PLEASE_WAIT = "Please wait...";
  private static final String ALIGN_REF_AUDIO = "Align ref audio";
  private static final String CHECK_AUDIO = "Check Audio";

  private static final String ID = "ID";
  private static final String DOMINO_ID = "Domino ID";

  private static final String HIERARCHY = "Hierarchy";
  private static final String COURSE = "Course";
  private static final String COURSE_OPTIONAL = "Course (optional)";
  private static final String HYDRA_HOST_PORT = "Hydra Host:Port";
  /**
   * @see #getFields
   */
  private static final String HYDRA_HOST_OPTIONAL = "Hydra Host (optional)";


  private static final String PLEASE_ENTER_A_LANGUAGE_MODEL_DIRECTORY = "Please enter a language model directory.";
  private static final String PLEASE_ENTER_A_PORT_NUMBER_FOR_THE_SERVICE = "Please enter a port number for the service.";

  private static final String PLEASE_SELECT_A_LANGUAGE = "Please select a language.";

  /**
   * @see #addDominoProject
   */
  private static final String PLEASE_SELECT_A_DOMINO_PROJECT = "Please select a domino project.";
  private static final String NAME = "Name";
  private static final String PROJECT_NAME = "Project Name";
  private static final String FIRST_TYPE_HINT = "(e.g. Unit)";
  private static final String SECOND_TYPE_HINT = "(e.g. Chapter) OPTIONAL";

  private static final String LANGUAGE_MODEL = "Lang. Model";
  private static final String LANGUAGE_MODEL_OPTIONAL = "Language Model (optional)";

  private static final int MIN_LENGTH_USER_ID = 4;
  private static final int USER_ID_MAX_LENGTH = 5;

  private final LifecycleSupport lifecycleSupport;
  private final MessageHelper messageHelper;
  private ListBox statusBox, typeBox;
  private ProjectInfo info;
  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);

  private HTML feedback;
  private FormField hydraPort, nameField, unit, chapter, course, hydraHost;
  /**
   * @see #addLanguage
   */
  private ListBox language;
  private ListBox dominoProjects;
  private FormField model;
  private CheckBox showOniOSBox;
  private final Services services;

  /**
   * @param lifecycleSupport
   * @see ProjectChoices#getCreateNewButton(DivWidget)
   */
  ProjectEditForm(LifecycleSupport lifecycleSupport, ExerciseController controller) {
    super(controller.getProps());
    this.lifecycleSupport = lifecycleSupport;
    services = controller;
    messageHelper = controller.getMessageHelper();
  }

  /**
   * @param info
   * @param isNew
   * @return
   * @see ProjectChoices#showEditDialog
   */
  Widget getForm(ProjectInfo info, boolean isNew) {
    this.info = info;
    return getFields(info, isNew);
  }

  @Override
  protected Form getUserForm() {
    Form signInForm = new Form() {
      protected void onLoad() {
        checkIsHydraRunning();
      }
    };
    signInForm.addStyleName("topMargin");
    signInForm.getElement().getStyle().setBackgroundColor("white");
    return signInForm;
  }

  private void checkIsHydraRunning() {
    services.getScoringServiceAsyncForHost(info.getHost()).isHydraRunning(info.getID(), new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        messageHelper.handleNonFatalError("Checking for hydra status", caught);
      }

      @Override
      public void onSuccess(Boolean result) {
        if (!result) {
          int port = info.getPort();
          if (port == -1) {
            feedback.setText("No Hydra service for this language.");
          } else {
            feedback.setText("Hydra service is not available on port " + port);
          }
        }
      }
    });
  }

  /**
   * So we can change the name, the language?, the model dir,
   * the port
   *
   * @see ProjectChoices#showEditDialog
   */
  void updateProject() {
    info.setLanguage(language.getSelectedValue());
    DominoProject id = dominoToProject.get(dominoProjects.getSelectedValue());

    if (id != null) {
      info.setDominoID(id.getDominoID());
      //logger.info(" project domino id now " + id.getDominoID());
    } else {
      logger.info("no project for " + dominoProjects.getSelectedValue());
    }

    setCommonFields();

    info.setStatus(ProjectStatus.valueOf(statusBox.getValue()));

    info.setHost(hydraHost.getSafeText());
    setPort();

    info.setShowOniOS(showOniOSBox.getValue());
    //   logger.info("updateProject now " + info);

    projectServiceAsync.update(info, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        messageHelper.handleNonFatalError("Updating project.", caught);
      }

      @Override
      public void onSuccess(Boolean result) {
        lifecycleSupport.refreshStartupInfo(true);
        tellHydraServerToRefreshProject(info.getID());
      }
    });
  }

  private void tellHydraServerToRefreshProject(int projID) {
    services.getScoringService().configureAndRefresh(projID, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        messageHelper.handleNonFatalError("Updating project on hydra server.", caught);
      }

      @Override
      public void onSuccess(Void result) {
        logger.info("updateProject did update on project #" + projID + " on hydra server (maybe h2).");
      }
    });
  }

  private void setPort() {
    try {
      info.setPort(Integer.parseInt(hydraPort.getSafeText()));
    } catch (NumberFormatException e) {
    }
  }

  private void setCommonFields() {
    info.setName(nameField.getSafeText());
    info.setCourse(course.getSafeText());
    info.setFirstType(unit.getSafeText());
    info.setSecondType(chapter.getSafeText());
    info.setModelsDir(model.getSafeText());
    info.setProjectType(ProjectType.valueOf(typeBox.getValue()));
    setPort();
  }

  @Nullable
  private ProjectStatus checkHydraModelAndPort() {
    ProjectStatus status = ProjectStatus.valueOf(statusBox.getValue());

    if (status == ProjectStatus.EVALUATION || status == ProjectStatus.PRODUCTION) {
      try {
        info.setPort(Integer.parseInt(hydraPort.getSafeText()));

        if (model.isEmpty()) {
          markError(model, PLEASE_ENTER_A_LANGUAGE_MODEL_DIRECTORY);
        }

      } catch (NumberFormatException e) {
        markError(hydraPort, PLEASE_ENTER_A_PORT_NUMBER_FOR_THE_SERVICE);
        return null;
      }
    } else {
      clearError(model.getGroup());
      clearError(hydraPort.getGroup());
    }
    return status;
  }

  boolean isValid() {
    logger.info("isValid unit " + unit.getSafeText());

    if (nameField.getSafeText().isEmpty()) {
      markErrorNoGrabRight(nameField, PLEASE_ENTER_A_PROJECT_NAME);
      return false;
    } else if (isLanguageNotValid()) {
      //markErrorNoGrab(language, PLEASE_SELECT_A_LANGUAGE);
      Window.alert(PLEASE_SELECT_A_LANGUAGE);
      return false;
      // } else if (dominoProjects.getSelectedIndex() == -1 && dominoProjects.getItemCount() > 0) {
    } else if (unit.getSafeText().isEmpty()) {
      logger.info("isValid : selected " + dominoProjects.getSelectedIndex() + " vs " + dominoProjects.getItemCount() +
          " unit = '" + unit.getSafeText() + "'");
      Window.alert(PLEASE_SELECT_A_DOMINO_PROJECT);
      return false;
    } else {
      return true;
    }
  }

  private boolean isLanguageNotValid() {
    return getLanguageChoice().equalsIgnoreCase(PLEASE_SELECT_A_LANGUAGE);
  }

  private String getLanguageChoice() {
    return language.getValue();
  }

  /**
   * TODO : how to handle didn't create the project?
   * @see ProjectChoices#showNewProjectDialog
   */
  void newProject() {
    info.setLanguage(getLanguageChoice());

    {
      DominoProject id = dominoToProject.get(dominoProjects.getValue());

      if (id != null) {
        info.setDominoID(id.getDominoID());
      }
    }

//    logger.info("domino id is " + info.getDominoID());
    setCommonFields();

    projectServiceAsync.create(info, new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {
        messageHelper.handleNonFatalError("Creating project.", caught);
      }

      @Override
      public void onSuccess(Integer projID) {
        if (projID == -1) {
          logger.warning("coudn't create project?");
          Window.alert("Sorry, couldn't create a new project.");
        } else {
          lifecycleSupport.refreshStartupInfo(true);
          tellHydraServerToRefreshProject(projID);
        }
      }
    });
  }

  private Fieldset getFields(ProjectInfo info, boolean isNew) {
    Fieldset fieldset = new Fieldset();
    int id1 = info.getID();
    if (id1 > 0) {
      Heading id = getHeading(ID, id1);
      if (info.getDominoID() > 0) {
        DivWidget hDiv = getHDiv(id);
        hDiv.setWidth("100%");
        Heading domino_id = getHeading(DOMINO_ID, info.getDominoID());
        domino_id.getElement().getStyle().setMarginLeft(LEFT_MARGIN_FOR_DOMINO, PX);
        hDiv.add(domino_id);
        fieldset.add(hDiv);
      } else {
        fieldset.add(id);
      }
    }

    {
      nameField = getName(getHDivLabel(fieldset, NAME, true), info.getName(), PROJECT_NAME);
      checkNameOnBlur(nameField);
      Scheduler.get().scheduleDeferred(() -> nameField.getWidget().setFocus(true));
    }

    addLanguage(info, fieldset, isNew);
    addDominoProject(info, fieldset, isNew);

    {
      course = getName(getHDivLabel(fieldset, COURSE, false), info.getCourse(), COURSE_OPTIONAL);
      course.setText(info.getCourse());
    }

    // these are generally not editable
    {
      DivWidget typesRow = getHDivLabel(fieldset, HIERARCHY, false);

      unit = getName(typesRow, info.getFirstType(), FIRST_TYPE_HINT, 150, 40, false);
      unit.setText(info.getFirstType());

      chapter = getName(typesRow, info.getSecondType(), SECOND_TYPE_HINT, 150, 40, false);
      chapter.setText(info.getSecondType());

      if (isNew) typesRow.setVisible(false);
    }

    {
      DivWidget widgets = addLifecycle(info, fieldset);
      if (isNew) widgets.setVisible(false);
    }

    if (SHOW_PROJECT_TYPE){
      addProjectType(info, fieldset);
    }

    {
      DivWidget hDivLabel = getHDivLabel(fieldset, HYDRA_HOST_PORT, false);

      String currentName = isNew ? "" : info.getHost();

      hydraHost = getName(hDivLabel, currentName, HYDRA_HOST_OPTIONAL, 100, 30, true);

      //String safeText = hydraHost.getSafeText();
      //  logger.info("host " + safeText);
      // hydraHost.setText(info.getHost());

      if (isNew) info.setHost("");
      hydraPort = getHydraPort(hDivLabel, info.getPort());
      if (isNew) hDivLabel.setVisible(false);
    }

    DivWidget hDivLabel = getHDivLabel(fieldset, LANGUAGE_MODEL, false);
    model = getModel(hDivLabel, info.getModelsDir());

    if (isNew) hDivLabel.setVisible(false);

    if (!isNew) {
      fieldset.add(getCheckAudio(info));
      fieldset.add(getRecalcRefAudio(info));
    }

    {
      feedback = new HTML();
      feedback.addStyleName("topFiveMargin");
      addBottomMargin(feedback);
      fieldset.add(feedback);
    }

    return fieldset;
  }

  @NotNull
  private Heading getHeading(String text, int id1) {
    return new Heading(4, text, "" + id1);
  }

  /**
   * @param info
   * @param fieldset
   * @return
   * @see #getFields
   */
  private DivWidget addLifecycle(ProjectInfo info, Fieldset fieldset) {
    DivWidget lifecycle = getHDivLabel(fieldset, LIFECYCLE, false);

    lifecycle.add(statusBox = getBox());
    {
      showOniOSBox = new CheckBox(SHOW_ON_I_OS);
      showOniOSBox.setValue(info.isShowOniOS());
      lifecycle.add(showOniOSBox);
      showOniOSBox.addStyleName("leftTenMargin");
    }

    checkPortOnBlur(statusBox);
    setBoxForStatus(statusBox, info.getStatus());

    return lifecycle;
  }

  private DivWidget addProjectType(ProjectInfo info, Fieldset fieldset) {
    DivWidget lifecycle = getHDivLabel(fieldset, PROJECT_TYPE, false);
    lifecycle.add(typeBox = getTypeBox());
    setBoxForType(typeBox, info.getProjectType());
    return lifecycle;
  }

  private final Map<String, DominoProject> dominoToProject = new HashMap<>();

  /**
   * @param info
   * @param fieldset
   * @param isNew
   * @see #getFields
   */
  private void addLanguage(ProjectInfo info, Fieldset fieldset, boolean isNew) {
    DivWidget name = getHDivLabel(fieldset, LANGUAGE, false);
    name.getElement().getStyle().setMarginTop(0, PX);

    this.language = new ListBox();
    final ListBox outer = this.language;
    this.language.addChangeHandler(event -> projectServiceAsync.getDominoForLanguage(outer.getSelectedValue(), new AsyncCallback<List<DominoProject>>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(List<DominoProject> result) {
        dominoProjects.clear();

        result.forEach(dominoProject -> {
          String item = dominoProject.getDominoID() + " : " + dominoProject.getName();
          dominoProjects.addItem(item);
          dominoToProject.put(item, dominoProject);

        });

        Scheduler.get().scheduleDeferred(() -> {
          if (result.size() == 1) {
            setUnitAndChapter("", result.iterator().next());
          }
        });
      }
    }));
    this.language.addStyleName("leftTenMargin");

    name.add(this.language);

    if (isNew) {
      this.language.addItem(PLEASE_SELECT_A_LANGUAGE);
    }
    int i = 0;

    for (Language value : Language.values()) {
      this.language.addItem(value.toDisplay());
      if (info.getLanguage().equalsIgnoreCase(value.toString())) {
        this.language.setItemSelected(i, true);
      }
      i++;
    }
  }

  /**
   * @param info
   * @param fieldset
   * @param isNew
   * @see #getFields
   */
  private void addDominoProject(ProjectInfo info, Fieldset fieldset, boolean isNew) {
    DivWidget name = getHDivLabel(fieldset, DOMINO_PROJECT, false);
    name.getElement().getStyle().setMarginTop(0, PX);

    this.dominoProjects = new ListBox();
    this.dominoProjects.addStyleName("leftTenMargin");

    name.add(this.dominoProjects);

    dominoProjects.addChangeHandler(event -> {
      String selectedValue = dominoProjects.getSelectedValue();
      setUnitAndChapter(selectedValue, dominoToProject.get(selectedValue));
    });

    if (isNew) {
      this.dominoProjects.addItem(PLEASE_SELECT_A_DOMINO_PROJECT);
    } else {
      ListBox outer = dominoProjects;
      projectServiceAsync.getDominoForLanguage(info.getLanguage(), new AsyncCallback<List<DominoProject>>() {
        @Override
        public void onFailure(Throwable caught) {

        }

        @Override
        public void onSuccess(List<DominoProject> result) {
          dominoProjects.clear();

          result.forEach(dominoProject -> {
            String item = dominoProject.getDominoID() + " : " + dominoProject.getName();
            dominoProjects.addItem(item);
            dominoToProject.put(item, dominoProject);
            if (info.getDominoID() == dominoProject.getDominoID()) {
              outer.setItemSelected(dominoProjects.getItemCount() - 1, true);
            }
          });

      /*    if (dominoToProject.size() == 1) {
            if (info.getDominoID() == -1) {

            }
          }*/
        }
      });
    }
  }

  private void setUnitAndChapter(String selectedValue, DominoProject dominoProject) {
    if (dominoProject != null) {
      //logger.info("setUnitAndChapter got " + dominoProject);
      unit.setText(dominoProject.getFirstType());
      chapter.setText(dominoProject.getSecondType());
//      logger.info("setUnitAndChapter set unit " + dominoProject.getFirstType());
    } else {
      logger.info("setUnitAndChapter no domino project for " + selectedValue);
    }
  }

  private DivWidget getHDivLabel(Fieldset fieldset, String name1, boolean addTopMargin) {
    DivWidget name = getHDivLabel(name1, addTopMargin);
    fieldset.add(name);
    return name;
  }

  private DivWidget getHDivLabel(String language, boolean addTopMargin) {
    Heading left = new Heading(5, language);
    if (addTopMargin) left.getElement().getStyle().setMarginTop(14, PX);
    left.setWidth("100px");
    return getHDiv(left);
  }

  private DivWidget getHDiv(Widget left) {
    DivWidget hdiv = new DivWidget();
    hdiv.addStyleName("inlineFlex");
    hdiv.add(left);
    return hdiv;
  }

  /**
   * @param fieldset
   * @param currentName
   * @param hint
   * @return
   * @see #getFields
   */
  private FormField getName(HasWidgets fieldset, String currentName, String hint) {
    return getName(fieldset, currentName, hint, 200, 50, true);
  }

  private FormField getName(HasWidgets fieldset, String currentName, String hint, int width, int maxLength, boolean isEnabled) {
    FormField userField = addControlFormFieldWithPlaceholder(fieldset, false, 3, maxLength, hint);
    userField.box.addStyleName("topMargin");
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("name");
    userField.box.setWidth(width + "px");
    userField.getGroup().getElement().getStyle().setMarginBottom(5, PX);

    userField.box.setEnabled(isEnabled);

    if (currentName != null && !currentName.isEmpty()) {
      userField.box.setText(currentName);
    }

    return userField;
  }

  private void checkNameOnBlur(FormField userField) {
    userField.box.addBlurHandler(event -> {
      String safeText = userField.getSafeText();
      if (!safeText.equalsIgnoreCase(info.getName())) {
        //   logger.info("checking name " + safeText);
        if (!isLanguageNotValid()) {
          projectServiceAsync.existsByName(getLanguageChoice(), safeText, new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) {
              messageHelper.handleNonFatalError("Checking name.", caught);
            }

            @Override
            public void onSuccess(Boolean result) {
              if (result) {
                markErrorNoGrabRight(userField, "Project with this name already exists.");
              }
            }
          });
        }
      }
    });
  }

  private void checkPortOnBlur(ListBox userField) {
    userField.addBlurHandler(event -> checkHydraModelAndPort());
  }

  private FormField getHydraPort(HasWidgets fieldset, int currentPort) {
    FormField userField =
        addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, USER_ID_MAX_LENGTH, "Port (optional)");
    userField.box.addStyleName("topMargin");
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("hydraPort");
    userField.box.setWidth(100 + "px");
    if (currentPort > 0) {
      userField.box.setText("" + currentPort);
    }
    return userField;
  }

  /**
   * @param fieldset
   * @param model
   * @return
   * @see #getFields(ProjectInfo, boolean)
   */
  private FormField getModel(HasWidgets fieldset, String model) {
    FormField userField =
        addControlFormFieldWithPlaceholder(fieldset, false, 5, 75, LANGUAGE_MODEL_OPTIONAL);
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("languageModel");
    userField.box.setWidth(350 + "px");
    if (model != null)
      userField.box.setText("" + model);
    return userField;
  }

  private Button getCheckAudio(final ProjectInfo info) {
    Button w = new Button(CHECK_AUDIO, IconType.STETHOSCOPE);
    w.addClickHandler(event -> clickCheckAudio(info, w));
    addBottomMargin(w);
    return w;
  }

  private void clickCheckAudio(ProjectInfo info, Button w) {
    w.setEnabled(false);
    feedback.setText(PLEASE_WAIT);
    checkAudio(info, w);
  }

  private void checkAudio(ProjectInfo info, Button w) {
    //  feedback.setText("Checking audio and making mp3's...");
    services
        .getAudioServiceAsyncForHost(info.getHost())
        .checkAudio(info.getID(), new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            w.setEnabled(true);
            messageHelper.handleNonFatalError("check audo for project", caught);
          }

          @Override
          public void onSuccess(Void result) {
            w.setEnabled(true);
            feedback.setText("Checking audio and making mp3's...");
          }
        });
  }

  /**
   * @param info
   * @return
   * @see #getFields(ProjectInfo, boolean)
   */
  private Button getRecalcRefAudio(final ProjectInfo info) {
    final Button w = new Button(ALIGN_REF_AUDIO, IconType.STETHOSCOPE);

    w.addClickHandler(event -> clickRecalc(info, w));

    w.addStyleName("leftFiveMargin");
    addBottomMargin(w);

    return w;
  }

  private void addBottomMargin(UIObject w) {
    w.addStyleName("bottomFiveMargin");
  }

  private void clickRecalc(ProjectInfo info, Button w) {
    w.setEnabled(false);
    feedback.setText(PLEASE_WAIT);
    recalcRefAudio(info, w);
  }

  private void recalcRefAudio(ProjectInfo info, Button w) {
    services.getAudioServiceAsyncForHost(info.getHost()).recalcRefAudio(info.getID(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        w.setEnabled(true);
        messageHelper.handleNonFatalError("recalc audio alignments for project", caught);
      }

      @Override
      public void onSuccess(Void result) {
        w.setEnabled(true);
        feedback.setText("In progress...");
      }
    });
  }

  private ListBox getBox() {
    ListBox affBox = new ListBox();
    affBox.getElement().setId(STATUS_BOX);
    affBox.addStyleName("leftTenMargin");

    for (ProjectStatus status : ProjectStatus.values()) {
      if (status.shouldShow()) {
        affBox.addItem(status.name());
      }
    }

    return affBox;
  }

  private ListBox getTypeBox() {
    ListBox affBox = new ListBox();
    affBox.addStyleName("leftTenMargin");

    for (ProjectType status : ProjectType.values()) {
      if (status.shouldShow()) {
        affBox.addItem(status.name());
      }
    }

    return affBox;
  }

  private void setBoxForStatus(ListBox statusBox, ProjectStatus statusValue) {
    int i = 0;
    boolean found = false;

    for (ProjectStatus status : ProjectStatus.values()) {
      if (status == statusValue) {
        found = true;
        break;
      } else i++;
    }

    // first is please select.
    statusBox.setSelectedIndex(found ? i : 0);
  }

  private void setBoxForType(ListBox statusBox, ProjectType projectType) {
    int i = 0;
    boolean found = false;

    for (ProjectType projectType1 : ProjectType.values()) {
      if (projectType1 == projectType) {
        found = true;
        break;
      } else i++;
    }

    // first is please select.
    statusBox.setSelectedIndex(found ? i : 0);
  }
}
