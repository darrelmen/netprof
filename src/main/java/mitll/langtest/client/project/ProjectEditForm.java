package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.Services;
import mitll.langtest.client.initial.LifecycleSupport;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.client.user.FormField;
import mitll.langtest.client.user.UserDialog;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * Created by go22670 on 1/17/17.
 */
public class ProjectEditForm extends UserDialog {
  private final Logger logger = Logger.getLogger("ProjectEditForm");

  private static final String HIERARCHY = "Hierarchy";
  private static final String COURSE = "Course";
  private static final String COURSE_OPTIONAL = "Course (optional)";
  private static final String HYDRA_HOST_PORT = "Hydra Host:Port";
  private static final String HYDRA_HOST_OPTIONAL = "Hydra Host (optional)";

  private static final String PLEASE_ENTER_A_LANGUAGE_MODEL_DIRECTORY = "Please enter a language model directory.";
  private static final String PLEASE_ENTER_A_PORT_NUMBER_FOR_THE_SERVICE = "Please enter a port number for the service.";

  private static final String PLEASE_SELECT_A_LANGUAGE = "Please select a language.";
  private static final String NAME = "Name";
  private static final String PROJECT_NAME = "Project Name";
  private static final String FIRST_TYPE_HINT = "(e.g. Unit)";
  private static final String SECOND_TYPE_HINT = "(e.g. Chapter) OPTIONAL";

  private static final String LANGUAGE_MODEL = "Language Model";

  private static final int MIN_LENGTH_USER_ID = 4;
  private static final int USER_ID_MAX_LENGTH = 5;

