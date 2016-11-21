/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.ItemSorter;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.SectionNode;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.custom.UserList;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/5/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class SingleSelectExerciseList extends HistoryExerciseList<AmasExerciseImpl, AmasExerciseImpl, ButtonBarSectionWidget> {
  private final Logger logger = Logger.getLogger("SingleSelectExerciseList");
  private static final int NUM_CHOICES = 3;

  private static final String PLEASE_SELECT  = "Please select a quiz, test type, ILR level, and response type";
  private static final String PLEASE_SELECT2 = "Please select a test type, ILR level, and response type";
  private static final int CLASSROOM_VERTICAL_EXTRA = 270;
  private static final String SHOWING_ALL_ENTRIES = "Showing all entries";

  private final List<ButtonType> buttonTypes = new ArrayList<>();
  private final Heading statusHeader = new Heading(4);
  private Collection<String> typeOrder;
  private final Panel sectionPanel;

  /**
   * @param secondRow             add the section panel to this row
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param controller
   * @param instance
   * @param incorrectFirst
   * @param showFirstNotCompleted
   * @see ResponseExerciseList#ResponseExerciseList(Panel, Panel, LangTestDatabaseAsync, UserFeedback, ExerciseController, String)
   */
  SingleSelectExerciseList(Panel secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                           UserFeedback feedback,
                           ExerciseController controller,
                           String instance, boolean incorrectFirst, boolean showFirstNotCompleted) {
    super(currentExerciseVPanel, service, feedback, controller, true, instance, incorrectFirst, showFirstNotCompleted);

    sectionPanel = new FluidContainer();
    sectionPanel.getElement().setId("sectionPanel_" + instance);

    sectionPanel.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    sectionPanel.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
    secondRow.add(new Column(12, sectionPanel));

    buttonTypes.add(ButtonType.PRIMARY);
    buttonTypes.add(ButtonType.SUCCESS);
    buttonTypes.add(ButtonType.INFO);
    buttonTypes.add(ButtonType.WARNING);
    setUnaccountedForVertical(CLASSROOM_VERTICAL_EXTRA);
  }

  /**
   * @see mitll.langtest.client.list.PagingExerciseList#PagingExerciseList
   */
  @Override
  protected void addComponents() {
    addTableWithPager(makePagingContainer());
  }

  /**
   * @param userID
   * @see mitll.langtest.client.list.HistoryExerciseList#noSectionsGetExercises
   */
  public boolean getExercises(final long userID) {
    addWidgets();
    return false;
  }

  /**
   * @see #getExercises(long)
   * @see mitll.langtest.client.bootstrap.FlexSectionExerciseList#getExercises(long)
   * @see mitll.langtest.client.custom.content.FlexListLayout#doInternalLayout(UserList, String)
   */
  @Override
  public void addWidgets() {
    sectionPanel.clear();
    sectionPanel.add(getWidgetsForTypes());
  }

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @return
   * @see #addWidgets()
   */
  private Panel getWidgetsForTypes() {
    final FluidContainer container = new FluidContainer();
    container.getElement().setId("typeOrderContainer");
    container.getElement().getStyle().setPaddingLeft(2, Style.Unit.PX);
    container.getElement().getStyle().setPaddingRight(2, Style.Unit.PX);

    getTypeOrder(container);
    return container;
  }

  private void getTypeOrder(final FluidContainer container) {
    typeOrder = controller.getStartupInfo().getTypeOrder();
    // logger.info("type order is " +typeOrder);
    addButtonRow(controller.getStartupInfo().getSectionNodes(), container, typeOrder);
  }

/*  @Override
  protected Collection<String> getTypeOrder(Map<String, Collection<String>> selectionState2) {
    return typeOrder;
  }*/

  Panel firstTypeRow;

  /**
   * @param rootNodes
   * @param container
   * @param types
   * @see #getTypeOrder(com.github.gwtbootstrap.client.ui.FluidContainer)
   */
  private void addButtonRow(Collection<SectionNode> rootNodes, final FluidContainer container, Collection<String> types) {
/*    logger.info("addButtonRow for user = " + controller.getUser() + " got types " +
        types + " num root nodes " + rootNodes.size());*/
    if (types.isEmpty()) {
      logger.warning("addButtonRow : huh? types is empty?");
      return;
    }
    showDefaultStatus();
    HorizontalPanel widgets = new HorizontalPanel();
    firstTypeRow = widgets;
    firstTypeRow.getElement().setId("firstTypeRow");
    container.add(firstTypeRow);
    firstTypeRow.addStyleName("alignTop");

    int index = 0;
    for (String type : types) {
      Collection<String> sectionsInType = new ItemSorter().getSortedItems(getLabels(rootNodes));

      ButtonBarSectionWidget value = new ButtonBarSectionWidget(type);
      sectionWidgetContainer.setWidget(type, value);
      value.getButtonBar(firstTypeRow, type, sectionsInType, type, buttonTypes.get(index++), this);

      List<SectionNode> newNodes = new ArrayList<>();

      for (SectionNode node : rootNodes) {
        newNodes.addAll(node.getChildren());
      }
      rootNodes = newNodes;
    }
    makeDefaultSelections();

    DivWidget bottomRow = getBottomRow();
    addBottomText(bottomRow);
    container.add(bottomRow);
  }

  /**
   * @see ButtonBarSectionWidget#getChoice(ButtonGroup, String)
   */
  public void gotSelection() {
    int count = getNumSelections();
    if (count == getNumChoices()) {
    //  logger.info("gotSelection count = " + count);
      pushNewSectionHistoryToken();
    } else {
      //logger.info("gotSelection count " + count + " < " + getNumChoices());
    }
  }

  /**
   * @return
   * @see #addButtonRow
   */
  private DivWidget getBottomRow() {
    DivWidget bottomRow = new DivWidget();
    bottomRow.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);
    DivWidget left = new DivWidget();
    left.addStyleName("floatLeftList");
    bottomRow.add(left);
    return bottomRow;
  }

  private List<String> getLabels(Collection<SectionNode> nodes) {
    List<String> items = new ArrayList<>();
    for (SectionNode n : nodes) items.add(n.getName());
    return items;
  }

  /**
   * @param selectionState
   * @see mitll.langtest.client.list.HistoryExerciseList#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  @Override
  protected void restoreListBoxState(SelectionState selectionState) {
    // logger.info("restoreListBoxState " + selectionState);
    super.restoreListBoxState(selectionState);
    showSelectionState(selectionState);
  }

  /**
   * Add a line that spells out in text which lessons have been chosen, derived from the selection state.
   * Show in the same type order as the button rows.
   *
   * @param selectionState to get the current selection state from
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  private void showSelectionState(SelectionState selectionState) {
    // keep the download link info in sync with the selection
    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();

    logger.info("showSelectionState : typeOrder " + typeOrder + " selection state " + typeToSection);

    if (typeToSection.isEmpty()) {
      showDefaultStatus();
    } else {
      StringBuilder status = new StringBuilder();
      // logger.info("\tshowSelectionState : typeOrder " + typeOrder + " selection state " + typeToSection);
      for (String type : typeOrder) {
        Collection<String> selectedItems = typeToSection.get(type);
        if (selectedItems != null) {
          List<String> sorted = new ArrayList<>();
          for (String selectedItem : selectedItems) {
            sorted.add(selectedItem);
          }
          Collections.sort(sorted);
          StringBuilder status2 = new StringBuilder();
          for (String item : sorted) status2.append(item).append(", ");
          String s = status2.toString();
          if (!s.isEmpty()) s = s.substring(0, s.length() - 2);
          String statusForType = type + " " + s;
          status.append(statusForType).append(" and ");
        }
      }
      String text = status.toString();
      if (text.length() > 0) text = text.substring(0, text.length() - " and ".length());
      statusHeader.setText(text);
    }
  }

  private void showDefaultStatus() {
    statusHeader.setText(SHOWING_ALL_ENTRIES);
  }

  /**
   * @param container
   * @see #addButtonRow
   */
  void addBottomText(Panel container) {
    Panel status = getStatusRow();
    container.add(status);
    status.addStyleName("leftFiftyPercentMargin");
  }

  /**
   * @return
   * @see #addBottomText
   */
  private Panel getStatusRow() {
    Panel status = new DivWidget();
    status.getElement().setId("statusRow");
    status.addStyleName("alignCenter");
    status.addStyleName("inlineBlockStyle");
    status.add(statusHeader);
    statusHeader.getElement().setId("statusHeader");
    statusHeader.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
    statusHeader.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    return status;
  }

  /**
   * @seex HistoryExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.amas.ExerciseListWrapper)
   */
  @Override
  protected void gotEmptyExerciseList() {
    logger.info("gotEmptyExerciseList");
    SectionWidget quiz = getSectionWidget("Quiz");
    int numChoices = getNumChoices();
    if (getNumSelections() < numChoices) {
      showMessage(quiz == null || quiz.hasOnlyOne() ? PLEASE_SELECT2 : PLEASE_SELECT, false);
    } else {
      showMessage("There are no questions for this quiz, test type, and ILR Level.<br/>Please make another selection.", false);
    }
  }

  private int getNumChoices() {
    return getSectionWidget("Quiz") == null ? NUM_CHOICES-1:NUM_CHOICES;
  }

  /**
   * @param toShow
   * @see SingleSelectExerciseList#gotEmptyExerciseList()
   * @see ResponseExerciseList#quizCompleteDisplay
   */
  void showMessage(String toShow, boolean addStartOver) {
    createdPanel = new SimplePanel();
    createdPanel.getElement().setId("placeHolderWhenNoExercises");
    createdPanel.add(new Heading(3, toShow));

    //innerContainer.getElement().getStyle().setMarginLeft(180, Style.Unit.PX);
    VerticalPanel vp = new VerticalPanel();
    vp.add(createdPanel);

 /*   if (addStartOver) {
      Button startOver = getStartOver();
      vp.add(startOver);
    }*/

    innerContainer.setWidget(vp);
  }

