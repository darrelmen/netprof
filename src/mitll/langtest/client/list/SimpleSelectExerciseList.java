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
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.bootstrap.SectionNodeItemSorter;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.custom.UserList;

import java.util.*;
import java.util.logging.Logger;

public abstract class SimpleSelectExerciseList extends NPExerciseList<ListSectionWidget> {
  private final Logger logger = Logger.getLogger("SimpleSelectExerciseList");
  private static final int CLASSROOM_VERTICAL_EXTRA = 270;
  private static final String SHOWING_ALL_ENTRIES = "Showing all entries";

  private final Heading statusHeader = new Heading(4);
  private List<String> typeOrder;
  private final Panel sectionPanel;
  private final DownloadHelper downloadHelper;

  /**
   * @param secondRow             add the section panel to this row
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param controller
   * @param instance
   * @param incorrectFirst
   */
  public SimpleSelectExerciseList(Panel secondRow,
                                  Panel currentExerciseVPanel,
                                  ExerciseServiceAsync service,
                                  UserFeedback feedback,
                                  ExerciseController controller,
                                  String instance,
                                  boolean incorrectFirst) {
    super(currentExerciseVPanel, service, feedback, controller, true, instance, incorrectFirst,false);

    sectionPanel = new FluidContainer();
    sectionPanel.getElement().setId("sectionPanel_" + instance);

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
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#getExercises(long)
   * @see mitll.langtest.client.custom.content.FlexListLayout#doInternalLayout(UserList, String)
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
    typeOrder = controller.getProjectStartupInfo().getTypeOrder();
    //  logger.info("getTypeOrder type order is " + typeOrder);
    addChoiceRow(controller.getProjectStartupInfo().getSectionNodes(), container, typeOrder);
  }

  private Panel firstTypeRow;

  /**
   * @param rootNodes
   * @param container
   * @param types
   * @see #getTypeOrder(FluidContainer)
   */
  private void addChoiceRow(Collection<SectionNode> rootNodes, final FluidContainer container, List<String> types) {
/*    logger.info("addChoiceRow for user = " + controller.getUser() + " got types " +
        types + " num root nodes " + rootNodes.size());*/
    if (types.isEmpty()) {
      logger.warning("addChoiceRow : huh? types is empty?");
      return;
    }
    showDefaultStatus();
    HorizontalPanel widgets = new HorizontalPanel();
    firstTypeRow = widgets;
    firstTypeRow.getElement().setId("firstTypeRow");
    container.add(firstTypeRow);
    firstTypeRow.addStyleName("alignTop");

//    logger.info("iter order ");
    rootNodes = new SectionNodeItemSorter().getSortedItems(rootNodes);
    // for (SectionNode node:rootNodes) logger.info("\t" + node.getType() + " " +node.getName());

    //int index = 0;

    ListSectionWidget parent = null;
    int i = 0;

    for (String type : types) {
//      List<String> sectionsInType = new ArrayList<>();
//      for (SectionNode node:rootNodes) {
//        sectionsInType.add(node.getName());
//      }
      // if (!type.equals("Sound")) {
      //   for (SectionNode node : rootNodes) logger.info("#" + (i++) + "\t" + node.getName());
      // }
      //List<String> sectionsInType = new ItemSorter().getSortedItems(getLabels(rootNodes));
      List<String> sectionsInType = getLabels(rootNodes);

      //   logger.info("got " + type + " num sections " + sectionsInType.size());

      //   MenuSectionWidget value = new MenuSectionWidget(type, rootNodes, this);
      ListSectionWidget value = new ListSectionWidget(type, rootNodes, this);
      if (parent != null) {
        parent.addChild(value);
      }
      parent = value;
      sectionWidgetContainer.setWidget(type, value);

      //   logger.info("for " + type + " : " + sectionsInType);
      value.addChoices(firstTypeRow, type, sectionsInType);

      if (types.indexOf(type) < types.size() - 1) {
        rootNodes = getChildSectionNodes(rootNodes);
        rootNodes = new SectionNodeItemSorter().getSortedItems(rootNodes);
      }
      i = 0;
    }
    makeDefaultSelections();

    DivWidget bottomRow = getBottomRow();
    addBottomText(bottomRow);
    container.add(bottomRow);


    pushFirstListBoxSelection();
  }

  private List<SectionNode> getChildSectionNodes(Collection<SectionNode> rootNodes) {
    List<SectionNode> newNodes = new ArrayList<>();
    for (SectionNode node : rootNodes) {
      //  logger.info("getChildSectionNodes " + node.getType() + " "+ node.getName());

      Collection<SectionNode> children = node.getChildren();

      if (!children.isEmpty() && !children.iterator().next().getType().equals("Sound")) {
        //    for (SectionNode child : children) {
        //      logger.info("\tAdding " + child.getType() + " "+ child.getName());
        //   }
      }
      newNodes.addAll(children);

    }
    return newNodes;
  }

