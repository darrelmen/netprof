/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.shared.AudioAnswer;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/25/13
 * Time: 1:43 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class FlashcardRecordButtonPanel extends RecordButtonPanel implements RecordButton.RecordingListener {
  private final AudioAnswerListener exercisePanel;

  private IconAnchor waiting;
  private IconAnchor correctIcon;
  private IconAnchor incorrect;
  protected final String instance;

  /**
   * @param exercisePanel
   * @param service
   * @param controller
   * @param exerciseID
   * @param index
   * @param instance
   * @see BootstrapExercisePanel#getAnswerWidget(mitll.langtest.shared.exercise.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, boolean, String)
   */
  public FlashcardRecordButtonPanel(AudioAnswerListener exercisePanel,
                                    LangTestDatabaseAsync service,
                                    ExerciseController controller,
                                    String exerciseID,
                                    int index,
                                    String instance,
                                    Map<String, Collection<String>> typeToSelection) {
    super(service, controller, exerciseID, index, true, "avp", "Record", typeToSelection);
    this.instance = instance;
    this.exercisePanel = exercisePanel;
  }

  /**
   * @see #layoutRecordButton(com.google.gwt.user.client.ui.Widget)
   */
  @Override
  protected void addImages() {
    waiting = new IconAnchor();
    correctIcon = new IconAnchor();
    incorrect = new IconAnchor();

    waiting.setBaseIcon(MyCustomIconType.waiting);
    waiting.setVisible(false);

    correctIcon.setBaseIcon(MyCustomIconType.correct);
    correctIcon.setVisible(false);

    incorrect.setBaseIcon(MyCustomIconType.incorrect);
    incorrect.setVisible(false);
  }

  /**
   * @return
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#getAnswerAndRecordButtonRow(mitll.langtest.shared.exercise.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController)
   */
  @Override
  public Widget getRecordButton() {
    Widget recordButton1 = super.getRecordButton();
    Panel hp = new FlowPanel();
    hp.add(recordButton1);
    hp.add(waiting);
    hp.add(correctIcon);
    hp.add(incorrect);
    return hp;
  }

  @Override
  public void initRecordButton() {
    super.initRecordButton();
    correctIcon.setVisible(false);
    incorrect.setVisible(false);
    waiting.setVisible(false);
  }

  @Override
  protected abstract RecordButton makeRecordButton(ExerciseController controller, String title);

  /**
   * Deal with three cases: <br></br>
   * * the audio was invalid in some way : too short, too quiet, too loud<br></br>
   * * the audio was the correct response<br></br>
   * * the audio was incorrect<br></br><p></p>
   * <p>
   * And then move on to the next item.
   *
   * @param result response from server
   * @param outer  ignored here
   * @see mitll.langtest.client.recorder.RecordButtonPanel#postAudioFile
   */
  @Override
  protected void receivedAudioAnswer(final AudioAnswer result, Panel outer) {
    // System.out.println("FlashcardRecordButtonPanel.receivedAudioAnswer " + result);
    recordButton.setVisible(false);
    waiting.setVisible(false);
    if (result.isCorrect()) {
      correctIcon.setVisible(true);
    } else {
      incorrect.setVisible(true);
    }

    exercisePanel.receivedAudioAnswer(result);
  }

  @Override
  public void flip(boolean first) {
  }

  @Override
  public void stopRecording() {
    super.stopRecording();
    recordButton.setVisible(false);
    waiting.setVisible(true);
  }
}
