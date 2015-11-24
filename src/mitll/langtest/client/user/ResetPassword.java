/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.user;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.github.gwtbootstrap.client.ui.Form;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.instrumentation.EventRegistration;

/**
 * Created by go22670 on 10/2/14.
 */
public class ResetPassword extends UserDialog {

  private static final int MIN_PASSWORD = 4;

  private static final String PASSWORD = "Password";

  private static final String PLEASE_ENTER_A_PASSWORD = "Please enter a password";
  private static final String PLEASE_ENTER_A_LONGER_PASSWORD = "Please enter a longer password";
  private static final String PLEASE_ENTER_THE_SAME_PASSWORD = "Please enter the same password";
  private static final String PASSWORD_HAS_BEEN_CHANGED = "Password has been changed";
  private static final String SUCCESS = "Success";
  private static final String CHANGE_PASSWORD = "Change Password";
  private static final String CHOOSE_A_NEW_PASSWORD = "Choose a new password";
  private final EventRegistration eventRegistration;
  private KeyPressHelper enterKeyButtonHelper;

  public ResetPassword(LangTestDatabaseAsync service, PropertyHandler props, EventRegistration eventRegistration) {
    super(service, props);
    this.eventRegistration = eventRegistration;
    enterKeyButtonHelper = new KeyPressHelper(false);
  }

  public Panel getResetPassword(final String token) {
    Panel container = new DivWidget();
    container.getElement().setId("ResetPassswordContent");

    DivWidget child = new DivWidget();
    container.add(child);
    child.addStyleName("loginPageBack");

    Panel leftAndRight = new DivWidget();
    leftAndRight.addStyleName("resetPage");
    container.add(leftAndRight);

    DivWidget right = new DivWidget();

    leftAndRight.add(right);
    right.addStyleName("floatRight");

    DivWidget rightDiv = new DivWidget();

    Form form = new Form();
    form.getElement().setId("resetForm");
    rightDiv.add(form);

    form.addStyleName("topMargin");
    form.addStyleName("formRounded");
    form.getElement().getStyle().setBackgroundColor("white");

    final Fieldset fieldset = new Fieldset();
    form.add(fieldset);

    Heading w = new Heading(3, CHOOSE_A_NEW_PASSWORD);
    fieldset.add(w);
    w.addStyleName("leftFiveMargin");
    final BasicDialog.FormField firstPassword  = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, PASSWORD);
    final BasicDialog.FormField secondPassword = addControlFormFieldWithPlaceholder(fieldset, true, MIN_PASSWORD, 15, "Confirm " + PASSWORD);

  //  firstPassword.getWidget().setTabIndex(0);
   // secondPassword.getWidget().setTabIndex(1);

    getChangePasswordButton(token, fieldset, firstPassword, secondPassword);
    right.add(rightDiv);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        firstPassword.getWidget().setFocus(true);
      }
    });
    return container;
  }

  private void getChangePasswordButton(final String token, Fieldset fieldset, final BasicDialog.FormField firstPassword, final BasicDialog.FormField secondPassword) {
    final Button changePassword = new Button(CHANGE_PASSWORD);
   // changePassword.setTabIndex(3);
    changePassword.getElement().setId("changePassword");
    eventRegistration.register(changePassword);
    changePassword.addStyleName("floatRight");
    fieldset.add(changePassword);
    changePassword.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String first = firstPassword.box.getText();
        String second = secondPassword.box.getText();
        if (first.isEmpty()) {
          markErrorBlur(firstPassword, PLEASE_ENTER_A_PASSWORD);
        } else if (first.length() < MIN_PASSWORD) {
          markErrorBlur(firstPassword, PLEASE_ENTER_A_LONGER_PASSWORD);
        } else if (second.isEmpty()) {
          markErrorBlur(secondPassword, PLEASE_ENTER_A_PASSWORD);
        } else if (second.length() < MIN_PASSWORD) {
          markErrorBlur(secondPassword, PLEASE_ENTER_A_LONGER_PASSWORD);
        } else if (!second.equals(first)) {
          markErrorBlur(secondPassword, PLEASE_ENTER_THE_SAME_PASSWORD);

        } else {
          changePassword.setEnabled(false);
          enterKeyButtonHelper.removeKeyHandler();
          service.changePFor(token, Md5Hash.getHash(first), new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) {
              changePassword.setEnabled(true);
              markErrorBlur(changePassword, "Can't communicate with server - check network connection.");
            }

            @Override
            public void onSuccess(Boolean result) {
              if (!result) {
                markErrorBlur(changePassword, "Password has already been changed?");
              } else {
                markErrorBlur(changePassword, SUCCESS, PASSWORD_HAS_BEEN_CHANGED, Placement.LEFT);
                Timer t = new Timer() {
                  @Override
                  public void run() {

                    String newURL = trimURL(Window.Location.getHref());
                    //  System.out.println("url now " +newURL);
                    Window.Location.replace(newURL);
                    Window.Location.reload();

                  }
                };
                t.schedule(3000);
              }
            }
          });
        }
      }
    });
    enterKeyButtonHelper.addKeyHandler(changePassword);

    changePassword.addStyleName("rightFiveMargin");
    changePassword.addStyleName("leftFiveMargin");

    changePassword.setType(ButtonType.PRIMARY);
  }

}
