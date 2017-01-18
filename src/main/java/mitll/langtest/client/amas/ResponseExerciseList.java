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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.services.ExerciseServiceAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.amas.AmasExerciseImpl;
import mitll.langtest.shared.exercise.STATE;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/8/13
 * Time: 4:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResponseExerciseList extends SingleSelectExerciseList {
  private final Logger logger = Logger.getLogger("ResponseExerciseList");

  private static final String SPEECH = "Speech";
  private static final int MARGIN_TOP = 12;
  private static final int MAX_LENGTH_ID = 35;

  private static final String RESPONSE_TYPE = "responseType";
  private static final String RESPONSE_TYPE_DIVIDER = "###";
//  private QuizScorePanel quizPanel;
//  private static final String CONGRATULATIONS = "Congratulations!";

  /**
   * @param secondRow
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param controller
   * @see SimpleChapterNPFHelper#getMyListLayout
   */
  public ResponseExerciseList(Panel secondRow,
                              Panel currentExerciseVPanel,
                              ExerciseServiceAsync service,
                              UserFeedback feedback,
                              final ExerciseController controller, String instance) {
    super(secondRow, currentExerciseVPanel, service, feedback, controller, instance, false, true);
    innerContainer.getElement().getStyle().setPadding(5, Style.Unit.PX);
    String responseType = controller.getProps().getResponseType();
    responseTypeChanged(responseType);
    //controller.showAdvance(this);
    innerContainer.addStyleName("userNPFContent");
  }

  /**
   * @param responseType
   * @see #getChoice(ButtonGroup, String, IconType)
   * @see #getResponseWidget()
   * @see #ResponseExerciseList
   */
  private void responseTypeChanged(String responseType) {
    setResponseType(responseType);
    setHistoryItem(History.getToken());
  }

  private void setResponseType(String responseType) {
    this.response = responseType;
    controller.getProps().setResponseType(responseType);
  }

  /**
   * @return
   * @seex mitll.langtest.client.bootstrap.FlexSectionExerciseList#addComponents()
   * @see #addComponents()
   */
  protected ClickablePagingContainer<AmasExerciseImpl> makePagingContainer() {
    final PagingExerciseList<AmasExerciseImpl, AmasExerciseImpl> outer = this;
    pagingContainer = new ClickablePagingContainer<AmasExerciseImpl>(controller) {
      @Override
      protected void gotClickOnItem(AmasExerciseImpl e) {
        outer.gotClickOnItem(e);
      }
      protected void addColumnsToTable()
      {
        addColumn(getExerciseIdColumn2(), new TextHeader("Item"));
      }

      /**
       * @see SimplePagingContainer SimplePagingContainer#addColumnsToTable
       * @return
       */
      private Column<AmasExerciseImpl, SafeHtml> getExerciseIdColumn2() {
        return new Column<AmasExerciseImpl, SafeHtml>(new MySafeHtmlCell(true)) {

          @Override
          public void onBrowserEvent(Cell.Context context, Element elem, AmasExerciseImpl object, NativeEvent event) {
            super.onBrowserEvent(context, elem, object, event);
            if (BrowserEvents.CLICK.equals(event.getType())) {
              gotClickOnItem(object);
            }
          }

          @Override
          public SafeHtml getValue(AmasExerciseImpl shell) {
            String columnText = shell.getOldID();
            String html = shell.getOldID();
            if (columnText != null) {
              if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
              STATE state = shell.getState();

              boolean isDefect = state == STATE.DEFECT;
              boolean isFixed  = state == STATE.FIXED;

              boolean recorded = state == STATE.RECORDED;
              boolean approved = state == STATE.APPROVED || recorded;

              boolean isSet = isDefect || isFixed || approved;

              String icon =
                  approved ? "icon-check" :
                      isDefect ? "icon-bug" :
                          isFixed ? "icon-thumbs-up" :
                              "";

              html = (isSet ?
                  "<i " +
                      (isDefect ? "style='color:red'" :
                          isFixed ? "style='color:green'" :
                              "") +
                      " class='" +
                      icon +
                      "'></i>" +

                      "&nbsp;" : "") + columnText;
            }
            return new SafeHtmlBuilder().appendHtmlConstant(html).toSafeHtml();
          }
        };
      }

    };
    return pagingContainer;
  }

  /**
   * Adds the response type to the end of the history token
   *
   * @param historyToken
   */
  protected void setHistoryItem(String historyToken) {
    //logger.info("------------ ResponseExerciseList.setHistoryItem '" + historyToken + "' -------------- ");
    historyToken = historyToken.contains(RESPONSE_TYPE_DIVIDER) ? historyToken.split(RESPONSE_TYPE_DIVIDER)[0] : historyToken;
    String historyToken1 = historyToken + RESPONSE_TYPE_DIVIDER + RESPONSE_TYPE + "=" + response;
    logger.info("ResponseExerciseList.setHistoryItem : history new item '" +historyToken1 +"'");
    History.newItem(historyToken1);
  }

  /**
   * Label and two choice widget.
   *
   * @param container
   * @return
   * @see mitll.langtest.client.amas.SingleSelectExerciseList#addButtonRow
   */
  @Override
  protected void addBottomText(Panel container) {
    super.addBottomText(container);

    Panel horizontalPanel = new HorizontalPanel();
    Heading child = new Heading(5, "Respond with ");
    child.getElement().getStyle().setMarginTop(MARGIN_TOP, Style.Unit.PX);
    horizontalPanel.add(child);

    horizontalPanel.add(getResponseWidget());

    DivWidget right = new DivWidget();
    right.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    right.add(horizontalPanel);
    firstTypeRow.add(right);
  }

  private static final String TEXT = "Text";
  private static final String AUDIO = "Audio";
  private String response;

  /**
   * @return
   * @see #addBottomText(Panel)
   */
  private Widget getResponseWidget() {
    ButtonToolbar toolbar = new ButtonToolbar();
    toolbar.getElement().setId("WasIRight");
    styleToolbar(toolbar);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    toolbar.add(buttonGroup);

    String responseType = controller.getProps().getResponseType();

    Button choice1 = getChoice(buttonGroup, TEXT, IconType.PENCIL);

    Button choice2 = getChoice(SPEECH, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        responseTypeChanged(AUDIO);
      }
    });
    buttonGroup.add(choice2);
    choice2.setIcon(IconType.MICROPHONE);

    if (responseType.equals(TEXT)) choice1.setActive(true);
    else choice2.setActive(true);
    return toolbar;
  }

  private Button getChoice(ButtonGroup buttonGroup, final String text, IconType pencil) {
    Button choice1 = getChoice(text, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        responseTypeChanged(text);
      }
    });
    buttonGroup.add(choice1);
    choice1.setIcon(pencil);
    return choice1;
  }

  private void styleToolbar(ButtonToolbar toolbar) {
    Style style = toolbar.getElement().getStyle();
    int topToUse = 10;
    style.setMarginTop(topToUse, Style.Unit.PX);
    style.setMarginBottom(topToUse, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
  }

  private Button getChoice(String title, ClickHandler handler) {
    Button onButton = new Button(title);
    onButton.setType(ButtonType.INFO);
    String s = "Choice_" + title;
    onButton.getElement().setId(s);
    controller.register(onButton, s);
    onButton.addClickHandler(handler);
    onButton.setActive(false);
    return onButton;
  }

  /**
   * @see ListInterface#loadNextExercise
   */
  @Override
  protected void onLastItem() {
//    takeAgain();
  }

  /**
   * TODO : what do we do when we get to the end of a test?
   * @see ResponseExerciseList#onLastItem()
   */
/*
  private void takeAgain() {
    String first = getQuizStatus();
    Collection<String> singleton = Arrays.asList(first, "Would you like to take this test again?");
    new DialogHelper(CONGRATULATIONS, singleton, new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
        incrementSession();
      }

      @Override
      public void gotNo() {
        quizCompleteDisplay();
      }
    });
  }
*/

/*
  private String getQuizStatus() {
    String quiz = "Quiz #" + getQuiz();
    String level = "ILR Level " + getILRLevel();
    return getTestType() + " " + quiz + " at " + level + " is complete.";
  }
*/

/*
  private String getQuiz() {
    Collection<String> quiz1 = getTypeToSelection().get("Quiz");
    return quiz1 == null ? "" : quiz1.iterator().next();
  }

  private String getTestType() {
    Collection<String> strings = getTypeToSelection().get("Test type");
    return strings == null ? "" : strings.iterator().next();
  }

  private String getILRLevel() {
    Collection<String> strings = getTypeToSelection().get("ILR Level");
    return strings == null ? "" : strings.iterator().next();
  }
*/

/*  private void quizCompleteDisplay() {
    showMessage(getQuizStatus(), true);
    getScores();
  }*/

  // @Override
  public Map<String, Collection<String>> getTypeToSelection() {
    return getSelectionState(getInitialHistoryToken()).getTypeToSection();
  }

  /**
   * TODO : who should call this????
   *
   * @param typeToSection
   */
//  @Override
  protected void loadExercises(final Map<String, Collection<String>> typeToSection) {
    loadExercisesUsingPrefix(typeToSection, getPrefix(), -1, false, false, false, false);
  }

  /**
   * @see FeedbackRecordPanel#postAnswers
   * @paramz current
   * @return
   */
/*
  public boolean loadNextExercise(AmasExerciseImpl current) {
    //logger.info("loadNextExercise " + current.getOldID() );
    boolean b = super.loadNextExercise(current);
    // controller.showAdvance(this);
    return b;
  }
*/

  public void setQuizPanel(QuizScorePanel quizPanel) {

    //this.quizPanel = quizPanel;
  }

/*  private void getScores() {
    service.getScoresForUser(getTypeToSelection(), controller.getUser(), getIDs(), new AsyncCallback<QuizCorrectAndScore>() {
      @Override
      public void onFailure(Throwable throwable) {
        logger.warning("didn't do scores?");
      }

      @Override
      public void onSuccess(QuizCorrectAndScore correctAndScores) {
        quizPanel.setScores(correctAndScores.getCorrectAndScoreCollection());
        quizPanel.setVisible(true);
      }
    });
  }*/

  private static class MySafeHtmlCell extends SafeHtmlCell {
    private final boolean consumeClicks;
    public MySafeHtmlCell(boolean consumeClicks) {
      this.consumeClicks = consumeClicks;
    }

    @Override
    public Set<String> getConsumedEvents() {
      Set<String> events = new HashSet<String>();
      if (consumeClicks) events.add(BrowserEvents.CLICK);
      return events;
    }
  }
}
