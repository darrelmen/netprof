package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.BootstrapExercisePanel;
import mitll.langtest.client.flashcard.ScoreFeedback;
import mitll.langtest.client.flashcard.TextResponse;
import mitll.langtest.client.list.ResponseChoice;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/1/13
 * Time: 6:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class FeedbackRecordPanel extends SimpleRecordExercisePanel {
  public static final int FEEDBACK_WIDTH = 250;
  private static final String TEXT = "Text";
  private static final String AUDIO = "Audio";
  private List<TextResponse> textResponses;

  private SoundFeedback soundFeedback;

  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public FeedbackRecordPanel(Exercise e, LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller) {
    super(e, service, userFeedback, controller, controller.getExerciseList());
    getElement().setId("FeedbackRecordPanel");
  }

  /**
   * @see #addQuestionPrompt(com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.Exercise)
   * @param promptInEnglish
   * @return
   */
  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) {
    String responseType = controller.getProps().getResponseType();
    if (responseType.equalsIgnoreCase(TEXT)) {
      return getWrittenPrompt(promptInEnglish);
    } else if (responseType.equals(AUDIO)) {
      return getSpokenPrompt(promptInEnglish);
    } else return "";
  }

  /**
   * @see #getQuestionPanel
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                   final int index) {
    if (textResponses == null) textResponses = new ArrayList<TextResponse>();
    String responseType = controller.getProps().getResponseType();
    String secondResponseType = controller.getProps().getSecondResponseType();
    final boolean isNone = secondResponseType.equals(ResponseChoice.NONE);

    final String recordButtonTitle = isNone ? "Record" : "Record in Arabic";
    final String textButtonTitle = isNone ? "Answer" : "Answer in Arabic";

    Widget widget = addComboAnswer(exercise, service, controller, index, responseType, isNone, recordButtonTitle, textButtonTitle, true,false);

    if (isNone) {
      return widget;
    }
    else {
      Panel vert = new VerticalPanel();
      vert.add(widget);
      vert.add(addComboAnswer(exercise,service,controller,index,secondResponseType,false, "Record in English", "Answer in English",false,true));
      return vert;
    }
  }

  private Widget addComboAnswer(final Exercise exercise, final LangTestDatabaseAsync service,
                                final ExerciseController controller, final int index, String responseType, final boolean isNone,
                                final String recordButtonTitle,
                                String buttonTitle,boolean getFocus, boolean isEnglish) {
    AutoCRTRecordPanel autoCRTRecordPanel = new AutoCRTRecordPanel(service, controller, exercise, this, index,
      FEEDBACK_WIDTH, recordButtonTitle) {
     //  @Override
      protected void setRecordButtonWidth(Panel recordButtonContainer) {
       // if (isNone) super.setRecordButtonWidth(recordButtonContainer);
      }
    };

    if (responseType.equalsIgnoreCase(AUDIO)) {
      Panel widgets ;
      if (isEnglish) {
        widgets = getRightSideAudioWidget(autoCRTRecordPanel);
      }
      else {
        widgets = addAudioAnswer(index, autoCRTRecordPanel);
      }
      return widgets;
    }
    else if (responseType.equalsIgnoreCase(TEXT)){
      return doText(exercise, service, controller, index,autoCRTRecordPanel,buttonTitle, getFocus);
    }
    else {  // both
      return getBothAudioAndText(exercise, service, controller, index, autoCRTRecordPanel, buttonTitle, getFocus);
    }
  }

  private Widget getBothAudioAndText(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                     int index, AutoCRTRecordPanel autoCRTRecordPanel,
                                     String buttonTitle,boolean getFocus) {
    Panel row = new DivWidget();
    row.addStyleName("trueInlineStyle");
    row.getElement().setId("FeedbackRecordPanel_getAnswerWidget_Row");

    // add text widget to left side
    Panel textWidget = doText(exercise, service, controller, index, autoCRTRecordPanel, buttonTitle, getFocus);
    textWidget.addStyleName("floatLeft");

    // add audio record widget to right side
    //autoCRTRecordPanel.getElement().setId("recordButtonPanel_"+index);

  //  addAnswerWidget(index, autoCRTRecordPanel);

    Panel outerContainer = getRightSideAudioWidget(autoCRTRecordPanel);
    row.add(textWidget);
    row.add(outerContainer);
    return row;
  }

  private FluidContainer addAudioAnswer(int index, AutoCRTRecordPanel autoCRTRecordPanel) {
 //   autoCRTRecordPanel.setWidth("100%");
//    addAnswerWidget(index, autoCRTRecordPanel);
    FluidContainer outerContainer = new FluidContainer();
 //   outerContainer.add(autoCRTRecordPanel);
    outerContainer.addStyleName("floatLeft");
    outerContainer.getElement().setId("FeedbackRecordPanel_outerContainer");

    addScoreFeedback(autoCRTRecordPanel, outerContainer);
    return outerContainer;
  }

  private Panel getRightSideAudioWidget(AutoCRTRecordPanel autoCRTRecordPanel) {
    Panel outerContainer = new FlowPanel();
    outerContainer.addStyleName("floatRight");
    outerContainer.addStyleName("blockStyle");
    outerContainer.getElement().setId("FeedbackRecordPanel_outerContainer_both");
  //  outerContainer.add(autoCRTRecordPanel);
    addScoreFeedback(autoCRTRecordPanel, outerContainer);
    return outerContainer;
  }

  private void addScoreFeedback(AutoCRTRecordPanel autoCRTRecordPanel, Panel outerContainer) {
    ScoreFeedback scoreFeedback = new ScoreFeedback(true);
    addScoreFeedback(outerContainer, scoreFeedback);

    getFeedbackContainer(outerContainer, scoreFeedback, autoCRTRecordPanel);
  }

  private Panel doText(Exercise exercise, final LangTestDatabaseAsync service, final ExerciseController controller,
                       int index, AutoCRTRecordPanel autoCRTRecordPanel,
                       String buttonTitle, boolean getFocus) {
    final TextResponse textResponse = new TextResponse(controller.getUser(), soundFeedback);
    textResponse.setAnswerPostedCallback(
      new TextResponse.AnswerPosted() {
        @Override
        public void answerPosted() {
          recordCompleted(textResponse.getTextResponseWidget());
        }
      });
    textResponses.add(textResponse);

    FluidContainer outerContainer = new FluidContainer();

    Panel row1 = new VerticalPanel();
    row1.getElement().setId("text_row1");
    outerContainer.add(row1);
    outerContainer.addStyleName("floatLeft");
    Widget widget = textResponse.addWidgets(row1, exercise, service, controller, false, true, false, index, buttonTitle, getFocus);
    widget.getElement().setId("textResponse_"+index);
    addAnswerWidget(index, widget);

    FluidRow row2 = new FluidRow();
    row2.getElement().setId("row2");

    outerContainer.add(row2);

    getFeedbackContainer(row2, textResponse.getTextScoreFeedback(),autoCRTRecordPanel);
    outerContainer.getElement().setId("outerContainer");

    textResponse.setSoundFeedback(soundFeedback);
    return outerContainer;
  }

  private void getFeedbackContainer(Panel container, ScoreFeedback scoreFeedback, AutoCRTRecordPanel autoCRTRecordPanel) {
    HTML warnNoFlash = new HTML(BootstrapExercisePanel.WARN_NO_FLASH);
    soundFeedback = new SoundFeedback(controller.getSoundManager(), warnNoFlash);
    autoCRTRecordPanel.setSoundFeedback(soundFeedback);
    autoCRTRecordPanel.setScoreFeedback(scoreFeedback);
    warnNoFlash.setVisible(false);
    FluidRow row3 = new FluidRow();
    container.add(row3);
    row3.getElement().setId("warnNoFlash_row");
    row3.add(warnNoFlash);
  }

  private void addScoreFeedback(Panel container, ScoreFeedback scoreFeedback) {
    SimplePanel simplePanel = new SimplePanel(scoreFeedback.getFeedbackImage());
    simplePanel.addStyleName("floatLeft");

  //  Panel scoreFeedbackRow = scoreFeedback.getSimpleRow(simplePanel, 40, FEEDBACK_WIDTH);
    scoreFeedback.getScoreFeedback().setWidth(Window.getClientWidth() * 0.5 + "px");
    scoreFeedback.getScoreFeedback().addStyleName("topBarMargin");

//    container.add(scoreFeedbackRow);
  }

  @Override
  protected void enableNext() {
    super.enableNext();
    if (isCompleted()) {
      service.getCompletedExercises(controller.getUser(), false, new AsyncCallback<Set<String>>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Set<String> result) {
          controller.getExerciseList().setCompleted(result);
          controller.showProgress();
        }
      });
    }
  }

  @Override
  protected String getInstructions() {  return "";  }

  @Override
  protected void onUnload() {
    super.onUnload();
    for (TextResponse textResponse : textResponses) textResponse.onUnload();
  }
}
