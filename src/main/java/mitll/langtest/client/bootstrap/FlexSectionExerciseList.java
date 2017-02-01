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

package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.content.NPFlexSectionExerciseList;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.client.list.NPExerciseList;
import mitll.langtest.client.list.SectionWidgetContainer;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.answer.ActivityType;
import mitll.langtest.shared.project.ProjectStartupInfo;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/5/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 * @deprecated - we don't do the wall of buttons anymore
 */
public class FlexSectionExerciseList extends NPExerciseList<ButtonGroupSectionWidget> {
  private final Logger logger = Logger.getLogger("FlexSectionExerciseList");
  private static final int LABEL_MARGIN_BOTTOM = 10;

  private static final int HEADING_FOR_LABEL = 4;
  private static final int UNACCOUNTED_WIDTH = 60;
  private static final int CLASSROOM_VERTICAL_EXTRA = 330;
  // private static final String SHOWING_ALL_ENTRIES = "Showing all entries";

  private final List<ButtonType> buttonTypes = new ArrayList<ButtonType>();
  private final Map<String, ButtonType> typeToButton = new HashMap<String, ButtonType>();
  private int numSections = 0;
  private Panel panelInsideScrollPanel;
  private ScrollPanel scrollPanel;
  private Panel clearColumnContainer;
  private Panel labelColumn;
  private final Heading statusHeader = new Heading(4);
  private final Panel sectionPanel;
  private final DownloadHelper downloadHelper;
  private int rememberedID = -1;

  /**
   * @param secondRow             add the section panel to this row
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param controller
   * @param instance
   * @param incorrectFirst
   * @param showFirstNotCompleted
   * @param activityType
   * @see NPFlexSectionExerciseList#NPFlexSectionExerciseList
   */
  protected FlexSectionExerciseList(Panel secondRow,
                                    Panel currentExerciseVPanel,
                                    ExerciseServiceAsync service,
                                    UserFeedback feedback,
                                    ExerciseController controller,
                                    String instance,
                                    boolean incorrectFirst,
                                    boolean showFirstNotCompleted, ActivityType activityType) {
    super(currentExerciseVPanel, service, feedback, controller, true, instance, incorrectFirst, showFirstNotCompleted, activityType);

    sectionPanel = new FluidContainer();
    sectionPanel.getElement().setId("sectionPanel_" + instance);

    sectionPanel.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    sectionPanel.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
    secondRow.add(new Column(12, sectionPanel));

    buttonTypes.add(ButtonType.PRIMARY);
    buttonTypes.add(ButtonType.SUCCESS);
    buttonTypes.add(ButtonType.INFO);
    buttonTypes.add(ButtonType.WARNING);
    setUnaccountedForVertical(CLASSROOM_VERTICAL_EXTRA);
    downloadHelper = new DownloadHelper(this);
  }

  protected SectionWidgetContainer<ButtonGroupSectionWidget> getSectionWidgetContainer() {
    return new SectionWidgetContainer<ButtonGroupSectionWidget>() {
      /**
       * @see #restoreListBoxState(SelectionState, Collection)
       * @param type
       * @param sections
       */
      protected void selectItem(String type, Collection<String> sections) {
        //   logger.info("FlexSectionExerciseList.selectItem : selecting " + type + "=" + sections);
        ButtonGroupSectionWidget listBox = getGroupSection(type);
        listBox.clearSelectionState();
        listBox.selectItem(sections);
      }
    };
  }

  /**
   * @param userID
   * @seex mitll.langtest.client.InitialUI#configureUIGivenUser
   */
  public boolean getExercises(final long userID) {
    //logger.info("FlexSectionExerciseList.getExercises : Get exercises for user=" + userID + " instance " + getInstance());
    this.userID = userID;
    addWidgets();
    return false;
  }

