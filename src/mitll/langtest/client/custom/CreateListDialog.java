package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.EnterKeyButtonHelper;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserManager;

public class CreateListDialog extends BasicDialog {
  private static final String CLASS = "Course Info";
  private static final boolean REQUIRE_DESC = false;
  private static final boolean REQUIRE_CLASS = false;

  private final Navigation navigation;
  private LangTestDatabaseAsync service;
  private UserManager userManager;

  public CreateListDialog(Navigation navigation, LangTestDatabaseAsync service,UserManager userManager) {
    this.navigation = navigation;
    this.service = service;
    this.userManager = userManager;
  }

  /**
   * @param thirdRow
   * @seex
   */
  void doCreate(Panel thirdRow) {
    thirdRow.clear();
    final EnterKeyButtonHelper enterKeyButtonHelper = new EnterKeyButtonHelper(true);
    Panel child = new DivWidget() {
      @Override
      protected void onUnload() {
        super.onUnload();
        enterKeyButtonHelper.removeKeyHandler();
      }
    };
    thirdRow.add(child);
    zeroPadding(child);
    child.addStyleName("userListContainer");

    FluidRow row = new FluidRow();
    child.add(row);
    final Heading header = new Heading(2, "Create a New List");
    row.add(header);

    row = new FluidRow();
    child.add(row);
    final BasicDialog.FormField titleBox = addControlFormField(row, "Title");

    row = new FluidRow();
    child.add(row);
    final TextArea area = new TextArea();
    final BasicDialog.FormField description = getFormField(row, "Description", area, 1);

    row = new FluidRow();
    child.add(row);

    final BasicDialog.FormField classBox = addControlFormField(row, CLASS);
    row = new FluidRow();
    child.add(row);

    Button submit = new Button("Create List");
    submit.setType(ButtonType.PRIMARY);

    DOM.setStyleAttribute(submit.getElement(), "marginBottom", "10px");

    submit.addStyleName("leftFiveMargin");
    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        //System.out.println("creating list for " + titleBox + " " + area.getText() + " and " + classBox.getText());
        enterKeyButtonHelper.removeKeyHandler();
        if (validateCreateList(titleBox, description, classBox)) {
          addUserList(titleBox, area, classBox);
        }
      }
    });
    enterKeyButtonHelper.addKeyHandler(submit);
    row.add(submit);

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        titleBox.box.setFocus(true);
      }
    });
  }

  public void addUserList(final FormField titleBox, TextArea area, FormField classBox) {
    service.addUserList(userManager.getUser(), titleBox.getText(), area.getText(),
      classBox.getText(), new AsyncCallback<Long>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Long result) {
        if (result == -1) {
          markError(titleBox,"You already have a list named "+ titleBox.getText());
        } else {
          navigation.showInitialState();
        }
      }
    });
  }

  void zeroPadding(Panel createContent) {
    DOM.setStyleAttribute(createContent.getElement(), "paddingLeft", "0px");
    DOM.setStyleAttribute(createContent.getElement(), "paddingRight", "0px");
  }

  private boolean validateCreateList(BasicDialog.FormField titleBox, BasicDialog.FormField description, BasicDialog.FormField classBox) {
    if (titleBox.getText().isEmpty()) {
      markError(titleBox, "Please fill in a title");
      return false;
    } else if (REQUIRE_DESC && description.getText().isEmpty()) {
      markError(description, "Please fill in a description");
      return false;
    } else if (REQUIRE_CLASS && classBox.getText().isEmpty()) {
      markError(classBox, "Please fill in course information");
      return false;
    }
    return true;
  }
}