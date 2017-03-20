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

package mitll.langtest.client.list;


import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.github.gwtbootstrap.client.ui.base.UnorderedList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.FilterResponse;
import mitll.langtest.shared.exercise.Pair;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

public abstract class FacetExerciseList extends NPExerciseList<ListSectionWidget> {
  public static final int MAX_TO_SHOW = 4;
  private final Logger logger = Logger.getLogger("FacetExerciseList");
  private static final int CLASSROOM_VERTICAL_EXTRA = 270;

  private List<String> typeOrder;
  private final Panel sectionPanel;
  private final DownloadHelper downloadHelper;

  /**
   * @param secondRow             add the section panel to this row
   * @param currentExerciseVPanel
   * @param controller
   * @param listOptions
   */
  public FacetExerciseList(Panel secondRow,
                           Panel currentExerciseVPanel,
                           ExerciseController controller,
                           ListOptions listOptions) {
    super(currentExerciseVPanel, controller, listOptions.setShowTypeAhead(true));

    sectionPanel = new DivWidget();
    sectionPanel.getElement().setId("sectionPanel_" + getInstance());
    sectionPanel.addStyleName("rightFiveMargin");
    secondRow.add(sectionPanel);
    setUnaccountedForVertical(CLASSROOM_VERTICAL_EXTRA);
    downloadHelper = new DownloadHelper(this);
    //  logger.info("made ex list....");
  }

  /**
   * @see PagingExerciseList#PagingExerciseList
   */
  @Override
  protected void addComponents() {
    addTableWithPager(makePagingContainer());
  }

  /**
   * @param userID
   * @see HistoryExerciseList#noSectionsGetExercises
   */
  public boolean getExercises(final long userID) {
    addWidgets();
    return false;
  }

  /**
   * @seex mitll.langtest.client.bootstrap.FlexSectionExerciseList#getExercises(long)
   * @seex mitll.langtest.client.custom.content.FlexListLayout#doInternalLayout(UserList, String)
   * @see #getExercises(long)
   */
  @Override
  public void addWidgets() {
    sectionPanel.clear();
    sectionPanel.add(typeOrderContainer = getWidgetsForTypes());
  }

  Panel typeOrderContainer;

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @return
   * @see #addWidgets
   */
  private Panel getWidgetsForTypes() {
    final UnorderedList container = new UnorderedList();
    container.getElement().setId("typeOrderContainer");
//    container.getElement().getStyle().setPaddingLeft(2, Style.Unit.PX);
//    container.getElement().getStyle().setPaddingRight(2, Style.Unit.PX);

    getTypeOrder(container);
    return container;
  }

  private void getTypeOrder(final UnorderedList container) {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    typeOrder = projectStartupInfo.getTypeOrder();
    //  logger.info("getTypeOrder type order is " + typeOrder);
    addFacets(container, projectStartupInfo.getRootNodes(), projectStartupInfo.getTypeToDistinct());
  }

  private Panel firstTypeRow;

  /**
   * structure
   * nav
   * header (optional)
   * ul
   * li - each dimension
   * span - header
   * ul - choices in each dimension
   * choice - with span (qty)
   * <p>
   * optional collapse although it's really nice
   * <p>
   * breadcrumb with choices
   * <p>
   * rules -
   * click on header, removes selection of choice
   * click on choice, down select for that choice
   *
   * @param nav
   * @param types
   * @param typeToDistinct
   * @paramx rootNodes
   * @see #getTypeOrder
   */
  private void addFacets(
      final UnorderedList nav,
      Collection<String> types,
      Map<String, Set<String>> typeToDistinct) {
    logger.info("addChoiceRow for user = " + controller.getUser() + " got types " +
        types);// + " num root nodes " + rootNodes.size());

    if (types.isEmpty()) {
      logger.warning("addChoiceRow : huh? types is empty?");
      return;
    }

    addFacetsForReal(typeToDistinct, nav);
    makeDefaultSelections();
    pushFirstListBoxSelection();
  }

  /**
   * TODO : for now only single selection
   */
  private Map<String, String> typeToSelection = new HashMap<>();
  private Map<String, Boolean> typeToShowAll = new HashMap<>();

  private Set<String> types = new HashSet<>();

