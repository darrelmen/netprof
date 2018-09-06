package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Divider;
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
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.exercise.NewListButton;
import mitll.langtest.client.custom.exercise.PopupContainerFactory;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.custom.IUserList;
import mitll.langtest.shared.custom.IUserListWithIDs;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by go22670 on 4/19/17.
 */
public class UserListSupport {
  //private final Logger logger = Logger.getLogger("UserListSupport");

  private static final String SHARE_NETPROF = "Share netprof ";

  private static final String ADD_TO_LIST = "Add to List";
  private static final String REMOVE_FROM_LIST = "Remove from List";
  private static final String NEW_LIST = "New List";
  private static final String NOT_ON_ANY_LISTS = "Not on any lists.";
  private static final String EMAIL = "Share";
  private static final String EMAIL_LIST = EMAIL + " List";
  /**
   * @see #addSendLinkWhatYouSee(DropdownBase)
   */
  private static final String SHARE_THESE_ITEMS = EMAIL + " these items";
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
   * @see ItemMenu#getDropdown
   */
  void addListOptions(Dropdown dropdownContainer, int exid) {
    DropdownSubmenu addToList = new DropdownSubmenu(ADD_TO_LIST);

    DropdownSubmenu removeFromList = new DropdownSubmenu(REMOVE_FROM_LIST);
    removeFromList.setRightDropdown(true);

    DropdownSubmenu sendList = new DropdownSubmenu(EMAIL_LIST);
    sendList.setRightDropdown(true);

    dropdownContainer.addClickHandler(event -> populateListChoices(exid, addToList, removeFromList, sendList, dropdownContainer));

    addNewListChoice(dropdownContainer, exid, this);


    dropdownContainer.add(addToList);
    dropdownContainer.add(removeFromList);
    dropdownContainer.add(new Divider());

    dropdownContainer.add(sendList);
  }

  private void addNewListChoice(Dropdown dropdownContainer, int exid, UserListSupport outer) {
    NavLink widget = new NavLink(NEW_LIST);
    widget.addClickHandler(event -> {
      NewListButton newListButton = new NewListButton(exid, controller, outer, dropdownContainer);
      newListButton.showOrHide(newListButton.getNewListButton2(), widget);
    });
    dropdownContainer.add(widget);
  }

