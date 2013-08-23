package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
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
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionExerciseList;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.exercise.SelectionState;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.SectionNode;

import java.util.ArrayList;
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
public class FlexSectionExerciseList extends SectionExerciseList {
  private static final int HEADING_FOR_LABEL = 4;

  private final List<ButtonType> buttonTypes = new ArrayList<ButtonType>();
  private Map<String,ButtonType> typeToButton = new HashMap<String, ButtonType>();
  private int numSections = 0;
  private Panel panelInsideScrollPanel;
  private ScrollPanel scrollPanel;
  private Panel clearColumnContainer;
  private Panel labelColumn;
  private Heading statusHeader = new Heading(4);

  public FlexSectionExerciseList(FluidRow secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                 UserFeedback feedback,
                                 boolean showTurkToken, boolean showInOrder, boolean showListBox, ExerciseController controller) {
    super(currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, showListBox, controller);

    Panel child = sectionPanel = new FluidContainer();
    DOM.setStyleAttribute(sectionPanel.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(sectionPanel.getElement(), "paddingRight", "2px");
    secondRow.add(new Column(12,child));

    buttonTypes.add(ButtonType.PRIMARY);
    buttonTypes.add(ButtonType.SUCCESS);
    buttonTypes.add(ButtonType.INFO);
    buttonTypes.add(ButtonType.WARNING);
  }

  @Override
  protected void addComponents() {
    addTableWithPager();
  }

  /**
   * @see mitll.langtest.client.LangTest#doEverythingAfterFactory
   * @param userID
   */
  public void getExercises(final long userID) {
    //System.out.println("FlexSectionExerciseList : getExercises : Get exercises for user=" + userID);
    this.userID = userID;
    sectionPanel.clear();

    Panel flexTable = getWidgetsForTypes(userID);

    if (!showListBoxes) {
      SelectionState selectionState = getSelectionState(History.getToken());
      System.out.println("FlexSectionExerciseList : getExercises for " +userID + " selectionState " + selectionState);

      loadExercises(selectionState.getTypeToSection(), selectionState.getItem());
    }

    sectionPanel.add(flexTable);
  }

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @see #getExercises(long)
   * @param userID
   * @return
   */
  protected Panel getWidgetsForTypes(final long userID) {
    final FluidContainer container = new FluidContainer();
    DOM.setStyleAttribute(container.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(container.getElement(), "paddingRight", "2px");

    getTypeOrder(userID, container);

    return container;
  }

  protected void getTypeOrder(final long userID, final FluidContainer container) {
    service.getTypeOrder(new AsyncCallback<Collection<String>>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("getTypeOrder can't contact server. got " + caught);
      }

      @Override
      public void onSuccess(final Collection<String> sortedTypes) {
        if (showListBoxes) {
          service.getSectionNodes(new AsyncCallback<List<SectionNode>>() {
            @Override
            public void onFailure(Throwable caught) {
              Window.alert("getSectionNodes can't contact server. got " + caught);
            }

            @Override
            public void onSuccess(List<SectionNode> result) {
              addButtonRow(result, userID, container, sortedTypes, !controller.isGoodwaveMode()); // TODO do something better here
            }
          });
        }
        else {
          addStudentTypeAndSection(container, sortedTypes);
        }
      }
    });
  }

  private void addStudentTypeAndSection(FluidContainer container,Collection<String> sortedTypes) {
    String token = unencodeToken(History.getToken());
    SelectionState selectionState = getSelectionState(token);
    System.out.println("\n\nsorted types " +sortedTypes);
    for (final String type : sortedTypes) {
      Collection<String> typeValue = selectionState.getTypeToSection().get(type);
      if (typeValue != null) {
        FluidRow fluidRow = new FluidRow();
        container.add(fluidRow);

        fluidRow.add(new Column(2, new Heading(4,type)));
        fluidRow.add(new Column(1, new Heading(4,typeValue.toString())));
      }
    }
  }

