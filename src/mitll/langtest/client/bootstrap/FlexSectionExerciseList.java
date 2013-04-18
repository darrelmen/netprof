package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
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
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionExerciseList;
import mitll.langtest.client.exercise.SectionWidget;
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
  private int numExpectedSections = 0;
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
    System.out.println("Get exercises for user=" + userID);
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
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void onSuccess(final Collection<String> sortedTypes) {

        if (showListBoxes) {
          service.getSectionNodes(new AsyncCallback<List<SectionNode>>() {
            @Override
            public void onFailure(Throwable caught) {
              //To change body of implemented methods use File | Settings | File Templates.
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
//  private List<List<Widget>> rows = new ArrayList<List<Widget>>();
  private void addButtonRow(List<SectionNode> rootNodes, long userID, FluidContainer container, Collection<String> types) {
    System.out.println("getWidgetsForTypes (success) for user = " + userID + " got types " + types);
    typeToBox.clear();
    typeToButton.clear();

    String firstType = types.iterator().next(); // e.g. unit!

    container.add(getInstructionRow());
    FlexTable firstTypeRow = new FlexTable();
    container.add(firstTypeRow);

    for (String type : types) {
      ButtonGroupSectionWidget buttonGroupSectionWidget1 = new ButtonGroupSectionWidget(type);
      typeToBox.put(type,buttonGroupSectionWidget1);
    }

    SectionWidget buttonGroupSectionWidget = typeToBox.get(firstType);

    Container labelContainer = getLabelWidgetForRow(firstType);
    label = labelContainer;

    firstTypeRow.setWidget(0, 0, labelContainer);

    Panel clearColumnContainer = addColumnButton(ANY, buttonGroupSectionWidget, true);

    firstTypeRow.setWidget(0, 1, clearColumnContainer);

    int j = 0;
    for (String type : types) {
      typeToButton.put(type, buttonTypes.get(j++ % types.size()));
      if (type.equals(firstType)) continue;
      makeSectionWidget(labelContainer, clearColumnContainer, type, typeToBox.get(type));
    }

    clear = clearColumnContainer;

    panelInsideScrollPanel = new HorizontalPanel();

    this.scrollPanel = new FlowPanel();
    scrollPanel.add(panelInsideScrollPanel);

    firstTypeRow.setWidget(0,2,scrollPanel);
    scrollPanel.addStyleName("overflowStyle");

    // add columns for each section within first type...

    Collection<String> sectionsInType = getLabels(rootNodes);
    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);
    sectionsInType = getSortedItems(sectionsInType);
    numExpectedSections = sectionsInType.size();
    numSections = numExpectedSections;
/*    for (int i = 0; i < numExpectedSections; i++) {
      rows.add(new ArrayList<Widget>());
    }*/

    List<String> subTypes = new ArrayList<String>(types);
    List<String> subs = subTypes.subList(1, subTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    // add a column for every root type
    Widget last = null;
    long then = System.currentTimeMillis();
    for (String sectionInFirstType : sectionsInType) {
      Container columnContainer = addColumnButton(sectionInFirstType, buttonGroupSectionWidget, false);
      last = columnContainer;
      DOM.setStyleAttribute(columnContainer.getElement(), "marginBottom", "5px");
      panelInsideScrollPanel.add(columnContainer);

      if (subType != null) {
        SectionNode sectionNode = nameToNode.get(sectionInFirstType);
        SectionWidget sectionWidget1 = typeToBox.get(subType);

        HorizontalPanel rowAgain = new HorizontalPanel();
        addButtonGroup(rowAgain, sectionNode.getChildren(), subType, subs, sectionWidget1);
        columnContainer.add(rowAgain);
      }
    }
    long now = System.currentTimeMillis();
    System.out.println("\tgetWidgetsForTypes took " + (now-then) + " millis");

    setSizesAndPushFirst(last);
    addBottomText(container,types);
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

  private Container getLabelWidgetForRow(String firstType) {
    Container labelContainer = new FluidContainer();
    FluidRow rowAgain = new FluidRow();

    labelContainer.addStyleName("inlineStyle");
    DOM.setStyleAttribute(labelContainer.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(labelContainer.getElement(), "paddingRight", "2px");

    Heading widget = makeLabelWidget(firstType);

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
    String rawToken = getTokenFromEvent(event);

    String token = getCleanToken(rawToken);

    SelectionState selectionState = getSelectionState(token);

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
    //status.add(new Heading(5,"Showing "));

    for (String type : types) {
      Heading w = new Heading(4);
      typeToStatus.put(type,w);
      status.add(w);
    }
    typeToStatus.values().iterator().next().setText("Showing all entries");

    FluidRow fluidRow = new FluidRow();
    container.add(fluidRow);
 //   Widget emailWidget = getEmailWidget();
  //  fluidRow.add(new Column(2, /*3,*/ emailWidget));

    Widget hideBoxesWidget = getHideBoxesWidget();
    fluidRow.add(new Column(2, hideBoxesWidget));
  }

  private FluidRow getInstructionRow() {
    FluidRow fluidRow1 = new FluidRow();
    fluidRow1.add(new Column(7, 3, new Heading(5, "Click on the buttons to select just what you want to see.")));
    return fluidRow1;
  }


  private Container addColumnButton(final String sectionInFirstType,
                                    final SectionWidget buttonGroupSectionWidget, boolean isClear) {
    Container columnContainer = new FluidContainer();
    columnContainer.addStyleName("inlineStyle");
    DOM.setStyleAttribute(columnContainer.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(columnContainer.getElement(), "paddingRight", "2px");

    ButtonGroup group = new ButtonGroup();
    group.setWidth("100%");

    // add a button
    Button overallButton = makeOverallButton(sectionInFirstType, isClear);
   // System.out.println("making button "+sectionInFirstType);

    group.add(overallButton);

    FluidRow rowAgain = new FluidRow();
    columnContainer.add(rowAgain);
    rowAgain.add(group);

    overallButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        List<String> selections = new ArrayList<String>();
        selections.add(sectionInFirstType);
        buttonGroupSectionWidget.selectItem(selections, true);
        pushNewSectionHistoryToken();
  /*      for (Widget w : rows.get(0)) {
          w.addStyleName("serverResponseLabelError");
        }*/
      }
    });

    buttonGroupSectionWidget.addButton(overallButton);
    return columnContainer;
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

  //  System.out.println("width is " + width);

    scrollPanel.setWidth(Math.max(300, width) + "px");

    pushFirstListBoxSelection();
  }

  private SectionWidget makeSectionWidget(Container labelContainer, Panel clearColumnContainer, String typeForOriginal, SectionWidget sectionWidget) {
    // make label

   labelContainer.add(getLabelWidget(typeForOriginal));

    // make
    ButtonType buttonType = typeToButton.get(typeForOriginal);

    // make clear button

    FlexTable table2 = new FlexTable();
    ButtonGroup group2 = new ButtonGroup();
    table2.setWidget(0, 0, group2);

    Button sectionButton = makeSubgroupButton(sectionWidget, typeForOriginal, ANY, buttonType, true);
    group2.add(sectionButton);
    sectionWidget.addButton(sectionButton);

    clearColumnContainer.add(table2);
    return sectionWidget;
  }

  /**
   * @see FlexSectionExerciseList#addButtonRow(java.util.List, long, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @param container
   * @param rootNodes
   * @param typeForOriginal
   * @param remainingTypes
   * @param sectionWidget
   */
  private void addButtonGroup(Panel container, List<SectionNode> rootNodes, String typeForOriginal,
                              List<String> remainingTypes, SectionWidget sectionWidget) {
    //String typeForOriginal = subs.isEmpty() ? null : subs.iterator().next();

    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);

    List<String> sortedItems = getSortedItems(getLabels(rootNodes));
    ButtonType buttonType = typeToButton.get(typeForOriginal);
    int n = sortedItems.size();

    List<String> objects = Collections.emptyList();
    List<String> subs = remainingTypes.isEmpty() ? objects : remainingTypes.subList(1, remainingTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();

    for (int i = 0; i < n; i++) {
      String section = sortedItems.get(i);

      SectionNode sectionNode = nameToNode.get(section);

      List<SectionNode> children = sectionNode.getChildren();

      Panel toAddToContainer;
      Button buttonForSection = makeSubgroupButton(sectionWidget, typeForOriginal,
        section,
        buttonType, false);

      if (!children.isEmpty() && subType != null) {
        toAddToContainer = new VerticalPanel();

        DOM.setStyleAttribute(buttonForSection.getElement(), "paddingLeft", "0px");
        DOM.setStyleAttribute(buttonForSection.getElement(), "paddingRight", "0px");
        buttonForSection.setWidth("100%");
        toAddToContainer.add(buttonForSection);
        // recurse on children
        Panel horizontalContainerForChildren = new HorizontalPanel();
        toAddToContainer.add(horizontalContainerForChildren);

/*        System.out.println("for " + typeForOriginal + "=" + section + " recurse on " + children.size() +
          " children at " + subType);*/
        addButtonGroup(horizontalContainerForChildren, children, subType, subs, typeToBox.get(subType));
      } else {
        toAddToContainer = buttonForSection;
      }
      container.add(toAddToContainer);
      if (i != n - 1) {
        SimplePanel child = new SimplePanel();
        child.setWidth("3px");
        container.add(child);
      }
    }
  }

  @Override
  public void onResize() {
    super.onResize();

   // int offsetHeight = lastColumn.getOffsetHeight()+5;
   // System.out.println("onResize height is " + offsetHeight);

    if (label != null) {
      int width = Window.getClientWidth() - label.getOffsetWidth() - clear.getOffsetWidth() - 100;
//      System.out.println("width is " + width);
      scrollPanel.setWidth(Math.max(300, width) + "px");
    }

 /*   if (scrollPanel.getOffsetHeight() != offsetHeight) {
    scrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
    }
*/
  }

  private Widget getLabelWidget(String typeForOriginal) {
    FlexTable table3 = new FlexTable();
    Heading widget = new Heading(HEADING_FOR_LABEL, typeForOriginal);
    table3.setWidget(0, 0, widget);
    DOM.setStyleAttribute(widget.getElement(), "webkitMarginBefore", "10px");
    DOM.setStyleAttribute( widget.getElement(), "webkitMarginAfter", "10px");
    DOM.setStyleAttribute( widget.getElement(), "marginTop", "2px");
    DOM.setStyleAttribute( widget.getElement(), "marginBottom", "10px");
    return table3;
  }

  /**
   * @see
   * @param sectionWidgetFinal
   * @param type
   * @param section
   * @param buttonType
   * @param isClear
   * @return
   */
  private Button makeSubgroupButton(final SectionWidget sectionWidgetFinal, final String type, final String section,
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

    sectionButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        System.out.println("got click on button " + type + "=" + section);
        List<String> selections = new ArrayList<String>();
        selections.add(section);
        sectionWidgetFinal.selectItem(selections, true);
        pushNewSectionHistoryToken();
      }
    });
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
