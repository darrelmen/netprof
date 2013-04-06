package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
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
  List<ButtonType> types = new ArrayList<ButtonType>();
  Map<String,ButtonType> typeToButton = new HashMap<String, ButtonType>();

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
    currentExerciseVPanel.add(sectionPanel = new FluidContainer());
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
    FluidContainer container = new FluidContainer();
    Set<String> types = result.keySet();
    System.out.println("getWidgetsForTypes (success) for user = " + userID + " got types " + types);
    typeToBox.clear();
    typeToButton.clear();
  //  String token = unencodeToken(History.getToken());
 //   SelectionState selectionState = getSelectionState(token);

    String firstType = types.iterator().next(); // e.g. unit!

    FluidRow firstTypeRow = new FluidRow();
    container.add(firstTypeRow);

    Collection<String> sectionsInType = result.get(firstType).keySet();
    sectionsInType = getSortedItems(sectionsInType);

    ButtonGroupSectionWidget buttonGroupSectionWidget = new ButtonGroupSectionWidget(firstType);
    typeToBox.put(firstType, buttonGroupSectionWidget);

    Container clearColumnContainer = addColumnButton(firstType,ANY,buttonGroupSectionWidget, true);
    firstTypeRow.add(clearColumnContainer);

    for (String sectionInFirstType : sectionsInType) {
      Container columnContainer = addColumnButton(firstType,sectionInFirstType, buttonGroupSectionWidget, false);
      firstTypeRow.add(columnContainer);

      service.getTypeToSectionsForTypeAndSection(firstType, sectionInFirstType,
        new TypeToSectionsAsyncCallback(firstType, sectionInFirstType, columnContainer,clearColumnContainer));
    }

    if (false) {
    for (final String type : types) {
      Map<String, Integer> sections = result.get(type);
      System.out.println("\tgetExercises sections for " + type + " = " + sections);

      final SectionWidget listBox = makeListBox(type);
      typeToBox.put(type, listBox);
      populateListBox(listBox, sections);
     // int col = 0;
      FluidRow leftRow = new FluidRow();
  //    left.add(leftRow);
      FluidRow rightRow = new FluidRow();
   //   right.add(rightRow);

      //HorizontalPanel fluidRow = new HorizontalPanel();
      //FlowPanel fluidRow = new FlowPanel();
     // fluidRow.setWidth("100%");

      //container.add(fluidRow);

      if (showListBoxes) {
        HTML html = new HTML(type);
        html.addStyleName("floatLeft");
        leftRow.add(new Column(1,html));
      //  fluidRow.add(html);
        //fluidRow.add(new Column(1, html));
        Widget widget = listBox.getWidget();
        //Column w = new Column(11, widget);
        //w.setWidth("100%");
    //    widget.setWidth("100%");
//        fluidRow.add(widget);
        rightRow.add(new Column(11,widget));
      //  rightRow.add(widget);
        //right.add(widget);

      } else {
    /*    String typeValue = selectionState.typeToSection.get(type);
        if (typeValue != null) {
          fluidRow.add(new Column(1,new HTML(type)));
          fluidRow.add(new Column(11,new HTML("<b>" + typeValue + "</b>")));
        }*/
      }
    }           }

    if (showListBoxes) {
/*      flexTable.setWidget(row, 0, getEmailWidget());
      flexTable.getFlexCellFormatter().setColSpan(row, 0, 2);*/

      FluidRow fluidRow = new FluidRow();
     // container.add(fluidRow);
      Widget emailWidget = getEmailWidget();
      fluidRow.add(new Column(12, emailWidget));

      fluidRow = new FluidRow();
    //  container.add(fluidRow);
      Widget hideBoxesWidget = getHideBoxesWidget();
      fluidRow.add(new Column(12, hideBoxesWidget));
    }
    else {
/*      flexTable.setWidget(row, 0, new HTML("&nbsp;"));
      flexTable.getFlexCellFormatter().setColSpan(row, 0, 2);*/
    }
    return container;
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
    Button overallButton = makeOverallButton(sectionInFirstType);
    group.add(overallButton);
    rowAgain.add(group);

    overallButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        setListBox(type,sectionInFirstType);
        buttonGroupSectionWidget.selectItem(sectionInFirstType);
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

  private Button makeOverallButton(String title) {
    Button overallButton = new Button(title);
    overallButton.setWidth("100%");
    DOM.setStyleAttribute(overallButton.getElement(), "paddingLeft", "0px");
    DOM.setStyleAttribute(overallButton.getElement(), "paddingRight", "0px");
    DOM.setStyleAttribute(overallButton.getElement(), "borderWidth", "0");

    overallButton.setType(ButtonType.PRIMARY);
    return overallButton;
  }

  private class ButtonGroupSectionWidget implements SectionWidget {
    private List<Button> buttons = new ArrayList<Button>();
    private Button clearButton;
    private String type;
    public ButtonGroupSectionWidget(String type) { this.type = type; }

    public void addButton(Button b) {
      this.buttons.add(b);
    }
    public void addClearButton(Button b) {
      clearButton = b;
      clearButton.setEnabled(false);
      clearButton.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          for (Button b : buttons) {
            if (b.isActive()) {
              b.setActive(false);
              break;
            }
          }
          System.out.println("disable clear button " +clearButton);

          clearButton.setEnabled(false);
        }
      });
    }

    @Override
    public String getCurrentSelection() {
      for (Button b : buttons) {
        if (b.isActive()) {
          //System.out.println("getCurrentSelection for " +type + "=" + b.getText());
          return b.getText().trim();
        }
      }
      return ANY;
    }

    /**
     * @see #selectItem(String)
     * @param inSet
     */
    @Override
    public void enableInSet(Collection<String> inSet) {
      System.out.println("enableInSet for " + type + " : " + inSet);

      for (Button b : buttons) {
        String trim = b.getText().trim();
        b.setEnabled(inSet.contains(trim));
      }
    }

    @Override
    public String getFirstItem() {
      return buttons.iterator().next().getText().trim();
    }

    @Override
    public void selectFirstAfterAny() {
      System.out.println("selectFirstAfterAny called?? --------------");
      selectItem(getFirstItem());
    }

    @Override
    public void selectItem(String section) {
      System.out.println("selectItem " + type + "="+section);

      for (Button b : buttons) {
        if (b.isActive()) {
          b.setActive(false);
          break;
        }
      }
      for (Button b : buttons) {
        if (b.getText().trim().equals(section)) {
          b.setActive(true);
          break;
        }
      }
      if (clearButton != null) {
        System.out.println("selectItem " +section);
        clearButton.setEnabled(true);
      }
      else {
        System.err.println("clear button is not set? ");
      }

      final String section1 = section.equals(ANY) ? "" : section;
      System.out.println("selectItem 2 " + type + "="+section1);

      service.getTypeToSectionsForTypeAndSection(type, section1, new AsyncCallback<Map<String, Collection<String>>>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Map<String, Collection<String>> result) {
          System.out.println("selectItem onSuccess " + type + "="+section1);

          for (Map.Entry<String,Collection<String>> pair : result.entrySet()) {
            SectionWidget sectionWidget = typeToBox.get(pair.getKey());
            sectionWidget.enableInSet(pair.getValue());
          }
        }
      });
    }

    @Override
    public void retainCurrentSelectionState(String currentSelection) {
      for (Button b : buttons) {
        if (b.isActive()) {
          b.setActive(false);
          break;
        }
      }
      boolean found = false;
      for (Button b : buttons) {
        if (b.getText().trim().equals(currentSelection)) {
          b.setActive(true);
          found = true;
          break;
        }
      }
      if (!found) buttons.iterator().next().setActive(true);
    }

    @Override
    public void populateTypeWidget(Collection<String> items, Map<String, Integer> sectionToCount) {
    }

    @Override
    public Widget getWidget() {
      return null;
    }
  }

  private class TypeToSectionsAsyncCallback implements AsyncCallback<Map<String, Collection<String>>> {
    private final String type;
    private final String itemText;
    private Container columnContainer;
    Container clearColumnContainer;
    public TypeToSectionsAsyncCallback(String type, String itemText,  Container columnContainer,
                                       Container clearColumnContainer) {
      this.type = type;
      this.itemText = itemText;
      this.columnContainer = columnContainer;
      this.clearColumnContainer = clearColumnContainer;
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
        System.out.println("type " + typeForOriginal + " : " +pair.getValue());

        SectionWidget sectionWidget = typeToBox.get(typeForOriginal);
        if (sectionWidget == null) {
          typeToButton.put(typeForOriginal, types.get(typeToBox.size() % types.size()));
          typeToBox.put(typeForOriginal, sectionWidget = new ButtonGroupSectionWidget(typeForOriginal));
          ButtonType buttonType = typeToButton.get(typeForOriginal);
          final SectionWidget sectionWidgetFinal = sectionWidget;

          FlexTable table2 = new FlexTable();
          ButtonGroup group2 = new ButtonGroup();
          table2.setWidget(0, 0, group2);

          Button sectionButton = makeSubgroupButton(sectionWidgetFinal, typeForOriginal, ANY,buttonType);
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

          Button sectionButton = makeSubgroupButton(sectionWidgetFinal, type, section,buttonType);
          group.add(sectionButton);
          ((ButtonGroupSectionWidget)sectionWidget).addButton(sectionButton);
        }
        row++;
      }
       columnContainer.add(table);
    }
  }

  private Button makeSubgroupButton(final SectionWidget sectionWidgetFinal, final String type, final String section, ButtonType buttonType) {
    Button sectionButton = new Button(section);

    DOM.setStyleAttribute(sectionButton.getElement(), "borderWidth", "0");
    sectionButton.setType(buttonType);

    sectionButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        setListBox(type,section);
        sectionWidgetFinal.selectItem(section);
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
        getListBoxOnClick(type);
      }
    });

    return widgets;
  }

  protected void setOtherListBoxes(final String selectedType, final String section) {
    System.out.println("setOtherListBoxes skipping! type " + selectedType + "=" + section);
  }

  protected void populateListBox(Map<String, Collection<String>> result) {
    //System.out.println("populateListBox skipping! ");
    for (Map.Entry<String,Collection<String>> pair : result.entrySet()) {
      typeToBox.get(pair.getKey()).enableInSet(pair.getValue());
    }
  }
}
