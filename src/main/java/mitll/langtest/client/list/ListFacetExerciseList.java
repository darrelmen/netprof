package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.github.gwtbootstrap.client.ui.base.UnorderedList;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.banner.QuizHelper;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.ListChangedEvent;
import mitll.langtest.shared.custom.IUserList;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * A facet list with dynamic facets like lists and a content filter.
 *
 * dynamic in the sense that you can change what's on a list and the list facets on the left update.
 *
 * @param <T>
 */
public class ListFacetExerciseList<T extends CommonShell & ScoredExercise>
    extends ClientExerciseFacetExerciseList<T> {
  private final Logger logger = Logger.getLogger("ListFacetExerciseList");

  private static final String CONTENT = "Content";
  // private static final String SENTENCES_ONLY = "Sentences Only";
  private static final String ADDING_VISITOR = "adding visitor";

  private final ListFacetHelper listFacetHelper;

  public ListFacetExerciseList(Panel secondRow,
                               Panel currentExerciseVPanel,
                               ExerciseController controller,
                               ListOptions listOptions,
                               DivWidget listHeader,
                               INavigation.VIEWS views) {
    super(secondRow, currentExerciseVPanel, controller, listOptions, listHeader, views);
    this.listFacetHelper = getListFacetHelper(controller);
    LangTest.EVENT_BUS.addHandler(ListChangedEvent.TYPE, authenticationEvent -> listFacetHelper.gotListChanged());
  }

  @NotNull
  private String getDynamicFacet() {
    return LISTS;
  }

  @NotNull
  private boolean isDynamicFacetInteger() {
    return true;
  }

  @Override
  protected String getChoiceHandlerValue(String type, String key, int newUserListID) {
    return isListType(type) ? "" + newUserListID : key;
  }

  @Override
  protected boolean isListType(String type) {
    return type.equalsIgnoreCase(getDynamicFacet());
  }

  /**
   * @param typeToSection
   * @param prefix
   * @param onlyUninspected
   * @return
   */
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                       String prefix,
                                                       boolean onlyUninspected) {
    ExerciseListRequest exerciseListRequest = super.getExerciseListRequest(typeToSection, prefix, onlyUninspected);

    //logger.info("Type->sel " + typeToSection);
    String dynamicFacet = getDynamicFacet();
    if (typeToSection.containsKey(dynamicFacet)) {
      Collection<String> strings = typeToSection.get(dynamicFacet);

      if (isDynamicFacetInteger()) {
        //only one user list can be selected, and they don't nest
        String next = strings.iterator().next();
        try {
          exerciseListRequest.setUserListID(Integer.parseInt(next));
          //   logger.info("getExerciseListRequest userlist = " + userListID);
        } catch (NumberFormatException e) {
          logger.warning("getExerciseListRequest couldn't parse " + next);
        }
      }
    }
    return exerciseListRequest;
  }

  /**
   * TODO : push down the part about CONTENT and maybe list.
   *
   * @param typeToSelection
   * @return
   * @see #getTypeToValues
   */
  @NotNull
  protected List<Pair> getPairs(Map<String, String> typeToSelection) {
    List<Pair> pairs = super.getPairs(typeToSelection);

    for (String type : getTypeOrderSimple()) {
      String s = typeToSelection.get(type);
      pairs.add(new Pair(type, (s == null) ? ANY : s));
    }

    addPairForTypeSelection(typeToSelection, pairs, getDynamicFacet());

    {
//      addPairForTypeSelection(typeToSelection, pairs, CONTENT);
      String s = typeToSelection.get(CONTENT);
      pairs.add(new Pair(CONTENT, s == null ? ANY : s));

    }

    return pairs;
  }

  private void addPairForTypeSelection(Map<String, String> typeToSelection, List<Pair> pairs, String dynamicFacet) {
    if (typeToSelection.containsKey(dynamicFacet)) {
      pairs.add(new Pair(dynamicFacet, typeToSelection.get(dynamicFacet)));
    }
  }

  @Override
  protected int getUserListID(SelectionState selectionState, Map<String, String> newTypeToSelection) {
    int userListID = isDynamicFacetInteger() ? getUserListID(newTypeToSelection) : -1;
    if (userListID != -1) {
      int projid = selectionState.getProject();
      int currentProject = getCurrentProject();
      boolean isForSameProject = projid == -1 || projid == currentProject;
      userListID = isForSameProject ? userListID : -1;

      if (!isForSameProject) {
        logger.warning("getProjectIDForList : list is for project " + projid + " but current is " + currentProject);
/*
        new ModalInfoDialog(LINK_FOR_CONTENT, PLEASE_CHANGE);
*/
      }
    }
    return userListID;
  }

  private int getUserListID(Map<String, String> newTypeToSelection) {
    int userListID = -1;
    try {
      userListID = newTypeToSelection.containsKey(getDynamicFacet()) ? Integer.parseInt(newTypeToSelection.get(getDynamicFacet())) : -1;
    } catch (NumberFormatException e) {
      logger.warning("getUserListID can't parse " + newTypeToSelection.get(getDynamicFacet()));
    }
    return userListID;
  }


  /**
   * From the selection state in the URL.
   *
   * @param selectionState
   * @param typeOrder
   * @return
   * @see #getSectionWidgetContainer
   */
  @NotNull
  protected Map<String, String> getNewTypeToSelection(SelectionState selectionState, Collection<String> typeOrder) {
    typeOrder = new ArrayList<>(typeOrder);
    typeOrder.add(getDynamicFacet());
    typeOrder.add(CONTENT);

    return getTypeToSelection(selectionState, typeOrder);
  }


