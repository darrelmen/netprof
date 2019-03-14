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

  ListFacetHelper(ExerciseController controller,
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

  /**
   * @see ListFacetExerciseList#addListChoice
   * @param id
   * @return
   */
  String getListName(int id) {
    return idToListName.get(id);
  }

  /**
   * @see FacetExerciseList#addFacetsForReal
   * @param typeToValues
   * @param allTypesContainer
   * @param hasSelection
   */
  void reallyAddListFacet(Map<String, Set<MatchInfo>> typeToValues,
                          UnorderedList allTypesContainer, boolean hasSelection) {
    liForDimensionForList = active ? addListFacet(typeToValues, hasSelection) : null;
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
  private ListItem addListFacet(Map<String, Set<MatchInfo>> typeToValues, boolean hasSelection) {
    ListItem liForDimensionForType = choicesContainer.getTypeContainer(dynamicFacet, hasSelection);
    populateListChoices(liForDimensionForType, typeToValues);
    return liForDimensionForType;
  }

  /**
   * Ask the server for lists
   * @param liForDimensionForType
   * @param typeToValues
   */
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
        if (l - then > 350) {
          logger.info("addListsAsLinks : took " + (l - then) + " to get lists for user.");
        }
        addListsAsLinks(result, finalTypeToValues, liForDimensionForType);
      }
    });
  }

  private void addListsAsLinks(Collection<IUserList> result,
                               Map<String, Set<MatchInfo>> finalTypeToValues,
                               ListItem liForDimensionForType) {
    finalTypeToValues.put(dynamicFacet, getMatchInfoForEachList(result));

    {
      Widget favorites = liForDimensionForType.getWidget(0);
      liForDimensionForType.clear();
      liForDimensionForType.add(favorites);
    }

    //  logger.info("populateListChoices --- for " + result.size() + " lists ");
    liForDimensionForType.add(choicesContainer.addChoices(finalTypeToValues, dynamicFacet, false));
  }

  @NotNull
  private Set<MatchInfo> getMatchInfoForEachList(Collection<IUserList> result) {
    Set<MatchInfo> value = new HashSet<>();
    idToListName.clear();
    int currentUser = controller.getUser();
    for (IUserList list : result) {
      boolean isVisit = list.getUserID() != currentUser;
      String tooltip = isVisit ? " from " + list.getFirstInitialName() : "";
      value.add(new MatchInfo(list.getName(), list.getNumItems(), list.getID(), isVisit, tooltip));
      idToListName.put(list.getID(), list.getName());
      idToList.put(list.getID(), list);
    }
    return value;
  }
}
