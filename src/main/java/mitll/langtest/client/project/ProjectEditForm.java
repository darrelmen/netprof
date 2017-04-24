package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.services.AudioServiceAsync;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.client.services.ScoringServiceAsync;
import mitll.langtest.client.user.FormField;
import mitll.langtest.client.user.UserDialog;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Created by go22670 on 1/17/17.
 */
class ProjectEditForm extends UserDialog {
  private final Logger logger = Logger.getLogger("ProjectEditForm");

  private static final int MIN_LENGTH_USER_ID = 4;
  static final int USER_ID_MAX_LENGTH = 5;

 // private final EventRegistration eventRegistration;
  private final ProjectOps projectOps;
  private ListBox statusBox;
  private ProjectInfo info;
  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);
  private AudioServiceAsync audioServiceAsync;
  private ScoringServiceAsync scoringServiceAsync;

  private HTML feedback;
  private FormField hydraPort;
  private FormField model;

  /**
   * @param projectOps
   * @paramx props
   * @see ProjectContainer#gotClickOnItem
   */
  ProjectEditForm(ProjectOps projectOps,
                  ExerciseController controller
//      ,
//                  PropertyHandler props,
//                  EventRegistration eventRegistration,
//                  AudioServiceAsync audioServiceAsync,
//                  ScoringServiceAsync scoringServiceAsync
  ) {
    super(controller.getProps());
    this.projectOps = projectOps;
  //  this.eventRegistration = controller;
    this.audioServiceAsync = controller.getAudioService();
    this.scoringServiceAsync = controller.getScoringService();
  }

  /**
   * @param info
   * @return
   * @see ProjectContainer#gotClickOnItem
   */
  Widget getForm(ProjectInfo info) {
    logger.info("getForm " +info);

    this.info = info;
    String name = info.getName();
    Heading heading = new Heading(3, name);
    heading.addStyleName("signUp");

    //Button editProject = getEditButton();

    Fieldset fields = getFields(info/*, editProject*/);
    //fields.add(editProject);

    return getTwoPartForm(heading, fields);
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
    scoringServiceAsync.isHydraRunning(info.getID(), new AsyncCallback<Boolean>() {
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

 /* @NotNull
  private Button getEditButton() {
    Button editProject = getFormButton("editProject", "Edit", eventRegistration);
    editProject.setEnabled(false);
    editProject.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        updateProject();
      }
    });
    return editProject;
  }*/

  public void updateProject() {
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

    info.setStatus(status);
    info.setModelsDir(model.getSafeText());

    projectServiceAsync.update(info, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Boolean result) {
        projectOps.refreshStartupInfo();
        projectOps.reload();
      }
    });
  }

  private Fieldset getFields(ProjectInfo info/*, Button editButton*/) {
    Fieldset fieldset = new Fieldset();
    Heading id = new Heading(4, "ID", "" + info.getID());
    fieldset.add(id);

    addControlGroupEntrySimple(
        fieldset,
        "",
        statusBox = getBox(/*editButton*/))
        .setWidth("366px");

    setBox(info.getStatus());

    fieldset.add(new Heading(5, "Hydra Port"));
    hydraPort = getHydraPort(fieldset, info.getPort());
    fieldset.add(new Heading(5, "Language Model"));
    model = getModel(fieldset, info.getModelsDir());

    feedback = new HTML();
    feedback.addStyleName("topFiveMargin");
    feedback.addStyleName("bottomFiveMargin");
    fieldset.add(feedback);


    fieldset.add(getCheckAudio(info));
    fieldset.add(getRecalcRefAudio(info));

    return fieldset;
  }

  private FormField getHydraPort(Fieldset fieldset, int currentPort) {
    FormField userField =
        addControlFormFieldWithPlaceholder(fieldset, false, MIN_LENGTH_USER_ID, USER_ID_MAX_LENGTH, "Hydra Port");
    userField.box.addStyleName("topMargin");
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("hydraPort");
    userField.box.setWidth("50px");
    if (currentPort > 0)
      userField.box.setText("" + currentPort);
    return userField;
  }

  private FormField getModel(Fieldset fieldset, String model) {
    FormField userField =
        addControlFormFieldWithPlaceholder(fieldset, false, 5, 100, "Language Model");
    //  userField.box.addStyleName("topMargin");
    userField.box.addStyleName("rightFiveMargin");
    userField.box.getElement().setId("languageModel");
    userField.box.setWidth("366px");
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

    w.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
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
      }
    });
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

  private boolean changed() {
    return (info.getStatus() != ProjectStatus.valueOf(statusBox.getValue()));
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
    statusBox.setSelectedIndex(found ? i : 0);
  }
}
