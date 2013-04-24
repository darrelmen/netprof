package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
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
import java.util.HashSet;
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
  public static final int HEADING_FOR_LABEL = 4;
  private final List<ButtonType> buttonTypes = new ArrayList<ButtonType>();
  private Map<String,ButtonType> typeToButton = new HashMap<String, ButtonType>();
  private int numSections = 0;

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

  public void getExercises(final long userID) {
    System.out.println("Flex : Get exercises for user=" + userID);
    this.userID = userID;
    sectionPanel.clear();

    Panel flexTable = getWidgetsForTypes(userID);

    if (!showListBoxes) {
      SelectionState selectionState = getSelectionState(History.getToken());
      loadExercises(selectionState.typeToSection, selectionState.item);
    }

    sectionPanel.add(flexTable);
  }

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @see mitll.langtest.client.exercise.SectionExerciseList#useInitialTypeToSectionMap(java.util.Map, long)
   * @paramx result
   * @param userID
   * @return
   */
  protected Panel getWidgetsForTypes(final long userID) {
    final FluidContainer container = new FluidContainer();
    DOM.setStyleAttribute(container.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(container.getElement(), "paddingRight", "2px");

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
              addButtonRow(result, userID, container, sortedTypes);
            }
          });
        }
        else {
          addStudentTypeAndSection(container, sortedTypes);
        }
      }
    });

    return container;
  }

  private void addStudentTypeAndSection(FluidContainer container,Collection<String> sortedTypes) {
    String token = unencodeToken(History.getToken());
    SelectionState selectionState = getSelectionState(token);

    for (final String type : sortedTypes) {
      Collection<String> typeValue = selectionState.typeToSection.get(type);
      if (typeValue != null) {
        FluidRow fluidRow = new FluidRow();
        container.add(fluidRow);

        fluidRow.add(new Column(2, new Heading(4,type)));
        fluidRow.add(new Column(1, new Heading(4,typeValue.toString())));
      }
    }
  }

  private Panel panelInsideScrollPanel;
  private Panel scrollPanel;
  private Panel label, clear;

  /**
   * @see #getWidgetsForTypes(long)
   * @param rootNodes
   * @param userID
   * @param container
   * @param types
   */
  private void addButtonRow(List<SectionNode> rootNodes, long userID, FluidContainer container, Collection<String> types) {
    System.out.println("getWidgetsForTypes (success) for user = " + userID + " got types " + types);
    String firstType = types.iterator().next(); // e.g. unit!

    container.add(getInstructionRow());
    FlexTable firstTypeRow = new FlexTable();
    container.add(firstTypeRow);
    firstTypeRow.addStyleName("alignTop");

    populateButtonGroups(types);

    ButtonGroupSectionWidget buttonGroupSectionWidget = (ButtonGroupSectionWidget)typeToBox.get(firstType);

    Container labelContainer = getLabelWidgetForRow(firstType,typeToButton.get(firstType),buttonGroupSectionWidget);
    label = labelContainer;

    firstTypeRow.setWidget(0, 0, labelContainer);

    Panel clearColumnContainer = addColumnButton(ANY, buttonGroupSectionWidget, true);

    firstTypeRow.setWidget(0, 1, clearColumnContainer);

    for (String type : types) {
      if (type.equals(firstType)) continue;
      makeSectionWidget(labelContainer, clearColumnContainer, type, (ButtonGroupSectionWidget)typeToBox.get(type));
    }

    clear = clearColumnContainer;

    HorizontalPanel widgets = new HorizontalPanel();
    panelInsideScrollPanel = widgets;
    panelInsideScrollPanel.addStyleName("blueBackground");
    panelInsideScrollPanel.addStyleName("borderSpacing");

    makeScrollPanel(firstTypeRow, panelInsideScrollPanel);

    // add columns for each section within first type...

    Collection<String> sectionsInType = getLabels(rootNodes);
    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);
    sectionsInType = getSortedItems(sectionsInType);
    numSections = sectionsInType.size();

    List<String> subTypes = new ArrayList<String>(types);
    List<String> subs = subTypes.subList(1, subTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    // add a column for every root type
    Widget last = null;
    long then = System.currentTimeMillis();
    for (String sectionInFirstType : sectionsInType) {
      Panel columnContainer = addColumnButton(sectionInFirstType, buttonGroupSectionWidget, false);
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
        addButtonGroup(rowForChildren, sectionNode.getChildren(), subType, subs, sectionWidget1);
        //columnContainer.add(rowForChildren);
        rowContainer.add(rowForChildren);
        columnContainer.add(rowContainer);

        sectionWidget1.addRow(rowContainer);
      }
    }
    long now = System.currentTimeMillis();
    System.out.println("\tgetWidgetsForTypes took " + (now-then) + " millis");

    setSizesAndPushFirst(last);
    addBottomText(container,types);
  }

  private void makeScrollPanel(FlexTable firstTypeRow, Panel panelInside) {
    this.scrollPanel = new ScrollPanel(panelInside);
//    scrollPanel.addStyleName("border");
    firstTypeRow.setWidget(0,2,scrollPanel);
  }

  private void populateButtonGroups(Collection<String> types) {
    typeToBox.clear();
    typeToButton.clear();

    int j = 0;
    for (String type : types) {
      ButtonGroupSectionWidget buttonGroupSectionWidget1 = new ButtonGroupSectionWidget(type);
      typeToBox.put(type,buttonGroupSectionWidget1);
      typeToButton.put(type, buttonTypes.get(j++ % types.size()));
    }
  }

  private Map<String, SectionNode> getNameToNode(List<SectionNode> rootNodes) {
    Map<String,SectionNode> nameToNode = new HashMap<String, SectionNode>();
    for (SectionNode n : rootNodes) nameToNode.put(n.getName(),n);
    return nameToNode;
  }


  protected List<String> getLabels(List<SectionNode> nodes) {
    List<String> items = new ArrayList<String>();
    for (SectionNode n : nodes) items.add(n.getName());
    return items;
  }

  private Container getLabelWidgetForRow(String firstType, ButtonType buttonType,SectionWidget buttonGroupSectionWidget) {
    Container labelContainer = new FluidContainer();
    FluidRow rowAgain = new FluidRow();

    labelContainer.addStyleName("inlineStyle");
    DOM.setStyleAttribute(labelContainer.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(labelContainer.getElement(), "paddingRight", "2px");

    Heading widget = makeLabelWidget(firstType);
    String color = getButtonTypeStyle(buttonType);

    buttonGroupSectionWidget.addLabel(widget,color);
    rowAgain.add(widget);
    labelContainer.add(rowAgain);
    return labelContainer;
  }

  private Heading makeLabelWidget(String firstType) {
    Heading widget = new Heading(HEADING_FOR_LABEL, firstType);
    DOM.setStyleAttribute(widget.getElement(), "webkitMarginBefore", "0");
    DOM.setStyleAttribute( widget.getElement(), "webkitMarginAfter", "0");
    DOM.setStyleAttribute( widget.getElement(), "marginBottom", "0px");
    return widget;
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    super.onValueChange(event);

    if (showListBoxes) {
      showSelectionState(event);
    }
  }

  private void showSelectionState(ValueChangeEvent<String> event) {
    SelectionState selectionState = new SelectionState(event);

    for (Heading widget : typeToStatus.values()) {
      widget.setText("");
    }

    Set<Map.Entry<String, Collection<String>>> entries = selectionState.typeToSection.entrySet();
    for (Map.Entry<String, Collection<String>> part : entries) {
      Heading heading = typeToStatus.get(part.getKey());

      if (heading == null) {
        System.err.println("can't find " + part.getKey() + " in " + typeToStatus.keySet() + "?");
      } else {
        heading.setText(part.getKey() + " " + part.getValue().toString().replaceAll("\\[", "").replaceAll("\\]", ""));
      }
    }
    if (entries.isEmpty()) {
      typeToStatus.values().iterator().next().setText("Showing all entries");
    }
  }

  private Map<String,Heading> typeToStatus = new HashMap<String, Heading>();
  private void addBottomText(FluidContainer container, Collection<String> types) {
    FluidRow status = new FluidRow();
    status.addStyleName("alignCenter");
    container.add(status);

    for (String type : types) {
      Heading w = new Heading(4);
      typeToStatus.put(type,w);
      status.add(w);
    }
    typeToStatus.values().iterator().next().setText("Showing all entries");
  }

  protected void addPreviewWidgets(Panel container) {
    FluidRow fluidRow = new FluidRow();
    container.add(fluidRow);
    //   Widget emailWidget = getEmailWidget();
    //  fluidRow.add(new Column(2, /*3,*/ emailWidget));

//    Widget hideBoxesWidget = getHideBoxesWidget();
    Widget flashcardWidget = getFlashcard();
    fluidRow.add(new Column(2, new Heading(5, "Preview this list in these modes: ")));
    fluidRow.add(new Column(2, flashcardWidget));

    Widget flashcardWidget2 = getFlashcard2();
    fluidRow.add(new Column(3, flashcardWidget2));
  }

  private Anchor flashcardLink;

  protected Widget getFlashcard() {
    flashcardLink = new Anchor("<h4>Flashcard</h4>", true,"?flashcard=true#", "_blank");
    return flashcardLink;
  }

  private Anchor flashcardLink2;

  protected Widget getFlashcard2() {
    flashcardLink2 = new Anchor("<h4>Timed Flashcard</h4>", true, "?flashcard=true&timedGame=true#", "_blank");
    return flashcardLink2;
  }

  protected void setModeLinks(String historyToken) {
    super.setModeLinks(historyToken);
    if (flashcardLink != null) {
      flashcardLink.setHref(GWT.getHostPageBaseURL() + "?flashcard=true#" + historyToken);
    }
    if (flashcardLink2 != null) {
      flashcardLink2.setHref(GWT.getHostPageBaseURL() + "?flashcard=true&timedGame=true#" + historyToken);
    }
  }

  private FluidRow getInstructionRow() {
    FluidRow fluidRow1 = new FluidRow();
    fluidRow1.add(new Column(7, 3, new Heading(5, "Click on the buttons to select just what you want to see.")));
    return fluidRow1;
  }

 // private Map<String,Collection<Panel>> typeToRows = new HashMap<String, Collection<Panel>>();

  /**
   * @see #addButtonRow(java.util.List, long, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @paramx rowType
   * @param sectionInFirstType
   * @param buttonGroupSectionWidget
   * @param isClear
   * @return
   */
  private Panel addColumnButton(final String sectionInFirstType,
                                    final ButtonGroupSectionWidget buttonGroupSectionWidget, boolean isClear) {
    FlowPanel columnContainer = new FlowPanel();
    columnContainer.addStyleName("inlineStyle");
    // add a button
    Button overallButton = makeOverallButton(sectionInFirstType, isClear);
    addClickHandlerToButton(overallButton, sectionInFirstType, buttonGroupSectionWidget);
    buttonGroupSectionWidget.addButton(overallButton);
   // System.out.println("making button "+sectionInFirstType);

    Panel rowAgain = new FlowPanel();
    columnContainer.add(rowAgain);

    rowAgain.add(overallButton);
    buttonGroupSectionWidget.addRow(rowAgain);
    rowAgain.addStyleName("rowPadding");

    return columnContainer;
  }

  private void addClickHandlerToButton(Button overallButton, final String sectionInFirstType, final ButtonGroupSectionWidget buttonGroupSectionWidget) {
    overallButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        List<String> selections = new ArrayList<String>();
        selections.add(sectionInFirstType);
        buttonGroupSectionWidget.selectItem(selections, true);
        pushNewSectionHistoryToken();
      }
    });
  }

  private Button makeOverallButton(String title, boolean isClear) {
    Button overallButton = new Button(title);

    overallButton.setWidth("100%");
    DOM.setStyleAttribute(overallButton.getElement(), "paddingLeft", "0px");
    DOM.setStyleAttribute(overallButton.getElement(), "paddingRight", "0px");
    DOM.setStyleAttribute(overallButton.getElement(), "borderWidth", "0");

    overallButton.setType(isClear ? ButtonType.DEFAULT : ButtonType.PRIMARY);
    return overallButton;
  }

  private void setSizesAndPushFirst(Widget columnContainer) {
    int offsetHeight = columnContainer.getOffsetHeight()+5;

    System.out.println("height is " + offsetHeight);
    //scrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
    panelInsideScrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
    // panelInsideScrollPanel.getParent().setHeight(Math.max(50, offsetHeight) + "px");

    int width = Window.getClientWidth() - label.getOffsetWidth() - clear.getOffsetWidth()- 100;
    scrollPanel.setWidth(Math.max(300, width) + "px");

    pushFirstListBoxSelection();
  }

  /**
   * Add label and clear button to row for each type
   * @see #addButtonRow(java.util.List, long, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @param labelContainer
   * @param clearColumnContainer
   * @param typeForOriginal
   * @param sectionWidget
   * @return
   */
  private SectionWidget makeSectionWidget(Container labelContainer, Panel clearColumnContainer, String typeForOriginal,
                                          ButtonGroupSectionWidget sectionWidget) {
    ButtonType buttonType = typeToButton.get(typeForOriginal);

    String color = getButtonTypeStyle(buttonType);
    // make label

    Widget labelWidget = getLabelWidget(typeForOriginal);
    labelContainer.add(labelWidget);
    sectionWidget.addLabel(labelWidget,color);
    // make

    // make clear button

/*    FlexTable table2 = new FlexTable();
    ButtonGroup group2 = new ButtonGroup();
    table2.setWidget(0, 0, group2);*/

    Button sectionButton = makeSubgroupButton(sectionWidget, typeForOriginal, ANY, buttonType, true);
  //  group2.add(sectionButton);
    sectionWidget.addButton(sectionButton);

    clearColumnContainer.add(sectionButton);
    return sectionWidget;
  }

  private String getButtonTypeStyle(ButtonType buttonType) {
    return (buttonType.equals(ButtonType.PRIMARY)) ? "primaryButtonColor" :buttonType.equals(ButtonType.SUCCESS) ? "successButtonColor"
        : (buttonType.equals(ButtonType.INFO)) ? "infoButtonColor" : "warningButtonColor";
  }

  /**
   * @see FlexSectionExerciseList#addButtonRow(java.util.List, long, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @param parentColumn
   * @param rootNodes
   * @param typeForOriginal
   * @param remainingTypes
   * @param sectionWidget
   */
  private void addButtonGroup(HorizontalPanel parentColumn, List<SectionNode> rootNodes, String typeForOriginal,
                              List<String> remainingTypes, ButtonGroupSectionWidget sectionWidget) {
    //String typeForOriginal = subs.isEmpty() ? null : subs.iterator().next();

    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);

    List<String> sortedItems = getSortedItems(getLabels(rootNodes));
    ButtonType buttonType = typeToButton.get(typeForOriginal);

    List<String> objects = Collections.emptyList();
    List<String> subs = remainingTypes.isEmpty() ? objects : remainingTypes.subList(1, remainingTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    int n = sortedItems.size();
    for (int i = 0; i<n; i++){
      String section = sortedItems.get(i);
      SectionNode sectionNode = nameToNode.get(section);

      List<SectionNode> children = sectionNode.getChildren();

      Panel rowForSection;
      Button buttonForSection = makeSubgroupButton(sectionWidget, typeForOriginal,
        section,
        buttonType, false);
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

/*        System.out.println("for " + typeForOriginal + "=" + section + " recurse on " + children.size() +
          " children at " + subType);*/
        addButtonGroup(horizontalContainerForChildren, children, subType, subs, (ButtonGroupSectionWidget) typeToBox.get(subType));
      } else {
        rowForSection = buttonForSection;
      }
      parentColumn.add(rowForSection);
    }
  }

  @Override
  public void onResize() {
    super.onResize();

   // int offsetHeight = lastColumn.getOffsetHeight()+5;
   // System.out.println("onResize height is " + offsetHeight);

    setScrollPanelWidth();

 /*   if (scrollPanel.getOffsetHeight() != offsetHeight) {
    scrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
    }
*/
  }

  protected void setScrollPanelWidth() {
    if (label != null) {
      int width = Window.getClientWidth() - label.getOffsetWidth() - clear.getOffsetWidth() - 100;
//      System.out.println("width is " + width);
      scrollPanel.setWidth(Math.max(300, width) + "px");
    }
  }

  /**
   * @see #makeSectionWidget
   * @param typeForOriginal
   * @return
   */
  private Widget getLabelWidget(String typeForOriginal) {
    FlexTable table3 = new FlexTable();
    Heading widget = new Heading(HEADING_FOR_LABEL, typeForOriginal);
    table3.setWidget(0, 0, widget);
    DOM.setStyleAttribute(widget.getElement(), "webkitMarginBefore", "10px");
    DOM.setStyleAttribute( widget.getElement(), "webkitMarginAfter", "10px");
    DOM.setStyleAttribute( widget.getElement(), "marginTop", "2px");
    DOM.setStyleAttribute( widget.getElement(), "marginBottom", "10px");
    return widget;
  }

  /**
   * @see #addButtonGroup(com.google.gwt.user.client.ui.HorizontalPanel, java.util.List, String, java.util.List, ButtonGroupSectionWidget)
   * @see #makeSectionWidget(com.github.gwtbootstrap.client.ui.Container, com.google.gwt.user.client.ui.Panel, String, ButtonGroupSectionWidget)
   * @param sectionWidgetFinal
   * @param type
   * @param section
   * @param buttonType
   * @param isClear
   * @return
   */
  private Button makeSubgroupButton(final ButtonGroupSectionWidget sectionWidgetFinal, final String type, final String section,
                                    ButtonType buttonType, boolean isClear) {
    //System.out.println("making button " + type + "=" + section);
    Button sectionButton = new Button(section);
    DOM.setStyleAttribute(sectionButton.getElement(), "borderWidth", "0");

    boolean shrinkHorizontally = numSections > 5;
  //  System.out.println("num " + numSections + " shrinkHorizontally " +shrinkHorizontally );

    if (!isClear && shrinkHorizontally) { // squish buttons if we have lots of sections
      DOM.setStyleAttribute(sectionButton.getElement(), "paddingLeft", "6px");
      DOM.setStyleAttribute(sectionButton.getElement(), "paddingRight", "6px");
    }

    sectionButton.setType(isClear ? ButtonType.DEFAULT : buttonType);
    sectionButton.setEnabled(!isClear);

    addClickHandlerToButton(sectionButton, section, sectionWidgetFinal);

    sectionWidgetFinal.addButton(sectionButton);

    return sectionButton;
  }

  /**
   * @see #getWidgetsForTypes(java.util.Map, long)
   * @param type
   * @return
   */
  @Override
  protected SectionWidget makeListBox(String type) {
    SectionWidget widgets = new BarSectionWidget(type, new ItemClickListener() {
      @Override
      public void gotClick(String type, String item) {
        pushNewSectionHistoryToken();
      }
    });

    return widgets;
  }

  /**
   * @see #setOtherListBoxes(java.util.Map)
   * @param result
   */
  @Override
  protected void populateListBoxAfterSelection(Map<String, Collection<String>> result) {
    Set<String> typesMentioned = new HashSet<String>(typeToBox.keySet());
    for (Map.Entry<String, Collection<String>> pair : result.entrySet()) {
      SectionWidget sectionWidget = typeToBox.get(pair.getKey());
      if (sectionWidget != null) {
        sectionWidget.enableInSet(pair.getValue());
      }
    }
    typesMentioned.removeAll(result.keySet());
    for (String remainingType : typesMentioned) {
      SectionWidget sectionWidget = typeToBox.get(remainingType);
      if (sectionWidget != null) {
        sectionWidget.enableAll();
      }
    }
  }
}
