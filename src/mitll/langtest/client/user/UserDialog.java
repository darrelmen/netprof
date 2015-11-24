/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.PasswordTextBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/10/13
 * Time: 4:26 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class UserDialog extends BasicDialog {
  protected static final int USER_ID_MAX_LENGTH = 35;

  public static final int MIN_AGE = 12;
  public static final int MAX_AGE = 90;
  static final int TEST_AGE = 100;
  final PropertyHandler props;
  final LangTestDatabaseAsync service;

  UserDialog(LangTestDatabaseAsync service, PropertyHandler props) {
    this.service = service;
    this.props = props;
  }

  protected FormField addControlFormField(Panel dialogBox, String label, boolean isPassword, int minLength, int maxLength, String hint) {
    final TextBox user = isPassword ? new PasswordTextBox() : new TextBox();
    user.setMaxLength(maxLength);
    return getFormField(dialogBox, label, user, minLength, hint);
  }

  private FormField getFormField(Panel dialogBox, String label, TextBox user, int minLength, String hint) {
    final ControlGroup userGroup = addControlGroupEntry(dialogBox, label, user, hint);
    return new FormField(user, userGroup, minLength);
  }

  protected void markError(FormField dialectGroup, String message) {
    markError(dialectGroup.group, dialectGroup.box, TRY_AGAIN, message, Placement.TOP);
  }

  protected void markErrorBlur(FormField dialectGroup, String message) {
    markErrorBlur(dialectGroup.group, dialectGroup.box, TRY_AGAIN, message, Placement.TOP);
  }

  protected String trimURL(String url) {
    if (url.contains("127.0.0.1")) return url.split("#")[0];
    else return url.split("\\?")[0].split("#")[0];
  }
}