  @Override
  public void addWidgets() {
    sectionPanel.clear();
    sectionPanel.add(getWidgetsForTypes());
  }

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @return
   * @see mitll.langtest.client.list.ListInterface#getExercises(long)
   */
  private Panel getWidgetsForTypes() {
    final FluidContainer container = new FluidContainer();
    container.getElement().setId("typeOrderContainer");
    container.getElement().getStyle().setPaddingLeft (2, Style.Unit.PX);
    container.getElement().getStyle().setPaddingRight(2, Style.Unit.PX);

    getTypeOrder(container);

    return container;
  }

  private void getTypeOrder(final FluidContainer container) {
    ProjectStartupInfo startupInfo = controller.getProjectStartupInfo();
    if (startupInfo != null) {
      addButtonRow(startupInfo.getSectionNodes(), container, controller.getTypeOrder());
    }
  }

  /**
   * @param rootNodes
   * @param container
   * @param types
   * @see #getTypeOrder(com.github.gwtbootstrap.client.ui.FluidContainer)
   */
  private void addButtonRow(Collection<SectionNode> rootNodes, final FluidContainer container, Collection<String> types) {
/*    System.out.println("FlexSectionExerciseList.addButtonRow for user = " + userID + " got types " +
      types + " num root nodes " + rootNodes.size() + " instance " + instance);*/
    if (types.isEmpty()) {
      logger.warning("huh? types is empty?");
      return;
    }
    showDefaultStatus();

    FlexTable firstTypeRow = new FlexTable();
    firstTypeRow.getElement().setId("firstTypeRow");
    container.add(firstTypeRow);
    firstTypeRow.addStyleName("alignTop");

    populateButtonGroups(types);

    String firstType = types.iterator().next(); // e.g. unit!
    ButtonGroupSectionWidget buttonGroupSectionWidget = getGroupSection(firstType);

    boolean usuallyThereWillBeAHorizScrollbar = rootNodes.size() > 6;

    this.labelColumn = makeLabelColumn(firstType, firstTypeRow, buttonGroupSectionWidget);
    this.clearColumnContainer = makeClearColumn(usuallyThereWillBeAHorizScrollbar, types, firstType, firstTypeRow, buttonGroupSectionWidget);

    makePanelInsideScrollPanel(firstTypeRow);

    // add columns for each section within first type...

    Collection<String> sectionsInType = new ItemSorter().getSortedItems(getLabels(rootNodes));
    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);
    numSections = sectionsInType.size();

    List<String> subTypes = new ArrayList<String>(types);
    List<String> subs = subTypes.subList(1, subTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    // add a column for every root type
    Widget last = null;
    long then = System.currentTimeMillis();
    for (String sectionInFirstType : sectionsInType) {
      Panel sectionColumn = new FlowPanel();
      ButtonWithChildren buttonWithChildren = addColumnButton(sectionColumn, sectionInFirstType, buttonGroupSectionWidget);
      buttonWithChildren.setButtonGroup(buttonGroupSectionWidget);
      last = sectionColumn;
      sectionColumn.getElement().getStyle().setMarginBottom((usuallyThereWillBeAHorizScrollbar ? 5 : 0), Style.Unit.PX);
      panelInsideScrollPanel.add(sectionColumn);

      if (subType != null) {
        ButtonGroupSectionWidget sectionWidget1 = getGroupSection(subType);

        // System.out.println("addButtonRow adding row for " + subType + " under " + sectionInFirstType);

        HorizontalPanel rowForChildren = new HorizontalPanel();
        rowForChildren.setWidth("100%");
        rowForChildren.setHorizontalAlignment(ALIGN_LEFT);
        SectionNode sectionNode = nameToNode.get(sectionInFirstType);
        buttonWithChildren.setChildren(addButtonGroup(rowForChildren, sectionNode.getChildren(),
            subType, subs, sectionWidget1));
        //sectionColumn.add(rowForChildren);

        // make row container
        Panel rowContainer = new FlowPanel();
        rowContainer.addStyleName("rowPadding");
        rowContainer.add(rowForChildren);
        sectionColumn.add(rowContainer);

        sectionWidget1.addRow(rowContainer);
      }
    }

    long now = System.currentTimeMillis();
    if (now - then > 300)
      logger.info("\taddButtonRow took " + (now - then) + " millis" + " instance " + getInstance());

    if (last != null) setSizesAndPushFirst();

    DivWidget bottomRow = getBottomRow();
    addBottomText(bottomRow);
    container.add(bottomRow);
  }