  /**
   * @seex ButtonBarSectionWidget#getChoice(ButtonGroup, String)
   */
  public void gotSelection() {
    //   logger.info("gotSelection --- >");
    pushNewSectionHistoryToken();
  }

//  private DivWidget getBottomRow() {
//    FlexTable links = downloadHelper.getDownloadLinks();
//    // else {
//    //   logger.info("user is not a teacher.");
//    // }
//    DivWidget bottomRow = new DivWidget();
//    bottomRow.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);
//    DivWidget left = new DivWidget();
//    left.addStyleName("floatLeftList");
//    left.add(links);
//    bottomRow.add(left);
//    return bottomRow;
//  }

  private DivWidget getBottomRow() {
    Panel links = downloadHelper.getDownloadLinks();
    // else {
    //   logger.info("user is not a teacher.");
    // }
    DivWidget bottomRow = new DivWidget();
    bottomRow.getElement().getStyle().setMarginBottom(18, Style.Unit.PX);
    DivWidget left = new DivWidget();
    left.addStyleName("floatLeftList");
    left.add(links);
    bottomRow.add(left);
    return bottomRow;
  }


  private List<String> getLabels(Collection<SectionNode> nodes) {
    List<String> items = new ArrayList<>();
    Set<String> added = new HashSet<>();
    for (SectionNode n : nodes) {
      if (added.add(n.getName())) {
        items.add(n.getName());
      }
    }
    return items;
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
    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();

    //  logger.info("showSelectionState : typeOrder " + typeOrder + " selection state " + typeToSection);

    downloadHelper.updateDownloadLinks(selectionState, typeOrder);

    if (typeToSection.isEmpty()) {
      showDefaultStatus();
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
    }
  }

  private void showDefaultStatus() {
    statusHeader.setText(SHOWING_ALL_ENTRIES);
  }

  /**
   * @param container
   * @see #addChoiceRow
   */
  private void addBottomText(Panel container) {
    Panel status = getStatusRow();
    container.add(status);
    status.addStyleName("leftFiftyPercentMargin");
  }


  /**
   * @return
   * @see #addBottomText
   */
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

  /**
   * @seex HistoryExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.amas.ExerciseListWrapper)
   */
  @Override
  protected void gotEmptyExerciseList() {
    showEmptySelection();
  }


  /**
   * @param toShow
   * @see SimpleSelectExerciseList#gotEmptyExerciseList()
   * @seex ResponseExerciseList#quizCompleteDisplay
   */
/*  void showMessage(String toShow, boolean addStartOver) {
    createdPanel = new SimplePanel();
    createdPanel.getElement().setId("placeHolderWhenNoExercises");
    createdPanel.add(new Heading(3, toShow));

    VerticalPanel vp = new VerticalPanel();
    vp.add(createdPanel);

    innerContainer.setWidget(vp);
  }*/

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
    for (SectionWidget v : sectionWidgetContainer.getValues()) {
      v.selectFirst();
    }
  }

  protected SectionWidgetContainer<ListSectionWidget> getSectionWidgetContainer() {
    return new SectionWidgetContainer<ListSectionWidget>() {
      protected String getAnySelectionValue() {
        return "All";
      }

      /**
       * @see #restoreListBoxState(SelectionState, Collection)
       * @param type
       * @param sections
       */
      protected void selectItem(String type, Collection<String> sections) {
        SectionWidget widget = sectionWidgetContainer.getWidget(type);
        widget.selectItem(sections.iterator().next());
      }
    };
  }

  /**
   * When we log in we want to check the history in the url and set the current selection state to reflect it.
   *
   * @seex AMASInitialUI#configureUIGivenUser()
   */
/*  public void restoreListFromHistory() {
    restoreListFromHistory(History.getToken());
  }*/
/*  private void restoreListFromHistory(String token) {
    try {
      SelectionState selectionState = getSelectionState(token);
      if (DEBUG_ON_VALUE_CHANGE) {
        logger.info(" HistoryExerciseList.onValueChange : restoreListBoxState '" + selectionState + "'");
      }
      restoreListBoxState(selectionState);
      if (DEBUG_ON_VALUE_CHANGE) {
        logger.info("HistoryExerciseList.onValueChange : selectionState '" + selectionState + "'");
      }

      // logger.info("gotSelection : got type " + type + " and " + text);
      int count = getNumSelections();
      if (count == 3) {
        //    logger.info("push new token " + getHistoryTokenFromUIState());
        logger.info("gotSelection count = " + count);
        loadExercisesUsingPrefix(selectionState.getTypeToSection(), getPrefix(), false, -1);
      } else {
        // logger.warning("not enough selections " +count);
        gotEmptyExerciseList();
      }
    } catch (Exception e) {
      logger.warning("HistoryExerciseList.onValueChange " + token + " badly formed. Got " + e);
      e.printStackTrace();
    }
  }*/
}
