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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.SectionExerciseList;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.user.UserFeedback;

import java.util.ArrayList;
import java.util.Collection;
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
  private List<ButtonType> types = new ArrayList<ButtonType>();
  private Map<String,ButtonType> typeToButton = new HashMap<String, ButtonType>();
  int numExpectedTypes = 0;

  public FlexSectionExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                 UserFeedback feedback,
                                 boolean showTurkToken, boolean showInOrder, boolean showListBox) {
    super(currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, showListBox);

    types.add(ButtonType.PRIMARY);
    types.add(ButtonType.SUCCESS);
    types.add(ButtonType.INFO);
    types.add(ButtonType.WARNING);
  }

  @Override
  protected void addWidgets(Panel currentExerciseVPanel) {
    Panel child = sectionPanel = new FluidContainer();
    DOM.setStyleAttribute(sectionPanel.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(sectionPanel.getElement(), "paddingRight", "2px");
    currentExerciseVPanel.add(child);
    super.addWidgets(currentExerciseVPanel);
  }

  @Override
  protected void addComponents() {
    addTableWithPager();
  }

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @see mitll.langtest.client.exercise.SectionExerciseList#useInitialTypeToSectionMap(java.util.Map, long)
   * @param result
   * @param userID
   * @return
   */
  protected Panel getWidgetsForTypes(Map<String, Map<String, Integer>> result, long userID) {
    numExpectedTypes = result.keySet().size();
    FluidContainer container = new FluidContainer();
    DOM.setStyleAttribute(container.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(container.getElement(), "paddingRight", "2px");

    Set<String> types = result.keySet();
    if (showListBoxes) {
      System.out.println("getWidgetsForTypes (success) for user = " + userID + " got types " + types);
      typeToBox.clear();
      typeToButton.clear();

      String firstType = types.iterator().next(); // e.g. unit!

      FluidRow firstTypeRow = new FluidRow();
      container.add(firstTypeRow);

      Collection<String> sectionsInType = result.get(firstType).keySet();
      sectionsInType = getSortedItems(sectionsInType);

      ButtonGroupSectionWidget buttonGroupSectionWidget = new ButtonGroupSectionWidget(firstType);
      typeToBox.put(firstType, buttonGroupSectionWidget);

      Container labelContainer = new FluidContainer();
      FluidRow rowAgain = new FluidRow();

      labelContainer.addStyleName("inlineStyle");
      DOM.setStyleAttribute(labelContainer.getElement(), "paddingLeft", "2px");
      DOM.setStyleAttribute(labelContainer.getElement(), "paddingRight", "2px");

      Heading widget = makeLabelWidget(firstType);

      rowAgain.add(widget);
      labelContainer.add(rowAgain);

      firstTypeRow.add(labelContainer);

      Container clearColumnContainer = addColumnButton(firstType, ANY, buttonGroupSectionWidget, true);
      firstTypeRow.add(clearColumnContainer);

      for (String sectionInFirstType : sectionsInType) {
        Container columnContainer = addColumnButton(firstType, sectionInFirstType, buttonGroupSectionWidget, false);
        firstTypeRow.add(columnContainer);

        service.getTypeToSectionsForTypeAndSection(firstType, sectionInFirstType,
          new TypeToSectionsAsyncCallback(firstType, sectionInFirstType, columnContainer, clearColumnContainer, labelContainer));
      }

      addBottomText(container);
    } else {
      String token = unencodeToken(History.getToken());
      SelectionState selectionState = getSelectionState(token);

      for (final String type : types) {
        String typeValue = selectionState.typeToSection.get(type);
        if (typeValue != null) {
          FluidRow fluidRow = new FluidRow();
          container.add(fluidRow);

          fluidRow.add(new Column(2, new Heading(4,type)));
          fluidRow.add(new Column(1, new Heading(4,typeValue)));
        }
      }
    }
    return container;
  }

  @Override
  protected void useInitialTypeToSectionMap(Map<String, Map<String, Integer>> result, long userID) {
    super.useInitialTypeToSectionMap(result, userID);

    System.out.println("useInitialTypeToSectionMap push history : " +History.getToken());

    if (!showListBoxes) {
      SelectionState selectionState = getSelectionState(History.getToken());
      loadExercises(selectionState.typeToSection, selectionState.item);
    }
  }

  private void addBottomText(FluidContainer container) {
    FluidRow fluidRow1 = new FluidRow();
    container.add(fluidRow1);
    fluidRow1.add(new Column(7, 3, new Heading(5, "Click on the buttons to select just what you want to see.")));

    FluidRow fluidRow = new FluidRow();
    container.add(fluidRow);
    Widget emailWidget = getEmailWidget();
    fluidRow.add(new Column(2, 3, emailWidget));

    Widget hideBoxesWidget = getHideBoxesWidget();
    fluidRow.add(new Column(2, 1, hideBoxesWidget));
  }

  private Heading makeLabelWidget(String firstType) {
    Heading widget = new Heading(HEADING_FOR_LABEL, firstType);
    DOM.setStyleAttribute(widget.getElement(), "webkitMarginBefore", "0");
    DOM.setStyleAttribute( widget.getElement(), "webkitMarginAfter", "0");
    DOM.setStyleAttribute( widget.getElement(), "marginBottom", "0px");
    return widget;
  }

  private Container addColumnButton(final String type, final String sectionInFirstType,
                                    final ButtonGroupSectionWidget buttonGroupSectionWidget, boolean isClear) {
    Container columnContainer = new FluidContainer();
    columnContainer.addStyleName("inlineStyle");
    DOM.setStyleAttribute(columnContainer.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(columnContainer.getElement(), "paddingRight", "2px");

    FluidRow rowAgain = new FluidRow();
    columnContainer.add(rowAgain);

    ButtonGroup group = new ButtonGroup();
    group.setWidth("100%");

    // add a button
    Button overallButton = makeOverallButton(sectionInFirstType, isClear);
    //System.out.println("making button " + type + "="+sectionInFirstType);

    group.add(overallButton);
    rowAgain.add(group);

    overallButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        System.out.println("got click on button " + type + "=" + sectionInFirstType);

        buttonGroupSectionWidget.selectItem(sectionInFirstType, true);
        pushNewSectionHistoryToken();
      }
    });

    if (isClear) {
      buttonGroupSectionWidget.addClearButton(overallButton);
    }
    else {
      buttonGroupSectionWidget.addButton(overallButton);
    }
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

  private class TypeToSectionsAsyncCallback implements AsyncCallback<Map<String, Collection<String>>> {
    private final String type;
    private final String itemText;
    private Container columnContainer;
    private Container clearColumnContainer;
    private Container labelContainer;

    /**
     * @see FlexSectionExerciseList#getWidgetsForTypes(java.util.Map, long)
     * @param type
     * @param itemText
     * @param columnContainer
     * @param clearColumnContainer
     * @param labelContainer
     */
    public TypeToSectionsAsyncCallback(String type, String itemText, Container columnContainer,
                                       Container clearColumnContainer, Container labelContainer) {
      this.type = type;
      this.itemText = itemText;
      this.columnContainer = columnContainer;
      this.clearColumnContainer = clearColumnContainer;
      this.labelContainer = labelContainer;
    }

    @Override
    public void onFailure(Throwable caught) {
      Window.alert("Can't contact server.");
    }

    @Override
    public void onSuccess(Map<String, Collection<String>> result) {
      System.out.println("Flex  : TypeToSectionsAsyncCallback onSuccess " + type + "=" + itemText + " yielded " +result);
      FlexTable table = new FlexTable();
      int row = 0;
      for (Map.Entry<String,Collection<String>> pair : result.entrySet()) {
        String typeForOriginal = pair.getKey();
        System.out.println("\ttype " + typeForOriginal + " : " +pair.getValue());

        SectionWidget sectionWidget = typeToBox.get(typeForOriginal);
        if (sectionWidget == null) {
          // make label

          labelContainer.add(getLabelWidget(typeForOriginal));

          // make
          typeToButton.put(typeForOriginal, types.get(typeToBox.size() % types.size()));
          typeToBox.put(typeForOriginal, sectionWidget =
            new ButtonGroupSectionWidget(typeForOriginal));
          ButtonType buttonType = typeToButton.get(typeForOriginal);
          final SectionWidget sectionWidgetFinal = sectionWidget;

          // make clear button

          FlexTable table2 = new FlexTable();
          ButtonGroup group2 = new ButtonGroup();
          table2.setWidget(0, 0, group2);

          Button sectionButton = makeSubgroupButton(sectionWidgetFinal, typeForOriginal, ANY, buttonType, true);
          group2.add(sectionButton);
          ((ButtonGroupSectionWidget)sectionWidget).addClearButton(sectionButton);

          clearColumnContainer.add(table2);
        }

        final SectionWidget sectionWidgetFinal = sectionWidget;
        final String type = typeForOriginal;
        List<String> sortedItems = getSortedItems(pair.getValue());
        int col = 0;
        ButtonType buttonType = typeToButton.get(typeForOriginal);

        for (final String section : sortedItems) {
          ButtonGroup group = new ButtonGroup();
          table.setWidget(row, col++, group);

          Button sectionButton = makeSubgroupButton(sectionWidgetFinal, type, section,buttonType, false);
          group.add(sectionButton);
          ((ButtonGroupSectionWidget)sectionWidget).addButton(sectionButton);
        }
        row++;
      }
      columnContainer.add(table);
      if (typeToBox.size() == numExpectedTypes) {
        pushFirstListBoxSelection();
      }
    }
  }

  private Widget getLabelWidget(String typeForOriginal) {
    FlexTable table3 = new FlexTable();
    Heading widget = new Heading(HEADING_FOR_LABEL, typeForOriginal);
    table3.setWidget(0, 0, widget);
    DOM.setStyleAttribute(widget.getElement(), "webkitMarginBefore", "10px");
    DOM.setStyleAttribute( widget.getElement(), "webkitMarginAfter", "10px");
    DOM.setStyleAttribute( widget.getElement(), "marginBottom", "0px");
    return table3;
  }

  private Button makeSubgroupButton(final SectionWidget sectionWidgetFinal, final String type, final String section,
                                    ButtonType buttonType, boolean isClear) {
    //System.out.println("making button " + type + "=" + section);
    Button sectionButton = new Button(section);
    DOM.setStyleAttribute(sectionButton.getElement(), "borderWidth", "0");
    sectionButton.setType(isClear ? ButtonType.DEFAULT : buttonType);
    sectionButton.setEnabled(!isClear);

    sectionButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {

        System.out.println("got click on button " + type + "=" + section);

        sectionWidgetFinal.selectItem(section, true);
        pushNewSectionHistoryToken();
      }
    });
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
  protected void populateListBoxAfterSelection(Map<String, Set<String>> result) {
    Set<String> typesMentioned = new HashSet<String>(typeToBox.keySet());
    for (Map.Entry<String, Set<String>> pair : result.entrySet()) {
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
