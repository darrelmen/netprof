package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.user.UserDialog;
import mitll.langtest.shared.project.ProjectInfo;
import mitll.langtest.shared.project.ProjectStatus;

/**
 * Created by go22670 on 1/17/17.
 */
public class ProjectEditForm extends UserDialog {
  private final EventRegistration eventRegistration;
  private ListBox statusBox;

  /**
   * @param props
   * @see
   */
  ProjectEditForm(PropertyHandler props, EventRegistration eventRegistration) {
    super(props);
    this.eventRegistration = eventRegistration;
  }

  public Panel getForm(ProjectInfo info) {
    String name = info.getName();
    Heading heading = new Heading(3, name);
    heading.addStyleName("signUp");
    Fieldset fields = getFields(info);

    Button editProject = getFormButton("editProject", "Edit", eventRegistration);
    editProject.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {

      }
    });
    fields.add(editProject);

    return getTwoPartForm(heading, fields);
  }

  private Fieldset getFields(ProjectInfo info) {
    Fieldset fieldset = new Fieldset();
    Heading id = new Heading(4,"ID", ""+info.getID());
    fieldset.add(id);

    addControlGroupEntrySimple(
        fieldset,
        "",
        statusBox = getBox())
        .setWidth(SIGN_UP_WIDTH);

    setBox(info.getStatus());

    return fieldset;
  }

  private ListBox getBox() {
    ListBox affBox = new ListBox();
    affBox.getElement().setId("Status_Box");
  //  affBox.setWidth(SIGN_UP_WIDTH );
    affBox.addStyleName("leftTenMargin");

    for (ProjectStatus status : ProjectStatus.values()) {
      affBox.addItem(status.name());
    }
//    affBox.addFocusHandler(new FocusHandler() {
//      @Override
//      public void onFocus(FocusEvent event) {
//
//      }
//    });
    affBox.getElement().getStyle().setWidth(276, Style.Unit.PX);
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
    statusBox.setSelectedIndex(found ? i : 0);
  }
}
