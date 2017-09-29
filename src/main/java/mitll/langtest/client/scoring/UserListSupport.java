package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.DropdownSubmenu;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DropdownBase;
import com.google.gwt.dom.client.Style;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.exercise.NewListButton;
import mitll.langtest.client.custom.exercise.PopupContainerFactory;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/19/17.
 */
public class UserListSupport {
 private final Logger logger = Logger.getLogger("UserListSupport");

  private static final String ADD_TO_LIST = "Add to List";
  private static final String REMOVE_FROM_LIST = "Remove from List";
  private static final String NEW_LIST = "New List";
  private static final String NOT_ON_ANY_LISTS = "Not on any lists.";
  private static final String EMAIL_LIST = "Email List";
  private static final String EMAIL_THESE_ITEMS = "Email these items";
  private final PopupContainerFactory popupContainer = new PopupContainerFactory();

  private static final int END_INDEX = 15;
  private final ExerciseController controller;
  private static final String ITEM_ALREADY_ADDED = "Item already added.";
  private static final String ITEM_ADDED = "Item Added!";
  private final Set<String> knownNamesForDuplicateCheck = new HashSet<>();

  public UserListSupport(ExerciseController controller) {
    this.controller = controller;
  }

  /**
   * @param dropdownContainer
   * @param exid
   * @see TwoColumnExercisePanel#getDropdown
   */
  void addListOptions(Dropdown dropdownContainer, int exid) {
    DropdownSubmenu addToList = new DropdownSubmenu(ADD_TO_LIST);
    //  addToList.setRightDropdown(true);
    //  addToList.setStyleDependentName("pull-left", true);

    DropdownSubmenu removeFromList = new DropdownSubmenu(REMOVE_FROM_LIST);
    removeFromList.setRightDropdown(true);

    DropdownSubmenu sendList = new DropdownSubmenu(EMAIL_LIST);
    sendList.setRightDropdown(true);

    dropdownContainer.addClickHandler(event -> populateListChoices(exid, addToList, removeFromList, sendList, dropdownContainer));

    dropdownContainer.add(addToList);
    dropdownContainer.add(removeFromList);
    dropdownContainer.add(sendList);

    UserListSupport outer = this;

    {
      NavLink widget = new NavLink(NEW_LIST);
      widget.addClickHandler(event -> {
        NewListButton newListButton = new NewListButton(exid, controller, outer, dropdownContainer);
        newListButton.showOrHide(newListButton.getNewListButton2(), widget);
      });
      dropdownContainer.add(widget);
    }
  }

  /**
   * Ask server for the set of current lists for this user.
   * <p>
   * TODO : do this better -- tell server to return lists that don't have exercise in them.
   * <p>
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
                                   final DropdownBase emailList,
                                   Dropdown container
  ) {
    //  logger.info("asking for " + id );
    controller.getListService().getListsForUser(true, true, new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        useLists(result, addToList, removeFromList, emailList, id, container);
        //   addSendLinkWhatYouSee(container);
      }
    });
  }

  private void useLists(Collection<UserList<CommonShell>> result,
                        DropdownBase addToList,
                        DropdownBase removeFromList, DropdownBase emailList, int id, Dropdown container) {
    addToList.clear();
    removeFromList.clear();
    emailList.clear();

    boolean anyAdded = false;
    boolean anyToRemove = false;
    int user = controller.getUser();
    for (final UserList ul : result) {
      boolean isMyList = ul.getUserID() == user;
      if (isMyList) {
        knownNamesForDuplicateCheck.add(ul.getName().trim().toLowerCase());
        if (!ul.containsByID(id)) {
          anyAdded = true;
          getAddListLink(ul, addToList, id, container);
        } else {
          anyToRemove = true;
          getRemoveListLink(ul, removeFromList, id, container);
        }
      }

      addSendLink(ul, emailList, isMyList);
    }
    if (!anyAdded) {
      addToList.add(new NavLink(ITEM_ALREADY_ADDED));
    }
    if (!anyToRemove) {
      removeFromList.add(new NavLink(NOT_ON_ANY_LISTS));
    }
  }

  private void addSendLink(UserList ul, DropdownBase addToList, boolean isMyList) {
    final NavLink widget = getListLink(ul);
    if (!isMyList) {
      widget.getElement().getStyle().setFontStyle(Style.FontStyle.ITALIC);
    }
    addToList.add(widget);
    widget.setHref(getMailToList(ul));
    widget.addClickHandler(event -> controller.logEvent(addToList, "DropUp", ul.getID(), "sharing_" + ul.getID() + "/" + ul.getName()));
  }

  @NotNull
  String getMailToExercise(CommonExercise exercise) {
    String s = trimURL(Window.Location.getHref()) +
        "#" +
        SelectionState.SECTION_SEPARATOR + "search=" + exercise.getID() +
        getProjectParam();

    String encode = URL.encode(s);
    return "mailto:" +
        "?" +
        "Subject=Share netprof " + controller.getLanguage() +
        " item " + exercise.getEnglish() +
        "&body=" +
        getPrefix() + exercise.getEnglish() + "/" + exercise.getForeignLanguage() + " : " +
        encode +
        getSuffix();
  }
/*  @NotNull
  String getMailToExerciseTrouble(CommonExercise exercise) {
    String theURL = trimURL(Window.Location.getHref())
        //+
        //"#" +
        //SelectionState.SECTION_SEPARATOR + "search=" + exercise.getID() +
        //getProjectParam()
        ;

    String encodedURL = theURL;//URL.encode(theURL);
    //  String anchor = "<a href='" + encode+ "'>" +encode+"</a>";
    String english = exercise.getEnglish();
    String foreignLanguage = exercise.getForeignLanguage();
    String rawMailTO = "mailto:" +
        "?" +
        "Subject=Share netprof " + controller.getLanguage() +
        " item " + english +
        "&body=" +
        //getPrefix()
        "Hi,\n Here's a link to NetProF item\n\n"
        + english + "/" + foreignLanguage + " : " +
        //"\""+
        // "<"+
        theURL +
        // "/>"+
        //"\""+
        "\n\nThanks,\n"+
        getFullName()
        //getSuffix()
        ;
    String encode = URL.encode(rawMailTO);
    encode = encode.replaceAll("#","%23");
    logger.info("raw    " + rawMailTO);
    logger.info("encode " + encode);

    return encode;
  }*/

