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

package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DropdownBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.Navigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.ExerciseOptions;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/12/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class NPFExercise<T extends CommonExercise> extends GoodwaveExercisePanel<T> {
  private final Logger logger = Logger.getLogger("NPFExercise");

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

  private DropdownButton addToList;
  private int activeCount = 0;
  private final PopupContainerFactory popupContainer = new PopupContainerFactory();
  private Collection<UserList<CommonShell>> listsForUser = Collections.emptyList();

  /**
   * @param e
   * @param controller
   * @param listContainer
   * @param screenPortion
   * @param addKeyHandler
   * @param instance
   * @param allowRecording
   * @see mitll.langtest.client.custom.content.NPFHelper#setFactory(mitll.langtest.client.list.PagingExerciseList, String, boolean)
   */
  NPFExercise(T e, ExerciseController controller, ListInterface<CommonShell> listContainer,
              ExerciseOptions options
              //float screenPortion,
              //boolean addKeyHandler, String instance, boolean allowRecording
  ) {
    super(e, controller, listContainer, options);
//        new ExerciseOptions(screenPortion, addKeyHandler, instance, allowRecording, true));
  }

  /**
   * @param controller
   * @param listContainer
   * @param addKeyHandler
   * @param includeListButtons
   * @return
   * @see #GoodwaveExercisePanel
   */
  @Override
  protected NavigationHelper<CommonShell> getNavigationHelper(ExerciseController controller,
                                                              ListInterface<CommonShell> listContainer,
                                                              boolean addKeyHandler, boolean includeListButtons) {
    NavigationHelper<CommonShell> navigationHelper =
        super.getNavigationHelper(controller, listContainer, addKeyHandler, includeListButtons);

    if (includeListButtons) {
      addToList = new DropdownButton(ADD_TO_LIST);
      navigationHelper.add(makeAddToList(getLocalExercise().getID(), controller, addToList));
      navigationHelper.add(getNextListButton());
    }
    return navigationHelper;
  }

  /**
   * @param exercise
   * @param controller
   * @return
   * @see GoodwaveExercisePanel#getNavigationHelper
   */
  private Panel makeAddToList(int exercise, ExerciseController controller, DropdownButton addToList) {
    addToList.getElement().setId("NPFExercise_AddToList");
    addToList.setDropup(true);
    addToList.setIcon(IconType.PLUS_SIGN);
    addToList.setType(ButtonType.PRIMARY);
    addTooltip(addToList, ADD_ITEM);
    addToList.addStyleName("leftFiveMargin");
    populateListChoices(exercise, controller, addToList);
    return addToList;
  }

  /**
   * @return
   * @see #getNavigationHelper
   */
  private Widget getNextListButton() {
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
        boolean duplicateName = isDuplicateName(textBox.getText());
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
    controller.register(popupButton, exercise.getID(), "show new list");
  }

  private void makeANewList(TextBox textEntry) {
    String newListName = textEntry.getValue();
    if (!newListName.isEmpty()) {
      controller.logEvent(textEntry, "NewList_TextBox", exercise.getID(), "make new list called '" + newListName + "'");
      boolean duplicateName = isDuplicateName(newListName);
      if (duplicateName) {
        logger.info("---> not adding duplicate list " + newListName);
      } else {
        addUserList(newListName, textEntry);
      }
    }
  }

  private boolean isDuplicateName(String newListName) {
    boolean addIt = false;
    for (UserList ul : listsForUser) {
      if (ul.getName().equals(newListName)) {
        addIt = true;
        break;
      }
    }
    return addIt;
  }

  /**
   * @param title
   * @param textBox
   * @see #makeANewList(com.github.gwtbootstrap.client.ui.TextBox)
   */
  private void addUserList(String title, final TextBox textBox) {
//    logger.info("user " + userID + " adding list " + title);
    boolean isPublic = !controller.getUserState().getCurrent().isStudent();
    listService.addUserList(
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
              wasRevealed();
            }
          }
        }
    );
  }

  /**
   * Every time this panel becomes visible again, we need to check the lists for this user.
   *
   * @see Navigation#getTabPanel()
   */
  @Override
  public void wasRevealed() {
    populateListChoices(exercise.getID(), controller, addToList);
  }

  /**
   * Ask server for the set of current lists for this user.
   * <p>
   * TODO : do this better -- tell server to return lists that don't have exercise in them.
   *
   * @param id
   * @param controller
   * @param w1
   * @see #makeAddToList
   * @see #wasRevealed()
   */
  private void populateListChoices(final int id, final ExerciseController controller, final DropdownBase w1) {
    listService.getListsForUser(true, false, new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        listsForUser = result;
        w1.clear();
        activeCount = 0;
        boolean anyAdded = false;
        //    logger.info("\tpopulateListChoices : found list " + result.size() + " choices");
        for (final UserList ul : result) {
          if (!ul.containsByID(id)) {
            activeCount++;
            anyAdded = true;
            final NavLink widget = new NavLink(ul.getName());
            w1.add(widget);
            widget.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                controller.logEvent(w1, "DropUp", id, ADDING_TO_LIST + ul.getID() + "/" + ul.getName());

                listService.addItemToUserList(ul.getID(), id, new AsyncCallback<Void>() {
                  @Override
                  public void onFailure(Throwable caught) {
                  }

                  @Override
                  public void onSuccess(Void result) {
                    popupContainer.showPopup(ITEM_ADDED, w1);
                    widget.setVisible(false);
                    activeCount--;
                    if (activeCount == 0) {
                      NavLink widget = new NavLink(ITEM_ALREADY_ADDED);
                      w1.add(widget);
                    }
                  }
                });
              }
            });
          }
        }
        if (!anyAdded) {
          NavLink widget = new NavLink(ITEM_ALREADY_ADDED);
          w1.add(widget);
        }
      }
    });
  }
}