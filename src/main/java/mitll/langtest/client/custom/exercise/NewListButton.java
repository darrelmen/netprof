package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.custom.UserList;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Created by go22670 on 4/13/17.
 */
public class NewListButton {

  private final Logger logger = Logger.getLogger("NewListButton");

  public static final String MAKE_A_NEW_LIST = "Make a new list";

  private static final String ADD_ITEM = "Add Item to List";
  private static final String ITEM_ALREADY_ADDED = "Item already added to your list(s)";
  private static final String ADD_TO_LIST = "Add to List";
  /**
   * @see #getNextListButton
   */
  private static final String NEW_LIST = "New List";
  private static final String ITEM_ADDED = "Item Added!";
  private static final String ADDING_TO_LIST = "Adding to list ";

  int exid;
  ExerciseController controller;

  public NewListButton(int exid, ExerciseController controller) {
    this.exid = exid;
    this.controller = controller;
  }

  /**
   * @return
   * @see #getNavigationHelper
   */
  private Widget getNewListButton() {
    String buttonTitle = NEW_LIST;

    final PopupContainerFactory.HidePopupTextBox textBox = getTextBoxForNewList();

    final Button newListButton = new Button(buttonTitle);
    configureNewListButton(newListButton);

    Tooltip tooltip = addTooltip(newListButton, MAKE_A_NEW_LIST);
    // final DecoratedPopupPanel thePopup =
    new PopupContainerFactory().makePopupAndButton(textBox, newListButton, tooltip, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        makeANewList(textBox);
      }
    });

    return newListButton;
  }

  @NotNull
  private PopupContainerFactory.HidePopupTextBox getTextBoxForNewList() {
    final PopupContainerFactory.HidePopupTextBox textBox = new PopupContainerFactory.HidePopupTextBox() {
      @Override
      protected void onEnter() {
        makeANewList(this);
      }
    };
    textBox.addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        boolean duplicateName = false;//isDuplicateName(textBox.getText());
        if (duplicateName) {
          textBox.getElement().getStyle().setColor("red");
        } else {
          textBox.getElement().getStyle().setColor("black");
        }
      }
    });
    textBox.getElement().setId("NewList");
    textBox.setVisibleLength(60);
    return textBox;
  }

  /**
   * @param popupButton
   * @return
   */
  private void configureNewListButton(final Button popupButton) {
    popupButton.setIcon(IconType.LIST_UL);
    popupButton.setType(ButtonType.PRIMARY);
    popupButton.addStyleName("leftFiveMargin");
    popupButton.getElement().setId("NPFExercise_popup");
    controller.register(popupButton, exid, "show new list");
  }

  private void makeANewList(TextBox textEntry) {
    String newListName = textEntry.getValue();
    if (!newListName.isEmpty()) {
      controller.logEvent(textEntry, "NewList_TextBox", exid, "make new list called '" + newListName + "'");
//      boolean duplicateName = isDuplicateName(newListName);
//      if (duplicateName) {
//        logger.info("---> not adding duplicate list " + newListName);
//      } else {
      addUserList(newListName, textEntry);
      //}
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
        isPublic, new AsyncCallback<Long>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Long result) {
            if (result == -1) {
              logger.warning("should never happen!");
            } else {
              textBox.setText("");
              //wasRevealed();
            }
          }
        }
    );
  }

/*  private boolean isDuplicateName(String newListName) {
    boolean addIt = false;
    for (UserList ul : listsForUser) {
      if (ul.getName().equals(newListName)) {
        addIt = true;
        break;
      }
    }
    return addIt;
  }*/


  protected Tooltip addTooltip(Widget w, String tip) {
    return new TooltipHelper().addTooltip(w, tip);
  }
}
