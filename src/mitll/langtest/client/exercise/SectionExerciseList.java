package mitll.langtest.client.exercise;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 2/25/13
 * Time: 1:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class SectionExerciseList extends PagingExerciseList {
  private Map<String, Collection<String>> typeToSections;
  private Panel sectionPanel;
  private Map<String,ListBox> typeToBox = new HashMap<String, ListBox>();
  //private List<RadioButton> radios = new ArrayList<RadioButton>();

  public SectionExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                             UserFeedback feedback,
                             boolean arabicDataCollect, boolean showTurkToken, boolean showInOrder) {
    super(currentExerciseVPanel, service, feedback, arabicDataCollect, showTurkToken, showInOrder);
  }

  @Override
  protected void checkBeforeLoad(ExerciseShell e) {}

  @Override
  protected void addTableWithPager() {
    add(sectionPanel = new VerticalPanel());
    super.addTableWithPager();
  }

  @Override
  public void getExercises(final long userID) {
    //service.getSubsections();
    service.getTypeToSection(new AsyncCallback<Map<String, Collection<String>>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Map<String, Collection<String>> result) {
        typeToSections = result;
        sectionPanel.clear();

        final FlexTable g = new FlexTable(/*typeToSections.keySet().size()+1,2*/);
        String first = null;
        int row = 0;
        Set<String> types = result.keySet();

        System.out.println("types are " + types);
        for (final String type : types) {
          final ListBox listBox = new ListBox();
          typeToBox.put(type,listBox);
          if (first == null) first = type;

          List<String> sections = getSections(result, type);
          populateListBox(listBox, sections);
          listBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
              getListBoxOnClick(listBox, type);
            }
          });
          int col = 0;
    /*
          final RadioButton radio = new RadioButton("SectionType", type);
          radios.add(radio);
          radio.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              for (RadioButton rb : radios) {
                typeToBox.get(rb.getText()).setEnabled(rb == radio);
              }
            }
          });
          g.setWidget(row, col++, radio);
          radio.setValue(row == 0);
          listBox.setEnabled(row == 0);*/
          g.setWidget(row, col++, new HTML(type));

          g.setWidget(row++,col++,listBox);
        }
        setChapterBox(typeToBox.get("chapter"), "unit", typeToBox.get("unit").getItemText(0));

        g.setWidget(row, 0, getEmailWidget());
        g.getFlexCellFormatter().setColSpan(row, 0, 2);

        sectionPanel.add(g);

        if (first != null) {
          pushFirstSelection(first);
        }
        else {
          noSectionsGetExercises(userID);
        }
      }
    });
  }

  private void getListBoxOnClick(final ListBox listBox, final String type) {
    final String itemText = listBox.getItemText(listBox.getSelectedIndex());

    System.out.println("box " + type + " select " + itemText);
    //pushNewSectionHistoryToken(type, itemText);

    if (type.equals("unit")) {
      ListBox chapter = typeToBox.get("chapter");
      setChapterBox(chapter, type, itemText);
    } else {
      pushNewSectionHistoryToken(/*type, itemText*/);
    }
  }

  private void setChapterBox(final ListBox chapter, final String type, final String itemText) {
    System.out.println("setting chapter box given " + type + " select " + itemText);

    service.getSubsectionsForTypeAndSection(type, itemText, new AsyncCallback<Collection<String>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Collection<String> result) {
     /*   if (result.isEmpty()) {
          pushNewSectionHistoryToken(type, itemText);
        } else {*/
          // hack!
          //ListBox chapter = typeToBox.get("chapter");
        if (!result.isEmpty()) {
          chapter.clear();
          populateListBox(chapter, result);
          chapter.setSelectedIndex(0);
        }
        //}
        pushNewSectionHistoryToken();
      }
    });
  }

  private void populateListBox(ListBox listBox, Collection<String> sections) {
    for (String section : sections) {
      listBox.addItem(section);
    }
  }

  private Widget getEmailWidget() {
    FlexTable g = new FlexTable();
    Anchor widget = new Anchor("E-MAIL");
    //HTMLPanel container = new HTMLPanel("h3",widget.getHTML());
    int row = 0;
    //g.setText(row,0, "");
    g.setWidget(row, 0, new HTML("Share via "));
   // g.getFlexCellFormatter().setColSpan(row, 0, 2);
    //row++;
    //g.setText(row,0, "");
    g.setWidget(row,1, widget);
    //g.setWidget(row,2, widget);
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Triple triple = getTriple(History.getToken());

        feedback.showEmail("Lesson " +
          triple
          //triple.type + " : " + triple.section
          , "", History.getToken());
      }
    });

    FlowPanel widgets = new FlowPanel();
    widgets.add(g);
    return widgets;
  }

  private void pushFirstSelection(String first) {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      ListBox listBox = typeToBox.get(first);
      String itemText = listBox.getItemText(listBox.getSelectedIndex());
      System.out.println("push first " + first + " select " + itemText);

      pushNewSectionHistoryToken(/*first, itemText*/);
    } else {
      System.out.println("fire history ");
      History.fireCurrentHistoryState();
    }
  }

  private List<String> getSections(Map<String, Collection<String>> result, String type) {
    List<String> sections = new ArrayList<String>(result.get(type));
    return getSections(sections);
  }

  private List<String> getSections(List<String> sections) {
    boolean allInt = !sections.isEmpty();
    for (String s : sections) {
      try {
        Integer.parseInt(s);
      } catch (NumberFormatException e) {
        allInt = false;
        break;
      }
    }
    if (allInt) {
      Collections.sort(sections, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          int first = Integer.parseInt(o1);
          int second = Integer.parseInt(o2);
          return first < second ? -1 : first > second ? +1 : 0;
        }
      });
    } else {
      Collections.sort(sections);
    }
    return sections;
  }

  private void noSectionsGetExercises(long userID) {
    super.getExercises(userID);
  }

  private void loadExercises(final String type, final String section, final String item) {
    System.out.println("loadExercises " + type + " " + section + " item " +item);
    service.getExercisesForSection(type, section, new SetExercisesCallback() {
      @Override
      public void onSuccess(List<ExerciseShell> result) {
        if (!result.isEmpty()) {
          if (item != null) {
            rememberExercises(result);
            if (!loadByID(item)) {
              loadFirstExercise();
            }
          } else {
            super.onSuccess(result);
          }
        }
      }
    });
  }

  private void pushNewSectionHistoryToken(/*String type, String section*/) {
    String historyToken = getHistoryToken(null);
    System.out.println("------------ push history " + historyToken + " -------------- ");
    History.newItem(historyToken);//"type=" + type + ";section=" + section);
  }

  @Override
  protected void gotClickOnItem(ExerciseShell e) {
  }

  @Override
  protected String getHistoryTokenForLink(String id) {
    return "#"+getHistoryToken(id);
  }