  /**
   * @see #getTypeOrder(long, com.github.gwtbootstrap.client.ui.FluidContainer)
   * @param rootNodes
   * @param userID
   * @param container
   * @param types
   * @param addInstructions
   */
  private void addButtonRow(List<SectionNode> rootNodes, long userID, FluidContainer container, Collection<String> types,
                            boolean addInstructions) {
    System.out.println("addButtonRow (success) for user = " + userID + " got types " + types + " num root nodes " + rootNodes.size());
    if (types.isEmpty()) {
      System.err.println("huh? types is empty?");
      return;
    }
    showDefaultStatus();

    if (addInstructions) container.add(getInstructionRow());
    FlexTable firstTypeRow = new FlexTable();
    container.add(firstTypeRow);
    firstTypeRow.addStyleName("alignTop");

    populateButtonGroups(types);

    String firstType = types.iterator().next(); // e.g. unit!
    ButtonGroupSectionWidget buttonGroupSectionWidget = (ButtonGroupSectionWidget)typeToBox.get(firstType);

    boolean usuallyThereWillBeAHorizScrollbar = rootNodes.size() > 6;

    makeLabelColumn(usuallyThereWillBeAHorizScrollbar, firstType, firstTypeRow, buttonGroupSectionWidget);
    makeClearColumn(usuallyThereWillBeAHorizScrollbar, types, firstType, firstTypeRow, buttonGroupSectionWidget);

    makePanelInsideScrollPanel(firstTypeRow);

    // add columns for each section within first type...

    Collection<String> sectionsInType = getSortedItems(getLabels(rootNodes));
    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);
    numSections = sectionsInType.size();

