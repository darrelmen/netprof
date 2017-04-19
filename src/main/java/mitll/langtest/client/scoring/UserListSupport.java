package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.DropdownContainer;
import com.github.gwtbootstrap.client.ui.DropdownSubmenu;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DropdownBase;
import com.github.gwtbootstrap.client.ui.event.ShowEvent;
import com.github.gwtbootstrap.client.ui.event.ShowHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.Widget;
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
  private final PopupContainerFactory popupContainer = new PopupContainerFactory();
  ExerciseController controller;
  public static final String MAKE_A_NEW_LIST = "Make a new list";

  private static final String ADD_ITEM = "Add Item to List";
  private static final String ITEM_ALREADY_ADDED = "Item already added.";
  private static final String ADD_TO_LIST = "Add to List";
  /**
   * @seex #getNewListButton
   */
  private static final String NEW_LIST = "New List";
  private static final String ITEM_ADDED = "Item Added!";
  private static final String ADDING_TO_LIST = "Adding to list ";

  private Set<String> knownNames = new HashSet<>();

  UserListSupport(ExerciseController controller) {
    this.controller = controller;
  }

  public void addListOptions(//DropdownContainer dropdownContainer,
                             Dropdown dropdownContainer,
                             int exid) {
    DropdownSubmenu addToList = new DropdownSubmenu("Add to List");
    addToList.setRightDropdown(true);
    //  addToList.setStyleDependentName("pull-left", true);
    DropdownSubmenu removeFromList = new DropdownSubmenu("Remove from List");
    removeFromList.setRightDropdown(true);

  /*  dropdownContainer.addShowHandler(new ShowHandler() {
      @Override
      public void onShow(ShowEvent showEvent) {
        populateListChoices(exid, addToList, removeFromList, dropdownContainer);
      }
    });*/
    dropdownContainer.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        populateListChoices(exid, addToList, removeFromList, dropdownContainer);

      }
    });


    //  NavLink addToList = new NavLink("Add to List");
    dropdownContainer.add(addToList);
    dropdownContainer.add(removeFromList);

    UserListSupport outer = this;

    NavLink widget = new NavLink("New List");
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        NewListButton newListButton = new NewListButton(exid, controller, outer, dropdownContainer);
        DecoratedPopupPanel newListButton2 = newListButton.getNewListButton2();
        newListButton.showOrHide(newListButton2, widget);
      }
    });
    dropdownContainer.add(widget);
  }

  /**
   * Ask server for the set of current lists for this user.
   * <p>
   * TODO : do this better -- tell server to return lists that don't have exercise in them.
   *
   * @param id
   * @param addToList
   * @seex #makeAddToList
   * @seex #wasRevealed()
   */
  private void populateListChoices(final int id, final DropdownBase addToList, final DropdownBase removeFromList,
                                   // DropdownContainer container
                                   Dropdown container
  ) {
    ListServiceAsync listService = controller.getListService();
    listService.getListsForUser(true, false, new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        addToList.clear();
        removeFromList.clear();

        //  activeCount = 0;
        boolean anyAdded = false;
        boolean anyToRemove = false;
        //    logger.info("\tpopulateListChoices : found list " + result.size() + " choices");
        for (final UserList ul : result) {
          knownNames.add(ul.getName());
          if (!ul.containsByID(id)) {
            //    activeCount++;
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
          removeFromList.add(new NavLink("Not on any lists."));
        }
      }
    });
  }

  private void getAddListLink(UserList ul, DropdownBase addToList,
                              int exid,
                              // DropdownContainer container
                              Widget container
  ) {
    final NavLink widget = new NavLink(ul.getName());
    addToList.add(widget);
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(addToList, "DropUp", exid, "adding_" + ul.getID() + "/" + ul.getName());

        controller.getListService().addItemToUserList(ul.getID(), exid, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
            //container.hideContainer();
            popupContainer.showPopup(ITEM_ADDED, container);
            //widget.setVisible(false);
//            populateListChoices(exid, addToList, removeFromList, container);
            //          activeCount--;
            //        if (activeCount == 0) {
            //        NavLink widget = new NavLink(ITEM_ALREADY_ADDED);
            //      addToList.add(widget);
            //  }
          }
        });
      }
    });
  }

  private void getRemoveListLink(UserList ul, DropdownBase removeFromList, int exid,
                                 //DropdownContainer container
                                 Dropdown container
  ) {
    final NavLink widget = new NavLink(ul.getName());
    removeFromList.add(widget);
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(removeFromList, "DropUp", exid, "adding_" + ul.getID() + "/" + ul.getName());

        controller.getListService().deleteItemFromList(ul.getID(), exid, new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Boolean result) {
            //  container.hideContainer();
            popupContainer.showPopup(result ? "Item removed." : "Item *not* removed.", container);
            //widget.setVisible(false);

            //   widget.setVisible(false);
            //          activeCount--;
            //        if (activeCount == 0) {
            //        NavLink widget = new NavLink(ITEM_ALREADY_ADDED);
            //      addToList.add(widget);
            //  }
          }
        });
      }
    });
  }

  public Set<String> getKnownNames() {
    return knownNames;
  }
}