/*  protected String getHistoryToken(String id) {
    for (RadioButton rb : radios) {
      if (rb.getValue()) {
        String type = rb.getText();
        ListBox listBox = typeToBox.get(type);
        String section = listBox.getItemText(listBox.getSelectedIndex());
        // System.out.println("------------ push history " + type +"/"+ section + "/" +id+ " -------------- ");

        return "type=" + type + ";section=" + section + ";item=" + id;
      }
    }
    return "";
  }*/

  protected String getHistoryToken(String id) {
    StringBuilder builder = new StringBuilder();
    for (String type : typeToBox.keySet()) {
    //  if (rb.getValue()) {
    //    String type = rb.getText();
        ListBox listBox = typeToBox.get(type);
        String section = listBox.getItemText(listBox.getSelectedIndex());
        // System.out.println("------------ push history " + type +"/"+ section + "/" +id+ " -------------- ");

      // return "type=" + type + ";section=" + section + ";item=" + id;
      builder.append(type + "=" + section + ";");

      //}
    }
    if (id != null) {
      builder.append("item=" + id);
    }
    return builder.toString();
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    String token = event.getValue();
    token = token.replaceAll("%3D","=").replaceAll("%3B",";").replaceAll("%2"," ").replaceAll("\\+"," ");
    System.out.println("onValueChange " + token);
    if (token.contains("=")) {
      try {
        Triple triple = getTriple(token);

        //restoreRadioButtonState(triple.type);
        restoreListBoxState(triple);
        String type =  triple.typeToSection.containsKey("chapter") ? "chapter" : "week"; // cheesy hack
        String section = triple.typeToSection.containsKey("chapter") ? triple.typeToSection.get("chapter") :  triple.typeToSection.get("week"); // cheesy hack
        loadExercises(type, section, triple.item);
      } catch (Exception e) {
        System.out.println("onValueChange " + token + " badly formed. Got " +e);
        e.printStackTrace();
      }
    }
    else {
      super.onValueChange(event);
    }
  }

  private Triple getTriple(String token) {
    Triple triple = new Triple();
    String[] parts = token.split(";");

    for (String part : parts) {
      System.out.println("part " + part);
      String[] segments = part.split("=");
      String type = segments[0].trim();
      String section = segments[1].trim();
      triple.add(type,section);
    }

   // String item = null;

    if (token.contains("item")) {
      int item1 = token.indexOf("item=");
      String itemValue = token.substring(item1);
      System.out.println("got " + itemValue);
      triple.setItem(itemValue);
    }

    System.out.println("triple " +triple);

/*    String typePart = parts[0];
    String sectionPart = parts[1];
    String type = typePart.split("=")[1].trim();
    String section = sectionPart.split("=")[1].trim();*/
/*    if (parts.length == 3) {
      item = parts[2].split("=")[1].trim();
    }*/
    return triple;
  }

  private static class Triple {
    private String /*type, section,*/ item;
    public Map<String, String> typeToSection = new HashMap<String, String>();

    public void add(String type, String section) {
      typeToSection.put(type, section);
    }

    // public Triple(/*String type, String section,*/ String item) {/* this.type = type; this.section = section;*/ this.setItem(item);}

    public void setItem(String item) {
      this.item = item;
    }

    public String toString() {
      StringBuilder builder = new StringBuilder();
      for (String section : typeToSection.values()) builder.append(section).append(", ");
      String s = builder.toString();
      return s.substring(s.length() - 2);
    }
  }

  /*private void restoreRadioButtonState(String type) {
    boolean found = false;
    for (RadioButton rb : radios) {
      boolean foundMatch = rb.getText().equals(type);
      if (foundMatch) found = true;
      rb.setValue(foundMatch);
    }
    if (!found && !radios.isEmpty()) { // so if we get a bad type, something is selected
      radios.get(0).setValue(true);
    }
  }*/

  private void restoreListBoxState(Triple triple) {
    for (Map.Entry<String, String> pair : triple.typeToSection.entrySet()) {
      String type = pair.getKey();
      String section = pair.getValue();
 /*     for (ListBox lb : typeToBox.values()) {
        lb.setEnabled(false);
      }*/
      ListBox listBox = typeToBox.get(type);
      if (listBox == null) System.err.println("huh? bad type " + type);
        //  listBox.setEnabled(true);
      else {
        for (int i = 0; i < listBox.getItemCount(); i++) {
          String itemText = listBox.getItemText(i);
          if (itemText.equals(section)) {
            listBox.setSelectedIndex(i);
            break;
          }
        }
      }
    }
  }


  private static final int HEIGHT_OF_CELL_TABLE_WITH_15_ROWS = 390 - 20-65;

  protected int getTableHeaderHeight() {
    return 625 - HEIGHT_OF_CELL_TABLE_WITH_15_ROWS;
  }
}
