package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExerciseQuestionState;
import mitll.langtest.client.flashcard.SimpleTextResponse;
import mitll.langtest.client.list.ResponseChoice;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.AudioAnswer;
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
public class ComboRecordPanel extends SimpleRecordExercisePanel {
  private static final String TEXT = "Text";
  private static final String AUDIO = "Audio";
  public static final String SAW_ENGLISH_Q = "Eng. Q/";

  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public ComboRecordPanel(Exercise e, LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller) {
    super(e, service, userFeedback, controller, controller.getExerciseList());
    getElement().setId("ComboRecordPanel");
  }

  protected void addItemHeader(Exercise e) {
    if (!e.getContent().contains("Listen")) {
       super.addItemHeader(e);
    }
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

  private Set<Integer> showedEnglishQuestion = new HashSet<Integer>();

  /**
   * @see #getQuestionPanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, int, java.util.List, java.util.List, mitll.langtest.shared.Exercise.QAPair, com.google.gwt.user.client.ui.HasWidgets)
   * @param i
   * @param total
   * @param qaPair
   * @param englishPair
   * @param flQAPair
   * @param showAnswer
   * @param toAddTo
   */
  @Override
  protected void getQuestionHeader(final int i, int total,
                                   Exercise.QAPair qaPair,
                                   Exercise.QAPair englishPair,
                                   Exercise.QAPair flQAPair,
                                   boolean showAnswer, HasWidgets toAddTo) {
    String prefix = (total == 1) ? ("Question : ") : "";

    HTML maybeRTLContent = getMaybeRTLContent("<h4>" + prefix + qaPair.getQuestion() + "</h4>", false);
    DOM.setStyleAttribute(maybeRTLContent.getElement(), "marginTop", "0px");

    Panel hp = new HorizontalPanel();
    hp.add(maybeRTLContent);
    Button child = new Button("Show In English");
    child.setType(ButtonType.WARNING);
    child.addStyleName("leftFiveMargin");
    child.addStyleName("topFiveMargin");
    hp.add(child);
    toAddTo.add(hp);

    final HTML maybeRTLContentEnglish = getMaybeRTLContent("<h4>" + prefix + englishPair.getQuestion() + "</h4>", false);
    DOM.setStyleAttribute(maybeRTLContentEnglish.getElement(), "marginTop", "0px");
    maybeRTLContentEnglish.addStyleName("englishQuestionRightMargin");
    maybeRTLContentEnglish.setVisible(false);

    child.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (!maybeRTLContentEnglish.isVisible()) {
          maybeRTLContentEnglish.setVisible(true);
          showedEnglishQuestion.add(i);
        }
      }
    });

    toAddTo.add(maybeRTLContentEnglish);
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
    String responseType = controller.getProps().getResponseType();
    String secondResponseType = controller.getProps().getSecondResponseType();
    final boolean isNone = secondResponseType.equals(ResponseChoice.NONE);

    final String recordButtonTitle = isNone ? "Record" : "Record in Arabic";
    final String textButtonTitle   = isNone ? "Answer" : "Answer in Arabic";

    MySimpleRecordPanel autoCRTRecordPanel = makeRecordPanel(exercise, service, controller, index, isNone, recordButtonTitle, false);

   // Widget widget = getComboAnswer(exercise, service, controller, index, responseType, textButtonTitle, true, false, true, autoCRTRecordPanel);

    if (isNone) {
      //return widget;
      return new SimplePanel();
    }
    else {
      Panel vert = new VerticalPanel();
  //    vert.add(widget);
      MySimpleRecordPanel autoCRTRecordPanel2 = makeRecordPanel(exercise, service, controller, index, false, "Record in English", true);

      // they need to know about each other so we can prevent both recording at the same time
      autoCRTRecordPanel2.setOtherRecordPanel(autoCRTRecordPanel);
      autoCRTRecordPanel.setOtherRecordPanel(autoCRTRecordPanel2);

   /*   Widget child = getComboAnswer(exercise, service, controller, index, secondResponseType, "Answer in English", false, true,
        !responseType.equals(ResponseChoice.BOTH), autoCRTRecordPanel2);
      child.addStyleName("topFiveMargin");

      vert.add(child);*/
      return vert;
    }
  }

