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
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.shared.user.MiniUser;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/22/14.
 */
class RegistrationInfo extends BasicDialog {
  private static final String YOUR_AGE = "Your age";
  private static final String CHOOSE_A_GENDER = "Choose male or female.";
  private static final String DIALECT = "Dialect";
  private static final String PLEASE_ENTER_YOUR_AGE = "Please enter your age.";
  private static final String PLEASE_ENTER_A_VALID_AGE = "Please enter a valid age.";

  private final FormField ageEntryGroup;
  private FormField dialectGroup;

  /**
   *
   */
  private static final String GENDER_GROUP = "GenderGroup";
  private final RadioButton male = new RadioButton(GENDER_GROUP, "Male");
  private final RadioButton female = new RadioButton(GENDER_GROUP, "Female");

  private final Panel genders;
  private static final Boolean ADD_AGE = false;

  RegistrationInfo(ComplexWidget toAddTo, boolean includeDialect) {
    genders = new HorizontalPanel();
    genders.add(male);
    male.addStyleName("topFiveMargin");

    genders.add(female);
    genders.addStyleName("leftTenMargin");
    female.addStyleName("leftFiveMargin");
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

    if (includeDialect) {
      dialectGroup = getDialect(toAddTo);
    }
  }

  public void setVisible(boolean visible) {
    setGenderVisible(true);
    ageEntryGroup.setVisible(visible);
    if (dialectGroup != null) dialectGroup.setVisible(visible);
  }

  private void setGenderVisible(boolean visible) {
    genders.setVisible(visible);
  }

  public boolean isVisible() {
    return genders.isVisible();
  }

/*  void hideAge() {
    ageEntryGroup.setVisible(false);
  }*/

  private FormField getDialect(Panel dialogBox) {
    final FormField dialectGroup = addControlFormFieldWithPlaceholder(dialogBox, false, 3, 25, DIALECT);
    dialectGroup.box.addKeyUpHandler(event -> {
      if (!dialectGroup.getSafeText().isEmpty()) {
        dialectGroup.group.setType(ControlGroupType.NONE);
      }
    });
    return dialectGroup;
  }

  /**
   * If it's invisible, it's valid.
   *
   * @return
   * @see SignUpForm#isFormValid
   */
  boolean checkValid() {
    return !isVisible() || checkValidGender() && (!ADD_AGE || checkMissingAge());
  }

  boolean checkValidity() {
    return checkValidGender() && checkMissingAge();
  }

  /**
   * @return
   * @see #checkValidity
   */
  private boolean checkValidGender() {
    boolean valid = male.getValue() || female.getValue();
    if (!valid) {
      male.setFocus(true);
      setupPopover(male, TRY_AGAIN, CHOOSE_A_GENDER, Placement.LEFT, false, true);
    }
    return valid;
  }

  private boolean checkMissingAge() {
    if (ageEntryGroup.isVisible()) {
      if (ageEntryGroup.isEmpty()) {
        ageEntryGroup.box.setFocus(true);
        markErrorBlur(ageEntryGroup, "Add info", PLEASE_ENTER_YOUR_AGE, Placement.TOP, true);
        return false;
      } else {
        try {
          Integer.parseInt(ageEntryGroup.getSafeText());
          return true;
        } catch (NumberFormatException e) {
          markErrorBlur(ageEntryGroup, "Try again", PLEASE_ENTER_A_VALID_AGE, Placement.TOP, true);
          return false;
        }
      }
    } else {
      return true;
    }
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

  void addFocusHandler(FocusHandler f) {
    male.addFocusHandler(f);
    female.addFocusHandler(f);
  }
/*  private RadioButton getMale() {
    return male;
  }

  private RadioButton getFemale() {
    return female;
  }*/

  public void setGender(MiniUser.Gender gender) {
    if (gender == MiniUser.Gender.Male) male.setValue(true);
    else if (gender == MiniUser.Gender.Female) female.setValue(true);
  }
}
