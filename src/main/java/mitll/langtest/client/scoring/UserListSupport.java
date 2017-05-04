package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.DropdownSubmenu;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DropdownBase;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.exercise.NewListButton;
import mitll.langtest.client.custom.exercise.PopupContainerFactory;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by go22670 on 4/19/17.
 */
public class UserListSupport {
//  private final Logger logger = Logger.getLogger("UserListSupport");

  public static final String ADD_TO_LIST = "Add to List";
  public static final String REMOVE_FROM_LIST = "Remove from List";
  public static final String NEW_LIST = "New List";
  public static final String NOT_ON_ANY_LISTS = "Not on any lists.";
  private final PopupContainerFactory popupContainer = new PopupContainerFactory();

  private static final int END_INDEX = 15;
  private final ExerciseController controller;
  private static final String ITEM_ALREADY_ADDED = "Item already added.";
  private static final String ITEM_ADDED = "Item Added!";
  private final Set<String> knownNamesForDuplicateCheck = new HashSet<>();

  UserListSupport(ExerciseController controller) {
    this.controller = controller;
  }

  /**
   * @param dropdownContainer
   * @param exid
   */
  void addListOptions(Dropdown dropdownContainer, int exid) {
    DropdownSubmenu addToList = new DropdownSubmenu(ADD_TO_LIST);
    addToList.setRightDropdown(true);
    //  addToList.setStyleDependentName("pull-left", true);

    DropdownSubmenu removeFromList = new DropdownSubmenu(REMOVE_FROM_LIST);
    removeFromList.setRightDropdown(true);

    dropdownContainer.addClickHandler(event -> populateListChoices(exid, addToList, removeFromList, dropdownContainer));

    dropdownContainer.add(addToList);
    dropdownContainer.add(removeFromList);

    UserListSupport outer = this;

    NavLink widget = new NavLink(NEW_LIST);
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        NewListButton newListButton = new NewListButton(exid, controller, outer, dropdownContainer);
        newListButton.showOrHide(newListButton.getNewListButton2(), widget);
      }
    });
    dropdownContainer.add(widget);
  }

  /**
   * Ask server for the set of current lists for this user.
   * <p>
   * TODO : do this better -- tell server to return lists that don't have exercise in them.
   *
   * Visited are OK, I guess.
   *
   * @param id
   * @param addToList
   * @seex #makeAddToList
   * @seex #wasRevealed()
   */
  private void populateListChoices(final int id,
                                   final DropdownBase addToList,
                                   final DropdownBase removeFromList,
                                   Dropdown container
  ) {
    ListServiceAsync listService = controller.getListService();

  //  logger.info("asking for " + id );
    listService.getListsForUser(true, false, new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        addToList.clear();
        removeFromList.clear();

        boolean anyAdded = false;
        boolean anyToRemove = false;
        for (final UserList ul : result) {
          knownNamesForDuplicateCheck.add(ul.getName().trim().toLowerCase());
          if (!ul.containsByID(id)) {
            anyAdded = true;
            getAddListLink(ul, addToList, id, container);
          } else {
            anyToRemove = true;
            getRemoveListLink(ul, removeFromList, id, container);
          }
        }
        if (!anyAdded) {
          addToList.add(new NavLink(ITEM_ALREADY_ADDED));
        }
        if (!anyToRemove) {
          removeFromList.add(new NavLink(NOT_ON_ANY_LISTS));
        }
      }
    });
  }

  private void getAddListLink(UserList ul,
                              DropdownBase addToList,
                              int exid,
                              Widget container
  ) {
    String name = ul.getName();
    if (name.length() > END_INDEX) name = name.substring(0, END_INDEX) + "...";
    final NavLink widget = new NavLink(name);
    addToList.add(widget);
    widget.addClickHandler(event -> {
      controller.logEvent(addToList, "DropUp", exid, "adding_" + ul.getID() + "/" + ul.getName());

      controller.getListService().addItemToUserList(ul.getID(), exid, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Void result) {
          popupContainer.showPopup(ITEM_ADDED, container);
          LangTest.EVENT_BUS.fireEvent(new ListChangedEvent());
        }
      });
    });
  }

  private void getRemoveListLink(UserList ul, DropdownBase removeFromList, int exid,
                                 Dropdown container
  ) {
    final NavLink widget = new NavLink(ul.getName());
    removeFromList.add(widget);
    widget.addClickHandler(event -> {
      controller.logEvent(removeFromList, "DropUp", exid, "remove_" + ul.getID() + "/" + ul.getName());

      controller.getListService().deleteItemFromList(ul.getID(), exid, new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Boolean result) {
          popupContainer.showPopup(result ? "Item removed." : "Item *not* removed.", container);
          LangTest.EVENT_BUS.fireEvent(new ListChangedEvent());
        }
      });
    });
  }

  /**
   * @return
   */
  public Set<String> getKnownNamesForDuplicateCheck() {
    return knownNamesForDuplicateCheck;
  }
}