    List<String> subTypes = new ArrayList<String>(types);
    List<String> subs = subTypes.subList(1, subTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    // add a column for every root type
    Widget last = null;
    long then = System.currentTimeMillis();
    for (String sectionInFirstType : sectionsInType) {
      FlowPanel columnContainer = new FlowPanel();
      ButtonWithChildren buttonWithChildren = addColumnButton(columnContainer,sectionInFirstType, buttonGroupSectionWidget);
      last = columnContainer;
      DOM.setStyleAttribute(columnContainer.getElement(), "marginBottom", "5px");
      panelInsideScrollPanel.add(columnContainer);

      if (subType != null) {
        SectionNode sectionNode = nameToNode.get(sectionInFirstType);
        ButtonGroupSectionWidget sectionWidget1 = (ButtonGroupSectionWidget)typeToBox.get(subType);
        Panel rowContainer = new FlowPanel();

        HorizontalPanel rowForChildren = new HorizontalPanel();

        rowForChildren.setWidth("100%");
        rowContainer.addStyleName("rowPadding");
        rowForChildren.setHorizontalAlignment(ALIGN_LEFT);
        List<ButtonWithChildren> buttonWithChildrens = addButtonGroup(rowForChildren, sectionNode.getChildren(),
          subType, subs, sectionWidget1);
        buttonWithChildren.setChildren(buttonWithChildrens);
        buttonWithChildren.setButtonGroup(sectionWidget1);
        //columnContainer.add(rowForChildren);
        rowContainer.add(rowForChildren);
        columnContainer.add(rowContainer);

        sectionWidget1.addRow(rowContainer);
      }
    }
    long now = System.currentTimeMillis();
    if (now-then > 0) System.out.println("\taddButtonRow took " + (now-then) + " millis");

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
      makeSectionWidget(labelColumn, clearColumnContainer, type, (ButtonGroupSectionWidget)typeToBox.get(type));
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

  private void makePanelInsideScrollPanel(FlexTable firstTypeRow) {
    panelInsideScrollPanel = new HorizontalPanel();
    panelInsideScrollPanel.addStyleName("blueBackground");
    panelInsideScrollPanel.addStyleName("borderSpacing");

    makeScrollPanel(firstTypeRow, panelInsideScrollPanel);
  }

  private void makeScrollPanel(FlexTable firstTypeRow, Panel panelInside) {
    this.scrollPanel = new ScrollPanel(panelInside);
    this.scrollPanel.addStyleName("leftFiveMargin");
    firstTypeRow.setWidget(0,2,scrollPanel);
    setScrollPanelWidth();
  }

  private void populateButtonGroups(Collection<String> types) {
    typeToBox.clear();
    typeToButton.clear();

    int j = 0;
    for (String type : types) {
      ButtonGroupSectionWidget buttonGroupSectionWidget1 = new ButtonGroupSectionWidget(type,typeToBox);
      typeToBox.put(type,buttonGroupSectionWidget1);
      typeToButton.put(type, buttonTypes.get(j++ % types.size()));
    }

    //System.out.println("populateButtonGroups " + types + " : " + typeToBox);
  }

  private Map<String, SectionNode> getNameToNode(List<SectionNode> rootNodes) {
    Map<String,SectionNode> nameToNode = new HashMap<String, SectionNode>();
    for (SectionNode n : rootNodes) nameToNode.put(n.getName(), n);
    return nameToNode;
  }

  protected List<String> getLabels(List<SectionNode> nodes) {
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
    DOM.setStyleAttribute( widget.getElement(), "webkitMarginAfter", "0");
    DOM.setStyleAttribute( widget.getElement(), "marginTop", "0px");
    DOM.setStyleAttribute( widget.getElement(), "marginBottom", "15px");
    return widget;
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    super.onValueChange(event);

    if (showListBoxes) {
      showSelectionState(event);
    }
  }

  /**
   * Add a line that spells out in text which lessons have been chosen.
   * @param event
   */
  private void showSelectionState(ValueChangeEvent<String> event) {
    SelectionState selectionState = new SelectionState(event);
    System.out.println("FlexSectionExerciseList.showSelectionState : got " + event + " and state '" + selectionState +"'");
    StringBuilder status = new StringBuilder();
    Set<Map.Entry<String, Collection<String>>> entries = selectionState.getTypeToSection().entrySet();
    for (Map.Entry<String, Collection<String>> part : entries) {
        String statusForType = part.getKey() + " " + part.getValue().toString().replaceAll("\\[", "").replaceAll("\\]", "");
        status.append(statusForType).append(" ");
    }
    statusHeader.setText(status.toString());
    //System.out.println("showSelectionState : entries " + entries + " from " + selectionState.typeToSection + " status " + status);

    if (entries.isEmpty()) {
      showDefaultStatus();
    }
    else {
      System.out.println("FlexSectionExerciseList.showSelectionState : status now " + status);
    }
  }

  private void showDefaultStatus() {
    statusHeader.setText("Showing all entries");
  }

  /**
   * @see #addButtonRow
   * @param container
   */
  protected void addBottomText(FluidContainer container) {
    FluidRow status = new FluidRow();
    status.addStyleName("alignCenter");
    status.addStyleName("inlineStyle");
    container.add(status);
    status.add(statusHeader);
  }

  private Panel getInstructionRow() {
    Panel instructions = new FluidRow();
    instructions.addStyleName("alignCenter");
    instructions.addStyleName("inlineStyle");

    String userPrompt = getUserPrompt();
    if (userPrompt.length() > 0) {
      Heading heading = new Heading(5, userPrompt);
      instructions.add(heading);
    }
    return instructions;
  }

  protected String getUserPrompt() {
    return "";
  }

  /**
   * @see #addButtonRow
   * @param sectionInFirstType
   * @param buttonGroupSectionWidget
   * @return
   */
  private ButtonWithChildren addColumnButton(FlowPanel columnContainer,
                                             final String sectionInFirstType,
                                             final ButtonGroupSectionWidget buttonGroupSectionWidget) {
    columnContainer.addStyleName("inlineStyle");
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
        //  System.out.println("got click on " + overallButton + " with children " + overallButton.getButtonChildren());
/*        if (selectPathToParent) {
          buttonGroupSectionWidget.selectButton(overallButton, typeToBox);
        }
        else {*/
           buttonGroupSectionWidget.selectItem(sectionInFirstType, typeToBox);
  //      }
        pushNewSectionHistoryToken();
      }
    });
  }

  /**
   * So when we get a URL with a bookmark in it, we have to make the UI appear consistent with it.
   *
   * @see SectionExerciseList#restoreListBoxState(mitll.langtest.client.exercise.SelectionState)
   * @param type
   * @param sections
   */
  @Override
  protected void selectItem(String type, Collection<String> sections) {
    ButtonGroupSectionWidget listBox = (ButtonGroupSectionWidget) typeToBox.get(type);
    listBox.clearSelectionState();
    System.out.println("FlexSectionExerciseList.selectItem : selecting " + type + "=" + sections);
    listBox.selectItem(sections, false, typeToBox);
  }