/*  private Widget getComboAnswer(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, int index,
                                String responseType, String buttonTitle,
                                boolean getFocus,
                                boolean isEnglish,
                                boolean leftAlignAudio,
                                SimpleRecordPanel autoCRTRecordPanel) {
    if (responseType.equalsIgnoreCase(AUDIO)) {
      Panel widgets ;
      if (leftAlignAudio) {
        widgets = addAudioAnswer(index, autoCRTRecordPanel);
      }
      else {
        widgets = getRightSideAudioWidget(index, autoCRTRecordPanel);
      }

      return widgets;
    }
    else if (responseType.equalsIgnoreCase(TEXT)){
      String answerType = isEnglish ? "english text" : "arabic text";
      return doText(exercise, service, controller, index, getFocus, answerType, buttonTitle);
    }
    else {  // both
      String answerType = isEnglish ? "english text" : "arabic text";
      return getBothAudioAndText(exercise, service, controller, index, autoCRTRecordPanel, buttonTitle, getFocus, answerType);
    }
  }*/

  private MySimpleRecordPanel makeRecordPanel(final Exercise exercise, final LangTestDatabaseAsync service,
                                            final ExerciseController controller, final int index, final boolean isNone,
                                            final String recordButtonTitle, final boolean isEnglish) {
    return new MySimpleRecordPanel(service, controller, exercise, index, recordButtonTitle, isNone, isEnglish,this);
  }

  private Widget getBothAudioAndText(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller,
                                     int index, Panel autoCRTRecordPanel,
                                     String buttonTitle,boolean getFocus,String answerType) {
    Panel row = new DivWidget();
    row.addStyleName("trueInlineStyle");
    row.getElement().setId("ComboRecordPanel_getAnswerWidget_Row");

    // add text widget to left side
    Panel textWidget = doText(exercise, service, controller, index,  getFocus, answerType, buttonTitle);
    textWidget.addStyleName("floatLeft");

    // add audio record widget to right side
    autoCRTRecordPanel.getElement().setId("recordButtonPanel_"+index);

    Panel outerContainer = getRightSideAudioWidget(index,autoCRTRecordPanel);
    row.add(textWidget);
    row.add(outerContainer);
    return row;
  }

  private Panel addAudioAnswer(int index, Panel autoCRTRecordPanel) {
    autoCRTRecordPanel.setWidth("100%");
    addAnswerWidget(index, autoCRTRecordPanel);
    Panel outerContainer = new FluidContainer();
    outerContainer.add(autoCRTRecordPanel);
    outerContainer.addStyleName("floatLeft");
    outerContainer.getElement().setId("ComboRecordPanel_outerContainer");

    return outerContainer;
  }

  /**
   * @see #getBothAudioAndText(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, com.google.gwt.user.client.ui.Panel, String, boolean, String)
   * @see #getComboAnswer(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, int, String, String, boolean, boolean, boolean, SimpleRecordPanel)
   * @param index
   * @param autoCRTRecordPanel
   * @return
   */
  private Panel getRightSideAudioWidget(int index, Panel autoCRTRecordPanel) {
    addAnswerWidget(index, autoCRTRecordPanel);

    Panel outerContainer = new FlowPanel();
    outerContainer.addStyleName("floatRight");
    outerContainer.addStyleName("blockStyle");
    outerContainer.getElement().setId("ComboRecordPanel_outerContainer_both");
    outerContainer.add(autoCRTRecordPanel);
    return outerContainer;
  }

  private Panel doText(final Exercise exercise, final LangTestDatabaseAsync service, final ExerciseController controller,
                       final int index, boolean getFocus, final String answerType, String prompt) {
    final SimpleTextResponse textResponse = new SimpleTextResponse(controller.getUser()) {
      @Override
      protected String getAnswerType() {
        return (showedEnglishQuestion.contains(index) ? SAW_ENGLISH_Q : "") + answerType;
      }
    };
    textResponse.setAnswerPostedCallback(
      new SimpleTextResponse.AnswerPosted() {
        @Override
        public void answerTyped() {
          recordCompleted(textResponse.getTextResponseWidget());
        }

        @Override
        public void answerPosted() {
          recordCompleted(textResponse.getTextResponseWidget());
        }
      });
    Panel outerContainer = new FluidContainer();

    Panel row1 = new VerticalPanel();
    row1.getElement().setId("text_row1");
    outerContainer.add(row1);
    outerContainer.addStyleName("floatLeft");
    Widget widget = textResponse.addWidgets(row1, exercise, service, controller, index, getFocus, prompt);
    widget.getElement().setId("textResponse_"+index);
    addAnswerWidget(index, widget);

    outerContainer.getElement().setId("outerContainer");

    return outerContainer;
  }

/*  @Override
  public void postAnswers(final ExerciseController controller, Exercise completedExercise) {
    super.postAnswers(controller, completedExercise);

    if (isCompleted()) {
      service.getCompletedExercises(controller.getUser(),false, new AsyncCallback<Set<String>>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Set<String> result) {
          controller.getExerciseList().setCompleted(result);
          controller.showProgress();
        }
      });
    }
  }*/

  @Override
  protected String getInstructions() {  return "";  }

  private class MySimpleRecordPanel extends SimpleRecordPanel {
    private final int index;
    private final boolean isNone;
    private final boolean isEnglish;
    private  SimpleRecordPanel otherRecordPanel;

    public MySimpleRecordPanel(LangTestDatabaseAsync service, ExerciseController controller, Exercise exercise,
                               int index, String recordButtonTitle, boolean isNone, boolean isEnglish,
                               ExerciseQuestionState questionState) {
      super(service, controller, exercise, questionState, index/*, recordButtonTitle*/);
      this.index = index;
      this.isNone = isNone;
      this.isEnglish = isEnglish;
      getElement().setId("SimpleRecordPanel_panel_" + index + (isEnglish ? "english":"arabic"));
    }

/*    @Override
    protected void setRecordButtonWidth(Panel recordButtonContainer) {
      if (isNone) super.setRecordButtonWidth(recordButtonContainer);
    }*/

    @Override
    protected String getAudioType() {
      return (showedEnglishQuestion.contains(index) ? SAW_ENGLISH_Q : "") + (isEnglish ? " english audio" : "arabic audio");
    }

    @Override
    protected void receivedAudioAnswer(AudioAnswer result, ExerciseQuestionState questionState, Panel outer) {
      super.receivedAudioAnswer(result, questionState, outer);
      if (result.isValid()) {
        //recordCompleted(this);
      }
      else {
       // recordIncomplete(this);
      }
    }

    @Override
    public void startRecording() {
      super.startRecording();
      if (otherRecordPanel != null) otherRecordPanel.setRecordButtonEnabled(false);
    }

    @Override
    public void stopRecording() {
      super.stopRecording();
      if (otherRecordPanel != null) otherRecordPanel.setRecordButtonEnabled(true);
    }

    public void setOtherRecordPanel(SimpleRecordPanel otherRecordPanel) {
      this.otherRecordPanel = otherRecordPanel;
    }
  }
}