/*  @NotNull
  protected Map<String, String> getTypeToSelection(SelectionState selectionState, Collection<String> typeOrder) {
    Map<String, String> newTypeToSelection = new HashMap<>();
    for (String type : typeOrder) {
      Collection<String> selections = selectionState.getTypeToSection().get(type);
      if (selections != null && !selections.isEmpty()) {
        newTypeToSelection.put(type, selections.iterator().next());
      }
    }

    // logger.info("getTypeToSelection " + newTypeToSelection);

    return newTypeToSelection;
  }*/

  /**
   * @see QuizHelper#clearListSelection
   */
  public void clearListSelection() {
    //logger.info("in list ---> clearListSelection ");
    Map<String, String> candidate = new HashMap<>(getTypeToSelection());
    candidate.remove(getDynamicFacet());
    setHistory(candidate);
  }

  @NotNull
  private ListFacetHelper getListFacetHelper(ExerciseController controller) {
    return new ListFacetHelper(controller, getDynamicFacet(), getListType(), this, true);
  }

  @Override
  protected void addDynamicFacets(Map<String, Set<MatchInfo>> typeToValues, UnorderedList allTypesContainer) {
    listFacetHelper.reallyAddListFacet(typeToValues, allTypesContainer, getTypeToSelection().containsKey(getDynamicFacet()));


    Set<MatchInfo> matchInfos = typeToValues.get(CONTENT);

    //  logger.info("addDynamicFacets match infos " + matchInfos);

    if (matchInfos != null && !matchInfos.isEmpty()) {
      ListItem contentFacet = addContentFacet(allTypesContainer);
      addExerciseChoices(CONTENT, contentFacet, matchInfos.iterator().next());
    }
  }

  /**
   * Only for practice view
   *
   * @param allTypesContainer
   */
  private ListItem addContentFacet(UnorderedList allTypesContainer) {
    ListItem widgets = addContentFacet();
    if (widgets != null) {
      allTypesContainer.add(widgets);
    }
    return widgets;
  }

  private ListItem addContentFacet() {
    return getTypeContainer(CONTENT, getTypeToSelection().containsKey(CONTENT));
  }

  private void addExerciseChoices(String dynamicFacet, ListItem liForDimensionForType, MatchInfo e) {
    Set<MatchInfo> value = new HashSet<>();
    value.add(e);

    Map<String, Set<MatchInfo>> typeToValues = new HashMap<>();
    typeToValues.put(dynamicFacet, value);

    //logger.info("addExerciseChoices --- for " + value);
    liForDimensionForType.add(addChoices(typeToValues, dynamicFacet, false));
  }

  /**
   * @param type
   * @param choices
   * @param selectionForType
   * @param addTypePrefix
   * @see ChoicesContainer#addChoices(Map, String, boolean)
   */
  protected void addListChoice(String type, Panel choices, String selectionForType, boolean addTypePrefix) {
    try {
      String listName = selectionForType;
      boolean dynamicFacetInteger = isDynamicFacetInteger();
      if (dynamicFacetInteger) {
        int userListID = Integer.parseInt(selectionForType);
        listName = listFacetHelper.getListName(userListID);
      }

      if (listName != null) {
        //      logger.info("addListChoice selected : adding " + listName + " " + type);
        choices.add(getSelectedAnchor(type, listName, addTypePrefix));
      } else {

        int toUse = -1;
        try {
          toUse = this.userListID == -1 ?
              dynamicFacetInteger ?
                  Integer.parseInt(selectionForType) :
                  -1 :
              -1;
        } catch (NumberFormatException e) {
          logger.info("addListChoice couldn't parse " + selectionForType);
        }
        logger.info("addListChoice couldn't find list " + toUse + " in known lists...");
        addVisitor(type, choices, toUse);
      }
    } catch (NumberFormatException e) {
      logger.warning("addListChoice couldn't parse " + selectionForType);
    }
  }


  private void addVisitor(String type, Panel choices, int userListID) {
    //logger.info("addVisitor " + type + " : " + userListID);
    controller.getListService().addVisitor(userListID, controller.getUser(), new AsyncCallback<UserList>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError(ADDING_VISITOR, caught);
      }

      @Override
      public void onSuccess(UserList result) {
        if (result == null) {
          logger.info("addVisitor list " + userListID + " is a quiz or it doesn't exist any more.");
        } else {
          if (result.getProjid() != getCurrentProject()) {
            logger.warning("addVisitor list " + result.getName() + " is NOT in the project #" + result.getProjid());
          } else {
            choices.add(getSelectedAnchor(type, result.getName(), false));
          }
        }
      }
    });
  }

  protected Map<Integer, IUserList> getIdToList() {
    return listFacetHelper.getIdToList();
  }
}
