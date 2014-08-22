package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Panel;

/**
* Created by go22670 on 8/22/14.
*/
class RegistrationInfo extends BasicDialog {
  private final FormField ageEntryGroup;
  private final ListBoxFormField genderGroup;
  private final FormField dialectGroup;

  /**
   * @see mitll.langtest.client.user.StudentDialog#displayLoginBox()
   * @param dialogBox
   * @param lowerLeft
   */
  public RegistrationInfo(Panel dialogBox, Panel lowerLeft) {
    Form form = new Form();
    form.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);

    form.addStyleName("form-horizontal");

    Fieldset fieldsetLeft = new Fieldset();
    Fieldset fieldsetRight = new Fieldset();

    DivWidget divLeft = new DivWidget();
    divLeft.addStyleName("floatLeft");

    DivWidget divRight = new DivWidget();
    divRight.addStyleName("floatRight");

    divLeft.add(fieldsetLeft);
    form.add(divLeft);

    divRight.add(fieldsetRight);
    form.add(divRight);

    dialogBox.add(divLeft);
    dialogBox.add(divRight);

    genderGroup = getListBoxFormField(fieldsetRight, "Gender", getGenderBox());

    addControlGroupEntrySimple(fieldsetLeft, "Permissions", lowerLeft);
    ageEntryGroup = addControlFormField(fieldsetLeft, "Your age");
    dialectGroup = getDialect(fieldsetRight);
  }

  private FormField getDialect(Panel dialogBox) {
    final FormField dialectGroup = addControlFormField(dialogBox, StudentDialog.DIALECT);
    dialectGroup.group.addStyleName("topTwentyMargin");

    dialectGroup.box.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        if (dialectGroup.box.getText().length() > 0) {
          dialectGroup.group.setType(ControlGroupType.NONE);
        }
      }
    });
    return dialectGroup;
  }

  /**
   * @see #checkThenRegister(String, RegistrationInfo, com.github.gwtbootstrap.client.ui.Modal, String, java.util.Collection)
   * @return
   */
  public boolean checkValidGender() {
    boolean valid = !getGenderGroup().getValue().equals(UNSET);
    if (!valid) {
      getGenderGroup().markSimpleError(StudentDialog.CHOOSE_A_GENDER, Placement.LEFT);
    }
    return valid;
  }

  public FormField getAgeEntryGroup() {
    return ageEntryGroup;
  }

  public ListBoxFormField getGenderGroup() {
    return genderGroup;
  }

  public FormField getDialectGroup() {
    return dialectGroup;
  }
}
