package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.client.list.ItemSorter;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.SectionNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/5/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlexSectionExerciseList extends HistoryExerciseList {
  private static final int HEADING_FOR_LABEL = 4;
 // public static final int PANEL_INSIDE_SCROLL_MIN_HEIGHT = 50;
 // public static final int PANEL_INSIDE_SCROLL_MIN_HEIGHT_SMALL = 30;
  private static final int UNACCOUNTED_WIDTH = 150;

  private final List<ButtonType> buttonTypes = new ArrayList<ButtonType>();
  private Map<String, ButtonType> typeToButton = new HashMap<String, ButtonType>();
  private int numSections = 0;
  private Panel panelInsideScrollPanel;
  private ScrollPanel scrollPanel;
  private Panel clearColumnContainer;
  private Panel labelColumn;
  protected Heading statusHeader = new Heading(4);
  private Collection<String> typeOrder;
  private Panel sectionPanel;

  /**
   * @see mitll.langtest.client.ExerciseListLayout#makeExerciseList(com.github.gwtbootstrap.client.ui.FluidRow, boolean, mitll.langtest.client.user.UserFeedback, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @param secondRow
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param showInOrder
   * @param controller
   * @param showTypeAhead @paramx showListBox
   * @param instance          */
  public FlexSectionExerciseList(FluidRow secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                 UserFeedback feedback,
                                 boolean showTurkToken, boolean showInOrder,
                                 ExerciseController controller,
                                 boolean showTypeAhead, String instance) {
    super(currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, controller, showTypeAhead, instance);

    sectionPanel = new FluidContainer();
    sectionPanel.getElement().setId("sectionPanel");

    DOM.setStyleAttribute(sectionPanel.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(sectionPanel.getElement(), "paddingRight", "2px");
    secondRow.add(new Column(12, sectionPanel));

    buttonTypes.add(ButtonType.PRIMARY);
    buttonTypes.add(ButtonType.SUCCESS);
    buttonTypes.add(ButtonType.INFO);
    buttonTypes.add(ButtonType.WARNING);
  }

  @Override
  protected void addComponents() {
    PagingContainer<? extends ExerciseShell> exerciseShellPagingContainer = makePagingContainer();
    addTableWithPager(exerciseShellPagingContainer);
  }

  /**
   *
   * @param userID
   * @param getNext
   * @see mitll.langtest.client.LangTest#doEverythingAfterFactory
   */
  public void getExercises(final long userID, boolean getNext) {
    System.out.println("FlexSectionExerciseList : getExercises : Get exercises for user=" + userID + " crt mode " + allowPlusInURL);
    this.userID = userID;

    if (controller.showCompleted()) {
      service.getCompletedExercises((int)userID, controller.isReviewMode(), new AsyncCallback<Set<String>>() {
        @Override
        public void onFailure(Throwable caught) {}
        @Override
        public void onSuccess(Set<String> result) {
          System.out.println("\n\n\n\tFlexSectionExerciseList : getCompletedExercises : completed for user=" + userID + " result " + result.size());
          controller.getExerciseList().setCompleted(result);
          addWidgets();
        }
      });
    }
    else {
      addWidgets();
    }
  }

  private void addWidgets() {
    sectionPanel.clear();
    Panel flexTable = getWidgetsForTypes();
    sectionPanel.add(flexTable);
  }

  /**
   * @see #loadExercises(java.util.Map, String)
   * @see #pushNewSectionHistoryToken()
   * @param userID
   */
  protected void noSectionsGetExercises(long userID) {
    super.getExercises(userID, true);
  }

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @return
   * @see mitll.langtest.client.list.ListInterface#getExercises(long, boolean)
   */
  private Panel getWidgetsForTypes() {
    final FluidContainer container = new FluidContainer();
    container.getElement().setId("typeOrderContainer");
    DOM.setStyleAttribute(container.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(container.getElement(), "paddingRight", "2px");

    getTypeOrder(container);

    return container;
  }

  private void getTypeOrder(final FluidContainer container) {
    typeOrder = controller.getStartupInfo().getTypeOrder();
    addButtonRow(controller.getStartupInfo().getSectionNodes(), container, typeOrder, !controller.isGoodwaveMode());
  }

  @Override
  protected Collection<String> getTypeOrder(Map<String, Collection<String>> selectionState2) {
    return typeOrder;
  }

/*  private void addStudentTypeAndSection(FluidContainer container, Collection<String> sortedTypes) {
    String token = unencodeToken(History.getToken());
    SelectionState selectionState = getSelectionState(token);
    //System.out.println("\n\nsorted types " + sortedTypes);
    for (final String type : sortedTypes) {
      Collection<String> typeValue = selectionState.getTypeToSection().get(type);
      if (typeValue != null) {
        FluidRow fluidRow = new FluidRow();
        container.add(fluidRow);

        fluidRow.add(new Column(2, new Heading(4, type)));
        fluidRow.add(new Column(1, new Heading(4, typeValue.toString())));
      }
    }
  }*/

  /**
   * @param rootNodes
   * @param container
   * @param types
   * @param addInstructions
   * @see #getTypeOrder(com.github.gwtbootstrap.client.ui.FluidContainer)
   */
  private void addButtonRow(List<SectionNode> rootNodes, FluidContainer container, Collection<String> types,
                            boolean addInstructions) {
    //System.out.println("addButtonRow (success) for user = " + userID + " got types " + types + " num root nodes " + rootNodes.size());
    if (types.isEmpty()) {
      System.err.println("huh? types is empty?");
      return;
    }
    showDefaultStatus();

    if (addInstructions && getUserPrompt().length() > 0) container.add(getInstructionRow());
    FlexTable firstTypeRow = new FlexTable();
    firstTypeRow.getElement().setId("firstTypeRow");
    container.add(firstTypeRow);
    firstTypeRow.addStyleName("alignTop");
   // firstTypeRow.addStyleName("positionAbsolute");

    populateButtonGroups(types);

    String firstType = types.iterator().next(); // e.g. unit!
    ButtonGroupSectionWidget buttonGroupSectionWidget = (ButtonGroupSectionWidget) typeToBox.get(firstType);

    boolean usuallyThereWillBeAHorizScrollbar = rootNodes.size() > 6;

    makeLabelColumn(usuallyThereWillBeAHorizScrollbar, firstType, firstTypeRow, buttonGroupSectionWidget);
    makeClearColumn(usuallyThereWillBeAHorizScrollbar, types, firstType, firstTypeRow, buttonGroupSectionWidget);

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
      FlowPanel sectionColumn = new FlowPanel();
      ButtonWithChildren buttonWithChildren = addColumnButton(sectionColumn, sectionInFirstType, buttonGroupSectionWidget);
      buttonWithChildren.setButtonGroup(buttonGroupSectionWidget);
      last = sectionColumn;
      DOM.setStyleAttribute(sectionColumn.getElement(), "marginBottom", (usuallyThereWillBeAHorizScrollbar ? 5 :0) + "px");
      panelInsideScrollPanel.add(sectionColumn);

      if (subType != null) {
        SectionNode sectionNode = nameToNode.get(sectionInFirstType);
        ButtonGroupSectionWidget sectionWidget1 = (ButtonGroupSectionWidget) typeToBox.get(subType);
        Panel rowContainer = new FlowPanel();
        rowContainer.addStyleName("rowPadding");

       // System.out.println("addButtonRow adding row for " + subType + " under " + sectionInFirstType);

        HorizontalPanel rowForChildren = new HorizontalPanel();
        rowForChildren.setWidth("100%");
        rowForChildren.setHorizontalAlignment(ALIGN_LEFT);
        List<ButtonWithChildren> buttonWithChildrens = addButtonGroup(rowForChildren, sectionNode.getChildren(),
          subType, subs, sectionWidget1);
        buttonWithChildren.setChildren(buttonWithChildrens);
        //sectionColumn.add(rowForChildren);
        rowContainer.add(rowForChildren);
        sectionColumn.add(rowContainer);

        sectionWidget1.addRow(rowContainer);
      }
    }
    long now = System.currentTimeMillis();
    if (now - then > 200) System.out.println("\taddButtonRow took " + (now - then) + " millis");

    if (last != null) setSizesAndPushFirst(last);
    addBottomText(container);
  }

  private void makeLabelColumn(boolean usuallyThereWillBeAHorizScrollbar, String firstType, FlexTable firstTypeRow,
                               ButtonGroupSectionWidget buttonGroupSectionWidget) {
    this.labelColumn = new VerticalPanel();
    addLabelWidgetForRow(labelColumn, firstType, typeToButton.get(firstType), buttonGroupSectionWidget);

    FlowPanel l2 = makeFlowPanel(labelColumn, usuallyThereWillBeAHorizScrollbar);

    firstTypeRow.setWidget(0, 0, l2);
  }

  private void makeClearColumn(boolean usuallyThereWillBeAHorizScrollbar, Collection<String> types, String firstType,
                               FlexTable firstTypeRow,
                               ButtonGroupSectionWidget buttonGroupSectionWidget) {
    clearColumnContainer = new VerticalPanel();
    addClearButton(buttonGroupSectionWidget, clearColumnContainer);
    FlowPanel c2 = makeFlowPanel(clearColumnContainer, usuallyThereWillBeAHorizScrollbar);

    firstTypeRow.setWidget(0, 1, c2);

    for (String type : types) {
      if (type.equals(firstType)) continue;
      makeSectionWidget(labelColumn, clearColumnContainer, type, (ButtonGroupSectionWidget) typeToBox.get(type));
    }
  }

  private FlowPanel makeFlowPanel(Panel labelContainer, boolean usuallyThereWillBeAHorizScrollbar) {
    FlowPanel l2 = new FlowPanel();
    l2.add(labelContainer);
    if (usuallyThereWillBeAHorizScrollbar) { // hack rule of thumb
      l2.addStyleName("bottomMargin");
    }
    return l2;
  }

  private void addClearButton(ButtonGroupSectionWidget buttonGroupSectionWidget, Panel clearColumnContainer) {
    ButtonWithChildren clearButton = makeClearButton();
    addClickHandlerToButton(clearButton, ANY, buttonGroupSectionWidget);
    buttonGroupSectionWidget.addButton(clearButton);
    clearColumnContainer.add(clearButton);
  }

  /**
   * @see #addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection, boolean)
   * @param firstTypeRow
   */
  private void makePanelInsideScrollPanel(FlexTable firstTypeRow) {
    panelInsideScrollPanel = new HorizontalPanel();
    panelInsideScrollPanel.addStyleName("blueBackground");
    panelInsideScrollPanel.addStyleName("borderSpacing");
    panelInsideScrollPanel.getElement().setId("panelInsideScrollPanel");
    makeScrollPanel(firstTypeRow, panelInsideScrollPanel);
  }

  /**
   * @see #makePanelInsideScrollPanel(com.google.gwt.user.client.ui.FlexTable)
   * @param firstTypeRow
   * @param panelInside
   */
  private void makeScrollPanel(FlexTable firstTypeRow, Panel panelInside) {
    this.scrollPanel = new ScrollPanel(panelInside);
    this.scrollPanel.addStyleName("leftFiveMargin");
    this.scrollPanel.getElement().setId("scrollPanel");

/*    DivWidget div = new DivWidget();
    div.addStyleName("positionAbsolute");
    div.add(scrollPanel);*/
    firstTypeRow.setWidget(0, 2, scrollPanel);
   // setScrollPanelWidth();
  }

  private void populateButtonGroups(Collection<String> types) {
    typeToBox.clear();
    typeToButton.clear();

    int j = 0;
    for (String type : types) {
      ButtonGroupSectionWidget buttonGroupSectionWidget1 = new ButtonGroupSectionWidget(type);
      typeToBox.put(type, buttonGroupSectionWidget1);
      typeToButton.put(type, buttonTypes.get(j++ % types.size()));
    }
  }

  private Map<String, SectionNode> getNameToNode(List<SectionNode> rootNodes) {
    Map<String, SectionNode> nameToNode = new HashMap<String, SectionNode>();
    for (SectionNode n : rootNodes) nameToNode.put(n.getName(), n);
    return nameToNode;
  }

  private List<String> getLabels(List<SectionNode> nodes) {
    List<String> items = new ArrayList<String>();
    for (SectionNode n : nodes) items.add(n.getName());
    return items;
  }

  private void addLabelWidgetForRow(Panel labelRow, String firstType, ButtonType buttonType, SectionWidget buttonGroupSectionWidget) {
    Heading widget = makeLabelWidget(firstType);
    String color = getButtonTypeStyle(buttonType);

    buttonGroupSectionWidget.addLabel(widget, color);
    labelRow.add(widget);
  }

  private Heading makeLabelWidget(String firstType) {
    Heading widget = new Heading(HEADING_FOR_LABEL, firstType);
    DOM.setStyleAttribute(widget.getElement(), "webkitMarginBefore", "0");
    DOM.setStyleAttribute(widget.getElement(), "webkitMarginAfter", "0");
    DOM.setStyleAttribute(widget.getElement(), "marginTop", "0px");
    DOM.setStyleAttribute(widget.getElement(), "marginBottom", "15px");
    return widget;
  }

  /**
   * @see HistoryExerciseList#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param selectionState
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
    //System.out.println("FlexSectionExerciseList.showSelectionState : got " + event + " and state '" + selectionState +"'");
    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();

    if (typeToSection.isEmpty()) {
      showDefaultStatus();
    } else {
      StringBuilder status = new StringBuilder();

      System.out.println("showSelectionState : typeOrder " + typeOrder + " selection state " + typeToSection);

      for (String type : typeOrder) {
        Collection<String> selectedItems = typeToSection.get(type);
        if (selectedItems == null) {
          //System.out.println("showSelectionState : no value for '" + type + "' in " + typeToSection.keySet());
        } else {
          String statusForType = type + " " + selectedItems.toString().replaceAll("\\[", "").replaceAll("\\]", "");
          status.append(statusForType).append(" and ");
        }
      }
      String text = status.toString();
      if (text.length() > 0) text = text.substring(0, text.length() - " and ".length());
      statusHeader.setText(text);
    }
  }

  private void showDefaultStatus() {
    statusHeader.setText("Showing all entries");
  }

  /**
   * @param container
   * @see #addButtonRow
   */
  protected Widget addBottomText(FluidContainer container) {
    FluidRow status = new FluidRow();
    status.getElement().setId("statusRow");
    status.addStyleName("alignCenter");
    status.addStyleName("inlineBlockStyle");
    container.add(status);
    status.add(statusHeader);
    statusHeader.getElement().setId("statusHeader");
    DOM.setStyleAttribute(statusHeader.getElement(), "marginTop", "0px");
    DOM.setStyleAttribute(statusHeader.getElement(), "marginBottom", "0px");

    return status;
  }

  //@Override
  protected int getKludge() { return 170;}

  protected Panel getInstructionRow() {
    Panel instructions = new FluidRow();
    instructions.addStyleName("alignCenter");
    instructions.addStyleName("inlineBlockStyle");

    String userPrompt = getUserPrompt();
    if (userPrompt.length() > 0) {
      Heading heading = new Heading(5, userPrompt);
      instructions.add(heading);
    }
    return instructions;
  }

  protected String getUserPrompt() { return "";  }

  /**
   * @param sectionInFirstType
   * @param buttonGroupSectionWidget
   * @return
   * @see #addButtonRow
   */
  private ButtonWithChildren addColumnButton(FlowPanel columnContainer,
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
   * So when we get a URL with a bookmark in it, we have to make the UI appear consistent with it.
   *
   * @param type
   * @param sections
   * @see mitll.langtest.client.list.HistoryExerciseList#restoreListBoxState(mitll.langtest.client.list.SelectionState)
   */
  @Override
  protected void selectItem(String type, Collection<String> sections) {
    ButtonGroupSectionWidget listBox = (ButtonGroupSectionWidget) typeToBox.get(type);
    listBox.clearSelectionState();
    System.out.println("FlexSectionExerciseList.selectItem : selecting " + type + "=" + sections);
    listBox.selectItem(sections);
  }

  /**
   * @param type
   * @see HistoryExerciseList#restoreListBoxState(mitll.langtest.client.list.SelectionState)
   */
  @Override
  protected void clearEnabled(String type) {
    ButtonGroupSectionWidget listBox = (ButtonGroupSectionWidget) typeToBox.get(type);
    listBox.clearEnabled();
  }

  protected void enableAllButtonsFor(String type) {
    ButtonGroupSectionWidget listBox = (ButtonGroupSectionWidget) typeToBox.get(type);
    listBox.enableAll();
  }

  /**
   * @see HistoryExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   */
  protected void gotEmptyExerciseList() {
    List<String> strings = Arrays.asList("No items match the selection and search.", "Try clearing one of your selections or changing the search.");
    new ModalInfoDialog("Empty selection", strings);
  }

  /**
   * @param title
   * @return Button with this title and the initial type
   * @see #addColumnButton
   */
  private ButtonWithChildren makeOverallButton(String type, String title) {
    ButtonWithChildren overallButton = new ButtonWithChildren(title, type);

    overallButton.setWidth("100%");
    DOM.setStyleAttribute(overallButton.getElement(), "paddingLeft", "0px");
    DOM.setStyleAttribute(overallButton.getElement(), "paddingRight", "0px");
    //DOM.setStyleAttribute(overallButton.getElement(), "borderWidth", "0");

    overallButton.setType(ButtonType.PRIMARY);
    return overallButton;
  }

  private ButtonWithChildren makeClearButton() {
    ButtonWithChildren clear = new ButtonWithChildren(ANY, ANY);

    DOM.setStyleAttribute(clear.getElement(), "marginTop", "5px");
    DOM.setStyleAttribute(clear.getElement(), "marginBottom", "12px");

    clear.setType(ButtonType.DEFAULT);
    return clear;
  }

/*    int offsetHeight = columnContainer.getOffsetHeight() + (usuallyThereWillBeAHorizScrollbar ? 5 :0);
    int minHeight = usuallyThereWillBeAHorizScrollbar ? PANEL_INSIDE_SCROLL_MIN_HEIGHT : PANEL_INSIDE_SCROLL_MIN_HEIGHT_SMALL;
    panelInsideScrollPanel.setHeight(Math.max(minHeight, offsetHeight) + "px");
    scrollPanel.setWidth("100%");*/
  /**
   * @see #addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection, boolean)
   * @param columnContainer
   */
  private void setSizesAndPushFirst(Widget columnContainer) {
  //  int offsetHeight = columnContainer.getOffsetHeight() + 5;

    // System.out.println("height is " + offsetHeight);
    //scrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
 //   panelInsideScrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
    // panelInsideScrollPanel.getParent().setHeight(Math.max(50, offsetHeight) + "px");

    //int width = Window.getClientWidth() - labelColumn.getOffsetWidth() - clearColumnContainer.getOffsetWidth() - 90;
    //  System.out.println("setting width to " +width);
    // scrollPanel.setWidth(Math.max(300, width) + "px");
   // scrollPanel.setWidth("100%");
    pushFirstListBoxSelection();

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
                              System.out.println("deferred set scroll panel width");
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
   * @see #addButtonRow
   */
  private SectionWidget makeSectionWidget(Panel labelContainer, Panel clearColumnContainer, String typeForOriginal,
                                          ButtonGroupSectionWidget sectionWidget) {
    ButtonType buttonType = typeToButton.get(typeForOriginal);
    // make label

    Widget labelWidget = getLabelWidget(typeForOriginal);
    labelContainer.add(labelWidget);
    String color = getButtonTypeStyle(buttonType);
    sectionWidget.addLabel(labelWidget, color);

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
  private List<ButtonWithChildren> addButtonGroup(HorizontalPanel parentColumn, List<SectionNode> rootNodes,
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

      List<SectionNode> children = sectionNode.getChildren();

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

        DOM.setStyleAttribute(buttonForSection.getElement(), "paddingLeft", "0px");
        DOM.setStyleAttribute(buttonForSection.getElement(), "paddingRight", "0px");
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

        System.out.println("\taddButtonGroup : for " + typeForOriginal + "=" + section + " recurse on " + children.size() +
          " children at " + subType);
        ButtonGroupSectionWidget sectionWidget1 = (ButtonGroupSectionWidget) typeToBox.get(subType);
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
    setScrollPanelWidth();
  }

  protected void setScrollPanelWidth() {
    if (labelColumn != null) {
      int leftSideWidth = labelColumn.getOffsetWidth() + clearColumnContainer.getOffsetWidth();
      if (leftSideWidth == 0) leftSideWidth = 130;
      int width = Window.getClientWidth() - leftSideWidth - UNACCOUNTED_WIDTH;
      System.out.println("FlexSectionExeciseList.setScrollPanelWidth : scrollPanel width is " + width +"\n\tclient " +Window.getClientWidth() +
        " label col " +labelColumn.getOffsetWidth() + " clear " +clearColumnContainer.getOffsetWidth() + " unacct "+UNACCOUNTED_WIDTH);
      scrollPanel.setWidth(Math.max(300, width) + "px");
    }
    else {
      System.out.println("setScrollPanelWidth : labelColumn is null");
    }
  }

  /**
   * @param typeForOriginal
   * @return
   * @see #makeSectionWidget
   */
  private Widget getLabelWidget(String typeForOriginal) {
    Heading widget = new Heading(HEADING_FOR_LABEL, typeForOriginal);
    DOM.setStyleAttribute(widget.getElement(), "marginBottom", "10px");
    DOM.setStyleAttribute(widget.getElement(), "marginRight", "5px");
    return widget;
  }

  /**
   * @param sectionWidgetFinal
   * @param section
   * @param buttonType
   * @param isClear
   * @return
   * @see #addButtonGroup(com.google.gwt.user.client.ui.HorizontalPanel, java.util.List, String, java.util.List, ButtonGroupSectionWidget)
   * @see #makeSectionWidget
   */
  private ButtonWithChildren makeSubgroupButton(final ButtonGroupSectionWidget sectionWidgetFinal,
                                                final String section,
                                                ButtonType buttonType, boolean isClear) {
    //System.out.println("making button " + type + "=" + section);
    ButtonWithChildren sectionButton = new ButtonWithChildren(section, sectionWidgetFinal.getType());
    //  System.out.println("made button " + sectionButton);

    boolean shrinkHorizontally = numSections > 5;
    //  System.out.println("num " + numSections + " shrinkHorizontally " +shrinkHorizontally );

    if (!isClear && shrinkHorizontally) { // squish buttons if we have lots of sections
      DOM.setStyleAttribute(sectionButton.getElement(), "paddingLeft", "6px");
      DOM.setStyleAttribute(sectionButton.getElement(), "paddingRight", "6px");
    }

    sectionButton.setType(isClear ? ButtonType.DEFAULT : buttonType);
    sectionButton.setEnabled(!isClear);

    if (isClear) {
      DOM.setStyleAttribute(sectionButton.getElement(), "marginBottom", "15px");
    }

    addClickHandlerToButton(sectionButton, section, sectionWidgetFinal);

    sectionWidgetFinal.addButton(sectionButton);

    return sectionButton;
  }

  @Override
  protected int getVerticalUnaccountedFor() {
    return 160;
  }

  public static class ButtonWithChildren extends Button {
    private List<ButtonWithChildren> children = new ArrayList<ButtonWithChildren>();
    private String type;
    private ButtonWithChildren parent;
    private ButtonGroupSectionWidget buttonGroupContainer;

    public ButtonWithChildren(String caption, String type) {
      super(caption);
      this.type = type;
    }

    /**
     * @param children
     * @see FlexSectionExerciseList#addButtonGroup(com.google.gwt.user.client.ui.HorizontalPanel, java.util.List, String, java.util.List, ButtonGroupSectionWidget)
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

    public List<ButtonWithChildren> getSiblings() {
      List<ButtonWithChildren> siblings = new ArrayList<ButtonWithChildren>();
      if (parent != null) {
        for (ButtonWithChildren button : parent.children) {
          if (button != this) siblings.add(button);
        }
      }
      return siblings;
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
