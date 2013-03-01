package mitll.langtest.client.exercise;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
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
  public static final String ANY = "Any";
  private Panel sectionPanel;
  private long userID;
  private Map<String,ListBox> typeToBox = new HashMap<String, ListBox>();

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
    System.out.println("Get exercises " + userID);
    this.userID = userID;
    service.getTypeToSection(new AsyncCallback<Map<String, Collection<String>>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Map<String, Collection<String>> result) {
        //typeToSections = result;
        sectionPanel.clear();

        final FlexTable g = new FlexTable();
        String first = null;
        int row = 0;
        Set<String> types = result.keySet();
        System.out.println("got types " + types);
        typeToBox.clear();

        for (final String type : types) {
          if (first == null) first = type;

          final ListBox listBox = new ListBox();
          typeToBox.put(type,listBox);
          List<String> sections = getSections(result, type);
          populateListBox(listBox, sections);
          listBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
              getListBoxOnClick(listBox, type);
            }
          });
          int col = 0;

          g.setWidget(row, col++, new HTML(type));
          g.setWidget(row++,col,listBox);
        }
       // doNestedSections();
        g.setWidget(row, 0, getEmailWidget());
        g.getFlexCellFormatter().setColSpan(row, 0, 2);

        sectionPanel.add(g);

        if (first != null) {
          typeToBox.get(first).setSelectedIndex(1); // not any
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
    setListBox(type, itemText);
  }

  /**
   * @see #getListBoxOnClick(com.google.gwt.user.client.ui.ListBox, String)
   * @param type
   * @param itemText
   */
  private void setListBox(final String type, final String itemText) {
    System.out.println("setListBox given " + type + "=" + itemText);

    if (itemText.equals(ANY)) {
      service.getTypeToSection(new AsyncCallback<Map<String, Collection<String>>>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Map<String, Collection<String>> result) {
          Set<String> types = result.keySet();
          System.out.println("setListBox onSuccess " + type + "=" + itemText + " got types = " + types);

          for (final String type : types) {
            List<String> sections = getSections(result, type);
            populateListBox(type, sections);
          }
          pushNewSectionHistoryToken();

          //noSectionsGetExercises(userID);
        }
      });
    } else {
      service.getTypeToSectionsForTypeAndSection(type, itemText, new AsyncCallback<Map<String, List<String>>>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Can't contact server.");
        }

        /**
         * This is a map from type to sections
         * @param result
         */
        @Override
        public void onSuccess(Map<String, List<String>> result) {
          System.out.println("setListBox onSuccess " + type + "=" + itemText + " yielded " +result);

          for (Map.Entry<String, List<String>> pair : result.entrySet()) {
            String type = pair.getKey();
            List<String> sections = pair.getValue();
            populateListBox(type, sections);
          }
          pushNewSectionHistoryToken();
        }
      });
    }
  }

  /**
   * @see #setListBox(String, String)
   * @param type
   * @param sections
   */
  private void populateListBox(String type, Collection<String> sections) {
    ListBox listBox = typeToBox.get(type);
    populateListBox(listBox, sections);
    if (listBox.getItemCount() > 0) {
      listBox.setSelectedIndex(0);
    }
  }

  /**
   * @see #getExercises(long)
   * @see #populateListBox(String, java.util.Collection)
   * @param listBox
   * @param sections
   */
  private void populateListBox(ListBox listBox, Collection<String> sections) {
    listBox.clear();
    listBox.addItem(ANY);
    List<String> items = new ArrayList<String>(sections);
    Collections.sort(items);
    for (String section : items) {
      listBox.addItem(section);
    }
  }

  /**
   * @see #getExercises(long)
   * @return
   */
  private Widget getEmailWidget() {
    FlexTable g = new FlexTable();
    Anchor widget = new Anchor("E-MAIL");
    int row = 0;
    g.setWidget(row, 0, new HTML("Share via "));
    g.setWidget(row,1, widget);
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String token = History.getToken();
        if (token.trim().isEmpty()) token = getDefaultToken();
        Triple triple = getTriple(token);
        feedback.showEmail("Lesson " + triple, "", token);
      }
    });

    FlowPanel widgets = new FlowPanel();
    widgets.add(g);
    return widgets;
  }

  /**
   * @see #getExercises(long)
   * @param first
   */
  private void pushFirstSelection(String first) {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      ListBox listBox = typeToBox.get(first);
      String itemText = listBox.getItemText(listBox.getSelectedIndex());
      System.out.println("push first " + first + " select " + itemText);

      pushNewSectionHistoryToken();
    } else {
      System.out.println("fire history for " +initToken);
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

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   * @param type
   * @param section
   * @param item null is OK
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  private void loadExercises(final String type, final String section, final String item) {
    System.out.println("loadExercises " + type + " " + section + " item '" +item +"'");
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

  /**
   * @see #pushFirstSelection(String)
   * @see #setListBox(String, String)
   */
  private void pushNewSectionHistoryToken() {
    String historyToken = getHistoryToken(null);
    System.out.println("------------ push history " + historyToken + " -------------- ");
    History.newItem(historyToken);
  }

  @Override
  protected void gotClickOnItem(ExerciseShell e) {
  }

  protected String getHistoryToken(String id) {
    System.out.println("getHistoryToken for " + id + " examining " +typeToBox.size() + " boxes.");
    StringBuilder builder = new StringBuilder();
    for (String type : typeToBox.keySet()) {
      ListBox listBox = typeToBox.get(type);
      String section = listBox.getItemText(listBox.getSelectedIndex());
      if (section.equals(ANY)) {
        System.out.println("Skipping box " + type);
      } else {
        builder.append(type + "=" + section + ";");
      }
    }
    if (id != null) {
      builder.append(super.getHistoryToken(id));
    }
    return builder.toString();
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    String token = event.getValue();
    System.out.println("onValueChange " + token);
    if (token.length() == 0) {
      setListBox("", ANY);
      noSectionsGetExercises(userID);
    } else {
      token = token.replaceAll("%3D", "=").replaceAll("%3B", ";").replaceAll("%2", " ").replaceAll("\\+", " ");
      if (!token.contains("=")) token = getDefaultToken();
      System.out.println("onValueChange after " + token);
      try {
        Triple triple = getTriple(token);

        restoreListBoxState(triple);
        Map<String, String> typeToSection = triple.typeToSection;
        String type = typeToSection.keySet().iterator().next();
        String section = typeToSection.get(type);
        setOtherListBoxes(type, section);

        loadExercises(type, section, triple.item);
      } catch (Exception e) {
        System.out.println("onValueChange " + token + " badly formed. Got " + e);
        e.printStackTrace();
      }
    }
  }

  private void setOtherListBoxes(final String selectedType, String section) {
    service.getTypeToSectionsForTypeAndSection(selectedType, section, new AsyncCallback<Map<String, List<String>>>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Can't contact server.");
      }

      /**
       * This is a map from type to sections
       * @param result
       */
      @Override
      public void onSuccess(Map<String, List<String>> result) {
        for (Map.Entry<String, List<String>> pair : result.entrySet()) {
          String type = pair.getKey();
          if (type.equals(selectedType)) {
            System.out.println("setOtherListBoxes skipping " + type);

          } else {
            List<String> sections = pair.getValue();
            populateListBox(type, sections);
          }
        }
        pushNewSectionHistoryToken();
      }
    });
  }

  /**
   * @see #getEmailWidget()
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @return
   */
  private String getDefaultToken() {
    StringBuilder builder = new StringBuilder();
    for (String type : typeToBox.keySet()) {
      ListBox listBox = typeToBox.get(type);
      String section = listBox.getItemText(0);
      if (section.equals(ANY)) {
        // ?
      }
      else {
      // System.out.println("------------ push history " + type +"/"+ section + "/" +id+ " -------------- ");

        builder.append(type + "=" + section + ";");
      }
    }

    return builder.toString();
  }

  private Triple getTriple(String token) {
    Triple triple = new Triple();
    String[] parts = token.split(";");

    for (String part : parts) {
      if (part.contains("=")) {
        String[] segments = part.split("=");
        String type = segments[0].trim();
        String section = segments[1].trim();

        triple.add(type, section);
        System.out.println("part " + part + " : " + type + "->" +section + " : " + triple);
      }
      else {
        System.err.println("getTriple skipping part '" + part+ "'");
      }
    }

    if (token.contains("item")) {
      int item1 = token.indexOf("item=");
      String itemValue = token.substring(item1+"item=".length());
      System.out.println("got " + itemValue);
      triple.setItem(itemValue);
    }

    System.out.println("triple from token '" +token + "' = '" +triple + "'");

    return triple;
  }

  private static class Triple {
    private String item;
    public Map<String, String> typeToSection = new HashMap<String, String>();

    public void add(String type, String section) {
      typeToSection.put(type, section);
    }

    public void setItem(String item) {
      this.item = item;
    }

    public String toString() {
      StringBuilder builder = new StringBuilder();
      for (String section : typeToSection.values()) {
        builder.append(section).append(", ");
      }
      String s = builder.toString();
      return s.substring(0, s.length() - 2);
    }
  }

  /**
   * Given a triple state, make sure the list boxes are consistent with it.
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param triple
   */
  private void restoreListBoxState(Triple triple) {
    for (Map.Entry<String, String> pair : triple.typeToSection.entrySet()) {
      String type    = pair.getKey();
      String section = pair.getValue();
      ListBox listBox = typeToBox.get(type);
      if (listBox == null) {
        if (!type.equals("item")) {
          System.err.println("huh? bad type " + type);
        }
      } else {
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