/*  @Override
  protected void restoreListBoxState(SelectionState selectionState) {
    super.restoreListBoxState(selectionState);

    for (String type : typeToBox.keySet()) {
      ButtonGroupSectionWidget listBox = (ButtonGroupSectionWidget)typeToBox.get(type);
      Collection<Button> enabledButtons = listBox.getEnabledButtons();
      if (!enabledButtons.isEmpty())  {
        Button next = enabledButtons.iterator().next();
        System.out.println("ensure " + next + " is visible...");
        scrollPanel.ensureVisible(next);
        break;
      }
    }
  }*/

    /**
     * @see #addColumnButton
     * @param title
     * @return Button with this title and the initial type
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

  private void setSizesAndPushFirst(Widget columnContainer) {
    int offsetHeight = columnContainer.getOffsetHeight()+5;

   // System.out.println("height is " + offsetHeight);
    //scrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
    panelInsideScrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
    // panelInsideScrollPanel.getParent().setHeight(Math.max(50, offsetHeight) + "px");

    int width = Window.getClientWidth() - labelColumn.getOffsetWidth() - clearColumnContainer.getOffsetWidth()- 90;
  //  System.out.println("setting width to " +width);
   // scrollPanel.setWidth(Math.max(300, width) + "px");
    scrollPanel.setWidth("100%");
    pushFirstListBoxSelection();
  }

  /**
   * Add label and clear button to row for each type
   * @see #addButtonRow
   * @param labelContainer
   * @param clearColumnContainer
   * @param typeForOriginal
   * @param sectionWidget
   * @return
   */
  private SectionWidget makeSectionWidget(Panel labelContainer, Panel clearColumnContainer, String typeForOriginal,
                                          ButtonGroupSectionWidget sectionWidget) {
    ButtonType buttonType = typeToButton.get(typeForOriginal);
    // make label

    Widget labelWidget = getLabelWidget(typeForOriginal);
    labelContainer.add(labelWidget);
    String color = getButtonTypeStyle(buttonType);
    sectionWidget.addLabel(labelWidget,color);

    // make clear button
    Button sectionButton = makeSubgroupButton(sectionWidget, ANY, buttonType, true);
    sectionWidget.addButton(sectionButton);

    clearColumnContainer.add(sectionButton);
    return sectionWidget;
  }

  private String getButtonTypeStyle(ButtonType buttonType) {
    return (buttonType.equals(ButtonType.PRIMARY)) ? "primaryButtonColor" :buttonType.equals(ButtonType.SUCCESS) ? "successButtonColor"
        : (buttonType.equals(ButtonType.INFO)) ? "infoButtonColor" : "warningButtonColor";
  }

  /**
   * @see FlexSectionExerciseList#addButtonRow
   * @param parentColumn
   * @param rootNodes
   * @param typeForOriginal
   * @param remainingTypes
   * @param sectionWidget
   */
  private List<ButtonWithChildren> addButtonGroup(HorizontalPanel parentColumn, List<SectionNode> rootNodes, String typeForOriginal,
                              List<String> remainingTypes, ButtonGroupSectionWidget sectionWidget) {
    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);

    List<String> sortedItems = getSortedItems(getLabels(rootNodes));
    ButtonType buttonType = typeToButton.get(typeForOriginal);

    List<String> objects = Collections.emptyList();
    List<String> subs = remainingTypes.isEmpty() ? objects : remainingTypes.subList(1, remainingTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    int n = sortedItems.size();
    List<ButtonWithChildren> buttonChildren = new ArrayList<ButtonWithChildren>();
    for (int i = 0; i<n; i++){
      String section = sortedItems.get(i);
      SectionNode sectionNode = nameToNode.get(section);

      List<SectionNode> children = sectionNode.getChildren();

      Panel rowForSection;
      ButtonWithChildren buttonForSection = makeSubgroupButton(sectionWidget, section, buttonType, false);
      buttonChildren.add(buttonForSection);
      if (n > 1 && i < n-1) {
        buttonForSection.addStyleName("buttonMargin");
      }

      if (!children.isEmpty() && subType != null) {
        rowForSection = new VerticalPanel();

        DOM.setStyleAttribute(buttonForSection.getElement(), "paddingLeft", "0px");
        DOM.setStyleAttribute(buttonForSection.getElement(), "paddingRight", "0px");
        buttonForSection.setWidth("100%");
        rowForSection.add(buttonForSection);
        // recurse on children
        HorizontalPanel horizontalContainerForChildren = new HorizontalPanel();
        rowForSection.add(horizontalContainerForChildren);
        sectionWidget.addRow(rowForSection);
        rowForSection.addStyleName("rowPadding");

        System.out.println("for " + typeForOriginal + "=" + section + " recurse on " + children.size() +
          " children at " + subType);
        ButtonGroupSectionWidget sectionWidget1 = (ButtonGroupSectionWidget) typeToBox.get(subType);
        List<ButtonWithChildren> subButtons =
          addButtonGroup(horizontalContainerForChildren, children, subType, subs, sectionWidget1);
        buttonForSection.setChildren(subButtons);
        buttonForSection.setButtonGroup(sectionWidget1);
      } else {
        rowForSection = buttonForSection;
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
      int width = Window.getClientWidth() - labelColumn.getOffsetWidth() - clearColumnContainer.getOffsetWidth() - 90;
     // System.out.println("scrollPanel width is " + width);
      scrollPanel.setWidth(Math.max(300, width) + "px");
    }
  }

  /**
   * @see #makeSectionWidget
   * @param typeForOriginal
   * @return
   */
  private Widget getLabelWidget(String typeForOriginal) {
    Heading widget = new Heading(HEADING_FOR_LABEL, typeForOriginal);
    DOM.setStyleAttribute( widget.getElement(), "marginBottom", "10px");
    DOM.setStyleAttribute( widget.getElement(), "marginRight", "5px");
    return widget;
  }

  /**
   * @see #addButtonGroup(com.google.gwt.user.client.ui.HorizontalPanel, java.util.List, String, java.util.List, ButtonGroupSectionWidget)
   * @see #makeSectionWidget
   * @param sectionWidgetFinal
   * @param section
   * @param buttonType
   * @param isClear
   * @return
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
    //  DOM.setStyleAttribute(sectionButton.getElement(), "bottomMargin", "12px");
      DOM.setStyleAttribute(sectionButton.getElement(), "marginBottom", "15px");
    }

    addClickHandlerToButton(sectionButton, section, sectionWidgetFinal);

    sectionWidgetFinal.addButton(sectionButton);

    return sectionButton;
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
     * @see FlexSectionExerciseList#addButtonRow(java.util.List, long, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection, boolean)
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

    public boolean hasChildren() {
      return !children.isEmpty();
    }
  }

  /**
   * @see #setOtherListBoxes(java.util.Map)
   * @param result
   */
  @Override
  protected void populateListBoxAfterSelection(Map<String, Collection<String>> result) {
    throw new IllegalArgumentException("don't call me");
  }
}
