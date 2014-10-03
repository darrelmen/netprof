package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by go22670 on 8/22/14.
 */
class RegistrationInfo extends BasicDialog {
  private static final String YOUR_AGE = "Your age";
  private static final String GENDER_GROUP = "GenderGroup";
  private static final String CHOOSE_A_GENDER = "Choose male or female.";
  private static final String DIALECT = "Dialect";

  private final FormField ageEntryGroup;
  private final FormField dialectGroup;
  private RadioButton male = new RadioButton(GENDER_GROUP, "Male");
  private RadioButton female = new RadioButton(GENDER_GROUP, "Female");
  private Panel genders;

  public RegistrationInfo(ComplexWidget toAddTo) {
    genders = new HorizontalPanel();
    genders.add(male);
    female.addStyleName("leftFiveMargin");
    genders.add(female);
    genders.addStyleName("leftTenMargin");

    male.addStyleName("topFiveMargin");
    female.addStyleName("topFiveMargin");
    toAddTo.add(genders);
  //  ageEntryGroup = addDecoratedControlFormFieldWithPlaceholder(toAddTo, false, 2, 2, YOUR_AGE);
    ageEntryGroup = addControlFormFieldWithPlaceholder(toAddTo, false, 2, 2, YOUR_AGE);
    ageEntryGroup.box.setWidth("88px");
    genders.add(ageEntryGroup.getGroup());

    dialectGroup = getDialect(toAddTo);
  }

  public void setVisible(boolean visible) {
    genders.setVisible(visible);
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

  /**
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#getSignUpButton(com.github.gwtbootstrap.client.ui.base.TextBoxBase, com.github.gwtbootstrap.client.ui.base.TextBoxBase)
   */
  public boolean checkValidGender() {
    boolean valid = male.getValue() || female.getValue();
    if (!valid) {
      male.setFocus(true);
      setupPopover(male, TRY_AGAIN, CHOOSE_A_GENDER, Placement.LEFT, false);
    }
    return valid;
  }

  public boolean isMale() {
    return male.getValue();
  }


  public FormField getAgeEntryGroup() {
    return ageEntryGroup;
  }

/*  public Widget getAgeEntryGroup() {
    return ageEntryGroup;
  }*/

  public FormField getDialectGroup() {
    return dialectGroup;
  }

  public RadioButton getMale() {
    return male;
  }

  public RadioButton getFemale() {
    return female;
  }
}
