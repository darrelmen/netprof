package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.client.user.UserDialog;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 1/17/17.
 */
public class ProjectEditForm extends UserDialog {
  private final EventRegistration eventRegistration;
  private final ProjectOps projectOps;
  private ListBox statusBox;
  private ProjectInfo info;
  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);

  /**
   * @param projectOps
   * @param props
   * @see
   */
  ProjectEditForm(ProjectOps projectOps, PropertyHandler props, EventRegistration eventRegistration) {
    super(props);
    this.projectOps = projectOps;
    this.eventRegistration = eventRegistration;
  }

  /**
   * @param info
   * @return
   * @see ProjectContainer#gotClickOnItem
   */
  Widget getForm(ProjectInfo info) {
    this.info = info;
    String name = info.getName();
    Heading heading = new Heading(3, name);
    heading.addStyleName("signUp");

    Button editProject = getEditButton();

    Fieldset fields = getFields(info, editProject);

    fields.add(editProject);

    return getTwoPartForm(heading, fields);
  }

  @NotNull
  private Button getEditButton() {
    Button editProject = getFormButton("editProject", "Edit", eventRegistration);
    editProject.setEnabled(false);
    editProject.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        gotClick();
      }
    });
    return editProject;
  }

  private void gotClick() {
    String value = statusBox.getValue();

    info.setStatus(ProjectStatus.valueOf(value));

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

  HTML feedback;

  private Fieldset getFields(ProjectInfo info, Button editButton) {
    Fieldset fieldset = new Fieldset();
    Heading id = new Heading(4, "ID", "" + info.getID());
    fieldset.add(id);

    addControlGroupEntrySimple(
        fieldset,
        "",
        statusBox = getBox(editButton))
        .setWidth(SIGN_UP_WIDTH);

    setBox(info.getStatus());

    Button w = new Button("Check Audio", IconType.STETHOSCOPE);
    fieldset.add(w);
    w.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        w.setEnabled(false);
        feedback.setText("Please wait...");

        projectServiceAsync.checkAudio(info.getID(), new AsyncCallback<Void>() {
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
    });
    feedback = new HTML();
    fieldset.add(feedback);

    return fieldset;
  }

  private ListBox getBox(final Button editButton) {
    ListBox affBox = new ListBox();
    affBox.getElement().setId("Status_Box");
    //  affBox.setWidth(SIGN_UP_WIDTH );
    affBox.addStyleName("leftTenMargin");

    for (ProjectStatus status : ProjectStatus.values()) {
      affBox.addItem(status.name());
    }
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
    //affBox.getElement().getStyle().setWidth(276, Style.Unit.PX);
    return affBox;
  }

  private boolean changed() {
    if (info.getStatus() != ProjectStatus.valueOf(statusBox.getValue())) {
      return true;
    } else {
      return false;
    }
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
