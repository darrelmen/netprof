package mitll.langtest.client.custom.exercise;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.DropdownButton;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DropdownBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class NPFExercise extends GoodwaveExercisePanel {
  private final Logger logger = Logger.getLogger("NPFExercise");

  private static final String ADD_ITEM = "Add Item to List";
  private static final String ITEM_ALREADY_ADDED = "Item already added to your list(s)";
  private static final String ADD_TO_LIST = "Add to List";
  private static final String NEW_LIST = "New List";
  private static final String ITEM_ADDED = "Item Added!";
  private static final String ADDING_TO_LIST = "Adding to list ";
  private static final String PRACTICE = "Practice";

  private DropdownButton addToList;
  private int activeCount = 0;
  final PopupContainer popupContainer = new PopupContainer();

  /**
   * @param e
   * @param controller
   * @param listContainer
   * @param screenPortion
   * @param addKeyHandler
   * @param instance
   * @see mitll.langtest.client.custom.content.NPFHelper#setFactory(mitll.langtest.client.list.PagingExerciseList, String, boolean)
   */
  NPFExercise(CommonExercise e, ExerciseController controller, ListInterface listContainer, float screenPortion,
              boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
  }

  @Override
  protected NavigationHelper getNavigationHelper(ExerciseController controller,
                                                           ListInterface listContainer, boolean addKeyHandler) {
    NavigationHelper navigationHelper = super.getNavigationHelper(controller, listContainer, addKeyHandler);
    navigationHelper.add(makeAddToList(getLocalExercise(), controller));
    navigationHelper.add(getNextListButton());
    return navigationHelper;
  }

  /**
   * @see #getNavigationHelper(mitll.langtest.client.exercise.ExerciseController, mitll.langtest.client.list.ListInterface, boolean)
   * @param e
   * @param controller
   * @return
   */
  private Panel makeAddToList(CommonExercise e, ExerciseController controller) {
    addToList = new DropdownButton(ADD_TO_LIST);
    addToList.getElement().setId("NPFExercise_AddToList");
    addToList.setDropup(true);
    addToList.setIcon(IconType.PLUS_SIGN);
    addToList.setType(ButtonType.PRIMARY);
    addTooltip(addToList, ADD_ITEM);
    addToList.addStyleName("leftFiveMargin");
    populateListChoices(e, controller, addToList);
    return addToList;
  }

  private Widget getNextListButton() {
    final PopupContainer.HidePopupTextBox textBox = new PopupContainer.HidePopupTextBox() {
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
        }
        else {
          textBox.getElement().getStyle().setColor("black");
        }
      }
    });
    textBox.getElement().setId("NewList");
    textBox.setVisibleLength(60);

    PopupContainer popupContainer = new PopupContainer();
    final DecoratedPopupPanel commentPopup = popupContainer.makePopupAndButton(textBox, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        makeANewList(textBox);
      }
    });

    final Button newListButton = new Button(NEW_LIST);
    commentPopup.addAutoHidePartner(newListButton.getElement()); // fix for bug Wade found where click didn't toggle comment
    popupContainer.configureTextBox("", textBox, commentPopup);

    configureNewListButton(newListButton,
      commentPopup,
      textBox);
    return newListButton;
  }

  /**
   * @param popupButton
   * @param popup
   * @param textEntry
   * @return
   */
  private void configureNewListButton(final Button popupButton,
                                      final PopupPanel popup,
                                      final TextBox textEntry) {
    popupButton.setIcon(IconType.LIST_UL);
    popupButton.setType(ButtonType.PRIMARY);
    popupButton.addStyleName("leftFiveMargin");
    popupButton.getElement().setId("NPFExercise_popup");
    controller.register(popupButton, exercise.getID(), "show new list");

    new PopupContainer().configurePopupButton(popupButton, popup, textEntry, addTooltip(popupButton, "Make a new list"));
    popupButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        makeANewList(textEntry);

      }
    });
  }

  private void makeANewList(TextBox textEntry) {
    String newListName = textEntry.getValue();
    if (!newListName.isEmpty()) {
      controller.logEvent(textEntry, "NewList_TextBox", exercise.getID(), "make new list called '" + newListName + "'");
      boolean duplicateName = isDuplicateName(newListName);
      if (duplicateName) {
        logger.info("---> not adding duplicate list " + newListName);
      } else {
        addUserList(controller.getUser(), newListName, textEntry);
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
   * @see #makeANewList(com.github.gwtbootstrap.client.ui.TextBox)
   * @param userID
   * @param title
   * @param textBox
   */
  private void addUserList(long userID, String title, final TextBox textBox) {
    logger.info("user " +userID + " adding list " +title);
    String audioType = controller.getAudioType();
    boolean isStudent = audioType.equalsIgnoreCase(PRACTICE);
    service.addUserList(userID,
      title,
      "",
      "", !isStudent, new AsyncCallback<Long>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Long result) {
          if (result == -1) {
            System.err.println("should never happen!");
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
   * @see mitll.langtest.client.custom.Navigation#getTabPanel(com.google.gwt.user.client.ui.Panel)
   */
  @Override
  public void wasRevealed() { populateListChoices(exercise, controller, addToList);  }

  private Collection<UserList> listsForUser = Collections.emptyList();
  /**
   * Ask server for the set of current lists for this user.
   *
   * TODO : do this better -- tell server to return lists that don't have exercise in them.
   *
   * @param e
   * @param controller
   * @param w1
   * @see #makeAddToList(mitll.langtest.shared.CommonExercise, mitll.langtest.client.exercise.ExerciseController)
   * @see #wasRevealed()
   */
  private void populateListChoices(final CommonExercise e, final ExerciseController controller, final DropdownBase w1) {
    service.getListsForUser(controller.getUser(), true, false, new AsyncCallback<Collection<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Collection<UserList> result) {
        listsForUser = result;
        w1.clear();
        activeCount = 0;
        boolean anyAdded = false;
    //    logger.info("\tpopulateListChoices : found list " + result.size() + " choices");
        for (final UserList ul : result) {
          if (!ul.contains(new UserExercise(e))) {
            activeCount++;
            anyAdded = true;
            final NavLink widget = new NavLink(ul.getName());
            w1.add(widget);
            widget.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                controller.logEvent(w1,"DropUp",e.getID(), ADDING_TO_LIST + ul.getID() +"/"+ul.getName());
                service.addItemToUserList(ul.getUniqueID(), new UserExercise(e, controller.getUser()), new AsyncCallback<Void>() {
                  @Override
                  public void onFailure(Throwable caught) {}

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