  /**
   * Ask server for the set of current lists for this user.
   * <p>
   * TODO : do this better -- tell server to return lists that don't have exercise in them.
   * <p>
   * Visited are OK, I guess.
   *
   * @param exid
   * @param addToList
   * @seex #wasRevealed()
   * @see #addListOptions(Dropdown, int)
   */
  private void populateListChoices(final int exid,
                                   final DropdownBase addToList,
                                   final DropdownBase removeFromList,
                                   final DropdownBase emailList,
                                   Dropdown container
  ) {
    //  logger.info("asking for " + id );
    controller.getListService().getListsWithIDsForUser(true, true,
        new AsyncCallback<Collection<IUserListWithIDs>>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("get list with ids for user", caught);
          }

          @Override
          public void onSuccess(Collection<IUserListWithIDs> result) {
            useLists(result, addToList, removeFromList, emailList, exid, container);
          }
        });
  }

  private void useLists(Collection<IUserListWithIDs> result,
                        DropdownBase addToList,
                        DropdownBase removeFromList,
                        DropdownBase emailList,
                        int exid,
                        Dropdown container) {
    addToList.clear();
    removeFromList.clear();
    emailList.clear();

    boolean anyAdded = false;
    boolean anyToRemove = false;
    int user = controller.getUser();

    for (final IUserListWithIDs ul : result) {
      //  logger.info("useLists : " + ul);
      boolean isMyList = ul.getUserID() == user;
      if (isMyList) {
        knownNamesForDuplicateCheck.add(ul.getName().trim().toLowerCase());
        if (ul.containsByID(exid)) {
          anyToRemove = true;
          getRemoveListLink(ul, removeFromList, exid, container);
        } else {
          anyAdded = true;
          getAddListLink(ul, addToList, exid, container);
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

  private void addSendLink(IUserList ul, DropdownBase addToList, boolean isMyList) {
    final NavLink widget = getListLink(ul.getName());
    if (!isMyList) {
      widget.getElement().getStyle().setFontStyle(Style.FontStyle.ITALIC);
    }
    addToList.add(widget);
    widget.setHref(getMailToList(ul));
    widget.addClickHandler(event -> controller.logEvent(addToList, "DropUp", ul.getID(), "sharing_" + ul.getID() + "/" + ul.getName()));
  }

  @NotNull
  String getMailToExercise(CommonShell exercise) {
    String s = getURL() +
        "#" +
        SelectionState.SECTION_SEPARATOR +
        SelectionState.SEARCH +
        "=" + exercise.getID() +
        getProjectParam() +
        getInstance(false);

    String encode = URL.encode(s);
    return "mailto:" +
        "?" +

        "Subject=" +
        SHARE_NETPROF + controller.getLanguage() + " item " + exercise.getEnglish() +

        "&body=" +
        getPrefix() + exercise.getEnglish() + "/" + exercise.getFLToShow() + " : " +
        encode +
        getSuffix();
  }

  private String getMailToList(IUserList ul) {
    return getMailTo(ul.getID(), ul.getName(), false);
  }

  @NotNull
  public String getMailTo(int listid, String name, boolean isQuiz) {
    String selector = "Lists=" + listid;

    String s = getURL() +
        "#" +
        SelectionState.SECTION_SEPARATOR + selector +
        getProjectParam() +
        getInstance(isQuiz);

    String encode = URL.encode(s);
    String type = isQuiz ? "quiz" : "list";

    return "mailto:" +
        "?" +
        "Subject=" +
        getSubject(name, type) +
        "&body=" +
        getBody(name, encode, type);
  }


  @NotNull
  private String getInstance(boolean isQuiz) {
    return SelectionState.SECTION_SEPARATOR + SelectionState.INSTANCE + "=" +
        (isQuiz ?
            INavigation.VIEWS.QUIZ.toString() :
            INavigation.VIEWS.LEARN.toString()
        );
  }

  @NotNull
  private String getSubject(String name, String type) {
    return "Share netprof " + controller.getLanguage() +
        " " +
        type +
        " " + name;
  }

  private String getBody(String name, String encode, String type) {
    return getPrefix() + name + " " +
        type + " : " + encode
        + getSuffix();
  }

  @NotNull
  private String getProjectParam() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    return projectStartupInfo == null ? "" : SelectionState.SECTION_SEPARATOR + SelectionState.PROJECT + "=" + projectStartupInfo.getProjectid();
  }

  /**
   * @param addToList
   * @see ItemMenu#getDropdown
   */
  void addSendLinkWhatYouSee(DropdownBase addToList) {
    final NavLink widget = new NavLink(SHARE_THESE_ITEMS);
    addToList.add(widget);
    widget.setHref(getMailToThese());
  }

  @NotNull
  private String getMailToThese() {
    String token = History.getToken();
    SelectionState selectionState = new SelectionState(token, false);
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();

    boolean hasProject = selectionState.getProject() != -1;
    boolean hasInstance = selectionState.getView() != INavigation.VIEWS.NONE;

    String s = getURL() +
        "#" +
        token +
        (hasProject ? "" : getProjectParam()) +
        (hasInstance ? "" : getInstance(false));

    String encode = s.replaceAll("\\s", "+");//URL.encode(s);

    // logger.info("getMailToThese : encode " +encode);
    return "mailto:" +
        "?" +
        "Subject=Share netprof " + controller.getLanguage() +
        " items " +
        "&body=" +
        getPrefix() +
        selectionState.getDescription(projectStartupInfo.getTypeOrder(), false) + " : " +

        encode +

        getSuffix();
  }

  @NotNull
  private String getPrefix() {
    return "Hi%2C%0A%20Here's%20a%20link%20to%20";
  }

  /**
   * @return
   */
  @NotNull
  private String getSuffix() {
    return "%0A%20Thanks%2C%20%0A%20" + getFullName();
  }

  private String getFullName() {
    return controller.getUserManager().getCurrent().getFullName();
  }

  private String getURL() {
    return trimURL(Window.Location.getHref());
  }

  private String trimURL(String url) {
    return url.split("\\?")[0].split("#")[0];
  }


  private void getAddListLink(IUserList ul,
                              DropdownBase addToList,
                              int exid,
                              Widget container
  ) {
    final NavLink widget = getListLink(ul.getName());
    addToList.add(widget);
    widget.addClickHandler(event -> {
      controller.logEvent(addToList, "DropUp", exid, "adding_" + ul.getID() + "/" + ul.getName());

      // logger.info("got click on " + ul.getID() + " " + ul.getName());
      controller.getListService().addItemToUserList(ul.getID(), exid, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("add item to list", caught);
        }

        @Override
        public void onSuccess(Void result) {
          popupContainer.showPopup(ITEM_ADDED, container);
          //   logger.info("fire event on " + ul.getID() + " " + ul.getName());
          LangTest.EVENT_BUS.fireEvent(new ListChangedEvent());
        }
      });
    });
  }

  @NotNull
  private NavLink getListLink(String name) {
    //  String name = ul.getName();
    if (name.length() > END_INDEX) name = name.substring(0, END_INDEX) + "...";
    return new NavLink(name);
  }

  /**
   * @param ul
   * @param removeFromList
   * @param exid
   * @param container
   * @see #useLists
   */
  private void getRemoveListLink(IUserList ul, DropdownBase removeFromList, int exid, Dropdown container) {
    final NavLink widget = getListLink(ul.getName());
    removeFromList.add(widget);
    widget.addClickHandler(event -> {
      controller.logEvent(removeFromList, "DropUp", exid, "remove_" + ul.getID() + "/" + ul.getName());

      controller.getListService().deleteItemFromList(ul.getID(), exid, new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("delete item from list", caught);
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
