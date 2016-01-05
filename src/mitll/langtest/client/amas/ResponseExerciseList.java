package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonGroup;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.exercise.Shell;
import mitll.langtest.shared.flashcard.QuizCorrectAndScore;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/8/13
 * Time: 4:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResponseExerciseList<T extends Shell> extends SingleSelectExerciseList<T> {
  private Logger logger = Logger.getLogger("ResponseExerciseList");

  private static final String SPEECH = "Speech";
  private static final int MARGIN_TOP = 12;

  private static final String RESPONSE_TYPE = "responseType";
  public static final String RESPONSE_TYPE_DIVIDER = "###";
  private QuizScorePanel quizPanel;
  private static final String CONGRATULATIONS = "Congratulations!";

  /**
   * @param secondRow
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param controller
   * @see mitll.langtest.client.exercise.AutoCRTChapterNPFHelper#getMyListLayout
   */
  public ResponseExerciseList(Panel secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                              UserFeedback feedback,
                              final ExerciseController controller, String instance) {
    super(secondRow, currentExerciseVPanel, service, feedback, controller, instance, false);
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
   * @see #ResponseExerciseList(Panel, Panel, LangTestDatabaseAsync, UserFeedback, ExerciseController, String)
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
  protected ClickablePagingContainer<T> makePagingContainer() {
    final PagingExerciseList<T> outer = this;
    pagingContainer = new ClickablePagingContainer<T>(controller, getVerticalUnaccountedFor(),false) {
      @Override
      protected void gotClickOnItem(T e) {
        outer.gotClickOnItem(e);
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
    // logger.info("history new item  " +historyToken1);
    History.newItem(historyToken1);
  }

  /**
   * Label and two choice widget.
   *
   * @param container
   * @return
   * @see mitll.langtest.client.bootstrap.SingleSelectExerciseList#addButtonRow
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
   * Deal with responseType being after ###
   *
   * @param token
   * @return
   */
//  @Override
//  protected String getSelectionFromToken(String token) {
//    return token.split(ResponseExerciseList.RESPONSE_TYPE_DIVIDER)[0]; // remove any other parameters
//  }

  /**
   * @see ListInterface#loadNextExercise(Shell)
   */
  @Override
  protected void onLastItem() {
    takeAgain();
  }


  /**
   * @see ResponseExerciseList#onLastItem()
   */
  protected void takeAgain() {
    String first = getQuizStatus();
    Collection<String> singleton =  Arrays.asList(first, "Would you like to take this test again?");
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
  private String getQuizStatus() {
    String quiz = "Quiz #" +getQuiz();
    String level = "ILR Level " +getILRLevel();
    return getTestType() + " " + quiz + " at " + level + " is complete.";
  }

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

  private void quizCompleteDisplay() {
    showMessage(getQuizStatus(),true);
    getScores();
  }

  // @Override
  public Map<String, Collection<String>> getTypeToSelection() {
    return getSelectionState(getHistoryToken("")).getTypeToSection();
  }

  /**
   * TODO : who should call this????
   * @param typeToSection
   */
//  @Override
  protected void loadExercises(final Map<String, Collection<String>> typeToSection) {
    loadExercisesUsingPrefix(typeToSection, getPrefix(), false);
  }

  @Override
  public boolean loadNextExercise(T current) {
    //logger.info("loadNextExercise " + current.getID() );
    boolean b = super.loadNextExercise(current);
   // controller.showAdvance(this);
    return b;
  }

  public void setQuizPanel(QuizScorePanel quizPanel) {
    this.quizPanel = quizPanel;
  }

  protected void getScores() {
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
  }
}