  /**
   * ul
   * li - each dimension
   * span - header
   * ul - choices in each dimension
   * li - choice - with span (qty)
   *
   * @paramx rootNodes
   * @paramx types
   * @see #addFacets
   */
  private void addFacetsForReal(Map<String, Set<String>> typeToValues, Panel nav) {
    int i = 0;

    logger.info("addFacetsForReal\n\tfor " +
        //rootNodes.size() +
        " nodes" +
        "\n\tand " + types.size() +
        //" types " + types + " " +
        " type->distinct " + typeToValues.keySet() + "\n\ttype->sel " + typeToSelection);
    Collection<String> rootNodes = controller.getProjectStartupInfo().getRootNodes();

    this.types = new HashSet<>(controller.getProjectStartupInfo().getTypeOrder());
    this.types.retainAll(rootNodes);

    // nav -
    //   ul
    UnorderedList allTypesContainer = new UnorderedList(); //ul
    nav.clear();
    nav.add(allTypesContainer);
    // allTypesContainer.addStyleName("sidebar");

    for (String type : types) {
      //if (refined) logger.info("Sel on  " + type);

      // nav
      //  ul
      //   li - dimension
      ListItem liForDimensionForType = getLIDimension(typeToSelection.containsKey(type));
      allTypesContainer.add(liForDimensionForType);
      liForDimensionForType.add(getTypeHeader(type));

      // nav
      //  ul
      //   li
      //    ul
      //
      liForDimensionForType.add(addChoices(typeToValues, type));
    }
  }

  /**
   * ul
   * span
   * li
   * ul
   * span
   * li ...
   * <p>
   * ul
   *
   * @param typeToValues
   * @param type
   * @return
   */
  private Panel addChoices(Map<String, Set<String>> typeToValues, String type) {
    Panel choices = /*allShort ? new DivWidget() :*/ new UnorderedList(); // ul
    /*  if (allShort) {
        choices.addStyleName("symbolic");
        choices.addStyleName("topFiveMargin");
      }*/
    //  liForDimension.add(choices);

    String selectionForType = typeToSelection.get(type);
    if (selectionForType != null) {
      String childType = getChildForParent(type);
      if (childType != null) {
        Widget parentAnchor =
            typeToSelection.containsKey(childType) ?
                getParentAnchor(selectionForType, childType) :
                getSelectedAnchor(selectionForType);
        choices.add(parentAnchor);
        ListItem liForDimension = new ListItem();
        liForDimension.addStyleName("subdimension");
        liForDimension.addStyleName("refinement");
        choices.add(liForDimension);
        liForDimension.add(addChoices(typeToValues, childType));
      } else {
        choices.add(getSelectedAnchor(selectionForType));
      }
    } else {
      Set<String> keys = typeToValues.get(type);
      if (keys != null) {
        addChoicesForType(typeToValues, type, choices, keys);
      }
    }
    return choices;
  }

  private void addChoicesForType(Map<String, Set<String>> typeToValues, String type, Panel choices, Set<String> keys) {
    int j = 0;
    boolean hasMore = keys.size() > MAX_TO_SHOW;
    Boolean showAll = typeToShowAll.getOrDefault(type, false);
    for (String key : keys) {
      addLIChoice(choices, getAnchor(type, key));

      if (hasMore && !showAll && ++j == MAX_TO_SHOW) {
        addLIChoice(choices, getShowMoreAnchor(typeToValues, type));
        break;
      }
    }
    if (hasMore && showAll) {
      addLIChoice(choices, getShowLess(typeToValues, type));
    }
  }

  @NotNull
  private Anchor getShowLess(final Map<String, Set<String>> typeToValues, final String type) {
    Anchor anchor = new Anchor();
    anchor.setText("Show Less...");
    anchor.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        typeToShowAll.put(type, false);
        addFacetsForReal(typeToValues, typeOrderContainer);
      }
    });
    return anchor;
  }

  @NotNull
  private Anchor getShowMoreAnchor(final Map<String, Set<String>> typeToValues, final String type) {
    Anchor anchor = new Anchor();

    anchor.setText("Show More...");
    anchor.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        typeToShowAll.put(type, true);
        addFacetsForReal(typeToValues, typeOrderContainer);
      }
    });
    return anchor;
  }

  @NotNull
  private Widget getSelectedAnchor(String selectionForType) {
 /*   Anchor anchor = new Anchor();
    anchor.setText(selectionForType);
    anchor.addStyleName("selected");*/
    Panel anchor = new FlowPanel("span");
    anchor.getElement().setInnerHTML(selectionForType);
    anchor.addStyleName("selected");
    return anchor;
  }

  @NotNull
  private Panel getTypeHeader(String type) {
    Anchor headerAnchor = getHeaderAnchor(type);

    // DivWidget headerContainer = new DivWidget();
    Panel headerContainer = new FlowPanel("span");
    headerContainer.addStyleName("menuItem");

    headerContainer.add(headerAnchor);
    return headerContainer;
  }

  @NotNull
  private ListItem getLIDimension(boolean refined) {
    ListItem liForDimension = new ListItem();
    liForDimension.addStyleName("dimension");
    if (refined) liForDimension.addStyleName("refined");
    return liForDimension;
  }

  private void addLIChoice(Panel choices, Widget anchor1) {
    ListItem li = new ListItem();
    //li.addStyleName("menuItem");
    // NavLink w = new NavLink(key);
    li.add(anchor1);
    choices.add(li);
  }

