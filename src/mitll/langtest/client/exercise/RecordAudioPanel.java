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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Image;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.scoring.AudioPanel;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.AudioRefExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Shell;

import java.util.Map;

/**
 * A waveform record button and a play audio button.
 *
 * The record audio and play buttons are tied to each other in that when playing audio, you can't record, and vice-versa.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
public class RecordAudioPanel<T extends Shell & AudioRefExercise> extends AudioPanel<Shell> {
  private final int index;

  private PostAudioRecordButton postAudioRecordButton;
  private PlayAudioPanel playAudioPanel;
  protected final Panel exercisePanel;

  private final Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
  private final Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
  protected T exercise;
  protected String audioType;

  /**
   *
   * @param exercise
   * @param controller
   * @param service
   * @param index
   * @param showSpectrogram
   * @param audioType
   * @param instance
   * @see mitll.langtest.client.custom.dialog.NewUserExercise.CreateFirstRecordAudioPanel#CreateFirstRecordAudioPanel(CommonExercise, Panel, boolean, String)
   * @see mitll.langtest.client.exercise.WaveformExercisePanel#getAnswerWidget(mitll.langtest.shared.exercise.CommonExercise, mitll.langtest.client.LangTestDatabaseAsync, ExerciseController, int)
   */
  public RecordAudioPanel(T exercise, ExerciseController controller, Panel widgets,
                          LangTestDatabaseAsync service, int index, boolean showSpectrogram, String audioType, String instance) {
    super(service,
      // use full screen width
      // use keyboard
      controller, showSpectrogram,
      null // no gauge panel
      , 1.0f, 23, exercise.getID(), exercise, instance);
    this.exercisePanel = widgets;
    this.index = index;
    this.exercise = exercise;
    this.audioType = audioType;
    AudioAttribute attribute = getAudioAttribute();
/*    System.out.println("RecordAudioPanel for " + exercise.getID() +
      " audio type " + audioType + " ref " + exercise.getRefAudio() + " path " + attribute);
  */
    if (attribute != null) {
      this.audioPath = attribute.getAudioRef();
    }

    addWidgets("", audioType, getRecordButtonTitle());
    getElement().setId("RecordAudioPanel_" + exerciseID + "_" + index + "_" + audioType);
  }

  /**
   * Worries about context type audio too.
   * @return
   * @see #RecordAudioPanel(Shell, ExerciseController, Panel, LangTestDatabaseAsync, int, boolean, String, String)
   */
  public AudioAttribute getAudioAttribute() {
    AudioAttribute audioAttribute = audioType.equals(Result.AUDIO_TYPE_REGULAR) ? exercise.getRecordingsBy(controller.getUser(), true) :
        audioType.equals(Result.AUDIO_TYPE_SLOW) ? exercise.getRecordingsBy(controller.getUser(), false) : null;

    if (audioType.startsWith("context")) {
      for (AudioAttribute audioAttribute1 : exercise.getAudioAttributes()) {
        Map<String, String> attributes = audioAttribute1.getAttributes();
        if (attributes.containsKey("context") && audioAttribute1.getUserid() == controller.getUser()) {
          return audioAttribute1;
        }
      }
      return null;
    }
    else {
      return audioAttribute;
    }
  }

  private String getRecordButtonTitle() {
    return
      audioType.equals(Result.AUDIO_TYPE_REGULAR) ? "Record regular"
        :
        audioType.equals(Result.AUDIO_TYPE_SLOW)    ? "Record slow"  : "Record";
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getPlayButtons
   * @param toTheRightWidget
   * @param buttonTitle
   * @param recordButtonTitle
   * @return
   */
  @Override
  protected PlayAudioPanel makePlayAudioPanel(Widget toTheRightWidget, String buttonTitle, String audioType, String recordButtonTitle) {
    WaveformPostAudioRecordButton myPostAudioRecordButton = makePostAudioRecordButton(audioType, recordButtonTitle);
    postAudioRecordButton = myPostAudioRecordButton;

   // System.out.println("makePlayAudioPanel : audio type " + audioType + " suffix '" +playButtonSuffix +"'");
    playAudioPanel = new MyPlayAudioPanel(recordImage1, recordImage2, exercisePanel, buttonTitle, toTheRightWidget);
    myPostAudioRecordButton.setPlayAudioPanel(playAudioPanel);

    return playAudioPanel;
  }

  public void clickStop() {
    postAudioRecordButton.clickStop();
  }

  public boolean isRecording() {
    return postAudioRecordButton.isRecording();
  }

  /**
   * @see #makePlayAudioPanel(com.google.gwt.user.client.ui.Widget, String, String, String)
   * @param audioType
   * @param recordButtonTitle
   * @return
   */
  protected WaveformPostAudioRecordButton makePostAudioRecordButton(String audioType, String recordButtonTitle) {
    return new MyWaveformPostAudioRecordButton(audioType, recordButtonTitle);
  }

  protected void showStop() {
    recordImage1.setVisible(false);
    recordImage2.setVisible(false);
  }

  protected void showStart() {
    recordImage1.setVisible(true);
  }

  protected void flipRecordImages(boolean first) {
    recordImage1.setVisible(first);
    recordImage2.setVisible(!first);
  }

  public Button getButton() { return postAudioRecordButton; }

  public void setEnabled(boolean val) {
    //System.out.println("RecordAudioPanel.setEnabled " + val);
    postAudioRecordButton.setEnabled(val);
    if (postAudioRecordButton.hasValidAudio()) playAudioPanel.setEnabled(val);
  }

  public void setExercise(T exercise) {
    this.exercise = exercise;
    postAudioRecordButton.setExercise(exercise.getID());
  }

  /**
   * A play button that controls the state of the record button.
   */
  private class MyPlayAudioPanel extends PlayAudioPanel {
    public MyPlayAudioPanel(Image recordImage1, Image recordImage2, final Panel panel, String suffix, Widget toTheRightWidget) {
      super(RecordAudioPanel.this.soundManager, new PlayListener() {
        public void playStarted() {
          if (panel instanceof BusyPanel) {
            ((BusyPanel)panel).setBusy(true);
          }
          postAudioRecordButton.setEnabled(false);
        }

        public void playStopped() {
          if (panel instanceof BusyPanel) {
            ((BusyPanel)panel).setBusy(false);
          }
          postAudioRecordButton.setEnabled(true);
        }
      }, suffix, toTheRightWidget);
      add(recordImage1);
      recordImage1.setVisible(false);
      add(recordImage2);
      recordImage2.setVisible(false);
      getElement().setId("MyPlayAudioPanel");
    }

    /**
     * @see mitll.langtest.client.sound.PlayAudioPanel#PlayAudioPanel(mitll.langtest.client.sound.SoundManagerAPI, String, com.google.gwt.user.client.ui.Widget)
     * @param optionalToTheRight
     */
    @Override
    protected void addButtons(Widget optionalToTheRight) {
      if (postAudioRecordButton == null) System.err.println("huh? postAudioRecordButton is null???");
      else add(postAudioRecordButton);
      super.addButtons(optionalToTheRight);
    }
  }

  protected class MyWaveformPostAudioRecordButton extends WaveformPostAudioRecordButton {
   // private long then,now;

    /**
     * @see #makePostAudioRecordButton(String, String)
     * @param audioType
     * @param recordButtonTitle
     */
    public MyWaveformPostAudioRecordButton(String audioType, String recordButtonTitle) {
      super(RecordAudioPanel.this.exercise.getID(),
        RecordAudioPanel.this.controller,
        RecordAudioPanel.this.exercisePanel,
        RecordAudioPanel.this, RecordAudioPanel.this.service, RecordAudioPanel.this.index, true, recordButtonTitle, RecordButton.STOP1, audioType);
      setWidth("110px");
    }

    /**
     * @see mitll.langtest.client.recorder.RecordButton#start()
     */
    @Override
    public void startRecording() {
     /// then = System.currentTimeMillis();
      super.startRecording();
      showStart();
    }

    @Override
    public void stopRecording() {
    //  now = System.currentTimeMillis();
     // System.out.println("stopRecording " + now + " diff " + (now-then) + " millis");
      super.stopRecording();
      showStop();
    }

    @Override
    public void flip(boolean first) {
      flipRecordImages(first);
    }

    @Override
    public void useResult(AudioAnswer result) {
      super.useResult(result);
      if (result.isValid()) {
       // System.out.println("tell other tabs that audio has arrived!");
      }
    }
  }
}
