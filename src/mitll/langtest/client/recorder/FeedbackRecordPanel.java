package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.BootstrapExercisePanel;
import mitll.langtest.client.flashcard.ScoreFeedback;
import mitll.langtest.client.flashcard.TextResponse;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/1/13
 * Time: 6:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class FeedbackRecordPanel extends SimpleRecordExercisePanel {
  private AutoCRTRecordPanel autoCRTRecordPanel;
  private Set<Widget> completed = new HashSet<Widget>();

  public FeedbackRecordPanel(Exercise e, LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller) {
    super(e, service, userFeedback, controller);
  }

  public void recordIncomplete(Widget answer) {
    completed.remove(answer);
    enableNext();
  }

  public void recordCompleted(Widget answer) {
    completed.add(answer);
    enableNext();
  }

  private void enableNext() {
    navigationHelper.enableNextButton((completed.size() == (controller.getProps().getResponseType().equalsIgnoreCase("Both") ? 2 :1)));
  }

  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) {
    String responseType = controller.getProps().getResponseType();
    if (responseType.equalsIgnoreCase("Text")) {
      return getWrittenPrompt(promptInEnglish);
    } else if (responseType.equals("Audio")) {
      return getSpokenPrompt(promptInEnglish);
    } else return "";
  }

  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {
    autoCRTRecordPanel = new AutoCRTRecordPanel(service, controller, exercise, this, index);
    String responseType = controller.getProps().getResponseType();
    final Widget outer = this;

    if (responseType.equalsIgnoreCase("Audio")) {
      return getVerticalPanel(autoCRTRecordPanel.getPanel(), new ScoreFeedback(true),true);
    }
    else if (responseType.equalsIgnoreCase("Text")){
      Panel widget = doText(exercise, service, controller, outer);
      return widget;
    }
    else {
      Panel widget = doText(exercise, service, controller, outer);

      widget.add(getVerticalPanel(autoCRTRecordPanel.getPanel(), new ScoreFeedback(true), true));
      return widget;
    }
  }

  private Panel doText(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final Widget outer) {
    TextResponse textResponse = new TextResponse(controller.getUser(),soundFeedback,
      new TextResponse.AnswerPosted() {
      @Override
      public void answerPosted() {
        recordCompleted(outer);
      }
    });

    FluidContainer container = new FluidContainer();
    textResponse.addWidgets(container,exercise,service,controller,false, true);

    Panel verticalPanel = getVerticalPanel(container, textResponse.getTextScoreFeedback(),false);
    textResponse.setSoundFeedback(soundFeedback);
    return verticalPanel;
  }

  SoundFeedback soundFeedback;
  public Panel getVerticalPanel(Panel panel, ScoreFeedback scoreFeedback,boolean addToContainer) {
    FluidContainer container = new FluidContainer();
    FluidRow row1 = new FluidRow();
    container.add(row1);
    row1.add(panel);

    if (addToContainer) {
      SimplePanel simplePanel = new SimplePanel(scoreFeedback.getFeedbackImage());
      simplePanel.addStyleName("floatLeft");

      Panel scoreFeedbackRow = scoreFeedback.getSimpleRow(simplePanel, 40);
      scoreFeedback.getScoreFeedback().setWidth(Window.getClientWidth() * 0.5 + "px");
      scoreFeedback.getScoreFeedback().addStyleName("topBarMargin");

      container.add(scoreFeedbackRow);
    }

    HTML warnNoFlash = new HTML(BootstrapExercisePanel.WARN_NO_FLASH);
    soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);
    autoCRTRecordPanel.setSoundFeedback(soundFeedback);
    autoCRTRecordPanel.setScoreFeedback(scoreFeedback);
    warnNoFlash.setVisible(false);
    FluidRow row3 = new FluidRow();
    container.add(row3);
    row3.add(warnNoFlash);

    return container;
  }

  @Override
  protected String getInstructions() {  return "";  }
}
