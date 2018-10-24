package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.github.gwtbootstrap.client.ui.base.UnorderedList;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.custom.IUserList;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.MatchInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

public class ListFacetHelper {
  private final Logger logger = Logger.getLogger("ListFacetHelper");

  private static final String GETTING_LISTS_FOR_USER = "getting simple lists for user";

  private final Map<Integer, String> idToListName = new HashMap<>();
  private final Map<Integer, IUserList> idToList = new LinkedHashMap<>();


  private ListItem liForDimensionForList;

  private final ExerciseController controller;
  private final String dynamicFacet;

  private Map<String, Set<MatchInfo>> lastTypeToValues;

  private final UserList.LIST_TYPE list_type;
  private final ChoicesContainer choicesContainer;
  private final boolean active;

  public ListFacetHelper(ExerciseController controller,
                         String dynamicFacet,
                         UserList.LIST_TYPE list_type,
                         ChoicesContainer choicesContainer,
                         boolean active) {
    this.controller = controller;
    this.dynamicFacet = dynamicFacet;
    this.list_type = list_type;
    this.choicesContainer = choicesContainer;
    this.active = active;
  }

  Map<Integer, IUserList> getIdToList() {
    return idToList;
  }

  String getListName(int id) {
    return idToListName.get(id);
  }

  void reallyAddListFacet(Map<String, Set<MatchInfo>> typeToValues,
                          UnorderedList allTypesContainer) {
    liForDimensionForList = active ? addListFacet(typeToValues) : null;
    if (liForDimensionForList != null) {
      allTypesContainer.add(liForDimensionForList);
    }
  }


  /**
   * @see FacetExerciseList#FacetExerciseList
   */
  void gotListChanged() {
    if (liForDimensionForList != null) {
      populateListChoices(liForDimensionForList, null);
    }
  }

  /**
   * @param typeToValues
   * @return
   * @see #reallyAddListFacet
   */
  private ListItem addListFacet(Map<String, Set<MatchInfo>> typeToValues) {
    ListItem liForDimensionForType = choicesContainer.getTypeContainer(dynamicFacet);
    populateListChoices(liForDimensionForType, typeToValues);
    return liForDimensionForType;
  }

  private void populateListChoices(ListItem liForDimensionForType, Map<String, Set<MatchInfo>> typeToValues) {
    if (typeToValues == null) {
      typeToValues = lastTypeToValues;
    } else {
      lastTypeToValues = new HashMap<>();
      lastTypeToValues.putAll(typeToValues);
    }

    final Map<String, Set<MatchInfo>> finalTypeToValues = typeToValues;
    final long then = System.currentTimeMillis();

    controller.getListService().getSimpleListsForUser(true, true, list_type, new AsyncCallback<Collection<IUserList>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError(GETTING_LISTS_FOR_USER, caught);
      }

      @Override
      public void onSuccess(Collection<IUserList> result) {
        long l = System.currentTimeMillis();
        if (l - then > 250) {
          logger.info("addListsAsLinks : took " + (l - then) + " to get lists for user.");
        }
        addListsAsLinks(result, finalTypeToValues, liForDimensionForType);
      }
    });
  }

  private void addListsAsLinks(Collection<IUserList> result,
                               Map<String, Set<MatchInfo>> finalTypeToValues,
                               ListItem liForDimensionForType) {
    //String dynamicFacet = getDynamicFacet();

    finalTypeToValues.put(dynamicFacet, getMatchInfoForEachList(result));

    Widget favorites = liForDimensionForType.getWidget(0);
    liForDimensionForType.clear();
    liForDimensionForType.add(favorites);
    //  logger.info("populateListChoices --- for " + result.size() + " lists ");
    liForDimensionForType.add(choicesContainer.addChoices(finalTypeToValues, dynamicFacet));
  }

  @NotNull
  private Set<MatchInfo> getMatchInfoForEachList(Collection<IUserList> result) {
    Set<MatchInfo> value = new HashSet<>();
    idToListName.clear();
    int currentUser = controller.getUser();
    for (IUserList list : result) {
      boolean isVisit = list.getUserID() != currentUser;
      String tooltip = isVisit ? " from " + list.getUserChosenID() : "";
      value.add(new MatchInfo(list.getName(), list.getNumItems(), list.getID(), isVisit, tooltip));
      idToListName.put(list.getID(), list.getName());
      idToList.put(list.getID(), list);
    }
    return value;
  }

}