  private final LifecycleSupport lifecycleSupport;
  private ListBox statusBox;
  private ProjectInfo info;
  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);

  private HTML feedback;
  private FormField hydraPort, nameField, unit, chapter, course, hydraHost;
  private ListBox language;
  private FormField model;
  private CheckBox showOniOSBox;
  private final Services services;
  boolean isNew=false;

  /**
   * @param lifecycleSupport
   * @see ProjectChoices#getCreateNewButton(DivWidget)
   */
  ProjectEditForm(LifecycleSupport lifecycleSupport, ExerciseController controller) {
    super(controller.getProps());
    this.lifecycleSupport = lifecycleSupport;
    services = controller;
  }

  /**
   * @param info
   * @param isNew
   * @return
   * @see ProjectChoices#showEditDialog
   */
  Widget getForm(ProjectInfo info, boolean isNew) {
    this.info = info;
    this.isNew = isNew;
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
    info.setName(nameField.getSafeText());
    info.setLanguage(language.getSelectedValue());
    info.setCourse(course.getSafeText());
    info.setStatus(ProjectStatus.valueOf(statusBox.getValue()));

    info.setModelsDir(model.getSafeText());

    info.setHost(hydraHost.getSafeText());
    try {
      info.setPort(Integer.parseInt(hydraPort.getSafeText()));
    } catch (NumberFormatException e) {

    }

    info.setFirstType(unit.getSafeText());
    info.setSecondType(chapter.getSafeText());

    info.setShowOniOS(showOniOSBox.getValue());
    //   logger.info("updateProject now " + info);

    projectServiceAsync.update(info, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Boolean result) {
        lifecycleSupport.refreshStartupInfo(true);
      }
    });
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
    if (nameField.getSafeText().isEmpty()) {
      markErrorNoGrabRight(nameField, "Please enter a project name.");
      return false;
    }
    else  if (language.getValue().equalsIgnoreCase(PLEASE_SELECT_A_LANGUAGE)) {
      //markErrorNoGrab(language, PLEASE_SELECT_A_LANGUAGE);
      Window.alert(PLEASE_SELECT_A_LANGUAGE);
      return false;
    }
    else if (unit.getSafeText().isEmpty()) {
      markErrorNoGrabRight(unit, "Please enter the first hierarchy.");
      return false;
    }
    else {
      return true;
    }
  }
  /**
   * @see ProjectChoices#showNewProjectDialog
   */
  void newProject() {
    info.setName(nameField.getSafeText());
    info.setLanguage(language.getValue());
    info.setCourse(course.getSafeText());
    info.setFirstType(unit.getSafeText());
    info.setSecondType(chapter.getSafeText());
    info.setModelsDir(model.getSafeText());

    try {
      info.setPort(Integer.parseInt(hydraPort.getSafeText()));
    } catch (NumberFormatException e) {

    }

    projectServiceAsync.create(info, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Boolean result) {
        lifecycleSupport.refreshStartupInfo(true);
      }
    });
  }

  private Fieldset getFields(ProjectInfo info, boolean isNew) {
    Fieldset fieldset = new Fieldset();
    if (info.getID() > 0) {
      Heading id = new Heading(4, "ID", "" + info.getID());
      fieldset.add(id);
    }

    {
      nameField = getName(getHDivLabel(fieldset, NAME), info.getName(), PROJECT_NAME);
      checkNameOnBlur(nameField);
      Scheduler.get().scheduleDeferred(() -> nameField.getWidget().setFocus(true));
    }

    addLanguage(info, fieldset, isNew);

    {
      course = getName(getHDivLabel(fieldset, COURSE), info.getCourse(), COURSE_OPTIONAL);
      course.setText(info.getCourse());
    }

    {
      DivWidget typesRow = getHDivLabel(fieldset, HIERARCHY);
      unit = getName(typesRow, info.getFirstType(), FIRST_TYPE_HINT, 150, 40);
      unit.setText(info.getFirstType());

      chapter = getName(typesRow, info.getSecondType(), SECOND_TYPE_HINT, 150, 40);
      chapter.setText(info.getSecondType());
    }

    DivWidget widgets = addLifecycle(info, fieldset);
    if (isNew) widgets.setVisible(false);

    {
      DivWidget hDivLabel = getHDivLabel(fieldset, HYDRA_HOST_PORT);

      hydraHost = getName(hDivLabel, info.getHost(), HYDRA_HOST_OPTIONAL, 100, 30);
      hydraHost.setText(info.getHost());

      hydraPort = getHydraPort(hDivLabel, info.getPort());
      if (isNew) hDivLabel.setVisible(false);
    }

    DivWidget hDivLabel = getHDivLabel(fieldset, LANGUAGE_MODEL);
    model = getModel(hDivLabel, info.getModelsDir());

    if (isNew) hDivLabel.setVisible(false);

    if (!isNew) {
      fieldset.add(getCheckAudio(info));
      fieldset.add(getRecalcRefAudio(info));
    }

    {
      feedback = new HTML();
      feedback.addStyleName("topFiveMargin");
      feedback.addStyleName("bottomFiveMargin");
      fieldset.add(feedback);
    }

    return fieldset;
  }

  private DivWidget addLifecycle(ProjectInfo info, Fieldset fieldset) {
    DivWidget lifecycle = getHDivLabel(fieldset, "Lifecycle");

    lifecycle.add(statusBox = getBox());

    {
      showOniOSBox = new CheckBox("Show On iOS");

      logger.info("show on iOS " + info.isShowOniOS());
      showOniOSBox.setValue(info.isShowOniOS());
      lifecycle.add(showOniOSBox);
      showOniOSBox.addStyleName("leftTenMargin");
    }

    checkPortOnBlur(statusBox);
    setBox(info.getStatus());

    return lifecycle;
  }

  /**
   * @param info
   * @param fieldset
   * @param isNew
   * @see #getFields
   */
  private void addLanguage(ProjectInfo info, Fieldset fieldset, boolean isNew) {
    // fieldset.add(new Heading(5, "Language"));
    DivWidget name = getHDivLabel(fieldset, "Language");
    name.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    // language = getName(fieldset, info.getLanguage(), "Language");
    this.language = new ListBox();
    name.add(this.language);
    int i = 0;
    if (isNew) {
      this.language.addItem(PLEASE_SELECT_A_LANGUAGE);
    }
    for (Language value : Language.values()) {
      this.language.addItem(value.toDisplay());
      if (info.getLanguage().equalsIgnoreCase(value.toString())) this.language.setItemSelected(i, true);
      i++;
    }
    this.language.addStyleName("leftTenMargin");
  }

  private DivWidget getHDivLabel(Fieldset fieldset, String name1) {
    DivWidget name = getHDivLabel(name1);
    fieldset.add(name);
    return name;
  }

  private DivWidget getHDivLabel(String language) {
    Heading left = new Heading(5, language);
    left.setWidth("100px");
    // left.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    return getHDiv(left);
  }

  private DivWidget getHDiv(Widget left) {
    DivWidget hdiv = new DivWidget();
    hdiv.addStyleName("inlineFlex");
    hdiv.add(left);
    return hdiv;
  }

  private FormField getName(HasWidgets fieldset, String currentName, String hint) {
    return getName(fieldset, currentName, hint, 200, 50);
  }

  private FormField getName(HasWidgets fieldset, String currentName, String hint, int width, int maxLength) {
    FormField userField =
        addControlFormFieldWithPlaceholder(fieldset, false, 3, maxLength, hint);
    userField.box.addStyleName("topMargin");
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("name");
    userField.box.setWidth(width + "px");

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
        projectServiceAsync.existsByName(safeText, new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Boolean result) {
            if (result) {
              markErrorNoGrab(userField, "Project with this name already exists.");
            }
          }
        });
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
        addControlFormFieldWithPlaceholder(fieldset, false, 5, 75, "Language Model (optional)");
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("languageModel");
    userField.box.setWidth(350 + "px");
    if (model != null)
      userField.box.setText("" + model);
    return userField;
  }

  private Button getCheckAudio(final ProjectInfo info) {
    Button w = new Button("Check Audio", IconType.STETHOSCOPE);
    w.addClickHandler(event -> {
      w.setEnabled(false);
      feedback.setText("Please wait...");
      // logger.info("check audio for " + info);
      checkAudio(info, w);
    });
    w.addStyleName("bottomFiveMargin");
    return w;
  }

  private void checkAudio(ProjectInfo info, Button w) {
    services.getAudioServiceAsyncForHost(info.getHost()).checkAudio(info.getID(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        w.setEnabled(true);
      }

      @Override
      public void onSuccess(Void result) {
        w.setEnabled(true);
        feedback.setText("Audio check complete.");
      }
    });
  }

  /**
   * @see #getFields(ProjectInfo, boolean)
   * @param info
   * @return
   */
  private Button getRecalcRefAudio(final ProjectInfo info) {
    final Button w = new Button("Align ref audio", IconType.STETHOSCOPE);

    w.addClickHandler(event -> {
      w.setEnabled(false);
      feedback.setText("Please wait...");
      recalcRefAudio(info, w);
    });

    w.addStyleName("leftFiveMargin");
    w.addStyleName("bottomFiveMargin");

    return w;
  }

  private void recalcRefAudio(ProjectInfo info, Button w) {
    services.getAudioServiceAsyncForHost(info.getHost()).recalcRefAudio(info.getID(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        w.setEnabled(true);
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
    affBox.getElement().setId("Status_Box");
    affBox.addStyleName("leftTenMargin");
    for (ProjectStatus status : ProjectStatus.values()) {
      affBox.addItem(status.name());
    }

    return affBox;
  }

  private void setBox(ProjectStatus statusValue) {
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
}