  private ButtonGroupSectionWidget getGroupSection(String type) {
    return sectionWidgetContainer.getWidget(type);
  }

  private DivWidget getBottomRow() {
    Panel links = downloadHelper.getDownloadButton();
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

  /**
   * Label is in column 0
   *
   * @param firstType
   * @param firstTypeRow
   * @param buttonGroupSectionWidget
   * @see #addButtonRow
   */
  private Panel makeLabelColumn(String firstType, FlexTable firstTypeRow,
                                ButtonGroupSectionWidget buttonGroupSectionWidget) {
    Panel labelColumn = new VerticalPanel();
    labelColumn.getElement().setId("FlexSectionExerciseList_labelColumn");
    addLabelWidgetForRow(labelColumn, firstType, typeToButton.get(firstType), buttonGroupSectionWidget);
    firstTypeRow.setWidget(0, 0, makeFlowPanel(labelColumn, false));
    return labelColumn;
  }

  /**
   * Clear button is in column 1
   *
   * @param usuallyThereWillBeAHorizScrollbar
   * @param types
   * @param firstType
   * @param firstTypeRow
   * @param buttonGroupSectionWidget
   * @see #addButtonRow
   */
  private Panel makeClearColumn(boolean usuallyThereWillBeAHorizScrollbar, Collection<String> types, String firstType,
                                FlexTable firstTypeRow,
                                ButtonGroupSectionWidget buttonGroupSectionWidget) {
    Panel clearColumnContainer = new VerticalPanel();
    addClearButton(buttonGroupSectionWidget, clearColumnContainer);
    firstTypeRow.setWidget(0, 1, makeFlowPanel(clearColumnContainer, usuallyThereWillBeAHorizScrollbar));

    for (String type : types) {
      if (type.equals(firstType)) continue;
      makeSectionWidget(labelColumn, clearColumnContainer, type, getGroupSection(type));
    }
    return clearColumnContainer;
  }

  private void addClearButton(ButtonGroupSectionWidget buttonGroupSectionWidget, Panel clearColumnContainer) {
    ButtonWithChildren clearButton = makeClearButton();
    addClickHandlerToButton(clearButton, ANY, buttonGroupSectionWidget);
    buttonGroupSectionWidget.addButton(clearButton);
    clearColumnContainer.add(clearButton);
  }

  /**
   * @param labelContainer
   * @param usuallyThereWillBeAHorizScrollbar
   * @return
   * @see #makeLabelColumn
   */
  private Panel makeFlowPanel(Panel labelContainer, boolean usuallyThereWillBeAHorizScrollbar) {
    Panel l2 = new FlowPanel();
    l2.add(labelContainer);
/*    if (usuallyThereWillBeAHorizScrollbar) { // hack rule of thumb
      l2.addStyleName("bottomMargin");
    }*/
    return l2;
  }

  /**
   * @param firstTypeRow
   * @see #addButtonRow
   */
  private void makePanelInsideScrollPanel(FlexTable firstTypeRow) {
    panelInsideScrollPanel = new HorizontalPanel();
    panelInsideScrollPanel.addStyleName("blueBackground");
    panelInsideScrollPanel.addStyleName("borderSpacing");
    panelInsideScrollPanel.getElement().setId("panelInsideScrollPanel");
    makeScrollPanel(firstTypeRow, panelInsideScrollPanel);
  }

  /**
   * @param firstTypeRow
   * @param panelInside
   * @see #makePanelInsideScrollPanel(com.google.gwt.user.client.ui.FlexTable)
   * Button groups are in column 2
   */
  private void makeScrollPanel(FlexTable firstTypeRow, Panel panelInside) {
    this.scrollPanel = new ScrollPanel(panelInside);
    this.scrollPanel.addStyleName("leftFiveMargin");
    this.scrollPanel.getElement().setId("scrollPanel");
    firstTypeRow.setWidget(0, 2, scrollPanel);
  }

  /**
   * @param types
   * @see #addButtonRow(java.util.Collection, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   */
  private void populateButtonGroups(Collection<String> types) {
    sectionWidgetContainer.clear();
    typeToButton.clear();

    int j = 0;
    for (String type : types) {
      ButtonGroupSectionWidget buttonGroupSectionWidget1 = new ButtonGroupSectionWidget(type);
      sectionWidgetContainer.setWidget(type, buttonGroupSectionWidget1);
      typeToButton.put(type, buttonTypes.get(j++ % types.size()));
    }
  }

  private Map<String, SectionNode> getNameToNode(Collection<SectionNode> rootNodes) {
    Map<String, SectionNode> nameToNode = new HashMap<String, SectionNode>();
    for (SectionNode n : rootNodes) nameToNode.put(n.getName(), n);
    return nameToNode;
  }

  private List<String> getLabels(Collection<SectionNode> nodes) {
    List<String> items = new ArrayList<String>();
    for (SectionNode n : nodes) items.add(n.getName());
    return items;
  }

  /**
   * @param labelRow
   * @param firstType
   * @param buttonType
   * @param buttonGroupSectionWidget
   * @see #makeLabelColumn(String, com.google.gwt.user.client.ui.FlexTable, ButtonGroupSectionWidget)
   */
  private void addLabelWidgetForRow(Panel labelRow, String firstType, ButtonType buttonType, SectionWidget buttonGroupSectionWidget) {
    Widget widget = makeLabelWidget(firstType);
    String color = getButtonTypeStyle(buttonType);

    buttonGroupSectionWidget.addLabel(widget, color);
    labelRow.add(widget);
  }

  private Heading makeLabelWidget(String firstType) {
    Heading widget = new Heading(HEADING_FOR_LABEL, firstType);
    // TODO : necessary?
    //DOM.setStyleAttribute(widget.getElement(), "webkitMarginBefore", "0");
    // DOM.setStyleAttribute(widget.getElement(), "webkitMarginAfter", "0");
    widget.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    widget.getElement().getStyle().setMarginBottom(LABEL_MARGIN_BOTTOM, Style.Unit.PX);
    widget.getElement().getStyle().setProperty("whiteSpace", "nowrap");
    return widget;
  }

  /**
   * @param selectionState
   * @see HistoryExerciseList#restoreUIState
   */
  @Override
  protected void restoreListBoxState(SelectionState selectionState) {
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
    // logger.info("FlexSectionExerciseList.showSelectionState : state '" + selectionState +"'");
    // keep the download link info in sync with the selection
    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();
    downloadHelper.updateDownloadLinks(selectionState,controller.getTypeOrder());
    statusHeader.setText(selectionState.getDescription(controller.getTypeOrder()));
  }

  private void showDefaultStatus() {
    statusHeader.setText(SelectionState.SHOWING_ALL_ENTRIES);
  }

  /**
   * @param container
   * @see #addButtonRow
   */
  private void addBottomText(Panel container) {
    Panel status = getStatusRow();
    container.add(status);
    status.addStyleName("leftFiftyPercentMargin");
  }

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
   * @param sectionInFirstType
   * @param buttonGroupSectionWidget
   * @return
   * @see #addButtonRow
   */
  private ButtonWithChildren addColumnButton(Panel columnContainer,
                                             final String sectionInFirstType,
                                             final ButtonGroupSectionWidget buttonGroupSectionWidget) {
    columnContainer.addStyleName("inlineBlockStyle");
    // add a button
    ButtonWithChildren overallButton = makeOverallButton(buttonGroupSectionWidget.getType(), sectionInFirstType);
    addClickHandlerToButton(overallButton, sectionInFirstType, buttonGroupSectionWidget);
    buttonGroupSectionWidget.addButton(overallButton);

    Panel rowAgain = new FlowPanel();
    columnContainer.add(rowAgain);

    rowAgain.add(overallButton);
    buttonGroupSectionWidget.addRow(rowAgain);
    rowAgain.addStyleName("rowPadding");

    return overallButton;
  }

  /**
   * @param overallButton
   * @param sectionInFirstType
   * @param buttonGroupSectionWidget
   * @see #addClearButton(ButtonGroupSectionWidget, Panel)
   * @see #addColumnButton(Panel, String, ButtonGroupSectionWidget)
   */
  private void addClickHandlerToButton(final ButtonWithChildren overallButton, final String sectionInFirstType,
                                       final ButtonGroupSectionWidget buttonGroupSectionWidget) {
    overallButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        buttonGroupSectionWidget.simpleSelectItem(Collections.singleton(sectionInFirstType));
        pushNewSectionHistoryToken();
      }
    });
  }

  /**
   * if we can't find the exercise b/c the current list is for a chapter, clear all chapter selections
   *
   * @param id
   * @return
   */
  @Override
  public boolean loadByID(int id) {
   // logger.info("loadByID loading exercise " + id);
    if (hasExercise(id)) {
      //  logger.info("loadByID found exercise " + id);
      loadExercise(id);
      return true;
    } else {
      setHistoryItem("search=;item=" + id);
      rememberedID = id;
      return false;
    }
  }

  protected void listLoaded() {
    if (rememberedID != -1) {
      if (hasExercise(rememberedID)) {
        // logger.info("loading exercise " + id);
        loadExercise(rememberedID);
      } else {
        logger.warning("listLoaded no exercise with " + rememberedID);
      }
    }
    rememberedID = -1;
  }

  /**
   * @seex HistoryExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.exercise.ExerciseListWrapper)
   */
  protected void gotEmptyExerciseList() {
    showEmptySelection();
  }

  /**
   * @param title
   * @return Button with this title and the initial type
   * @see #addColumnButton
   */
  private ButtonWithChildren makeOverallButton(String type, String title) {
    ButtonWithChildren overallButton = makeButton(title, type);
    overallButton.setWidth("100%");
    overallButton.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    overallButton.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);

    overallButton.setType(ButtonType.PRIMARY);
    return overallButton;
  }

  private ButtonWithChildren makeClearButton() {
    ButtonWithChildren clear = makeButton(ANY, ANY);
    clear.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
    clear.getElement().getStyle().setMarginBottom(12, Style.Unit.PX);
    clear.setType(ButtonType.DEFAULT);
    return clear;
  }

  /**
   * Actually kick off getting the exercises.
   *
   * @see #addButtonRow(java.util.Collection, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   */
  private void setSizesAndPushFirst() {
    pushFirstListBoxSelection();
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        setScrollPanelWidth();
      }
    });
  }

  /**
   * Add label and clear button to row for each type
   *
   * @param labelContainer
   * @param clearColumnContainer
   * @param typeForOriginal
   * @param sectionWidget
   * @return
   * @see #makeClearColumn(boolean, java.util.Collection, String, com.google.gwt.user.client.ui.FlexTable, ButtonGroupSectionWidget)
   */
  private SectionWidget makeSectionWidget(Panel labelContainer, Panel clearColumnContainer, String typeForOriginal,
                                          ButtonGroupSectionWidget sectionWidget) {
    // make label
    Widget labelWidget = getLabelWidget(typeForOriginal);
    labelContainer.add(labelWidget);
    ButtonType buttonType = typeToButton.get(typeForOriginal);
    sectionWidget.addLabel(labelWidget, getButtonTypeStyle(buttonType));

    // make clear button
    Button sectionButton = makeSubgroupButton(sectionWidget, ANY, buttonType, true);
    sectionWidget.addButton(sectionButton);

    clearColumnContainer.add(sectionButton);
    return sectionWidget;
  }

  private String getButtonTypeStyle(ButtonType buttonType) {
    return (buttonType.equals(ButtonType.PRIMARY)) ? "primaryButtonColor" : buttonType.equals(ButtonType.SUCCESS) ? "successButtonColor"
        : (buttonType.equals(ButtonType.INFO)) ? "infoButtonColor" : "warningButtonColor";
  }

  /**
   * @param parentColumn
   * @param rootNodes
   * @param typeForOriginal
   * @param remainingTypes
   * @param sectionWidget
   * @see FlexSectionExerciseList#addButtonRow
   */
  private List<ButtonWithChildren> addButtonGroup(Panel parentColumn, Collection<SectionNode> rootNodes,
                                                  String typeForOriginal,
                                                  List<String> remainingTypes, ButtonGroupSectionWidget sectionWidget) {
    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);

    List<String> sortedItems = new ItemSorter().getSortedItems(getLabels(rootNodes));
    ButtonType buttonType = typeToButton.get(typeForOriginal);

    List<String> objects = Collections.emptyList();
    List<String> subs = remainingTypes.isEmpty() ? objects : remainingTypes.subList(1, remainingTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    int n = sortedItems.size();
    List<ButtonWithChildren> buttonChildren = new ArrayList<ButtonWithChildren>();
    for (int i = 0; i < n; i++) {
      String section = sortedItems.get(i);
      SectionNode sectionNode = nameToNode.get(section);

      Collection<SectionNode> children = sectionNode.getChildren();

      ButtonWithChildren buttonForSection = makeSubgroupButton(sectionWidget, section, buttonType, false);
      buttonChildren.add(buttonForSection);
      buttonForSection.setButtonGroup(sectionWidget);

      // System.out.println("addButtonGroup : for " + typeForOriginal + "=" + section + " recurse num children = " + children.size() + " group " + sectionWidget + " for " + buttonForSection);

      if ((n > 1 && i < n - 1) || n == 1) {
        buttonForSection.addStyleName("buttonMargin");
      }

      Panel rowForSection;
      if (!children.isEmpty() && subType != null) {
        rowForSection = new VerticalPanel();

        buttonForSection.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
        buttonForSection.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
        buttonForSection.setWidth("100%");
        rowForSection.add(buttonForSection);

/*        Panel containerForButtons = new SimplePanel();
        containerForButtons.add(buttonForSection);
        rowForSection.add(containerForButtons);
        sectionWidget.addRow(containerForButtons);*/

        // recurse on children
        HorizontalPanel horizontalContainerForChildren = new HorizontalPanel();
        rowForSection.add(horizontalContainerForChildren);
        sectionWidget.addRow(rowForSection);
        rowForSection.addStyleName("rowPadding");

//        logger.info("\taddButtonGroup : for " + typeForOriginal + "=" + section + " recurse on " + children.size() +
//            " children at " + subType);
        ButtonGroupSectionWidget sectionWidget1 = getGroupSection(subType);
        List<ButtonWithChildren> subButtons =
            addButtonGroup(horizontalContainerForChildren, children, subType, subs, sectionWidget1);
        buttonForSection.setChildren(subButtons);
        //buttonForSection.setButtonGroup(sectionWidget1);
      } else {

        rowForSection = buttonForSection;

      /*  rowForSection = new SimplePanel();
        rowForSection.addStyleName("topFiveMargin");
        sectionWidget.addRow(rowForSection);
        rowForSection.add(buttonForSection);*/
      }
      parentColumn.add(rowForSection);
    }
    return buttonChildren;
  }

  @Override
  public void onResize() {
    super.onResize();

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        //System.out.println("FlexSectionExerciseList : deferred set scroll panel width instance " + instance);
        setScrollPanelWidth();
      }
    });
  }

  private void setScrollPanelWidth() {
    if (labelColumn != null && clearColumnContainer != null) {
      int leftSideWidth = labelColumn.getOffsetWidth() + clearColumnContainer.getOffsetWidth();
      if (leftSideWidth == 0) leftSideWidth = 130;
      int width = Window.getClientWidth() - leftSideWidth - UNACCOUNTED_WIDTH;
/*      System.out.println("FlexSectionExeciseList.setScrollPanelWidth : scrollPanel width is " + width +" client " +Window.getClientWidth() +
        " label col " +labelColumn.getOffsetWidth() + " clear " +clearColumnContainer.getOffsetWidth() + " unacct "+UNACCOUNTED_WIDTH);*/
      scrollPanel.setWidth(Math.max(300, width) + "px");
    }
//    else {
    // System.out.println("\tsetScrollPanelWidth : labelColumn is null instance " + instance);
    //  }
  }

  /**
   * @param typeForOriginal
   * @return
   * @see #makeSectionWidget
   */
  private Widget getLabelWidget(String typeForOriginal) {
    Heading widget = new Heading(HEADING_FOR_LABEL, typeForOriginal);
    Style style = widget.getElement().getStyle();
    style.setMarginBottom(10, Style.Unit.PX);
    style.setMarginRight(5, Style.Unit.PX);
    return widget;
  }

  /**
   * @param sectionWidgetFinal
   * @param section
   * @param buttonType
   * @param isClear
   * @return
   * @see #addButtonGroup
   * @see #makeSectionWidget
   */
  private ButtonWithChildren makeSubgroupButton(final ButtonGroupSectionWidget sectionWidgetFinal,
                                                final String section,
                                                ButtonType buttonType, boolean isClear) {
    //System.out.println("making button " + type + "=" + section);
    ButtonWithChildren sectionButton = makeButton(section, sectionWidgetFinal.getType());

    //  System.out.println("made button " + sectionButton);

    boolean shrinkHorizontally = numSections > 5;
    //  System.out.println("num " + numSections + " shrinkHorizontally " +shrinkHorizontally );

    if (!isClear && shrinkHorizontally) { // squish buttons if we have lots of sections
      sectionButton.getElement().getStyle().setPaddingLeft(6, Style.Unit.PX);
      sectionButton.getElement().getStyle().setPaddingRight(6, Style.Unit.PX);
    }

    sectionButton.setType(isClear ? ButtonType.DEFAULT : buttonType);
    sectionButton.setEnabled(!isClear);

    if (isClear) {
      sectionButton.getElement().getStyle().setMarginBottom(LABEL_MARGIN_BOTTOM, Style.Unit.PX);
    }

    addClickHandlerToButton(sectionButton, section, sectionWidgetFinal);

    sectionWidgetFinal.addButton(sectionButton);

    return sectionButton;
  }

  private ButtonWithChildren makeButton(String caption, String type) {
    ButtonWithChildren widgets = new ButtonWithChildren(caption, type);
    controller.register(widgets, "unknown");

    return widgets;
  }

  static class ButtonWithChildren extends Button {
    private List<ButtonWithChildren> children = new ArrayList<ButtonWithChildren>();
    private final String type;
    private ButtonWithChildren parent;
    private ButtonGroupSectionWidget buttonGroupContainer;

    public ButtonWithChildren(String caption, String type) {
      super(caption);
      this.type = type;
      getElement().setId("Button_" + caption + "_" + type);
    }

    /**
     * @param children
     * @see FlexSectionExerciseList#addButtonGroup
     * @see FlexSectionExerciseList#addButtonRow
     */
    public void setChildren(List<ButtonWithChildren> children) {
      this.children = children;
      for (ButtonWithChildren child : children) {
        child.setParentButton(this);
      }
    }

    public void setButtonGroup(ButtonGroupSectionWidget buttonGroupContainer) {
      this.buttonGroupContainer = buttonGroupContainer;
    }

    public ButtonGroupSectionWidget getButtonGroup() {
      return buttonGroupContainer;
    }

    public ButtonWithChildren getParentButton() {
      return parent;
    }

    public void setParentButton(ButtonWithChildren parent) {
      this.parent = parent;
    }

    public List<ButtonWithChildren> getButtonChildren() {
      return children;
    }

    public String getButtonType() {
      return type;
    }

    public String getTypeOfChildren() {
      return children.isEmpty() ? "" : children.iterator().next().getButtonType();
    }

    public String toString() {
      return "Button " + type + " = " + getText().trim() +
          (children.isEmpty() ? "" :
              ", children type " + getTypeOfChildren() + " num " + getButtonChildren().size());
    }
  }
}
