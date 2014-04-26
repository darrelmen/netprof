package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.DropdownButton;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DropdownBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
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

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class NPFExercise extends GoodwaveExercisePanel {
  private static final String ADD_ITEM = "Add Item to List";
  private static final String ITEM_ALREADY_ADDED = "Item already added to your list(s)";

  private DropdownButton addToList;
  private int activeCount = 0;

  /**
   * @param e
   * @param controller
   * @param listContainer
   * @param screenPortion
   * @param addKeyHandler
   * @param instance
   * @see NPFHelper#setFactory(mitll.langtest.client.list.PagingExerciseList, String, boolean)
   */
  NPFExercise(CommonExercise e, ExerciseController controller, ListInterface listContainer, float screenPortion,
              boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
  }

  @Override
  protected NavigationHelper getNavigationHelper(ExerciseController controller,
                                                           ListInterface listContainer, boolean addKeyHandler) {
    NavigationHelper navigationHelper = super.getNavigationHelper(controller, listContainer, addKeyHandler);
    navigationHelper.add(makeAddToList(exercise, controller));
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
    addToList = new DropdownButton("Add to List");
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
    final HidePopupTextBox textBox = new HidePopupTextBox();
    textBox.getElement().setId("NewList");
    textBox.setVisibleLength(60);

    final DecoratedPopupPanel commentPopup = makePopupAndButton(textBox);

    final Button newListButton = new Button("New List");
    commentPopup.addAutoHidePartner(newListButton.getElement()); // fix for bug Wade found where click didn't toggle comment
    configureTextBox("", textBox, commentPopup);

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

    configurePopupButton(popupButton, popup, textEntry, addTooltip(popupButton, "Make a new list"));
  }

  protected void configurePopupButton(final Button popupButton, final PopupPanel popup, final TextBox textEntry, final Tooltip tooltip) {
    popupButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        boolean visible = popup.isShowing();

        if (visible) {// fix for bug that Wade found -- if we click off of popup, it dismisses it,
          // but if that click is on the button, it would immediately shows it again
          //System.out.println("popup visible " + visible);
          popup.hide();
        } else {
          popup.showRelativeTo(popupButton);
          textEntry.setFocus(true);
          tooltip.hide();
        }

        makeANewList(textEntry);
      }
    });
  }

  protected void makeANewList(TextBox textEntry) {
    String newListName = textEntry.getValue();
    controller.logEvent(textEntry, "NewList_TextBox", exercise.getID(), "make new list called '" + newListName +
      "'");
    boolean addIt = !newListName.isEmpty();
    for (UserList ul : listsForUser) {
      if (ul.getName().equals(newListName)) {
        addIt = false; break;
      }
    }
    if (addIt) {
      addUserList(controller.getUser(), newListName);
    }
  }

  /**
   * @seex #getEntry(String, com.google.gwt.user.client.ui.Widget, mitll.langtest.shared.ExerciseAnnotation)
   * @paramx xfield
   * @paramx commentButton
   * @param commentEntryText
   * @paramx clearButton
   * @return
   */
  private DecoratedPopupPanel makePopupAndButton(TextBox commentEntryText) {
    final DecoratedPopupPanel commentPopup = new DecoratedPopupPanel();
    commentPopup.setAutoHideEnabled(true);

    Panel hp = new HorizontalPanel();
    hp.add(commentEntryText);
    hp.add(getOKButton(commentPopup));

    commentPopup.add(hp);
    return commentPopup;
  }

  /**
   * Clicking OK just dismisses the popup.
   * @param commentPopup
   * @return
   */
  protected Button getOKButton(final PopupPanel commentPopup) {
    Button ok = new Button("OK");
    ok.setType(ButtonType.PRIMARY);
    ok.addStyleName("leftTenMargin");
    ok.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentPopup.hide();
      }
    });
    return ok;
  }

  private void addUserList(long userID, String title) {
    service.addUserList(userID,
      title,
      "",
      "", new AsyncCallback<Long>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Long result) {
          if (result == -1) {
            System.err.println("should never happen!");
          } else {
            wasRevealed();
          }
        }
      }
    );
  }

  /**
   * Every time this panel becomes visible again, we need to check the lists for this user.
   *
   * @see Navigation#getTabPanel(com.google.gwt.user.client.ui.Panel)
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
        //System.out.println("\tpopulateListChoices : found list " + result.size() + " choices");
        for (final UserList ul : result) {
          if (!ul.contains(new UserExercise(e))) {
            activeCount++;
            anyAdded = true;
            final NavLink widget = new NavLink(ul.getName());
            w1.add(widget);
            widget.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                controller.logEvent(w1,"DropUp",e.getID(),"Adding to list " + ul.getID() +"/"+ul.getName());
                service.addItemToUserList(ul.getUniqueID(), new UserExercise(e, controller.getUser()), new AsyncCallback<Void>() {
                  @Override
                  public void onFailure(Throwable caught) {
                  }

                  @Override
                  public void onSuccess(Void result) {
                    showPopup("Item Added!", w1);
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

  private void showPopup(String html, Widget target) {
    Widget content = new HTML(html);
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(content);
    pleaseWait.showRelativeTo(target);
    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(2000);
  }

  /**
   * For this field configure the textBox box to post annotation on blur and enter
   *
   * @param currentComment fill in with existing annotation, if there is one
   * @param textBox comment box to configure
   * @return
   */
  protected FocusWidget configureTextBox(String currentComment,
                                         final HidePopupTextBox textBox,
                                         final PopupPanel popup) {
    if (currentComment != null) {
      textBox.setText(currentComment);
      if (textBox.getVisibleLength() < currentComment.length()) {
        textBox.setVisibleLength(70);
      }
    }

    textBox.addStyleName("leftFiveMargin");
    textBox.configure(popup);

    return textBox;
  }

  protected static class HidePopupTextBox extends TextBox {
    public void configure( final PopupPanel popup) {
      addKeyPressHandler(new KeyPressHandler() {
        @Override
        public void onKeyPress(KeyPressEvent event) {
          int keyCode = event.getNativeEvent().getKeyCode();
          if (keyCode == KeyCodes.KEY_ENTER) {
            popup.hide();
          }
        }
      });
    }
  }
}
