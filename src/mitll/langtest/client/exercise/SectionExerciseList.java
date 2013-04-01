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
import java.util.Arrays;
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
  /**
   * So the concern is that if we allow people to send bookmarks with items, we can allow them to skip
   * forward in a list we're trying to present in a certain order.
   * I.e. when a new user logs in, we want them to do all of their items in the order we've chosen
   * for them (least answered/recorded first), and not let them skip forward in the list.
   */
  private boolean includeItemInBookmark = false;
  public SectionExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                             UserFeedback feedback,
                             boolean showTurkToken, boolean showInOrder) {
    super(currentExerciseVPanel, service, feedback, showTurkToken, showInOrder);
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
        sectionPanel.clear();

        final FlexTable g = new FlexTable();
        String first = null;
        int row = 0;
        Set<String> types = result.keySet();
        System.out.println("getExercises for user = " + userID + " got types " + types);
        typeToBox.clear();

        for (final String type : types) {
          if (first == null) first = type;

          final ListBox listBox = new ListBox();
          typeToBox.put(type,listBox);
          List<String> sections = getSections(result, type);
          System.out.println("\tgetExercises sections for " + type + " = " + sections);

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
          pushFirstListBoxSelection(first);
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
    int selectedIndex = listBox.getSelectedIndex();
    String currentSelection = listBox.getItemText(selectedIndex);

    //System.out.println("for list box " +type + " the items will be " + sections);
    //System.out.println("for list box " +type + " previous selection was " + currentSelection);

    populateListBox(listBox, sections);
    retainCurrentSelectionState(listBox, currentSelection);
  }

  private void retainCurrentSelectionState(ListBox listBox, String currentSelection) {
    int itemCount = listBox.getItemCount();

    // retain current selection state
    if (itemCount > 0) {
      boolean foundMatch = false;
      for (int i = 0; i < itemCount; i++) {
        if (listBox.getItemText(i).equals(currentSelection)) {
          listBox.setSelectedIndex(i);
          foundMatch = true;
          break;
        }
      }
      if (!foundMatch) {
        listBox.setSelectedIndex(0);
      }
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
    boolean isInt = true;
    for (String item : items) {
      try {
        Integer.parseInt(item);
      } catch (NumberFormatException e) {
        isInt = false;
      }
    }
    if (isInt) {
      Collections.sort(items, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          int first = Integer.parseInt(o1);
          int second = Integer.parseInt(o2);
          return first < second ? -1 : first > second ? +1 : 0;
        }
      });
    }
    else {
      sortWithCompoundKeys(items);
    }
    for (String section : items) {
      listBox.addItem(section);
    }
  }

  //Pattern pattern = Pattern.compile("(.+)(-|\\s)*(.+)");
  private void sortWithCompoundKeys(List<String> items) {
    Collections.sort(items, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        boolean firstHasSep = o1.contains("-");
        boolean secondHasSep = o2.contains("-");
        String left1 = o1;
        String left2 = o2;
        String right1 = "";
        String right2 = "";

        if (firstHasSep) {
          String[] first = o1.split("-");
          left1 = first[0];
          right1 = first[1];
        }
        else if (o1.contains(" ")) {
          firstHasSep = true;
          String[] first = o1.split("\\s");
          left1 = first[0];
          right1 = first[1];
        }

        if (secondHasSep) {
          String[] second = o2.split("-");
          left2 = second[0];
          right2 = second[1];
        }
        else if (o2.contains(" ")) {
          secondHasSep =true;
          String[] second = o2.split("\\s");
          left2 = second[0];
          right2 = second[1];
        }


        if (firstHasSep || secondHasSep) {
          int leftCompare = getIntCompare(left1, left2);
          if (leftCompare != 0) {
            return leftCompare;
          } else {
            return getIntCompare(right1, right2);
          }
        } else {
          return getIntCompare(o1,o2);
        }
      }
    });
  }

  private int getIntCompare(String right1, String right2) {
    try {
      int r1 = Integer.parseInt(right1);
      int r2 = Integer.parseInt(right2);
      return r1 < r2 ? -1 : r1 > r2 ? +1 : 0;
    } catch (NumberFormatException e) {
      return right1.compareToIgnoreCase(right2);
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
        SelectionState selectionState = getSelectionState(token);
        feedback.showEmail("Lesson " + selectionState, "", token);
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
  private void pushFirstListBoxSelection(String first) {
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

  /**
   * Sort as integers if they are all ints.
   * @param sections
   * @return
   */
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
   * @param typeToSection
   * @param item null is OK
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  //private void loadExercises(final String type, final String section, final String item) {
  private void loadExercises(Map<String,String> typeToSection, final String item) {
    System.out.println("loadExercises " + typeToSection + " and item '" +item +"'");
    //service.getExercisesForSection(type, section, userID, new MySetExercisesCallback(item));
    service.getExercisesForSelectionState(typeToSection, userID, new MySetExercisesCallback(item));
  }

  private class MySetExercisesCallback extends SetExercisesCallback {
    private final String item;

    public MySetExercisesCallback(String item) {
      this.item = item;
    }

    @Override
    public void onSuccess(List<ExerciseShell> result) {
      System.out.println("loadExercises : onSuccess " + result.size() + " items.");

      if (!result.isEmpty()) {
        if (item != null) {
          result = result.subList(0,3);
          rememberExercises(result);
          if (!loadByID(item)) {
            System.out.println("loadExercises : loading first exercise since couldn't load item=" +item);
            loadFirstExercise();
          }
        } else {
          super.onSuccess(result);
        }
      }
    }
  }

  /**
   * @see #pushFirstListBoxSelection(String)
   * @see #setListBox(String, String)
   */
  private void pushNewSectionHistoryToken() {
    String historyToken = getHistoryToken(null);
    String currentToken = History.getToken();
    if (currentToken.equals(historyToken)) {
      System.out.println("pushNewSectionHistoryToken : skipping same token " + historyToken);
    } else {
      System.out.println("------------ push history " + historyToken + " -------------- ");
      History.newItem(historyToken);
    }
  }

  protected void pushNewItem(String exerciseID) {
    String historyToken = getHistoryToken(exerciseID);
    System.out.println("------------ (section) pushNewItem : push history " + historyToken + " -------------- ");

    String token = History.getToken();
    System.out.println("pushNewItem : current token " + token + " vs new " + exerciseID);
    if (token != null && historyToken.equals(token)) {
      System.out.println("current token " + token + " same as new " + exerciseID);
      loadByIDFromToken(exerciseID);
    } else {
      History.newItem(historyToken);
    }
  }

  protected String getHistoryTokenForLink(String columnText) {
    return "#"+getHistoryToken(columnText);

   // return historyToken +";item="+columnText;
  }

  @Override
  protected void gotClickOnItem(ExerciseShell e) {
    System.out.println("----------- got click on " + e.getID() + " -------------- ");
    if (!includeItemInBookmark) {
      loadByID(e.getID());
    }
  }

  protected String getHistoryToken(String id) {
    //System.out.println("getHistoryToken for " + id + " examining " +typeToBox.size() + " boxes.");
    StringBuilder builder = new StringBuilder();
    for (String type : typeToBox.keySet()) {
      ListBox listBox = typeToBox.get(type);
      String section = listBox.getItemText(listBox.getSelectedIndex());
      if (section.equals(ANY)) {
       // System.out.println("Skipping box " + type + " (ANY) ");
      } else {
        builder.append(type + "=" + section + ";");
      }
    }
    if (id != null && includeItemInBookmark) {
      String historyToken = super.getHistoryToken(id);
      //System.out.println("getHistoryToken for " + id + " would add " +historyToken);


      builder.append(historyToken);
    }
    return builder.toString();
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    String rawToken = getTokenFromEvent(event);
    System.out.println("onValueChange : token is " + rawToken);
    String item = getSelectionState(rawToken).item;

    if (item != null && item.length() > 0 && hasExercise(item)) {
      if (includeItemInBookmark) {
        System.out.println("onValueChange : loading item " + item);
        loadByIDFromToken(item);
      }
      else {
        System.out.println("onValueChange : skipping item " + item);
      }
    } else {
      String token = event.getValue();
      System.out.println("onValueChange " + token);
      if (token.length() == 0) {
        setListBox("", ANY);
        noSectionsGetExercises(userID);
      } else {
        token = unencodeToken(token);
        if (!token.contains("=")) token = getDefaultToken();
        System.out.println("onValueChange after " + token);
        try {
          SelectionState selectionState = getSelectionState(token);

          restoreListBoxState(selectionState);
          Map<String, String> typeToSection = selectionState.typeToSection;
          String type = typeToSection.keySet().iterator().next();
          String section = typeToSection.get(type);
          System.out.println("onValueChange first type " + type + "=" + section);

          setOtherListBoxes(type, section);

          loadExercises(selectionState.typeToSection, selectionState.item);
        } catch (Exception e) {
          System.out.println("onValueChange " + token + " badly formed. Got " + e);
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param selectedType
   * @param section
   */
  private void setOtherListBoxes(final String selectedType, final String section) {
    System.out.println("setOtherListBoxes " + selectedType + " " + section);
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
        System.out.println("\tsetOtherListBoxes result is " + result);

        if (result == null) {
          System.err.println("couldn't get result for " + selectedType + "="+section);
          Window.alert("Sorry -- error on server.  Please report.");
        }
        else {
          for (Map.Entry<String, List<String>> pair : result.entrySet()) {
            String type = pair.getKey();
            if (type.equals(selectedType)) {        // this should never happen
              System.err.println("\tsetOtherListBoxes skipping " + type);
            } else {
              List<String> sections = pair.getValue();
              populateListBox(type, sections);
            }
          }
        }
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

  private SelectionState getSelectionState(String token) {
    SelectionState selectionState = new SelectionState();
    String[] parts = token.split(";");

    for (String part : parts) {
      if (part.contains("=")) {
        String[] segments = part.split("=");
        String type = segments[0].trim();
        String section = segments[1].trim();

        selectionState.add(type, section);
        System.out.println("getSelectionState : part " + part + " : " + type + "->" +section + " : " + selectionState);
      }
      else {
        System.err.println("getSelectionState skipping part '" + part+ "'");
      }
    }

    if (token.contains("item")) {
      int item1 = token.indexOf("item=");
      String itemValue = token.substring(item1+"item=".length());
      System.out.println("getSelectionState : got item = '" + itemValue +"'");
      selectionState.setItem(itemValue);
    }

    System.out.println("getSelectionState : selectionState from token '" +token + "' = '" + selectionState + "'");

    return selectionState;
  }

  private static class SelectionState {
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
      return s.substring(0, Math.max(0,s.length() - 2));
    }
  }

  /**
   * Given a selectionState state, make sure the list boxes are consistent with it.
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param selectionState
   */
  private void restoreListBoxState(SelectionState selectionState) {
    for (Map.Entry<String, String> pair : selectionState.typeToSection.entrySet()) {
      String type    = pair.getKey();
      String section = pair.getValue();
      ListBox listBox = typeToBox.get(type);
      if (listBox == null) {
        if (!type.equals("item")) {
          System.err.println("restoreListBoxState for " + selectionState + " : huh? bad type " + type);
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
/*
  public static void main(String [] args) {
    List<String> test = Arrays.asList("1","2","10","3","4","11","12","39 RC","39 LC","31 RC");
    new SectionExerciseList(null,null,null,false,false).sortWithCompoundKeys(test);
    System.out.println("Test " + test);
  }*/
}
