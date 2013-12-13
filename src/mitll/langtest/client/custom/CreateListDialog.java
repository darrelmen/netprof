package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
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
    // fill in the middle panel with a form to allow you to create a list
    // post the results to the server
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

    final BasicDialog.FormField classBox = addControlFormField(row, "Class");
    row = new FluidRow();
    child.add(row);

    Button submit = new Button("Create List");
    submit.setType(ButtonType.PRIMARY);
    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        System.out.println("creating list for " + titleBox + " " + area.getText() + " and " + classBox.getText());
        enterKeyButtonHelper.removeKeyHandler();
        // TODO : validate

        if (validateCreateList(titleBox, description, classBox)) {
          service.addUserList(userManager.getUser(), titleBox.getText(), area.getText(),
            classBox.getText(), new AsyncCallback<Long>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Long result) {
              navigation.setUserListID(result);
              System.out.println("userListID " + navigation.getUserListID());
              navigation.showInitialState();
              // TODO : show enter item panel
            }
          });
        }
      }
    });
    enterKeyButtonHelper.addKeyHandler(submit);
    row.add(submit);
  }


  void zeroPadding(Panel createContent) {
    DOM.setStyleAttribute(createContent.getElement(), "paddingLeft", "0px");
    DOM.setStyleAttribute(createContent.getElement(), "paddingRight", "0px");
  }


  private boolean validateCreateList(BasicDialog.FormField titleBox, BasicDialog.FormField description, BasicDialog.FormField classBox) {
    if (titleBox.getText().isEmpty()) {
      markError(titleBox, "Please fill in a title");
      return false;
    } else if (description.getText().isEmpty()) {
      markError(description, "Please fill in a description");
      return false;
    } else if (classBox.getText().isEmpty()) {
      markError(classBox, "Please fill in a class");
      return false;
    }
    return true;
  }
}