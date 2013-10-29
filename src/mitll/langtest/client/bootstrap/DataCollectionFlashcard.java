package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.media.client.Audio;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.NavigationHelper;
import mitll.langtest.shared.Exercise;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/25/13
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataCollectionFlashcard extends BootstrapExercisePanel {
  private static final String LISTEN_TO_THIS_AUDIO = "Listen to this audio";
  private Panel leftButtonContainer, rightButtonContainer;
  private NavigationHelper navigationHelper;

  /**
   * @see mitll.langtest.client.flashcard.DataCollectionFlashcardFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param controller
   */
  public DataCollectionFlashcard(Exercise e, LangTestDatabaseAsync service, ExerciseController controller) {
    super(e, service, controller);
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand () {
      public void execute () {
        int offsetHeight = cardPrompt.getOffsetHeight();
        DOM.setStyleAttribute(leftButtonContainer.getElement(), "marginTop", (offsetHeight / 2) + "px");
        DOM.setStyleAttribute(rightButtonContainer.getElement(), "marginTop", ((offsetHeight/2)-50) +"px");
      }
    });
    navigationHelper.enablePrevButton(!controller.onFirst(null));
  }

  @Override
  protected FlashcardRecordButtonPanel getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, int index) {
    return new FlashcardRecordButtonPanel(this, service, controller, exercise, index, false);
  }

  protected Widget getCardPrompt(Exercise e, ExerciseController controller) {
    FluidRow questionRow = new FluidRow();
    Widget questionContent = getQuestionContent2(e, controller);
    Column contentContainer = new Column(8, questionContent);
    contentContainer.addStyleName("blueBackground");
    contentContainer.addStyleName("userNPFContent");
    contentContainer.addStyleName("topMargin");
    contentContainer.addStyleName("marginBottomTen");
    navigationHelper = new NavigationHelper(e,controller,false);

    Widget prev = navigationHelper.getPrev();
    HorizontalPanel hp = getCenteredContainer(prev);
    leftButtonContainer = hp;
    questionRow.add(new Column(2, hp));
    questionRow.add(contentContainer);
    questionRow.add(new Column(2,rightButtonContainer = getCenteredContainer(navigationHelper.getNext())));

    return questionRow;
  }

  protected Widget getQuestionContent2(Exercise e, ExerciseController controller) {
    String stimulus = e.getEnglishSentence();
    String content = e.getContent();

    if (content == null) {
      content = stimulus;
    }
    return makeFlashcardForCRT(e, controller, content);
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

  private Panel makeFlashcardForCRT(Exercise e, ExerciseController controller, String content) {
    Exercise.QAPair qaPair = e.getForeignLanguageQuestions().get(0);
   // content = content.replaceAll("<p> &nbsp; </p>", "");
    String splitTerm = LISTEN_TO_THIS_AUDIO;
    String[] split = content.split(splitTerm);
    String prefix = split[0];
    HTML contentPrefix = getHTML(prefix, true, controller);
    contentPrefix.addStyleName("marginRight");

    Panel container = new FlowPanel();
    Heading child = new Heading(5, "Exercise " + e.getID());
    child.addStyleName("leftTenMargin");
    container.add(child);
    container.add(contentPrefix);

    // Todo : this is vulnerable to a variety of issues.
    if (e.getRefAudio() != null && e.getRefAudio().length() > 0) {
      Panel container2 = new FlowPanel();
      container2.addStyleName("rightFiveMargin");
      HTML prompt = getHTML("<h3 style='margin-right: 30px'>" + splitTerm + "</h3>", true, controller);
      container2.add(prompt);
      SimplePanel simplePanel = new SimplePanel();
      simplePanel.add(getAudioWidget(e));
      container2.add(simplePanel);

      String suffix = split[1];
      HTML contentSuffix = getHTML("<br/>" + // TODO: br is a hack
        "<h3 style='margin-right: 30px'>" + suffix + "</h3>", true, controller);
      contentSuffix.addStyleName("marginRight");
      container2.add(contentSuffix);
      container2.addStyleName("rightAlign");
      container.add(container2);
    }

    container.add(getHTML("<h2 style='margin-right: 30px'>" + qaPair.getQuestion() + "</h2>", true, controller));
    return container;
  }

  private Widget getAudioWidget(Exercise e) {
    String refAudio = e.getRefAudio();
    String type = refAudio.substring(refAudio.length() - 3);

    final Audio audio = getAudioNoFocus(refAudio, type);
    audio.addStyleName("floatRight");
    audio.addStyleName("rightFiveMargin");

    return audio;
  }

  private Audio getAudioNoFocus(String refAudio, String type) {
    final Audio audio = Audio.createIfSupported();
    audio.setControls(true);
    audio.addSource(refAudio, "audio/" + type);
    audio.addSource(refAudio.replace(".mp3", ".ogg"), "audio/ogg");
    audio.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        audio.setFocus(false);
      }
    });
    return audio;
  }

  private HTML getHTML(String content, boolean requireAlignment, ExerciseController controller) {
    boolean rightAlignContent = controller.isRightAlignContent();
    HasDirection.Direction direction =
      requireAlignment && rightAlignContent ? HasDirection.Direction.RTL : WordCountDirectionEstimator.get().estimateDirection(content);

    HTML html = new HTML(content, direction);
  //  html.setWidth("100%");
    if (requireAlignment && rightAlignContent) {
      html.addStyleName("rightAlign");
    }
    html.addStyleName("rightTenMargin");

    html.addStyleName("wrapword");
    return html;
  }
}
