/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
import mitll.langtest.shared.custom.QuizSpec;
import mitll.langtest.shared.custom.UserList;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/13/17.
 *
 * @see UserListSupport#addListOptions
 */
public class NewListButton {
  private final Logger logger = Logger.getLogger("NewListButton");

  private final UserListSupport userListSupport;

  private final int exid;
  private final ExerciseController controller;
  private final Widget dropdown;

  private final PopupContainerFactory popupContainerFactory = new PopupContainerFactory();
  private PopupContainerFactory.HidePopupTextBox textBox;

  /**
   * @param exid
   * @param controller
   * @param userListSupport
   * @param dropdown
   * @see UserListSupport#addListOptions
   */
  public NewListButton(int exid, ExerciseController controller, UserListSupport userListSupport,
                       Widget dropdown) {
    this.exid = exid;
    this.controller = controller;
    this.userListSupport = userListSupport;
    this.dropdown = dropdown;
  }

  /**
   * @see UserListSupport#addNewListChoice
   * @return
   */
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

  /**
   * @see #getNewListButton2()
   * @see #getTextBoxForNewList
   * @param textEntry
   */
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
        isPublic,
        UserList.LIST_TYPE.NORMAL, 100, new QuizSpec(), new HashMap<>(), new AsyncCallback<UserList>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("adding a new list", caught);
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
