package mitll.langtest.client.exercise;

import com.google.gwt.core.client.GWT;
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
  protected boolean showListBoxes;

  private Map<String,ListBox> typeToBox = new HashMap<String, ListBox>();
  /**
   * So the concern is that if we allow people to send bookmarks with items, we can allow them to skip
   * forward in a list we're trying to present in a certain order.
   * I.e. when a new user logs in, we want them to do all of their items in the order we've chosen
   * for them (least answered/recorded first), and not let them skip forward in the list.
   */
  private boolean includeItemInBookmark = false;

  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList(com.google.gwt.user.client.ui.Panel, boolean)
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param showInOrder
   * @param showListBoxes
   */
  public SectionExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                             UserFeedback feedback,
                             boolean showTurkToken, boolean showInOrder, boolean showListBoxes) {
    super(currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, showListBoxes);
    this.showListBoxes = showListBoxes;
  }

  @Override
  protected void checkBeforeLoad(ExerciseShell e) {}

  @Override
  protected void addTableWithPager() {
    add(sectionPanel = new VerticalPanel());
   // System.out.println("show list boxes " + showListBoxes);
   /* sectionPanel.setVisible(showListBoxes);
    if (!showListBoxes) {
      sectionPanel.setHeight("1px");
    }*/
    super.addTableWithPager();
  }

  /**
   * After logging in, we get the type->section map and build the list boxes.
   * @see mitll.langtest.client.LangTest#gotUser
   * @param userID
   */
  @Override
  public void getExercises(final long userID) {
    System.out.println("SectionExerciseList.getExercises : Get exercises for user=" + userID);
    this.userID = userID;
    service.getTypeToSection(new AsyncCallback<Map<String, Collection<String>>>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      @Override
      public void onSuccess(Map<String, Collection<String>> result) {
        sectionPanel.clear();

        Panel flexTable = getWidgetsForTypes(result, userID);

        sectionPanel.add(flexTable);

        if (result.isEmpty()) {  // fallback to non-section option
          System.out.println("SectionExerciseList.getExercises : no results ??? for user=" + userID);

          noSectionsGetExercises(userID);
        } else {
          String first = result.keySet().iterator().next();
          //System.out.println("\tselecting first key of first type=" + first);

          typeToBox.get(first).setSelectedIndex(0); // not any, which is the first list item
          pushFirstListBoxSelection();
        }
      }
    });
  }

  /**
   * Add a list box for each type to the flex table, then an EMAIL link below them.
   * @param result
   * @param userID
   * @return panel with all the widgets
   */
  private Panel getWidgetsForTypes(Map<String, Collection<String>> result, long userID) {
    final FlexTable flexTable = new FlexTable();
    int row = 0;
    Set<String> types = result.keySet();
    System.out.println("getExercises (success) for user = " + userID + " got types " + types);
    typeToBox.clear();

    String token = unencodeToken(History.getToken());
    SelectionState selectionState = getSelectionState(token);

    for (final String type : types) {
      Collection<String> sections = result.get(type);
      System.out.println("\tgetExercises sections for " + type + " = " + sections);

      final ListBox listBox = makeListBox(type, sections);
      typeToBox.put(type, listBox);
      int col = 0;

      if (showListBoxes) {
        flexTable.setWidget(row, col++, new HTML(type));
        flexTable.setWidget(row++, col, listBox);
      } else {
        String typeValue = selectionState.typeToSection.get(type);
        if (typeValue != null) {
          flexTable.setWidget(row, col++, new HTML(type));
          flexTable.setWidget(row++, col, new HTML("<b>" + typeValue + "</b>"));
        }
      }
    }
    if (showListBoxes) {
      flexTable.setWidget(row, 0, getEmailWidget());
      flexTable.getFlexCellFormatter().setColSpan(row, 0, 2);
      row++;
      flexTable.setWidget(row, 0, getHideBoxesWidget());
      flexTable.getFlexCellFormatter().setColSpan(row, 0, 2);
    }
    else {
      flexTable.setWidget(row, 0, new HTML("&nbsp;"));
      flexTable.getFlexCellFormatter().setColSpan(row, 0, 2);
    }
    return flexTable;
  }

  private ListBox makeListBox(final String type, Collection<String> sections) {
    final ListBox listBox = new ListBox();

    populateListBox(listBox, sections);
    listBox.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        getListBoxOnClick(listBox, type);
      }
    });
    return listBox;
  }

  /**
   * @see #getExercises(long)
   * @param listBox
   * @param type
   */
  private void getListBoxOnClick(final ListBox listBox, final String type) {
    final String itemText = listBox.getItemText(listBox.getSelectedIndex());
    setListBox(type, itemText);
  }

  /**
   * @see #getListBoxOnClick(com.google.gwt.user.client.ui.ListBox, String)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param type
   * @param itemText
   */
  private void setListBox(final String type, final String itemText) {
    System.out.println("setListBox given " + type + "=" + itemText);

    if (itemText.equals(ANY)) {
      service.getTypeToSection(new TypeToSectionsAsyncCallback(type, itemText));
    } else {
      service.getTypeToSectionsForTypeAndSection(type, itemText, new TypeToSectionsAsyncCallback(type, itemText));
    }
  }

  private class TypeToSectionsAsyncCallback implements AsyncCallback<Map<String, Collection<String>>> {
    private final String type;
    private final String itemText;

    public TypeToSectionsAsyncCallback(String type, String itemText) {
      this.type = type;
      this.itemText = itemText;
    }

    @Override
    public void onFailure(Throwable caught) {
      Window.alert("Can't contact server.");
    }

    @Override
    public void onSuccess(Map<String, Collection<String>> result) {
      System.out.println("TypeToSectionsAsyncCallback onSuccess " + type + "=" + itemText + " yielded " +result);

      populateListBox(result);
      pushNewSectionHistoryToken();
    }
  }

  private void populateListBox(Map<String, Collection<String>> result) {
    for (Map.Entry<String, Collection<String>> pair : result.entrySet()) {
      String type = pair.getKey();
      Collection<String> sections = pair.getValue();
      populateListBox(type, sections);
    }
  }

  /**
   * @see #setListBox(String, String)
   * @see #setOtherListBoxes(String, String)
   * @param type
   * @param sections
   */
  private void populateListBox(String type, Collection<String> sections) {
    ListBox listBox = typeToBox.get(type);
    int selectedIndex = listBox.getSelectedIndex();
    String currentSelection = listBox.getItemText(selectedIndex);

    populateListBox(listBox, sections);
    retainCurrentSelectionState(listBox, currentSelection);
  }

  /**
   * Don't change the selection unless it's not available in this list box.
   * @see #populateListBox(String, java.util.Collection)
   * @param listBox
   * @param currentSelection
   */
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
   * Sort the sections added to the list box in an intelligent way (deal with keys like 56-57, etc.)
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

  /**
   * @see #populateListBox(com.google.gwt.user.client.ui.ListBox, java.util.Collection)
   * @param items
   */
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
        } else if (o1.contains(" ")) {
          firstHasSep = true;
          String[] first = o1.split("\\s");
          left1 = first[0];
          right1 = first[1];
        }

        if (secondHasSep) {
          String[] second = o2.split("-");
          left2 = second[0];
          right2 = second[1];
        } else if (o2.contains(" ")) {
          secondHasSep = true;
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
          return getIntCompare(o1, o2);
        }
      }
    });
  }

  private int getIntCompare(String first, String second) {
    if (first.length() > 0 && !Character.isDigit(first.charAt(0))) {
      return first.compareToIgnoreCase(second);
    } else {
      try {
        int r1 = Integer.parseInt(first);
        int r2 = Integer.parseInt(second);
        return r1 < r2 ? -1 : r1 > r2 ? +1 : 0;
      } catch (NumberFormatException e) {
        return first.compareToIgnoreCase(second);
      }
    }
  }

  /**
   * @see #getWidgetsForTypes(java.util.Map, long)
   * @return
   */
  private Widget getEmailWidget() {
    FlexTable g = new FlexTable();
    int row = 0;
    g.setWidget(row, 0, new HTML("Share via "));
    g.setWidget(row, 1, getEmailLink());

    FlowPanel widgets = new FlowPanel();
    widgets.add(g);
    return widgets;
  }

  private Anchor studentLink;
  private Widget getHideBoxesWidget() {
    studentLink = new Anchor("Link for Students", "#?showSectionWidgets=false");
    return studentLink;
  }

  private Widget getEmailLink() {
    String linkLabel = "E-MAIL";
    Anchor widget = new Anchor(linkLabel);
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        String token = History.getToken();
        if (token.trim().isEmpty()) token = getDefaultToken();

        SelectionState selectionState = getSelectionState(token);
        feedback.showEmail("Lesson " + selectionState, "", token);
      }
    });
    return widget;
  }


  /**
   * So if we have an existing history token, use it to set current selection.
   * If not, push the current state of the list boxes and act on it
   * @see #getExercises(long)
   */
  private void pushFirstListBoxSelection() {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      System.out.println("pushFirstListBoxSelection : empty token");

      pushNewSectionHistoryToken();
    } else {
      System.out.println("fire history for " +initToken);
      History.fireCurrentHistoryState();
    }
  }

  /**
   * @see #getExercises(long)
   * @see #setListBox(String, String)
   * @param result
   * @param type
   * @return list of sections for this type, sorted
   */