/*  private boolean areAllShort(Set<String> keys) {
    boolean allShort = true;
    for (String key : keys) {
      if (key.length() > 3) {
        allShort = false;
        break;
      }
    }
    return allShort;
  }*/

  private Widget getAnchor(String type, String key) {
    Anchor anchor = getAnchor();

    anchor.setText(key);
    anchor.addClickHandler(getHandler(type, key));
    return anchor;
  }

  @NotNull
  private Anchor getAnchor() {
    Anchor anchor = new Anchor();
    removeAnchorStyle(anchor);
    return anchor;
  }

  private void removeAnchorStyle(Anchor anchor) {
    anchor.removeStyleName("gwt-Anchor");
  }

  @NotNull
  private Anchor getHeaderAnchor(final String type) {
    Anchor typeSection = getAnchor(); // li
    typeSection.setText(type);
    typeSection.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        removeSelection(type);
        logger.info("ty->sel remove " + typeToSelection);
        gotSelection();
        getTypeToValue();
      }
    });
    return typeSection;
  }

  @NotNull
  private Widget getParentAnchor(final String value, String childType) {
    Anchor typeSection = getAnchor(); // li
    typeSection.setText(value);
    typeSection.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        removeSelection(childType);
        logger.info("ty->sel remove " + typeToSelection);
        gotSelection();
        getTypeToValue();
      }
    });
    return typeSection;
  }

  private boolean removeSelection(String childType) {
    String remove = typeToSelection.remove(childType);
    boolean did = remove != null;
    String childOfChild = getChildForParent(childType);
    if (childOfChild != null) {
      did |= removeSelection(childOfChild);
    }
    return did;
  }

  private String getChildForParent(String childType) {
    return controller.getProjectStartupInfo().getParentToChild().get(childType);
  }

  @NotNull
  private ClickHandler getHandler(final String type, final String key) {
    return new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        typeToSelection.put(type, key);
        logger.info("getHandler t->sel " + typeToSelection);
        gotSelection();
        getTypeToValue();
      }
    };
  }

  int reqid = 0;

  private void getTypeToValue() {
    List<Pair> pairs = new ArrayList<>();

    for (String type : typeOrder) {
      String s = typeToSelection.get(type);
      if (s == null) {
        pairs.add(new Pair(type, "Any"));
      } else {
        pairs.add(new Pair(type, s));
      }
    }

    controller.getExerciseService().getTypeToValues(new FilterRequest(reqid++, pairs),
        new AsyncCallback<FilterResponse>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          /**
           * TODO : fix downstream selections that no longer make sense.
           * @param response
           */
          @Override
          public void onSuccess(FilterResponse response) {
            Map<String, Set<String>> result = response.getTypeToValues();
            logger.info("getTypeToValues for " + pairs + " got " + result);
            boolean b = changeSelection(response.getTypesToInclude());
            addFacetsForReal(result, typeOrderContainer);
            if (b) {
              logger.info("getTypeToValues gotSelection --- ");
              gotSelection();
            }
          }
        });
  }

  private boolean changeSelection(Set<String> typesToInclude) {
    boolean removed = false;

    Set<String> strings = typeToSelection.keySet();
    logger.info(" - found " + strings + " selected types vs "  + typesToInclude);

    for (String typeWithSelection : strings) {
      boolean clearSelection = !typesToInclude.contains(typeWithSelection);
      if (clearSelection) {
        if (removeSelection(typeWithSelection)) {
          logger.info("removed selection " + typeWithSelection);
          removed = true;
        }
        else {
          logger.info("no selection " + typeWithSelection);

        }
      }
      else {
        logger.info(" - found " + typeWithSelection + " selected type in "  + typesToInclude);
      }
    }

    logger.info("changeSelection removed --- " + removed);

    return removed;
  }

  /**
   * @param rootNodes
   * @return
   * @seex #addChoiceWidgets
   */
 /* private List<SectionNode> getChildSectionNodes(Collection<SectionNode> rootNodes) {
    List<SectionNode> newNodes = new ArrayList<>();
    for (SectionNode node : rootNodes) {
      logger.info("getChildSectionNodes " + node.getType() + " " + node.getName());

      // Collection<SectionNode> children = node.getChildren();
      Collection<SectionNode> children = node.getChildren();

      // if (!children.isEmpty() && !children.iterator().next().getProperty().equals("Sound")) {
      //    for (SectionNode child : children) {
      //      logger.info("\tAdding " + child.getProperty() + " "+ child.getName());
      //   }
      // }
      newNodes.addAll(children);
    }

    logger.info("getChildSectionNodes found " + newNodes.size());

    return newNodes;
  }*/

  /**
   * @seex ButtonBarSectionWidget#getChoice(ButtonGroup, String)
   */
  public void gotSelection() {
    //logger.info("gotSelection --- ");
    pushNewSectionHistoryToken();
  }

