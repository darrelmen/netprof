package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.shared.Exercise;

/**
 * Do text response in a flashcard format.
 *
 * User: GO22670
 * Date: 10/25/13
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataCollectionFlashcard extends BootstrapExercisePanel {
  private static final int LOGIN_WIDGET_HEIGHT = 50; // TODO : do something better to figure out the height of the login widget
  private Panel leftButtonContainer, rightButtonContainer;
  protected NavigationHelper navigationHelper;

  /**
   * @see mitll.langtest.client.flashcard.DataCollectionFlashcardFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param controller
   */
  public DataCollectionFlashcard(Exercise e, LangTestDatabaseAsync service, ExerciseController controller, int feedbackHeight) {
    super(e, service, controller, feedbackHeight);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand () {
      public void execute () {
        int offsetHeight = cardPrompt.getOffsetHeight();
        DOM.setStyleAttribute(leftButtonContainer.getElement(),  "marginTop", (offsetHeight / 2) + "px");
        DOM.setStyleAttribute(rightButtonContainer.getElement(), "marginTop", ((offsetHeight/2)- LOGIN_WIDGET_HEIGHT) +"px");
      }
    });
    navigationHelper.enablePrevButton(!controller.getExerciseList().onFirst(null));
    navigationHelper.enableNextButton(false);
    navigationHelper.setVisible(true);
    getElement().setId("DataCollectionFlashcard");
  }

  /**
   * @see BootstrapExercisePanel#getAnswerAndRecordButtonRow(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @param addKeyBinding
   * @return
   */
  @Override
  protected RecordButtonPanel getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, int index, boolean addKeyBinding) {
    return new FlashcardRecordButtonPanel(this, service, controller, exercise, index, false);
  }

  /**
   * @see BootstrapExercisePanel#BootstrapExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int)
   * @param e
   * @param controller
   * @return
   */
  protected FlowPanel getCardPrompt(Exercise e, ExerciseController controller) {
    FluidRow questionRow = new FluidRow();
    Widget questionContent = new AudioExerciseContent().getQuestionContent(e, controller, true, true);
    Column contentContainer = new Column(8, questionContent);
    contentContainer.addStyleName("blueBackground");
    contentContainer.addStyleName("userNPFContent");
    contentContainer.addStyleName("topMargin");
    contentContainer.addStyleName("marginBottomTen");
    makeNavigationHelper(e, controller);

    Widget prev = navigationHelper.getPrev();
    HorizontalPanel hp = getCenteredContainer(prev);
    leftButtonContainer = hp;
    questionRow.add(new Column(2, hp));
    questionRow.add(contentContainer);
    questionRow.add(new Column(2,rightButtonContainer = getCenteredContainer(navigationHelper.getNext())));

    return questionRow;
  }

  protected void makeNavigationHelper(Exercise e, ExerciseController controller) {
    navigationHelper = new NavigationHelper(e,controller,null,controller.getExerciseList(), true);
  }

  private HorizontalPanel getCenteredContainer(Widget prev) {
    HorizontalPanel hp = new HorizontalPanel();
    hp.setHeight("100%");
    hp.setWidth("100%");
    hp.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
    hp.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
    hp.add(prev);
    return hp;
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    navigationHelper.removeKeyHandler();
  }

  /**
   * @see FlashcardRecordButtonPanel#showCorrectFeedback(double)
   * @see FlashcardRecordButtonPanel#showIncorrectFeedback(mitll.langtest.shared.AudioAnswer, double, boolean)
   * @param score
   * @param scorePrefix
   */
  @Override
  public void showPronScoreFeedback(double score, String scorePrefix) {
    super.showPronScoreFeedback(score, scorePrefix);
    navigationHelper.enableNextButton(true);
  }
}