/*  private List<String> getSections(Map<String, Collection<String>> result, String type) {
    return new ArrayList<String>(result.get(type));
  }*/

  /**
   * @see #getExercises(long)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param userID
   */
  private void noSectionsGetExercises(long userID) {
    super.getExercises(userID);
  }

  /**
   * Ask the server for the items for the type->item map.  Remember the results and select the first one.
   */
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
          rememberExercises(result);
          if (!loadByID(item)) {
            System.out.println("loadExercises : loading first exercise since couldn't load item=" +item);
            loadFirstExercise();
          }
        } else {
          super.onSuccess(result);     // remember and load the first one
        }
      }
    }
  }

  /**
   * @see #pushFirstListBoxSelection
   * @see SectionExerciseList.TypeToSectionsAsyncCallback#onSuccess(java.util.Map)
   */
  private void pushNewSectionHistoryToken() {
    String historyToken = getHistoryToken(null);
    String currentToken = History.getToken();

    if (studentLink != null) {
      studentLink.setHref(GWT.getHostPageBaseURL() +"?showSectionWidgets=false#" + historyToken);
    }
    if (currentToken.equals(historyToken)) {
      if (historyToken.isEmpty() && (currentExercises == null || currentExercises.isEmpty())) {
        System.out.println("pushNewSectionHistoryToken : loading exercises given token '" + historyToken +"'");

        noSectionsGetExercises(userID);
      }
      else {
        System.out.println("pushNewSectionHistoryToken : skipping same token '" + historyToken +"'");
      }

    } else {
      System.out.println("------------ push history '" + historyToken + "' -------------- ");
      History.newItem(historyToken);
    }
  }

  /**
   * @see #loadExercise(mitll.langtest.shared.ExerciseShell)
   * @see #pushFirstSelection(String)
   * @param exerciseID
   */
  protected void pushNewItem(String exerciseID) {
    String historyToken = getHistoryToken(exerciseID);
    System.out.println("------------ (section) pushNewItem : push history '" + historyToken + "' -------------- ");

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

  /**
   * @see #getHistoryTokenForLink(String)
   * @see #pushNewItem(String)
   * @see #pushNewSectionHistoryToken()
   * @param id
   * @return
   */
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

  /**
   * Respond to push a history token.
   * @param event
   */
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
      if (token.length() == 0) {
        System.out.println("\tonValueChange empty token");

        setListBox("", ANY);
        noSectionsGetExercises(userID);
      } else {
        token = getCleanToken(token);
        System.out.println("\tonValueChange after " + token);
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

  private String getCleanToken(String token) {
    token = unencodeToken(token);
    if (!token.contains("=")) {
      token = getDefaultToken();
    }
    return token;
  }

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   * @param typeToSection
   * @param item null is OK
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  private void loadExercises(Map<String,String> typeToSection, final String item) {
    System.out.println("loadExercises " + typeToSection + " and item '" +item +"'");
    //service.getExercisesForSection(type, section, userID, new MySetExercisesCallback(item));
    service.getExercisesForSelectionState(typeToSection, userID, new MySetExercisesCallback(item));
  }

  /**
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param selectedType
   * @param section
   */
  private void setOtherListBoxes(final String selectedType, final String section) {
    System.out.println("setOtherListBoxes type " + selectedType + "=" + section);
    service.getTypeToSectionsForTypeAndSection(selectedType, section,
      new AsyncCallback<Map<String, Collection<String>>>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Can't contact server.");
        }

        /**
         * This is a map from type to sections
         *
         * @param result
         */
        @Override
        public void onSuccess(Map<String, Collection<String>> result) {
          System.out.println("\tsetOtherListBoxes result is " + result);

          if (result == null) {
            System.err.println("couldn't get result for " + selectedType + "=" + section);
            Window.alert("Sorry -- error on server.  Please report.");
          } else {
            populateListBox(result);
          }
        }
      });
  }

  /**
   * @see #getEmailWidget()
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @return default token from list boxes
   */
  private String getDefaultToken() {
    StringBuilder builder = new StringBuilder();
    for (String type : typeToBox.keySet()) {
      ListBox listBox = typeToBox.get(type);
      String section = listBox.getItemText(0);  // first is Any
      if (!section.equals(ANY)) {
        builder.append(type + "=" + section + ";");
      }
    }

    return builder.toString();
  }

  /**
   * @see #getEmailWidget()
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param token
   * @return object representing type=value pairs from history token
   */
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

  private static final int HEIGHT_OF_CELL_TABLE_WITH_15_ROWS = 390 - 90-65;

  protected int getTableHeaderHeight() {
    return 625 - HEIGHT_OF_CELL_TABLE_WITH_15_ROWS;
  }
}
