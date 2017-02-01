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

package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.recorder.RecordButtonPanel;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;

import static mitll.langtest.client.scoring.PostAudioRecordButton.MIN_DURATION;


/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/25/13
 * Time: 1:43 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class FlashcardRecordButtonPanel extends RecordButtonPanel implements RecordButton.RecordingListener {
//  private final Logger logger = Logger.getLogger("FlashcardRecordButtonPanel");
  private final AudioAnswerListener exercisePanel;

  private IconAnchor waiting;

/*  private IconAnchor correctIcon;
  private IconAnchor incorrect;*/

  /**
   * @param exercisePanel
   * @param controller
   * @param exerciseID
   * @param index
   * @see BootstrapExercisePanel#getAnswerWidget
   */
  public FlashcardRecordButtonPanel(AudioAnswerListener exercisePanel,
                                    ExerciseController controller,
                                    int exerciseID,
                                    int index) {
    super(controller, exerciseID, index, true, AudioType.PRACTICE, "Record");
   // this.instance = instance;
    this.exercisePanel = exercisePanel;
  }

  /**
   * @see #layoutRecordButton
   */
  @Override
  protected void addImages() {
    waiting     = new IconAnchor();
    waiting.setBaseIcon(MyCustomIconType.waiting);
    hideWaiting();

 /*   correctIcon = new IconAnchor();
    incorrect   = new IconAnchor();

    correctIcon.setBaseIcon(MyCustomIconType.correct);
    correctIcon.setVisible(false);

    incorrect.setBaseIcon(MyCustomIconType.incorrect);
    incorrect.setVisible(false);*/
  }

  private void hideWaiting() {
    waiting.setVisible(false);
  }

  /**
   * @return
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#getAnswerAndRecordButtonRow
   */
  @Override
  public Widget getRecordButton() {
    Widget recordButton1 = super.getRecordButton();
    Panel hp = new FlowPanel();
    hp.getElement().setId("flashcardButtonContainer");
    hp.add(recordButton1);
    hp.add(waiting);
/*    hp.add(correctIcon);
    hp.add(incorrect);*/
    return hp;
  }

  @Override
  public void initRecordButton() {
    super.initRecordButton();
/*    correctIcon.setVisible(false);
    incorrect.setVisible(false);*/
    hideWaiting();
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
    hideWaiting();
    showRecord();

//    hideRecordButton();
  //  showCorrectIcons(result);
    exercisePanel.receivedAudioAnswer(result);
  }
/*
  private void showCorrectIcons(AudioAnswer result) {
    if (result.isCorrect()) {
      correctIcon.setVisible(true);
    } else {
      incorrect.setVisible(true);
    }
  }*/

 /* @Override
  protected void hideRecordButton() {
    recordButton.setVisible(false);
    hideWaiting();
  }*/

  @Override
  public void flip(boolean first) {
  }

  @Override
  public void stopRecording(long duration) {
    super.stopRecording(duration);
    if (duration > MIN_DURATION) {
      hideRecord();
      showWaiting();
    }
  }

  private void hideRecord() {
    recordButton.setVisible(false);
  }
  private void showRecord() {
    recordButton.setVisible(true);
  }

  private void showWaiting() {
    waiting.setVisible(true);
  }
}
