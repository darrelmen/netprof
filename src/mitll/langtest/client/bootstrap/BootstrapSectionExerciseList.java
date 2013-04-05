package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
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
public class BootstrapSectionExerciseList extends SectionExerciseList {
  public BootstrapSectionExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service,
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
   * @see SectionExerciseList#useInitialTypeToSectionMap(java.util.Map, long)
   * @param result
   * @param userID
   * @return
   */
  protected Panel getWidgetsForTypes(Map<String, Map<String, Integer>> result, long userID) {
    //final FlexTable flexTable = new FlexTable();
    FluidContainer container = new FluidContainer();
    //VerticalPanel container = new VerticalPanel();
    //container.setWidth("100%");
  //  int row = 0;
    Set<String> types = result.keySet();
    System.out.println("getExercises (success) for user = " + userID + " got types " + types);
    typeToBox.clear();

  //  String token = unencodeToken(History.getToken());
 //   SelectionState selectionState = getSelectionState(token);

    FluidRow outerRow = new FluidRow();
    Column left = new Column(1);
    outerRow.add(left);
    Column right = new Column(11);
    outerRow.add(right);
    container.add(outerRow);

    for (final String type : types) {
      Map<String, Integer> sections = result.get(type);
      System.out.println("\tgetExercises sections for " + type + " = " + sections);

      final SectionWidget listBox = makeListBox(type);
      typeToBox.put(type, listBox);
      populateListBox(listBox, sections);
     // int col = 0;
      FluidRow leftRow = new FluidRow();
      left.add(leftRow);
      FluidRow rightRow = new FluidRow();
      right.add(rightRow);

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
    }

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
