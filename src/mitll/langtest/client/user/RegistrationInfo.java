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

  public RegistrationInfo(Fieldset toAddTo) {
    genderGroup = getListBoxFormFieldNoLabel(toAddTo, "Gender", getGenderBox());
    genderGroup.box.setWidth("100px");
    genderGroup.box.addStyleName("topMargin");
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
   * @see mitll.langtest.client.user.UserPassLogin#getSignUpButton(com.github.gwtbootstrap.client.ui.base.TextBoxBase, com.github.gwtbootstrap.client.ui.base.TextBoxBase)
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
