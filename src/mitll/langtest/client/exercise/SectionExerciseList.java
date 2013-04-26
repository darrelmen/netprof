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
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.ExerciseShell;

import java.util.*;

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
  protected boolean showListBoxes;

  protected Map<String,SectionWidget> typeToBox = new HashMap<String, SectionWidget>();

  /**
   * So the concern is that if we allow people to send bookmarks with items, we can allow them to skip
   * forward in a list we're trying to present in a certain order.
   * I.e. when a new user logs in, we want them to do all of their items in the order we've chosen
   * for them (least answered/recorded first), and not let them skip forward in the list.
   */
  private boolean includeItemInBookmark = false;
  //private boolean firstTime = true;

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
  protected void checkBeforeLoad(ExerciseShell e) {}

  @Override
  protected void addComponents() {
    add(sectionPanel = new VerticalPanel());
    addTableWithPager();
  }

  /**
   * @see mitll.langtest.client.LangTest#gotUser
   * @param userID
   */
  @Override
  public void getExercises(final long userID) {
    System.out.println("Get exercises for user=" + userID);
    this.userID = userID;
    service.getTypeToSectionToCount(new AsyncCallback<Map<String, Map<String, Integer>>>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
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
  protected void useInitialTypeToSectionMap(Map<String, Map<String, Integer>> result, long userID) {
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
  protected Panel getWidgetsForTypes(Map<String, Map<String, Integer>> result, long userID) {
    final FlexTable flexTable = new FlexTable();
    int row = 0;
    Set<String> types = result.keySet();
    System.out.println("getExercises (success) for user = " + userID + " got types " + types);
    typeToBox.clear();

    SelectionState selectionState = getSelectionState(History.getToken());

    for (final String type : types) {
      Map<String, Integer> sections = result.get(type);
      System.out.println("\tgetExercises sections for " + type + " = " + sections);

      final SectionWidget listBox = makeListBox(type);
      typeToBox.put(type, listBox);
      populateListBox(listBox, sections);
      int col = 0;

      if (showListBoxes) {
        flexTable.setWidget(row, col++, new HTML(type));
        flexTable.setWidget(row++, col, listBox.getWidget());
      } else {
        Collection<String> typeValue = selectionState.typeToSection.get(type);
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

  protected SectionWidget makeListBox(final String type) {
    final ListBoxSectionWidget listBox = new ListBoxSectionWidget();
    listBox.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        pushNewSectionHistoryToken();
      }
    });

    return listBox;
  }

  protected void populateListBoxAfterSelection(Map<String, Collection<String>> result) {
    for (Map.Entry<String, Collection<String>> pair : result.entrySet()) {
      String type = pair.getKey();
      populateListBox(type);
    }
  }

  /**
   * @seex #populateListBox(java.util.Map)
   * @deprecated
   * @param type
   * @paramx sections
   */
  private void populateListBox(String type) {
/*    System.out.println("populateListBox : " +type);

    SectionWidget listBox = typeToBox.get(type);
    String currentSelection = getCurrentSelection(type);
    System.out.println("current selection is " +currentSelection);*/
   // listBox.retainCurrentSelectionState(currentSelection);
  }

  private String getCurrentSelection(String type) {
    SectionWidget listBox = typeToBox.get(type);
    return listBox.getCurrentSelection();
  }

  private String getFirstItem(String type) {
    SectionWidget listBox = typeToBox.get(type);
    return listBox.getFirstItem();  // first is Any
  }

/*  private void selectFirst(String type) {
    SectionWidget listBox = typeToBox.get(type);
    if (listBox != null) {
      listBox.selectFirstAfterAny(); // not any, which is the first list item
    }
  }*/

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
   * @see #getExercises(long)
   * @see #populateListBox(String)
   * @param listBox
   * @param sectionToCount
   */
  protected void populateListBox(SectionWidget listBox,  Map<String, Integer>  sectionToCount) {
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
  protected Widget getEmailWidget() {
    FlexTable g = new FlexTable();
    int row = 0;
    g.setWidget(row, 0, new HTML("Share via "));
    g.setWidget(row, 1, getEmailLink());

    FlowPanel widgets = new FlowPanel();
    widgets.add(g);
    return widgets;
  }

  private Anchor studentLink;
  protected Widget getHideBoxesWidget() {
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
   * @see #getExercises(long)
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
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
    public void onSuccess(List<ExerciseShell> result) {
      System.out.println("MySetExercisesCallback : onSuccess " + result.size() + " items.");

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
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#addClickHandlerToButton(com.github.gwtbootstrap.client.ui.Button, String, mitll.langtest.client.bootstrap.ButtonGroupSectionWidget)
   */
  protected void pushNewSectionHistoryToken() {
    String historyToken = getHistoryToken(null);
    String currentToken = History.getToken();

    setModeLinks(historyToken);
    if (currentToken.equals(historyToken)) {
      if (currentExercises == null || currentExercises.isEmpty()) {
     //   if (firstTime) {
          noSectionsGetExercises(userID);
      //  }
      } else {
        System.out.println("pushNewSectionHistoryToken : skipping same token " + historyToken);
      }

    } else {
      System.out.println("pushNewSectionHistoryToken : currentToken " + currentToken);

      System.out.println("------------ push history '" + historyToken + "' -------------- ");
      History.newItem(historyToken);
    }
    //firstTime = false;
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
    System.out.println(new Date() + "------------ (section) pushNewItem : push history '" + historyToken + "' -------------- ");

    String token = History.getToken();
    System.out.println("pushNewItem : current token " + token + " vs new " + exerciseID);
    if (token != null && historyToken.equals(token)) {
      System.out.println("\tcurrent token '" + token + "' same as new " + historyToken);
      loadByIDFromToken(exerciseID);
    } else {
      System.out.println("\tcurrent token '" + token + "' different from new " + exerciseID);
      History.newItem(historyToken);
    }
  }

  /**
   * @see mitll.langtest.client.exercise.PagingExerciseList#getExerciseIdColumn()
   * @param columnText
   * @return
   */
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
    if (typeToBox.isEmpty()) return History.getToken();
    //System.out.println("getHistoryToken for " + id + " examining " +typeToBox.size() + " boxes.");
    StringBuilder builder = new StringBuilder();
    for (String type : typeToBox.keySet()) {
      String section = getCurrentSelection(type);
      //System.out.println("\tgetHistoryToken for " + type + " section = " +section);
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
      System.out.println("onValueChange '" + token + "'");
      //token = getCleanToken(token);
      try {
        SelectionState selectionState = getSelectionState(token);
        System.out.println("onValueChange '" + token + "' yields state " + selectionState.typeToSection);

        restoreListBoxState(selectionState);
        Map<String, Collection<String>> typeToSection = selectionState.typeToSection;

        System.out.println("onValueChange type->section " + typeToSection);

        setOtherListBoxes(typeToSection);

        loadExercises(selectionState.typeToSection, selectionState.item);
      } catch (Exception e) {
        System.out.println("onValueChange " + token + " badly formed. Got " + e);
        e.printStackTrace();
      }
    }
    System.out.println("onValueChange : ------ end : token is '" + rawToken + "' ------------ ");

  }

/*  protected String getCleanToken(String token) {
    return unencodeToken(token);
  }*/

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   * @param typeToSection
   * @param item null is OK
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  protected void loadExercises(Map<String, Collection<String>> typeToSection, final String item) {
    System.out.println("loadExercises " + typeToSection + " and item '" + item + "'");
    if (typeToSection.isEmpty() && item == null) {
      noSectionsGetExercises(userID);
    } else {
      service.getExercisesForSelectionState(typeToSection, userID, new MySetExercisesCallback(item));
    }
  }

  /**
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param typeToSection
   */
  protected void setOtherListBoxes(final Map<String, Collection<String>> typeToSection) {
    System.out.println("setOtherListBoxes type " + typeToSection + " skipping!!! ------------- ");
/*    service.getTypeToSectionsForTypeAndSection(typeToSection,
      new AsyncCallback<Map<String, Collection<String>>>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Can't contact server.");
        }

        *//**
         * This is a map from type to sections
         *
         * @param result
         *//*
        @Override
        public void onSuccess(Map<String, Collection<String>> result) {
          System.out.println("\tsetOtherListBoxes for type " + typeToSection + ", result is " + result);

          if (result == null) {
            System.err.println("couldn't get result for " + typeToSection);
            Window.alert("Sorry -- error on server.  Please report.");
          } else {
            populateListBoxAfterSelection(result);
          }
        }
      });*/
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

  /**
   * Given a selectionState state, make sure the list boxes are consistent with it.
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param selectionState
   */
  private void restoreListBoxState(SelectionState selectionState) {
    for (Map.Entry<String, Collection<String>> pair : selectionState.typeToSection.entrySet()) {
      String type = pair.getKey();
      Collection<String> section = pair.getValue();
      if (!typeToBox.containsKey(type)) {
        if (!type.equals("item")) {
          System.err.println("restoreListBoxState for " + selectionState + " : huh? bad type '" + type + "'");
        }
      } else {
        selectItem(type, section);
      }
    }
  }
}