//  private Widget getBottomRow() {
//    Panel links = downloadHelper.getDownloadButton();
//    links.addStyleName("topMargin");
//    links.addStyleName("leftFiveMargin");
//    return links;
//  }

  /**
   * @param selectionState
   * @see HistoryExerciseList#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  @Override
  protected void restoreListBoxState(SelectionState selectionState) {
    // logger.info("restoreListBoxState " + selectionState);
    super.restoreListBoxState(selectionState);
    showSelectionState(selectionState);
  }

  /**
   * Add a line that spells out in text which lessons have been chosen, derived from the selection state.
   * Show in the same type order as the button rows.
   *
   * @param selectionState to get the current selection state from
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  private void showSelectionState(SelectionState selectionState) {
    // keep the download link info in sync with the selection
    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();
    //  logger.info("showSelectionState : typeOrder " + typeOrder + " selection state " + typeToSection);

    downloadHelper.updateDownloadLinks(selectionState, typeOrder);

/*    if (typeToSection.isEmpty()) {
   //   showDefaultStatus();
    } else {
      StringBuilder status = new StringBuilder();
      // logger.info("\tshowSelectionState : typeOrder " + typeOrder + " selection state " + typeToSection);
      for (String type : typeOrder) {
        Collection<String> selectedItems = typeToSection.get(type);
        if (selectedItems != null) {
          List<String> sorted = new ArrayList<>();
          for (String selectedItem : selectedItems) {
            sorted.add(selectedItem);
          }
          Collections.sort(sorted);
          StringBuilder status2 = new StringBuilder();
          for (String item : sorted) status2.append(item).append(", ");
          String s = status2.toString();
          if (!s.isEmpty()) s = s.substring(0, s.length() - 2);
          String statusForType = type + " " + s;
          status.append(statusForType).append(" and ");
        }
      }

      String text = status.toString();
      if (text.length() > 0) text = text.substring(0, text.length() - " and ".length());
      statusHeader.setText(text);
    }*/
  }

/*
  private void showDefaultStatus() {
    statusHeader.setText(SHOWING_ALL_ENTRIES);
  }
*/


  /**
   * @seex HistoryExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.amas.ExerciseListWrapper)
   */
  @Override
  protected void gotEmptyExerciseList() {
    showEmptySelection();
  }

  /**
   * @seex #rememberAndLoadFirst(List, CommonExercise, String)
   */
  @Override
  protected void loadFirstExercise(String searchIfAny) {
    // logger.info("loadFirstExercise : ---");
    if (isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      logger.info("loadFirstExercise : current exercises is empty?");
      gotEmptyExerciseList();
    } else {
      super.loadFirstExercise(searchIfAny);
    }
  }

  /**
   * @seex #addChoiceRow
   */
  private void makeDefaultSelections() {
    //for (SectionWidget v : sectionWidgetContainer.getValues()) v.selectFirst();
  }

  protected FacetContainer getSectionWidgetContainer() {
    return new FacetContainer() {
      @Override
      public void restoreListBoxState(SelectionState selectionState, Collection<String> typeOrder) {
        logger.info("restoreListBoxState t->sel " + typeToSelection);

      }

      @Override
      public String getHistoryToken() {
        StringBuilder builder = new StringBuilder();
        logger.info("getHistoryToken t->sel " + typeToSelection);

        for (Map.Entry<String, String> pair : typeToSelection.entrySet()) {
          builder.append(pair.getKey()).append("=").append(pair.getValue()).append(SECTION_SEPARATOR);
        }

        String s = builder.toString();
        logger.info("getHistoryToken token " + s);

        return s;
      }

      @Override
      public int getNumSelections() {
        return 0;
      }

      /**
       * @see #restoreListBoxState(SelectionState, Collection)
       * @param type
       * @param sections
       */
//      protected void selectItem(String type, Collection<String> sections) {
//        SectionWidget widget = sectionWidgetContainer.getWidget(type);
//        widget.selectItem(sections.iterator().next());
//      }
    };
  }

  class FacetWidget {
    void selectItem(String item) {
// ?
    }
  }
}
