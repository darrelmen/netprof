package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
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
   * @seex mitll.langtest.client.user.StudentDialog#displayLoginBox()
   * @paramx dialogBox
   * @paramx lowerLeft
   */
/*  public RegistrationInfo(Panel dialogBox, Panel lowerLeft) {
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

    //addControlGroupEntrySimple(fieldsetLeft, "Permissions", lowerLeft);
    ageEntryGroup = addControlFormField(fieldsetLeft, "Your age");
    dialectGroup = getDialect(fieldsetRight);
  }*/

  public RegistrationInfo(Fieldset toAddTo) {
    genderGroup = getListBoxFormFieldNoLabel(toAddTo, "Gender", getGenderBox());
    genderGroup.box.setWidth("100px");
    genderGroup.box.addStyleName("topMargin");
    //addControlGroupEntrySimple(fieldsetLeft, "Permissions", lowerLeft);
    ageEntryGroup = addControlFormFieldWithPlaceholder(toAddTo, false,2,2,"Your age");
    ageEntryGroup.box.setWidth("88px");
    dialectGroup = getDialect(toAddTo);
  }

  public void setVisible(boolean visible) {
    genderGroup.setVisible(visible);
    ageEntryGroup.setVisible(visible);
    dialectGroup.setVisible(visible);
  }

  private FormField getDialect(Panel dialogBox) {
    final FormField dialectGroup = addControlFormFieldWithPlaceholder(dialogBox, false, 3, 25, DIALECT);
//    dialectGroup.group.addStyleName("topTwentyMargin");

    dialectGroup.box.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        if (dialectGroup.box.getText().length() > 0) {
          dialectGroup.group.setType(ControlGroupType.NONE);
        }
      }
    });
    return dialectGroup;
  }
  private static final String CHOOSE_A_GENDER = "Choose a gender.";
  private static final String DIALECT = "Dialect";

  /**
   * @see #checkThenRegister(String, RegistrationInfo, com.github.gwtbootstrap.client.ui.Modal, String, java.util.Collection)
   * @return
   */
  public boolean checkValidGender() {
    boolean valid = !getGenderGroup().getValue().equals(UNSET);
    if (!valid) {
      getGenderGroup().markSimpleError(CHOOSE_A_GENDER, Placement.LEFT);
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