/*  private Button getStartOver() {
    Button startOver = new Button("Start Test Over", IconType.REPEAT);
    startOver.setType(ButtonType.SUCCESS);
    startOver.getElement().getStyle().setMarginLeft(40, Style.Unit.PCT);
    startOver.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        incrementSession();
      }
    });
    return startOver;
  }*/

  /**
   * TODO : just for ODA - consider putting this back if we want to do ODA from amas...
   */
/*  void incrementSession() {
//    String quiz = getQuiz() + ":"+getTestType()+ ":"+getILRLevel();
//    service.incrementSession(controller.getUser(), quiz, new AsyncCallback<Void>() {
//      @Override
//      public void onFailure(Throwable caught) {
//      }
//
//      @Override
//      public void onSuccess(Void result) {
//        loadExercises();
//      }
//    });
  }*/

  /**
   * @seex #rememberAndLoadFirst(List, CommonExercise, String)
   * @param searchIfAny
   */
  @Override
  public void loadFirstExercise(String searchIfAny) {
    //logger.info("loadFirstExercise : ---");

    if (isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      //logger.info("loadFirstExercise : current exercises is empty?");
      gotEmptyExerciseList();
    } else {
      super.loadFirstExercise(searchIfAny);
    }
  }

  /**
   * @see #addButtonRow
   */
  private void makeDefaultSelections() {
    for (ButtonBarSectionWidget v : sectionWidgetContainer.getValues()) {
     // ButtonBarSectionWidget value = (ButtonBarSectionWidget) v;
      v.simpleSelectOnlyOne();
    }
  }

  /**
   * When we log in we want to check the history in the url and set the current selection state to reflect it.
   * @see AMASInitialUI#configureUIGivenUser()
   */
  public void restoreListFromHistory() {  restoreListFromHistory(History.getToken());  }

  private void restoreListFromHistory(String token) {
    try {
      SelectionState selectionState = getSelectionState(token);
      if (DEBUG_ON_VALUE_CHANGE) {
        logger.info(" HistoryExerciseList.onValueChange : restoreListBoxState '" + selectionState + "'");
      }
      restoreListBoxState(selectionState);
      if (DEBUG_ON_VALUE_CHANGE) {
        logger.info("HistoryExerciseList.onValueChange : selectionState '" + selectionState + "'");
      }

      // logger.info("gotSelection : got type " + type + " and " + text);
      int count = getNumSelections();
      if (count == 3) {
        //    logger.info("push new token " + getHistoryTokenFromUIState());
        logger.info("gotSelection count = " + count);
        loadExercisesUsingPrefix(selectionState.getTypeToSection(), getPrefix(), false, "", false, false);
      } else {
        // logger.warning("not enough selections " +count);
        gotEmptyExerciseList();
      }
    } catch (Exception e) {
      logger.warning("HistoryExerciseList.onValueChange " + token + " badly formed. Got " + e);
      e.printStackTrace();
    }
  }
}
