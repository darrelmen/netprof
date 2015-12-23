package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.MyCustomIconType;
import mitll.langtest.client.sound.SoundFeedback;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/1/13
 * Time: 6:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScoreFeedback {
  private IconAnchor feedbackImage;
  private Panel scoreFeedbackColumn;
  private final ProgressBar scoreFeedback = new ProgressBar();

  private SimplePanel feedbackDummyPanel;
  private final boolean useWhite;
  private ExerciseController controller;

  public ScoreFeedback(boolean useWhite) {
    this.useWhite = useWhite;
  }

  /**
   * Holds the pron score feedback.
   * Initially made with a placeholder.
   *
   * @return
   * @see mitll.langtest.client.flashcard.TextResponse#addWidgets
   */
  public FluidRow getScoreFeedbackRow(int height, ExerciseController controller) {
    this.controller = controller;

    FluidRow feedbackRow = new FluidRow();
    feedbackDummyPanel = new SimplePanel();
    feedbackDummyPanel.setHeight(height + "px");
    scoreFeedbackColumn = new SimplePanel(feedbackDummyPanel);

    feedbackRow.add(scoreFeedbackColumn);
    feedbackRow.getElement().setId("ScoreFeedback_feedbackRow");
    return feedbackRow;
  }

  /**
   * @param left
   * @param height
   * @return
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel#addScoreFeedback
   */
  public Panel getSimpleRow(Widget left, int height) {
    Panel feedbackRow = new FlowPanel();
    feedbackRow.add(left);
    feedbackDummyPanel = new SimplePanel();
    feedbackDummyPanel.setHeight(height + "px");
    feedbackDummyPanel.addStyleName("floatLeft");

    scoreFeedbackColumn = new SimplePanel(feedbackDummyPanel);
    return feedbackRow;
  }

  public void setWaiting() {
    feedbackImage.setVisible(true);
    feedbackImage.setBaseIcon(MyCustomIconType.waiting);
  }

  public void hideWaiting() {
    feedbackImage.setBaseIcon(useWhite ? MyCustomIconType.white : MyCustomIconType.gray);
  }

  /**
   * @paramx result
   * @paramx controller
   * @paramx width
   * @see mitll.langtest.client.flashcard.TextResponse#getScoreForGuess
   * @see mitll.langtest.client.flashcard.PressAndHoldExercisePanel#getAnswerWidget
   */
  public void showCRTFeedback() {
    playCorrect();
    hideWaiting();
  }

  /**
   * @see mitll.langtest.client.flashcard.TextResponse#getScoreForGuess
   * @see mitll.langtest.client.flashcard.PressAndHoldExercisePanel#getAnswerWidget
   * @param controller
   * @paramx width
   */
  public void showCRTFeedback(/*, int width*/ExerciseController controller) {
    this.controller = controller;
  }

  private MySoundFeedback soundFeedback;

  private void lazyMakeSoundFeedback() {
    if (soundFeedback == null) soundFeedback = new MySoundFeedback();
  }

  private void playCorrect() {
    lazyMakeSoundFeedback();
    soundFeedback.queueSong(SoundFeedback.CORRECT);
   }

  public class MySoundFeedback extends SoundFeedback {
    public MySoundFeedback() {
      super(controller.getSoundManager());
    }

    public synchronized void queueSong(String song) {
      destroySound(); // if there's something playing, stop it!
      createSound(song, null);
    }
  }


  public void clearFeedback() {
    scoreFeedbackColumn.clear();
    scoreFeedbackColumn.add(feedbackDummyPanel);
  }

  /**
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel.AnswerPanel#addScoreFeedback(ScoreFeedback)
   * @return
   */
  public IconAnchor getFeedbackImage() {
    IconAnchor image;
    image = new IconAnchor();
    image.addStyleName("leftFiveMargin");
    image.setBaseIcon(useWhite ? MyCustomIconType.white : MyCustomIconType.gray);
    image.setHeight("48px");
    feedbackImage = image;
    return image;
  }

  public ProgressBar getScoreFeedback() {
    return scoreFeedback;
  }
}
