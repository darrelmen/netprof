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
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseListWrapper;
import mitll.langtest.shared.ExerciseShell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
  public static final String ANY = "Clear";
  protected Panel sectionPanel;
  protected long userID;
  protected final boolean showListBoxes;

  protected final Map<String,SectionWidget> typeToBox = new HashMap<String, SectionWidget>();

  /**
   * So the concern is that if we allow people to send bookmarks with items, we can allow them to skip
   * forward in a list we're trying to present in a certain order.
   * I.e. when a new user logs in, we want them to do all of their items in the order we've chosen
   * for them (least answered/recorded first), and not let them skip forward in the list.
   */
  private final boolean includeItemInBookmark = false;

  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param showInOrder
   * @param showListBoxes
   * @param controller
   */
  public SectionExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                             UserFeedback feedback,
                             boolean showTurkToken, boolean showInOrder, boolean showListBoxes, ExerciseController controller) {
    super(currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, controller);
    this.showListBoxes = showListBoxes;
  }

  @Override
  protected void addComponents() {
    add(sectionPanel = new VerticalPanel());
    addTableWithPager();
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
    service.getTypeToSectionToCount(new AsyncCallback<Map<String, Map<String, Integer>>>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("getTypeToSectionToCount Couldn't contact server.");
      }

      @Override
      public void onSuccess(Map<String, Map<String, Integer>> result) {
        useInitialTypeToSectionMap(result, userID);
      }
    });
  }

  /**
   * Take a map of type-> section->count
   * e.g. unit->[1,2,3,...]
   * @param result
   * @param userID
   */
  private void useInitialTypeToSectionMap(Map<String, Map<String, Integer>> result, long userID) {
    sectionPanel.clear();

    //System.out.println("useInitialTypeToSectionMap for " + userID);

    Panel flexTable = getWidgetsForTypes(result, userID);

    sectionPanel.add(flexTable);
  }

  /**
   * Add a list box for each type to the flex table, then an EMAIL link below them.
   * @see #useInitialTypeToSectionMap(java.util.Map, long)
   * @param result
   * @param userID
   * @return panel with all the widgets
   */
  private Panel getWidgetsForTypes(Map<String, Map<String, Integer>> result, long userID) {
    final FlexTable flexTable = new FlexTable();
    int row = 0;
    Set<String> types = result.keySet();
    System.out.println("getExercises (success) for user = " + userID + " got types " + types);
    typeToBox.clear();

    SelectionState selectionState = getSelectionState(History.getToken());

    for (final String type : types) {
      Map<String, Integer> sections = result.get(type);
      System.out.println("\tgetExercises sections for " + type + " = " + sections);

      final SectionWidget listBox = makeListBox();
      typeToBox.put(type, listBox);
      populateListBox(listBox, sections);
      int col = 0;

      if (showListBoxes) {
        flexTable.setWidget(row, col++, new HTML(type));
        flexTable.setWidget(row++, col, listBox.getWidget());
      } else {
        Collection<String> typeValue = selectionState.getTypeToSection().get(type);
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

  private SectionWidget makeListBox() {
    final ListBoxSectionWidget listBox = new ListBoxSectionWidget();
    listBox.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        pushNewSectionHistoryToken();
      }
    });

    return listBox;
  }

  /**
   * @see #getHistoryToken(String)
   * @param type
   * @return
   */
  private String getCurrentSelection(String type) {
    SectionWidget listBox = typeToBox.get(type);
    return listBox.getCurrentSelection();
  }

  private String getFirstItem(String type) {
    SectionWidget listBox = typeToBox.get(type);
    return listBox.getFirstItem();  // first is Any
  }

  /**
   * @see #restoreListBoxState(SelectionState)
   * @param type
   * @param sections
   */
  protected void selectItem(String type, Collection<String> sections) {
    SectionWidget listBox = typeToBox.get(type);
    listBox.selectItem(sections, false);
  }

  /**
   * Sort the sections added to the list box in an intelligent way (deal with keys like 56-57, etc.)
   * @see #getWidgetsForTypes(java.util.Map, long)
   * @param listBox
   * @param sectionToCount
   */
  private void populateListBox(SectionWidget listBox,  Map<String, Integer>  sectionToCount) {
    List<String> items = getSortedItems(sectionToCount.keySet());
    listBox.populateTypeWidget(items, sectionToCount);
  }

  protected List<String> getSortedItems(Collection<String> sections) {
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
    return items;
  }

  /**
   * @see #getSortedItems(java.util.Collection)
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
          if (first.length == 1) {
        	  System.err.println("huh? couldn't split " + o1);
        	  right1 = "";
          }
          else {
        	  right1 = first[1];
          }
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
  protected void pushFirstListBoxSelection() {
    String initToken = History.getToken();
    if (initToken.length() == 0) {
      System.out.println("pushFirstListBoxSelection : history token is blank");

      pushNewSectionHistoryToken();
    } else {
      System.out.println("pushFirstListBoxSelection fire history for token from URL: " +initToken);
      setModeLinks(initToken);
      History.fireCurrentHistoryState();
    }
  }

  /**
   * @see #loadExercises(java.util.Map, String)
   * @see #pushNewSectionHistoryToken()
   * @param userID
   */
  protected void noSectionsGetExercises(long userID) {
    super.getExercises(userID);
  }

  /**
   * Ask the server for the items for the type->item map.  Remember the results and select the first one.
   */
  private class MySetExercisesCallback extends SetExercisesCallback {
    private final String item;

    /**
     * @see SectionExerciseList#loadExercises(java.util.Map, String)
     * @param item
     */
    public MySetExercisesCallback(String item) {
      this.item = item;
    }

    @Override
    public void onSuccess(ExerciseListWrapper result) {
      System.out.println("MySetExercisesCallback : onSuccess " + result.exercises.size() + " items.");

      if (isStaleResponse(result)) {
        System.out.println("\t----> ignoring result " + result.reqID + " b/c before latest " + lastReqID);
      } else if (!result.exercises.isEmpty()) {
        if (item != null) {
          rememberExercises(result.exercises);
          if (!loadByID(item)) {
            System.out.println("SectionExerciseList.loadExercises : loading first exercise since couldn't load item=" + item);
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
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#addClickHandlerToButton
   */
  protected void pushNewSectionHistoryToken() {
    String historyToken = getHistoryToken(null);
    String currentToken = History.getToken();

    setModeLinks(historyToken);
    if (currentToken.equals(historyToken)) {
      if (currentExercises == null || currentExercises.isEmpty() || historyToken.isEmpty()) {
        noSectionsGetExercises(userID);
      } else {
        System.out.println("pushNewSectionHistoryToken : skipping same token '" + historyToken + "'");
      }
    } else {
      System.out.println("pushNewSectionHistoryToken : currentToken " + currentToken);

      setHistoryItem(historyToken);
    }
  }

  private void setHistoryItem(String historyToken) {
    System.out.println("------------ SectionExerciseList.setHistoryItem '" + historyToken + "' -------------- ");

    History.newItem(historyToken);
  }

  protected void setModeLinks(String historyToken) {
    if (studentLink != null) {
      studentLink.setHref(GWT.getHostPageBaseURL() + "?showSectionWidgets=false#" + historyToken);
    }
  }

  /**
   * @see #loadExercise(mitll.langtest.shared.ExerciseShell)
   * @see #pushFirstSelection(String)
   * @param exerciseID
   */
  protected void pushNewItem(String exerciseID) {
    String historyToken = getHistoryToken(exerciseID);
    String trimmedToken = historyToken.length() > 2? historyToken.substring(0,historyToken.length()-2) : historyToken;
    System.out.println(new Date() + "------------ SectionExerciseList.pushNewItem : push history '" + historyToken + "' -------------- ");

    String token = History.getToken();
    getSelectionState(token);
    //System.out.println("pushNewItem : current token '" + token + "' vs new id '" + exerciseID +"'");
    if (token != null && (historyToken.equals(token) || trimmedToken.equals(token))) {
      System.out.println("\tpushNewItem : current token '" + token + "' same as new " + historyToken);
      loadByIDFromToken(exerciseID);
    } else {
      System.out.println("\tpushNewItem : current token '" + token + "' different menu state '" +historyToken+ "' from new " + exerciseID);
      setHistoryItem(historyToken);
    }
  }

  /**
   * @see mitll.langtest.client.exercise.PagingExerciseList#getExerciseIdColumn
   * @param e
   */
  @Override
  protected void gotClickOnItem(ExerciseShell e) {
    System.out.println("----------- got click on " + e.getID() + " -------------- ");
    if (!includeItemInBookmark) {
      loadByID(e.getID());
    }
  }

  /**
   * @see #pushNewItem(String)
   * @see #pushNewSectionHistoryToken()
   * @param id
   * @return
   */
  protected String getHistoryToken(String id) {
    if (typeToBox.isEmpty()) return History.getToken();
    //System.out.println("getHistoryToken for " + id + " examining " +typeToBox.size() + " boxes.");
    StringBuilder builder = new StringBuilder();
    for (String type : typeToBox.keySet()) {
      String section = getCurrentSelection(type);
      System.out.println("\tSectionExerciseList.getHistoryToken for " + type + " section = " +section);
      if (section.equals(ANY)) {
        //System.out.println("getHistoryToken : Skipping box " + type + " (ANY) ");
      } else {
        builder.append(type + "=" + section + ";");
      }
    }
    if (id != null && includeItemInBookmark) {
      String historyToken = super.getHistoryToken(id);
      //System.out.println("getHistoryToken for " + id + " would add " +historyToken);

      builder.append(historyToken);
    }
    if (false && builder.toString().length() > 0) {
      System.out.println("getHistoryToken for " + id + " is " +builder);
    }
    //System.out.println("\tgetHistoryToken for " + id + " is " +builder.toString());

    return builder.toString();
  }

    /**
     * Respond to push a history token.
     * @param event
     */
  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    String rawToken = getTokenFromEvent(event);
    System.out.println(new Date() +" onValueChange : ------ start: token is '" + rawToken +"' ------------ ");
    String item = getSelectionState(rawToken).getItem();

    if (item != null && item.length() > 0 && hasExercise(item)) {
      if (includeItemInBookmark) {
    //    System.out.println("onValueChange : loading item " + item);
        loadByIDFromToken(item);
      }
      else {
        System.out.println("onValueChange : skipping item " + item);
      }
    } else {
      String token = event.getValue();
    //  System.out.println("onValueChange '" + token + "'");
      try {
        SelectionState selectionState = getSelectionState(token);
        restoreListBoxState(selectionState);
        Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();

       // System.out.println("onValueChange '" + token + "' type->section " + typeToSection);

        setOtherListBoxes(typeToSection);

        loadExercises(selectionState.getTypeToSection(), selectionState.getItem());
      } catch (Exception e) {
        System.out.println("onValueChange " + token + " badly formed. Got " + e);
        e.printStackTrace();
      }
    }
    //System.out.println("onValueChange : ------ end : token is '" + rawToken + "' ------------ ");
  }

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   * @param typeToSection
   * @param item null is OK
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  protected void loadExercises(Map<String, Collection<String>> typeToSection, final String item) {
    System.out.println("SectionExerciseList.loadExercises : " + typeToSection + " and item '" + item + "'");
    if (typeToSection.isEmpty() && item == null) {
      noSectionsGetExercises(userID);
    } else {
      lastReqID++;
      service.getExercisesForSelectionState(lastReqID, typeToSection, userID, new MySetExercisesCallback(item));
    }
  }

  /**
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param typeToSection
   */
  private void setOtherListBoxes(final Map<String, Collection<String>> typeToSection) {
    System.out.println("setOtherListBoxes type " + typeToSection + " skipping!!! ------------- ");
  }

  /**
   * @see #getEmailWidget()
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @return default token from list boxes
   */
  private String getDefaultToken() {
    StringBuilder builder = new StringBuilder();
    for (String type : typeToBox.keySet()) {
      String section = getFirstItem(type);  // first is Any
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
  protected SelectionState getSelectionState(String token) {
    return new SelectionState(token);
  }

  private String lastSelectionState = "NO_SELECTION_STATE";

  /**
   * @see mitll.langtest.client.LangTest#setSelectionState
   * @param selectionState
   */
  @Override
  public void setSelectionState(Map<String, Collection<String>> selectionState) {
    String newSelectionState = selectionState.toString().replace("[", "").replace("]", "").replace("{", "").replace("}", "").replace(" ", "");
    if (!lastSelectionState.equals(newSelectionState)) {
      lastSelectionState = newSelectionState;
      SelectionState selectionState2 = getSelectionState(newSelectionState);
      System.out.println("SectionExerciseList.setSelectionState : setting selection state to : '" + newSelectionState + "' or '" + selectionState2 + "'");

      restoreListBoxState(selectionState2);
      String historyToken = getHistoryToken("1");
      if (historyToken.endsWith(",;")) historyToken = historyToken.substring(0, historyToken.length() - 2);
      if (!historyToken.equals(newSelectionState)) {
        System.err.println("\n\n\n\n----> after setting menu state, history token is '" + historyToken + "' vs expected '" + newSelectionState + "'");
      }
      if (!History.getToken().equals(newSelectionState)) {
        setHistoryItem(newSelectionState);
      }
    }
    else {
     // System.out.println("SectionExerciseList.setSelectionState : selection state still : '" + lastSelectionState + "'");

    }
  }

  /**
   * Given a selectionState state, make sure the list boxes are consistent with it.
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param selectionState
   */
  protected void restoreListBoxState(SelectionState selectionState) {
    Map<String, Collection<String>> selectionState2 = new HashMap<String, Collection<String>>();

    // make sure we all types have selections, even if it's the default Clear (ANY) selection
    for (String type : typeToBox.keySet()) {
      selectionState2.put(type,Collections.singletonList(ANY));
    }

    for (Map.Entry<String, Collection<String>> pair : selectionState.getTypeToSection().entrySet()) {
      String type = pair.getKey();
      Collection<String> section = pair.getValue();
      selectionState2.put(type,section);
    }

    boolean hasNonClearSelection = false;
    List<String> typesWithSelections = new ArrayList<String>();
    Collection<String> typeOrder = getTypeOrder(selectionState2);
    for (String type : typeOrder) {
      Collection<String> selections = selectionState2.get(type);
      if (selections.iterator().next().equals(ANY)) {
        if (hasNonClearSelection) {
          System.out.println("restoreListBoxState : skipping type since below a selection = " + type);
        }
        else {
          System.out.println("restoreListBoxState : clearing " + type);

          selectItem(type, selections);
        }
      }
      else {
        if (!hasNonClearSelection) {
          enableAllButtonsFor(type);  // first selection row should always be fully enabled -- there's nothing above it to constrain the selections
        }
        hasNonClearSelection = true;

        if (!typeToBox.containsKey(type)) {
          if (!type.equals("item")) {
            System.err.println("restoreListBoxState for " + selectionState + " : huh? bad type '" + type +
              "', expecting something in " + typeToBox.keySet());
          }
        } else {
          typesWithSelections.add(type);
          //selectItem(type, selections);
        }
      }
    }

    System.out.println("restoreListBoxState :typesWithSelections " + typesWithSelections);

    // clear enabled state for all items below first selection...
    if (!typesWithSelections.isEmpty()) {
      List<String> afterFirst = new ArrayList<String>();
      String first = typesWithSelections.get(0);
      boolean start = false;
      for (String type : typeOrder) {
         if (start) afterFirst.add(type);
         if (type.equals(first)) start = true;
      }

     // List<String> afterFirst = typesWithSelections.subList(1, typesWithSelections.size());
      System.out.println("restoreListBoxState : afterFirst " + afterFirst);

      for (String type : afterFirst) {
        System.out.println("restoreListBoxState : clearing enabled on " + type);

        clearEnabled(type);
      }
    }

    for (String type : typesWithSelections) {
      System.out.println("restoreListBoxState : selecting items for " + type);

      Collection<String> selections = selectionState2.get(type);
      selectItem(type, selections);
    }
/*    List<String> toRemove = new ArrayList<String>();
    for (Map.Entry<String, Collection<String>> typeSelectionPair : selectionState2.entrySet()) {
      Collection<String> selections = typeSelectionPair.getValue();
      if (selections.size() == 1 && selections.iterator().next().equals(ANY)) {
        toRemove.add(typeSelectionPair.getKey());
        selectItem(typeSelectionPair.getKey(), selections);
      }
    }

    System.out.println("restoreListBoxState : selection state " + selectionState2);
    Collection<String> types = new ArrayList<String>(getTypeOrder(selectionState2));
    types.removeAll(toRemove);
    // whatever is left here are actual selections
    for (String type : types) {
      if (selectionState2.containsKey(type)) {
        Collection<String> section = selectionState2.get(type);
        if (!typeToBox.containsKey(type)) {
          if (!type.equals("item")) {
            System.err.println("restoreListBoxState for " + selectionState + " : huh? bad type '" + type +
              "', expecting something in " + typeToBox.keySet());
          }
        } else {
          selectItem(type, section);
        }
      }
    }*/
  }

  protected void clearEnabled(String type) {}
  protected void enableAllButtonsFor(String type) {}
  protected Collection<String> getTypeOrder(Map<String, Collection<String>> selectionState2) {
    return selectionState2.keySet();
  }
}
