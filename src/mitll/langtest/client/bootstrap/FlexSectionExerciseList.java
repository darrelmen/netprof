package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
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

import java.util.Collection;
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
  public FlexSectionExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                 UserFeedback feedback,
                                 boolean showTurkToken, boolean showInOrder, boolean showListBox) {
    super(currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, showListBox);
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
    System.out.println("getExercises (success) for user = " + userID + " got types " + types);
    typeToBox.clear();

  //  String token = unencodeToken(History.getToken());
 //   SelectionState selectionState = getSelectionState(token);

    String firstType = types.iterator().next(); // e.g. unit!

    FluidRow firstTypeRow = new FluidRow();
    container.add(firstTypeRow);

    Collection<String> sectionsInType = result.get(firstType).keySet();
    sectionsInType = getSortedItems(sectionsInType);
    for (String sectionInFirstType : sectionsInType) {
      Container columnContainer = new FluidContainer();
      columnContainer.addStyleName("inlineStyle");
      DOM.setStyleAttribute(columnContainer.getElement(), "paddingLeft", "2px");
      DOM.setStyleAttribute(columnContainer.getElement(), "paddingRight", "2px");

      firstTypeRow.add(columnContainer);

      FluidRow rowAgain = new FluidRow();
      columnContainer.add(rowAgain);

      ButtonGroup group = new ButtonGroup();
      group.setWidth("100%");

      rowAgain.add(group);

      // add a button
      Button overallButton = new Button(sectionInFirstType);
      group.add(overallButton);
      overallButton.setWidth("100%");
      DOM.setStyleAttribute(overallButton.getElement(), "paddingLeft", "0px");
      DOM.setStyleAttribute(overallButton.getElement(), "paddingRight", "0px");
      DOM.setStyleAttribute(overallButton.getElement(), "borderWidth", "0");

      overallButton.setType(ButtonType.PRIMARY);


      service.getTypeToSectionsForTypeAndSection(firstType, sectionInFirstType,
        new TypeToSectionsAsyncCallback(firstType, sectionInFirstType,columnContainer));

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

  private class TypeToSectionsAsyncCallback implements AsyncCallback<Map<String, Collection<String>>> {
    private final String type;
    private final String itemText;
    private Container columnContainer;

    public TypeToSectionsAsyncCallback(String type, String itemText,  Container columnContainer ) {
      this.type = type;
      this.itemText = itemText;
      this.columnContainer = columnContainer;
    }

    @Override
    public void onFailure(Throwable caught) {
      Window.alert("Can't contact server.");
    }

    @Override
    public void onSuccess(Map<String, Collection<String>> result) {
      System.out.println("TypeToSectionsAsyncCallback onSuccess " + type + "=" + itemText + " yielded " +result);
      FlexTable table = new FlexTable();
      int row = 0;
      for (Map.Entry<String,Collection<String>> pair : result.entrySet()) {
       // FluidRow rowAgain = new FluidRow();
        //table.addRow();

        // ButtonGroup group = new ButtonGroup();
        //table.setWidget(row, 0, group);

        //group.setWidth("100%");
 //       rowAgain.add(group);

        System.out.println("type " + pair.getKey() + " : " +pair.getValue());
        List<String> sortedItems = getSortedItems(pair.getValue());
        int col = 0;
        for (String section : sortedItems) {
          ButtonGroup group = new ButtonGroup();
      //    group.setWidth("100%");
         // rowAgain.add(group);
          table.setWidget(row, col++, group);

          //sectionButton.setWidth("100%");
          //String width = Math.round(100f / (float) sortedItems.size()) + "%";
         // System.out.println("width " +width);
       //   sectionButton.setWidth("20%");

          Button sectionButton = new Button(section);
          //sectionButton.setWidth("100%");

         // Element parentElement = group.getParent().getElement();
         // Element element = group.getElement();
       //   com.google.gwt.dom.client.Element parentElement = element.getParentElement();
         // DOM.setStyleAttribute(parentElement, "paddingLeft",  "0px");
          // DOM.setStyleAttribute(parentElement, "paddingRight", "0px");
          DOM.setStyleAttribute(sectionButton.getElement(), "borderWidth", "0");
        //  sectionButton.addStyleName("addPadding");
          group.add(sectionButton);
          sectionButton.setType(ButtonType.PRIMARY);
        }
        //ButtonGroup group = new ButtonGroup();
       // columnContainer.add(rowAgain);
        row++;
      }
       columnContainer.add(table);

      //populateListBox(result);
      //pushNewSectionHistoryToken();
    }
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
    System.out.println("populateListBox skipping! ");
  }
}
