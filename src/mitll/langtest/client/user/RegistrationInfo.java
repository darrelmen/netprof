/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Fieldset;
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
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/22/14.
 */
class RegistrationInfo extends BasicDialog {
  private static final String YOUR_AGE = "Your age";
  private static final String GENDER_GROUP = "GenderGroup";
  private static final String CHOOSE_A_GENDER = "Choose male or female.";
  private static final String DIALECT = "Dialect";

  private final FormField ageEntryGroup;
  private final FormField dialectGroup;
  private final RadioButton male   = new RadioButton(GENDER_GROUP, "Male");
  private final RadioButton female = new RadioButton(GENDER_GROUP, "Female");
  private final Panel genders;
  private static final Boolean ADD_AGE = false;

  /**
   * @see UserPassLogin#makeRegistrationInfo
   * @param toAddTo
   */
  RegistrationInfo(ComplexWidget toAddTo) {
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

  void hideAge() {
    ageEntryGroup.setVisible(false);
  }

  private FormField getDialect(Panel dialogBox) {
    final FormField dialectGroup = addControlFormFieldWithPlaceholder(dialogBox, false, 3, 25, DIALECT);
    dialectGroup.box.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        if (!dialectGroup.getSafeText().isEmpty()) {
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
  boolean checkValidGender() {
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

  FormField getAgeEntryGroup() {
    return ageEntryGroup;
  }

  FormField getDialectGroup() {
    return dialectGroup;
  }

  public RadioButton getMale() {
    return male;
  }

  public RadioButton getFemale() {
    return female;
  }
}