  public String getMailToList(UserList ul) {  return getMailTo(ul.getID(), ul.getName());  }

  @NotNull
  private String getMailTo(int listid, String name) {
    String s = trimURL(Window.Location.getHref()) +
        "#" +
        SelectionState.SECTION_SEPARATOR + "Lists=" + listid +
        getProjectParam();

    String encode = URL.encode(s);
    return "mailto:" +
        "?" +
        "Subject=Share netprof " + controller.getLanguage() +
        " list " + name +
        "&body=" +
        getPrefix() + name + " list : " + encode + getSuffix();
  }

  @NotNull
  private String getProjectParam() {
    return SelectionState.SECTION_SEPARATOR + SelectionState.PROJECT + "=" + controller.getProjectStartupInfo().getProjectid();
  }

  void addSendLinkWhatYouSee(DropdownBase addToList) {
    final NavLink widget = new NavLink(EMAIL_THESE_ITEMS);
    addToList.add(widget);
    widget.setHref(getMailToThese());
//    widget.addClickHandler(event -> controller.logEvent(addToList, "DropUp", ul.getID(), "sharing_" + ul.getID() + "/" + ul.getName()));
  }

  @NotNull
  private String getMailToThese() {
    String token = History.getToken();
    SelectionState selectionState = new SelectionState(token, false);
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();

    String s = trimURL(Window.Location.getHref()) +
        "#" +
        token +
        SelectionState.SECTION_SEPARATOR + "project=" + projectStartupInfo.getProjectid();

    String encode = URL.encode(s);
    return "mailto:" +
        "?" +
        "Subject=Share netprof " + controller.getLanguage() +
        " items " +
        "&body=" +
        getPrefix() +
        selectionState.getDescription(projectStartupInfo.getTypeOrder()) + " : " +
        encode +
        getSuffix();
  }

  @NotNull
  public String getPrefix() {
    return "Hi%2C%0A%20Here's%20a%20link%20to%20";
  }

  /**
   * @return
   */
  @NotNull
  public String getSuffix() {
    return "%0A%20Thanks%2C%20%0A%20" + getFullName();
  }

  private String getFullName() {
    return controller.getUserManager().getCurrent().getFullName();
  }

  private String trimURL(String url) {
    return url.split("\\?")[0].split("#")[0];
  }


  private void getAddListLink(UserList ul,
                              DropdownBase addToList,
                              int exid,
                              Widget container
  ) {
    final NavLink widget = getListLink(ul);
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

  @NotNull
  private NavLink getListLink(UserList ul) {
    String name = ul.getName();
    if (name.length() > END_INDEX) name = name.substring(0, END_INDEX) + "...";
    return new NavLink(name);
  }

  private void getRemoveListLink(UserList ul, DropdownBase removeFromList, int exid, Dropdown container) {
    final NavLink widget = getListLink(ul);
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
