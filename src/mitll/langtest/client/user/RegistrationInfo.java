/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;

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
  private final RadioButton male = new RadioButton(GENDER_GROUP, "Male");
  private final RadioButton female = new RadioButton(GENDER_GROUP, "Female");
  private final Panel genders;
  private static final Boolean ADD_AGE = false;

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


    if (ADD_AGE) {
      ageEntryGroup = addControlFormFieldWithPlaceholder(toAddTo, false, 2, 2, YOUR_AGE);
      ageEntryGroup.box.setWidth("88px");
      genders.add(ageEntryGroup.getGroup());
    } else {
      ageEntryGroup = new FormField(new TextBox(), new ControlGroup(), 0);
    }
    dialectGroup = getDialect(toAddTo);
  }

  public void setVisible(boolean visible) {
    genders.setVisible(visible);
    ageEntryGroup.setVisible(visible);
    dialectGroup.setVisible(visible);
  }

  public void hideAge() {
    ageEntryGroup.setVisible(false);
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
