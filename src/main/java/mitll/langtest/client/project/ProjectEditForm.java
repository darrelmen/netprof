package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LifecycleSupport;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.AudioServiceAsync;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.client.services.ScoringServiceAsync;
import mitll.langtest.client.user.FormField;
import mitll.langtest.client.user.UserDialog;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;

import java.util.logging.Logger;

/**
 * Created by go22670 on 1/17/17.
 */
class ProjectEditForm extends UserDialog {
  private final Logger logger = Logger.getLogger("ProjectEditForm");

  private static final int MIN_LENGTH_USER_ID = 4;
  static final int USER_ID_MAX_LENGTH = 5;

  // private final EventRegistration eventRegistration;
  private final LifecycleSupport lifecycleSupport;
  private ListBox statusBox;
  private ProjectInfo info;
  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);
  private AudioServiceAsync audioServiceAsync;
  private ScoringServiceAsync scoringServiceAsync;

  private HTML feedback;
  private FormField hydraPort, nameField, unit, chapter;
  ListBox language;
  private FormField model;

  /**
   * @param lifecycleSupport
   * @see ProjectChoices#getCreateNewButton(DivWidget)
   */
  ProjectEditForm(LifecycleSupport lifecycleSupport,
                  ExerciseController controller
  ) {
    super(controller.getProps());
    this.lifecycleSupport = lifecycleSupport;
    this.audioServiceAsync = controller.getAudioService();
    this.scoringServiceAsync = controller.getScoringService();
  }

  /**
   * @param info
   * @param isNew
   * @return
   * @see ProjectContainer#gotClickOnItem
   * @see ProjectChoices#getEditButton
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
    //  signInForm.addStyleName("formRounded");
    signInForm.getElement().getStyle().setBackgroundColor("white");
    return signInForm;
  }

  private void checkIsHydraRunning() {
    int id = info.getID();
    scoringServiceAsync.isHydraRunning(id, new AsyncCallback<Boolean>() {
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
   */
  void updateProject() {
    ProjectStatus status = ProjectStatus.valueOf(statusBox.getValue());

    if (status == ProjectStatus.EVALUATION || status == ProjectStatus.PRODUCTION) {
      try {
        info.setPort(Integer.parseInt(hydraPort.getSafeText()));

        if (model.isEmpty()) {
          markError(model, "Please enter a language model directory.");
        }

      } catch (NumberFormatException e) {
        markError(hydraPort, "Please enter a port number for the service.");
        return;
      }
    }

    info.setName(nameField.getSafeText());
    info.setStatus(status);
    info.setModelsDir(model.getSafeText());

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

  void newProject() {
    info.setName(nameField.getSafeText());
    info.setLanguage(language.getValue());
    info.setModelsDir(model.getSafeText());

    projectServiceAsync.create(info, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Boolean result) {     lifecycleSupport.refreshStartupInfo(true);      }
    });
  }

  private Fieldset getFields(ProjectInfo info, boolean isNew) {
    Fieldset fieldset = new Fieldset();
    if (info.getID() > 0) {
      Heading id = new Heading(4, "ID", "" + info.getID());
      fieldset.add(id);
    }

    fieldset.add(new Heading(5, "Name"));
    nameField = getName(fieldset, info.getName(), "Project Name");
    checkNameOnBlur(nameField);

    addLanguage(info, fieldset, isNew);
    addLifecycle(info, fieldset);

    fieldset.add(new Heading(5, "Hydra Port"));
    hydraPort = getHydraPort(fieldset, info.getPort());
    fieldset.add(new Heading(5, "Language Model"));
    model = getModel(fieldset, info.getModelsDir());


    feedback = new HTML();
    feedback.addStyleName("topFiveMargin");
    feedback.addStyleName("bottomFiveMargin");
    fieldset.add(feedback);

    if (!isNew) {
      fieldset.add(getCheckAudio(info));
      fieldset.add(getRecalcRefAudio(info));
    }

    return fieldset;
  }

  private void addLifecycle(ProjectInfo info, Fieldset fieldset) {
    addControlGroupEntrySimple(
        fieldset,
        "Lifecycle",
        statusBox = getBox())
        .setWidth("366px");

    setBox(info.getStatus());
  }

  private void addLanguage(ProjectInfo info, Fieldset fieldset, boolean isNew) {
    fieldset.add(new Heading(5, "Language"));
    // language = getName(fieldset, info.getLanguage(), "Language");
    language = new ListBox();
    fieldset.add(language);
    int i = 0;
    if (isNew) {
      language.addItem("Please select a language.");
    }
    for (Language value : Language.values()) {
      language.addItem(value.toDisplay());
      if (info.getLanguage().equalsIgnoreCase(value.toString())) language.setItemSelected(i, true);
      i++;
    }
    language.addStyleName("leftFiveMargin");
  }

  private FormField getName(Fieldset fieldset, String currentName, String hint) {
    FormField userField =
        addControlFormFieldWithPlaceholder(fieldset, false, 3, 50, hint);
    userField.box.addStyleName("topMargin");
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("name");
    userField.box.setWidth("200px");
    if (currentName != null && !currentName.isEmpty())
      userField.box.setText(currentName);

    return userField;
  }

  private void checkNameOnBlur(FormField userField) {
    userField.box.addBlurHandler(event -> {
      String safeText = userField.getSafeText();
      if (!safeText.equalsIgnoreCase(info.getName())) {
        logger.info("checking name " + safeText);
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

  private FormField getHydraPort(Fieldset fieldset, int currentPort) {
    FormField userField =
        addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, USER_ID_MAX_LENGTH, "Hydra Port (optional)");
    userField.box.addStyleName("topMargin");
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("hydraPort");
    userField.box.setWidth("150" +
        "px");
    if (currentPort > 0)
      userField.box.setText("" + currentPort);
    return userField;
  }

  private FormField getModel(Fieldset fieldset, String model) {
    FormField userField =
        addControlFormFieldWithPlaceholder(fieldset, false, 5, 100, "Language Model (optional)");
    //  userField.box.addStyleName("topMargin");
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("languageModel");
    userField.box.setWidth("366" +
        "px");
    if (model != null)
      userField.box.setText("" + model);
    return userField;
  }

  private Button getCheckAudio(final ProjectInfo info) {
    Button w = new Button("Check Audio", IconType.STETHOSCOPE);
    w.addClickHandler(event -> {
      w.setEnabled(false);
      feedback.setText("Please wait...");

      logger.info("check audio for " + info);

      audioServiceAsync.checkAudio(info.getID(), new AsyncCallback<Void>() {
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
    });
    w.addStyleName("bottomFiveMargin");
    return w;
  }

  private Button getEnsureAudio() {
    Button w = new Button("Ensure ALL Audio", IconType.STETHOSCOPE);
    w.addClickHandler(event -> {
      w.setEnabled(false);
      feedback.setText("Please wait... for a long time.");

      audioServiceAsync.ensureAllAudio(new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          w.setEnabled(true);

        }

        @Override
        public void onSuccess(Void result) {
          w.setEnabled(true);
          feedback.setText("All Audio check is ongoing.");
        }
      });
    });
    return w;
  }

  private Button getRecalcRefAudio(final ProjectInfo info) {
    Button w = new Button("Align ref audio", IconType.STETHOSCOPE);

    w.addClickHandler(event -> {
      w.setEnabled(false);
      feedback.setText("Please wait...");

      audioServiceAsync.recalcRefAudio(info.getID(), new AsyncCallback<Void>() {
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
    });

    w.addStyleName("leftFiveMargin");
    w.addStyleName("bottomFiveMargin");

    return w;
  }

  private ListBox getBox(/*final Button editButton*/) {
    ListBox affBox = new ListBox();
    affBox.getElement().setId("Status_Box");
    //  affBox.setWidth(SIGN_UP_WIDTH );
    affBox.addStyleName("leftTenMargin");

    for (ProjectStatus status : ProjectStatus.values()) {
      affBox.addItem(status.name());
    }

/*

    affBox.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        editButton.setEnabled(changed());
      }
    });
    affBox.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        editButton.setEnabled(changed());
      }
    });
*/

    return affBox;
  }

/*
  private boolean changed() {
    return (info.getStatus() != ProjectStatus.valueOf(statusBox.getValue()));
  }
*/

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
