/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.recorder;

import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.flashcard.MyCustomIconType;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.shared.answer.AudioType;

import java.util.logging.Logger;

import static mitll.langtest.shared.answer.AudioType.PRACTICE;

public abstract class FlashcardRecordButton extends PostAudioRecordButton implements KeyPressDelegate {
  private final Logger logger = Logger.getLogger("FlashcardRecordButton");

  /**
   * @see #initRecordButton
   */
  private static final String PROMPT = "Press and hold to record";
  private static final int WIDTH_FOR_BUTTON = 360;

  private final ExerciseController controller;
 // private static int count = 0;
  private final RecordButton.RecordingListener outerRecordingListener;
  private final RecordingKeyPressHelper helper;

  /**
   * @param addKeyBinding
   * @param controller
   * @paramx delay
   * @paramx recordingListener
   * @paramx instance
   * @see mitll.langtest.client.flashcard.FlashcardRecordButtonPanel#makeRecordButton
   */
  public FlashcardRecordButton(int exerciseID,
                               final ExerciseController controller,
                               RecordButton.RecordingListener outerRecordingListener,
                               boolean addKeyBinding) {
    super(exerciseID, controller, "", "", WIDTH_FOR_BUTTON);
//    int id = count++;
//    String name = "FlashcardRecordButton_";

    this.outerRecordingListener = outerRecordingListener;
    helper = makeKeyPressHelper();
    if (addKeyBinding) {
      helper.addKeyListener(controller);
      // logger.info("FlashcardRecordButton : " + instance + " key is  " + listener.getName());
    }
    this.controller = controller;

//    setWidth(WIDTH_FOR_BUTTON + "px");
    setHeight("48px");
    Style style = getElement().getStyle();
    style.setProperty("fontSize", "x-large");
    style.setProperty("fontFamily", "Arial Unicode MS, Arial, sans-serif");

    style.setVerticalAlign(Style.VerticalAlign.MIDDLE);
    style.setLineHeight(37, Style.Unit.PX);

    initRecordButton();
    // getElement().setId("FlashcardRecordButton_" + instance + "_" + id);
  }

  protected RecordingKeyPressHelper makeKeyPressHelper() {
    return new RecordingKeyPressHelper(this, this, controller);
  }

  @Override
  protected int getDialogSessionID() {
    return -1;
  }

  @Override
  protected AudioType getAudioType() {
    return PRACTICE;
  }

  @Override
  public void startRecording() {
    super.startRecording();
    outerRecordingListener.startRecording();
  }

  @Override
  public boolean stopRecording(long duration, boolean abort) {
    boolean b = super.stopRecording(duration, abort);
    outerRecordingListener.stopRecording(duration, abort);
    return b;
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    helper.removeListener();

    if (isRecording()) {
      logger.info("stop recording since detach!");
      stop(MIN_DURATION + 1, true);
    }
  }

  @Override
  protected void onUnload() {
    super.onUnload();

    //  logger.info("onUnload ---> ");
    helper.removeListener();
    stopRecordingSafe();
  }


  public abstract void gotRightArrow();

  public abstract void gotLeftArrow();

  public abstract void gotUpArrow();

  public abstract void gotDownArrow();

  public abstract void gotEnter();

  @Override
  public void gotSpaceBar() {
    if (!mouseDown) {
      mouseDown = true;
      doClick(null);
    }
  }

  @Override
  public void gotSpaceBarKeyUp() {
    if (!mouseDown) {
      logger.warning("huh? mouse down = false");
    } else {
      mouseDown = false;
      doClick(null);
    }
  }

  protected void showInitialRecordImage() {
    setBaseIcon(MyCustomIconType.record1);
    setText("");
  }

  protected void hideBothRecordImages() {
    initRecordButton();
    removeImage();
    setIcon(IconType.MICROPHONE);
  }

  public void initRecordButton() {
    super.initRecordButton();
    setText("");
    setType(ButtonType.DANGER);
  }

  protected String getPrompt() {
    return PROMPT;
  }
}
