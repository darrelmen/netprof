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


import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.ComplexWidget;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.github.gwtbootstrap.client.ui.base.UnorderedList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.Pair;
import mitll.langtest.shared.exercise.SectionNode;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public abstract class FacetExerciseList extends NPExerciseList<ListSectionWidget> {
  private final Logger logger = Logger.getLogger("SimpleSelectExerciseList");
  private static final int CLASSROOM_VERTICAL_EXTRA = 270;
//  private static final String SHOWING_ALL_ENTRIES = "Showing all entries";

  //  private final Heading statusHeader = new Heading(4);
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
    secondRow.add(sectionPanel);
//    Style style = sectionPanel.getElement().getStyle();
//    style.setPaddingLeft(0, Style.Unit.PX);
//    style.setPaddingRight(0, Style.Unit.PX);
    // secondRow.add(new Column(12, sectionPanel));
    setUnaccountedForVertical(CLASSROOM_VERTICAL_EXTRA);
    downloadHelper = new DownloadHelper(this);
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
    addFacets(projectStartupInfo.getSectionNodes(), container, typeOrder, projectStartupInfo.getTypeToDistinct());
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
   * @param rootNodes
   * @param nav
   * @param types
   * @param typeToDistinct
   * @see #getTypeOrder
   */
  private void addFacets(Collection<SectionNode> rootNodes,
                         final UnorderedList nav,
                         List<String> types,
                         Map<String, Set<String>> typeToDistinct) {
    logger.info("addChoiceRow for user = " + controller.getUser() + " got types " +
        types + " num root nodes " + rootNodes.size());

    if (types.isEmpty()) {
      logger.warning("addChoiceRow : huh? types is empty?");
      return;
    }

    addFacetsForReal(rootNodes, types, typeToDistinct, nav);
    makeDefaultSelections();
    pushFirstListBoxSelection();
  }

  /**
   * TODO : for now only single selection
   */
  Map<String, String> typeToSelection = new HashMap<>();

  Set<String> types = new HashSet<>();

  /**
   * ul
   * li - each dimension
   * span - header
   * ul - choices in each dimension
   * li - choice - with span (qty)
   *
   * @param rootNodes
   * @param types
   * @see #addFacets
   */
  private void addFacetsForReal(Collection<SectionNode> rootNodes,
                                List<String> types,
                                Map<String, Set<String>> typeToDistinct,
                                Panel nav) {
    int i = 0;

    logger.info("addChoiceWidgets\n\tfor " +
        //rootNodes.size() +
        " nodes" +
        "\n\tand " + types.size() + " types " + types + " " +
        " type->distinct " + typeToDistinct);
    // SectionNodeItemSorter sectionNodeItemSorter = new SectionNodeItemSorter();

    this.types = new HashSet<>(types);
    // nav -
    //   ul
    UnorderedList allTypesContainer = new UnorderedList(); //ul
    nav.clear();
    nav.add(allTypesContainer);

    for (String type : types) {
      // List<String> sectionsInType = getLabels(rootNodes);
      Set<String> keys = typeToDistinct.get(type);

      boolean refined = typeToSelection.containsKey(type);

      // nav
      //  ul
      //   li - dimension
      ListItem liForDimension = new ListItem();
      if (refined) liForDimension.addStyleName("refined");
      allTypesContainer.add(liForDimension);

      NavLink typeSection = new NavLink(type + "::after"); // li
      typeSection.addStyleName("menuItem");
      liForDimension.add(typeSection);

      // nav
      //  ul
      //   li
      //    ul
      //
      UnorderedList choices = new UnorderedList(); // ul
      typeSection.add(choices);
//      NavHeader header = new NavHeader(type);

      String selectionForType = typeToSelection.get(type);
      if (selectionForType != null) {
        NavLink w = new NavLink(selectionForType);
        typeSection.add(w);
        w.addStyleName("selected");
      } else {
        if (keys != null) {
          for (String key : keys) {
            NavLink w = new NavLink(key);
            typeSection.add(w);
            w.addClickHandler(getHandler(type, key));
          }
        }
      }
    }
  }

  @NotNull
  private ClickHandler getHandler(final String type, final String key) {
    return new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        typeToSelection.put(type, key);
        gotSelection();
        getTypeToValue();
      }
    };
  }

  int reqid = 0;

  private void getTypeToValue() {
    List<Pair> pairs = new ArrayList<>();

    for (String type:typeOrder) {
      String s = typeToSelection.get(type);
      if (s == null) {
        pairs.add(new Pair(type, "Any"));
      }
      else {
        pairs.add(new Pair(type, s));
      }
    }

    controller.getExerciseService().getTypeToValues(new FilterRequest(reqid++, pairs), new AsyncCallback<Map<String, Set<String>>>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Map<String, Set<String>> result) {
        addFacetsForReal(null, typeOrder, result, typeOrderContainer);
      }
    });
  }

  /**
   * @param rootNodes
   * @return
   * @seex #addChoiceWidgets
   */
  private List<SectionNode> getChildSectionNodes(Collection<SectionNode> rootNodes) {
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
  }

  /**
   * @seex ButtonBarSectionWidget#getChoice(ButtonGroup, String)
   */
  public void gotSelection() {
    //   logger.info("gotSelection --- >");
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
   * @param container
   * @see #addChoiceRow
   */
/*
  private void addBottomText(Panel container) {
    Panel status = getStatusRow();
    container.add(status);
    status.addStyleName("leftFiftyPercentMargin");
  }
*/

  /**
   * @return
   * @see #addBottomText
   */
/*
  private Panel getStatusRow() {
    Panel status = new DivWidget();
    status.getElement().setId("statusRow");
    status.addStyleName("alignCenter");
    status.addStyleName("inlineBlockStyle");
    status.add(statusHeader);
    statusHeader.getElement().setId("statusHeader");
    statusHeader.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    statusHeader.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    return status;
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

      }

      @Override
      public String getHistoryToken() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> pair : typeToSelection.entrySet()) {
          builder.append(pair.getKey()).append("=").append(pair.getValue()).append(SECTION_SEPARATOR);
        }

        return builder.toString();
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
