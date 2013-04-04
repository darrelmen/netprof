package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.SectionExerciseList;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.user.UserFeedback;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/5/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class BootstrapSectionExerciseList extends SectionExerciseList {
  //private Map<String,SelList> typeToBox = new HashMap<String, SelList>();

  private static class SelList {
    private int current = 0;
    List<String> selections;
    public SelList(List<String> selections) { this.selections = selections; }

    public int getCurrent() {
      return current;
    }

    public String getCurrentValue() { return selections.get(current); }

    public void setCurrent(int current) {
      this.current = current;
    }
  }
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

  @Override
  protected SectionWidget makeListBox(String type) {
    BarSectionWidget widgets = new BarSectionWidget();

    return widgets;
  }

  /*  @Override
  public void getExercises(final long userID) {
    System.out.println("------- Get exercises for " + userID);
    this.userID = userID;
    service.getTypeToSection(new AsyncCallback<Map<String, Collection<String>>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Map<String, Collection<String>> result) {
        //typeToSections = result;
        sectionPanel.clear();

        //final FlexTable g = new FlexTable();
        String first = null;
        int row = 0;
        Set<String> types = result.keySet();
        System.out.println("got types " + types);
        typeToBox.clear();

        for (final String type : types) {
          if (first == null) first = type;

          //final ListBox listBox = new ListBox();
          //typeToBox.put(type,listBox);
          List<String> sections = getSections(result, type);
          typeToBox.put(type,new SelList(sections));
         // populateListBox(listBox, sections);
      *//*    listBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
              getListBoxOnClick(listBox, type);
            }
          });
          int col = 0;*//*
          StackProgressBar stackProgressBar = new StackProgressBar();
          boolean oddEven = false;
          for (String section : sections) {
            Bar bar = new Bar();
            int percent = (int) (100f * (1f / (float) sections.size())); // TODO : get per item quantity
            bar.setPercent(percent);
            bar.setColor(oddEven ? Bar.Color.INFO : Bar.Color.DEFAULT);
            bar.setText(section);
            stackProgressBar.add(bar);
            oddEven = !oddEven;
          }
          //g.setWidget(row, col++, new HTML(type));
          //g.setWidget(row++,col,listBox);
          FluidRow typeRow = new FluidRow();
          Heading heading = new Heading(4);
          heading.setText(type);
          typeRow.add(new Column(1, heading));
          typeRow.add(new Column(11,stackProgressBar));
          sectionPanel.add(typeRow);
        }
        // doNestedSections();
      //  g.setWidget(row, 0, getEmailWidget());
      //  g.getFlexCellFormatter().setColSpan(row, 0, 2);

       // sectionPanel.add(g);

        if (first != null) {
          //typeToBox.get(first).setSelectedIndex(1); // not any
          pushFirstSelection(first);
        }
        else {
          noSectionsGetExercises(userID);
        }
      }
    });
  }*/
}
