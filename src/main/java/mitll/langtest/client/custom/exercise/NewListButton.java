package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.ListChangedEvent;
import mitll.langtest.client.scoring.UserListSupport;
import mitll.langtest.shared.custom.UserList;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Created by go22670 on 4/13/17.
 */
public class NewListButton {
  private final Logger logger = Logger.getLogger("NewListButton");

  private final UserListSupport userListSupport;

  private final int exid;
  private final ExerciseController controller;
  private final Widget dropdown;

  public NewListButton(int exid, ExerciseController controller, UserListSupport userListSupport,
                       Widget dropdown) {
    this.exid = exid;
    this.controller = controller;
    this.userListSupport = userListSupport;
    this.dropdown = dropdown;
  }

  private final PopupContainerFactory popupContainerFactory = new PopupContainerFactory();
  private PopupContainerFactory.HidePopupTextBox textBox;

  public DecoratedPopupPanel getNewListButton2() {
    final PopupContainerFactory.HidePopupTextBox textBox = getTextBoxForNewList();
    this.textBox = textBox;
    return popupContainerFactory.getPopup(textBox, event -> makeANewList(textBox));
  }

  public void showOrHide(PopupPanel popupPanel, UIObject popupButton) {
    popupContainerFactory.showOrHideRelative(popupPanel, popupButton, textBox, null);
  }

  @NotNull
  private PopupContainerFactory.HidePopupTextBox getTextBoxForNewList() {
    final PopupContainerFactory.HidePopupTextBox textBox = new PopupContainerFactory.HidePopupTextBox() {
      @Override
      protected void onEnter() {
        makeANewList(this);
      }
    };
    textBox.addKeyUpHandler(event ->
        textBox.getElement().getStyle().setColor(isDuplicateName(textBox.getText()) ? "red" : "black"));
    textBox.getElement().setId("NewList");
    textBox.setVisibleLength(60);
    return textBox;
  }

  private void makeANewList(TextBox textEntry) {
    String newListName = textEntry.getValue().trim();
    if (!newListName.isEmpty()) {
      controller.logEvent(textEntry, "NewList_TextBox", exid, "make new list called '" + newListName + "'");
      if (isDuplicateName(newListName)) {
        logger.info("---> not adding duplicate list " + newListName);
        popupContainerFactory.showPopup("Already list with that name.", dropdown);
      } else {
        addUserList(newListName, textEntry);
      }
    }
  }

  /**
   * @param title
   * @param textBox
   * @see #makeANewList(com.github.gwtbootstrap.client.ui.TextBox)
   */
  private void addUserList(String title, final TextBox textBox) {
//    logger.info("user " + userID + " adding list " + title);
    boolean isPublic = !controller.getUserState().getCurrent().isStudent();
    controller.getListService().addUserList(
        title,
        "",
        "",
        isPublic, new AsyncCallback<UserList>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(UserList result) {
            if (result == null) {
              logger.warning("should never happen!");
            } else {
              textBox.setText("");
              popupContainerFactory.showPopup("List " + title + " added!", dropdown);
              LangTest.EVENT_BUS.fireEvent(new ListChangedEvent());
            }
          }
        }
    );
  }

  private boolean isDuplicateName(String newListName) {
    return userListSupport.getKnownNamesForDuplicateCheck().contains(newListName);
  }
}
