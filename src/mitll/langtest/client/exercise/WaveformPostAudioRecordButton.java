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

package mitll.langtest.client.exercise;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.exercise.Shell;

/**
* Tells playAudioPanel to be enabled/disabled in response to recording states
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/4/13
* Time: 11:46 PM
* To change this template use File | Settings | File Templates.
*/
public class WaveformPostAudioRecordButton extends PostAudioRecordButton {
  private static final String RECORD_BUTTON = "RecordButton";
  private final RecordAudioPanel recordAudioPanel;
  private PlayAudioPanel playAudioPanel;
  private final Panel parentPanel;
  private final String audioType;

  /**
   * @see RecordAudioPanel#makePostAudioRecordButton(String, String)
   * @see mitll.langtest.client.custom.dialog.NewUserExercise.CreateFirstRecordAudioPanel#makePostAudioRecordButton(String, String)
   * @param exerciseID
   * @param controller
   * @param widgets
   * @param recordAudioPanel
   * @param service
   * @param index
   * @param recordInResults
   * @param playButtonSuffix
   * @param stopButtonText
   */
  protected WaveformPostAudioRecordButton(String exerciseID,
                                       ExerciseController controller,
                                       Panel widgets,
                                       RecordAudioPanel recordAudioPanel, LangTestDatabaseAsync service, int index,
                                       boolean recordInResults, String playButtonSuffix, String stopButtonText, String audioType) {
    super(exerciseID, controller, service, index, recordInResults, playButtonSuffix, stopButtonText);
    this.recordAudioPanel = recordAudioPanel;
    this.parentPanel = widgets;
    getElement().setId("WaveformPostAudioRecordButton_" + index);
    this.audioType = audioType;
    addStyleName("minWidthRecordButton");
  }

  /**
   * So when we're recording reference audio for an item, we want to add the audio to the audio table and not
   * the results table.
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @see #postAudioFile
   */
  @Override
  protected boolean shouldAddToAudioTable() {
    return true;
  }

  private long then;
  @Override
  public void startRecording() {
    if (parentPanel instanceof BusyPanel) {
      ((BusyPanel) parentPanel).setBusy(true);
    }
    controller.logEvent(this, RECORD_BUTTON, getExerciseID(), "startRecording");
    then = System.currentTimeMillis();
    super.startRecording();
    setPlayEnabled(false);
  }

  @Override
  public void flip(boolean first) {} // force not to be abstract

  /**
   * @see mitll.langtest.client.recorder.RecordButton#stop()
   */
  @Override
  public void stopRecording() {
    if (parentPanel instanceof BusyPanel) {
      ((BusyPanel) parentPanel).setBusy(false);
    }
    controller.logEvent(this, RECORD_BUTTON, getExerciseID(), "stopRecording, duration " + (System.currentTimeMillis() - then) + " millis");

    recordAudioPanel.getWaveform().setUrl(LangTest.LANGTEST_IMAGES + "animated_progress.gif");

    super.stopRecording();
  }

  /**
   * @see #postAudioFile(String)
   * @return
   */
  @Override
  protected String getAudioType() {
    return audioType;
  }

  /**
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @param result
   */
  @Override
  public void useResult(AudioAnswer result) {
    recordAudioPanel.getImagesForPath(result.getPath());
    if (parentPanel instanceof ExercisePanel) {
      ((ExercisePanel) parentPanel).recordCompleted(recordAudioPanel);
    }
    setPlayEnabled(true);
  }

  @Override
  protected void useInvalidResult(AudioAnswer result) {
    super.useInvalidResult(result);

//    System.out.println("WaveformPostAudioRecordButton : " + getElement().getId() + " : got invalid result " +result);
    recordAudioPanel.getWaveform().setVisible(false);
    recordAudioPanel.getSpectrogram().setVisible(false);
    if (parentPanel instanceof ExercisePanel) {
      ((ExercisePanel) parentPanel).recordIncomplete(recordAudioPanel);
    }
  //  controller.logEvent(recordAudioPanel.getButton(), "recordButton", getExercise().getID(), "invalid recording " + result.getValidity());

    setPlayEnabled(false);
  }

  private void setPlayEnabled(boolean val) {
    //System.out.println("setPlayEnabled -- " + getElement().getId() + " : valid audio ? " + hasValidAudio() + " enable " + val);
    playAudioPanel.setEnabled(val && hasValidAudio());
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel(com.google.gwt.user.client.ui.Widget, String)
   * @param playAudioPanel
   */
  public void setPlayAudioPanel(PlayAudioPanel playAudioPanel) {
    this.playAudioPanel = playAudioPanel;
  }
}
