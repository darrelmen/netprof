package mitll.langtest.client.exercise;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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

import java.util.Collection;
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
public class SectionExerciseList extends HistoryExerciseList {
  protected Panel sectionPanel;
  protected final boolean showListBoxes;
  private String lastSelectionState = "NO_SELECTION_STATE";

  /**
   * @see mitll.langtest.client.LangTest#makeExerciseList
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param showTurkToken
   * @param showInOrder
   * @param showListBoxes
   * @param controller
   * @param isCRTDataMode
   */
  public SectionExerciseList(Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                             UserFeedback feedback,
                             boolean showTurkToken, boolean showInOrder, boolean showListBoxes,
                             ExerciseController controller, boolean isCRTDataMode) {
    super(currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, controller, isCRTDataMode);
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
   * @param getNext
   */
  @Override
  public void getExercises(final long userID, boolean getNext) {
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
   * Sort the sections added to the list box in an intelligent way (deal with keys like 56-57, etc.)
   * @see #getWidgetsForTypes(java.util.Map, long)
   * @param listBox
   * @param sectionToCount
   */
  private void populateListBox(SectionWidget listBox,  Map<String, Integer>  sectionToCount) {
    List<String> items = new ItemSorter().getSortedItems(sectionToCount.keySet());
    listBox.populateTypeWidget(items, sectionToCount);
  }

/*  protected List<String> getSortedItems(Collection<String> sections) {
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

  *//**
   * @see #getSortedItems(java.util.Collection)
   * @param items
   *//*
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
  }*/

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

  @Override
  protected void setModeLinks(String historyToken) {
    if (studentLink != null) {
      studentLink.setHref(GWT.getHostPageBaseURL() + "?showSectionWidgets=false#" + historyToken);
    }
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
   * @see mitll.langtest.client.LangTest#setSelectionState
   * @param selectionState
   */
/*  @Override
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
  }*/

}
