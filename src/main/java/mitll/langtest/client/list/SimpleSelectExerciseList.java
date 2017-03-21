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

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.bootstrap.SectionNodeItemSorter;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.shared.exercise.MatchInfo;
import mitll.langtest.shared.exercise.SectionNode;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.project.ProjectStartupInfo;

import java.util.*;
import java.util.logging.Logger;

public abstract class SimpleSelectExerciseList extends NPExerciseList<ListSectionWidget> {
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
  public SimpleSelectExerciseList(Panel secondRow,
                                  Panel currentExerciseVPanel,
                                  ExerciseController controller,
                                  ListOptions listOptions) {
    super(currentExerciseVPanel, controller,
        listOptions.setShowTypeAhead(true)
//        .setShowFirstNotCompleted(false)
    );

    sectionPanel = new FluidContainer();
    sectionPanel.getElement().setId("sectionPanel_" + getInstance());

    Style style = sectionPanel.getElement().getStyle();
    style.setPaddingLeft(0, Style.Unit.PX);
    style.setPaddingRight(0, Style.Unit.PX);
    secondRow.add(new Column(12, sectionPanel));
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
   * @see #getExercises(long)
   * @seex mitll.langtest.client.bootstrap.FlexSectionExerciseList#getExercises(long)
   * @seex mitll.langtest.client.custom.content.FlexListLayout#doInternalLayout(UserList, String)
   */
  @Override
  public void addWidgets() {
    sectionPanel.clear();
    sectionPanel.add(getWidgetsForTypes());
  }

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @return
   * @see #addWidgets()
   */
  private Panel getWidgetsForTypes() {
    final FluidContainer container = new FluidContainer();
    container.getElement().setId("typeOrderContainer");
    container.getElement().getStyle().setPaddingLeft(2, Style.Unit.PX);
    container.getElement().getStyle().setPaddingRight(2, Style.Unit.PX);

    getTypeOrder(container);
    return container;
  }

  private void getTypeOrder(final FluidContainer container) {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    typeOrder = projectStartupInfo.getTypeOrder();
    //  logger.info("getTypeOrder type order is " + typeOrder);
    addChoiceRow(projectStartupInfo.getSectionNodes(), container, typeOrder, projectStartupInfo.getTypeToDistinct());
  }

  private Panel firstTypeRow;

  /**
   * @param rootNodes
   * @param container
   * @param types
   * @param typeToDistinct
   * @see #getTypeOrder
   */
  private void addChoiceRow(Collection<SectionNode> rootNodes,
                            final FluidContainer container,
                            List<String> types,
                            Map<String, Set<MatchInfo>> typeToDistinct) {
    logger.info("addChoiceRow for user = " + controller.getUser() + " got types " +
        types + " num root nodes " + rootNodes.size());

    if (types.isEmpty()) {
      logger.warning("addChoiceRow : huh? types is empty?");
      return;
    }
    firstTypeRow = getChoicesRow();
    container.add(firstTypeRow);

    rootNodes = new SectionNodeItemSorter().getSortedItems(rootNodes);
    addChoiceWidgets(rootNodes, types, typeToDistinct);
    makeDefaultSelections();

    firstTypeRow.add(getBottomRow());
    pushFirstListBoxSelection();
  }

  private Panel getChoicesRow() {
    Panel firstTypeRow = new HorizontalPanel();
    firstTypeRow.getElement().setId("firstTypeRow");
    firstTypeRow.addStyleName("alignTop");
    return firstTypeRow;
  }

  /**
   * @param rootNodes
   * @param types
   * @see #addChoiceRow
   */
  private void addChoiceWidgets(Collection<SectionNode> rootNodes, List<String> types,
                                Map<String, Set<MatchInfo>> typeToDistinct) {
    ListSectionWidget parent = null;
    int i = 0;

    logger.info("addChoiceWidgets for " + rootNodes.size() + " nodes and " + types.size() + " types " + types);
    SectionNodeItemSorter sectionNodeItemSorter = new SectionNodeItemSorter();
    for (String type : types) {
     // List<String> sectionsInType = getLabels(rootNodes);
      Set<MatchInfo> keys = typeToDistinct.get(type);



      if (keys == null) {
        logger.warning("no type " + type + " in " + typeToDistinct.keySet());
      }
      else {
        Set<String> asKeys = new HashSet<>();
        for (MatchInfo info : keys) asKeys.add(info.getValue());
        List<String> sectionsInType = sectionNodeItemSorter.getSorted(asKeys);
        logger.info("\taddChoiceWidgets for " + type + " : " + sectionsInType.size());

        ListSectionWidget listSectionWidget = new ListSectionWidget(type, rootNodes, this, asKeys);
        if (parent != null) {
          parent.addChild(listSectionWidget);
        }
        parent = listSectionWidget;
        mySectionWidgetContainer.setWidget(type, listSectionWidget);
        listSectionWidget.addChoices(firstTypeRow, type, sectionsInType);

        if (types.indexOf(type) < types.size() - 1) {
          rootNodes = getChildSectionNodes(rootNodes);
          rootNodes = sectionNodeItemSorter.getSortedItems(rootNodes);
        }
        i = 0;
      }
    }
  }

  /**
   * @param rootNodes
   * @return
   * @see #addChoiceWidgets
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

  private Widget getBottomRow() {
    Panel links = downloadHelper.getDownloadButton();
    links.addStyleName("topMargin");
    links.addStyleName("leftFiveMargin");
    return links;
  }

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
//    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();
    //  logger.info("showSelectionState : typeOrder " + typeOrder + " selection state " + typeToSection);
    downloadHelper.updateDownloadLinks(selectionState, typeOrder);
  }

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
   * @see #addChoiceRow
   */
  private void makeDefaultSelections() {
    for (SectionWidget v : mySectionWidgetContainer.getValues()) v.selectFirst();
  }


  private SectionWidgetContainer<ListSectionWidget> mySectionWidgetContainer;
  protected SectionWidgetContainer<ListSectionWidget> getSectionWidgetContainer() {
    mySectionWidgetContainer = new SectionWidgetContainer<ListSectionWidget>() {
      protected String getAnySelectionValue() {
        return "All";
      }

      /**
       * @see #restoreListBoxState(SelectionState, Collection)
       * @param type
       * @param sections
       */
      protected void selectItem(String type, Collection<String> sections) {
        SectionWidget widget = mySectionWidgetContainer.getWidget(type);
        widget.selectItem(sections.iterator().next());
      }
    };
    return mySectionWidgetContainer;
  }
}
