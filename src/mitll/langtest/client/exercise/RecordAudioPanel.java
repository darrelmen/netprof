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
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
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
import java.util.logging.Logger;

/**
 * A waveform record button and a play audio button.
 * <p>
 * The record audio and play buttons are tied to each other in that when playing audio, you can't record, and vice-versa.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class RecordAudioPanel<T extends Shell & AudioRefExercise> extends AudioPanel<Shell> {
  public static final String DYNAMIC_RANGE = "Dynamic Range";
  private final Logger logger = Logger.getLogger("RecordAudioPanel");
  private static final int HEIGHT_OF_RECORD_ROW = 58;

  private static final int MIN_VALID_DYNAMIC_RANGE = 32;
  private static final int MIN_GOOD_DYNAMIC_RANGE = 40;

  private final int index;

  private PostAudioRecordButton postAudioRecordButton;
  private PlayAudioPanel playAudioPanel;
  protected final Panel exercisePanel;

  private final ProgressBar progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT);
  private final HorizontalPanel afterPlayWidget = new HorizontalPanel();

  /**
   * @see #flipRecordImages
   */
  private final Image recordImage1 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-3_32x32.png"));
  private final Image recordImage2 = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "media-record-4_32x32.png"));
  protected T exercise;
  protected String audioType;

  /**
   * @param exercise
   * @param controller
   * @param service
   * @param index
   * @param showSpectrogram
   * @param audioType
   * @param instance
   * @see mitll.langtest.client.custom.dialog.NewUserExercise.CreateFirstRecordAudioPanel#CreateFirstRecordAudioPanel(CommonExercise, Panel, boolean, String)
   * @see mitll.langtest.client.exercise.WaveformExercisePanel#getAnswerWidget
   */
  public RecordAudioPanel(T exercise, ExerciseController controller, Panel widgets,
                          LangTestDatabaseAsync service, int index, boolean showSpectrogram,
                          String audioType, String instance) {
    super(service,
        controller, showSpectrogram,
        null // no gauge panel
        , 1.0f, 0, exercise.getID(), exercise, instance);
    this.exercisePanel = widgets;
    this.index = index;
    this.exercise = exercise;
    this.audioType = audioType;
    AudioAttribute attribute = getAudioAttribute();
    if (attribute != null) {
      this.audioPath = attribute.getAudioRef();
    }

    addWidgets("", audioType, getRecordButtonTitle());
    getElement().setId("RecordAudioPanel_" + exerciseID + "_" + index + "_" + audioType);
  }

  /**
   * Add dynamic range feedback to the right of the play button.
   *
   * @return
   * @see mitll.langtest.client.scoring.AudioPanel#addWidgets(String, String, String)
   */
  @Override
  protected Widget getAfterPlayWidget() {
    HTML w = new HTML(DYNAMIC_RANGE);
    w.addStyleName("leftTenMargin");
    w.addStyleName("topBarMargin");
    afterPlayWidget.add(w);
    afterPlayWidget.add(progressBar);

    afterPlayWidget.setVisible(false);
    progressBar.setWidth("300px");
    Style style = progressBar.getElement().getStyle();
    style.setMarginLeft(5, Style.Unit.PX);
    progressBar.addStyleName("topBarMargin");
    return afterPlayWidget;
  }

  /**
   * Worries about context type audio too.
   *
   * @return
   */
  public AudioAttribute getAudioAttribute() {
    AudioAttribute audioAttribute =
        audioType.equals(Result.AUDIO_TYPE_REGULAR) ?
            exercise.getRecordingsBy(controller.getUser(), true) :
            audioType.equals(Result.AUDIO_TYPE_SLOW) ?
                exercise.getRecordingsBy(controller.getUser(), false) : null;

    if (audioType.startsWith("context")) {
      for (AudioAttribute audioAttribute1 : exercise.getAudioAttributes()) {
        Map<String, String> attributes = audioAttribute1.getAttributes();
        if (attributes.containsKey("context") && audioAttribute1.getUserid() == controller.getUser()) {
          return audioAttribute1;
        }
      }
      return null;
    } else {
      return audioAttribute;
    }
  }

  private String getRecordButtonTitle() {
    return
        audioType.equals(Result.AUDIO_TYPE_REGULAR) ? "Record regular"
            :
            audioType.equals(Result.AUDIO_TYPE_SLOW) ? "Record slow" : "Record";
  }

  /**
   * @param toTheRightWidget
   * @param buttonTitle
   * @param recordButtonTitle
   * @return
   * @see mitll.langtest.client.scoring.AudioPanel#getPlayButtons
   */
  @Override
  protected PlayAudioPanel makePlayAudioPanel(Widget toTheRightWidget, String buttonTitle, String audioType, String recordButtonTitle) {
    WaveformPostAudioRecordButton myPostAudioRecordButton = makePostAudioRecordButton(audioType, recordButtonTitle);
    postAudioRecordButton = myPostAudioRecordButton;

    // logger.info("makePlayAudioPanel : audio type " + audioType + " suffix '" +playButtonSuffix +"'");
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
   * @param audioType
   * @param recordButtonTitle
   * @return
   * @see #makePlayAudioPanel(com.google.gwt.user.client.ui.Widget, String, String, String)
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

  /**
   * @param first
   * @see RecordAudioPanel.MyWaveformPostAudioRecordButton#flip
   */
  protected void flipRecordImages(boolean first) {
    recordImage1.setVisible(first);
    recordImage2.setVisible(!first);
  }

  public Button getButton() {
    return postAudioRecordButton;
  }

  public void setEnabled(boolean val) {
    //logger.info("RecordAudioPanel.setEnabled " + val);
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
      super(RecordAudioPanel.this.soundManager,
          new PlayListener() {
            public void playStarted() {
              if (panel instanceof BusyPanel) {
                ((BusyPanel) panel).setBusy(true);
              }
              postAudioRecordButton.setEnabled(false);
            }

            public void playStopped() {
              if (panel instanceof BusyPanel) {
                ((BusyPanel) panel).setBusy(false);
              }
              postAudioRecordButton.setEnabled(true);
            }

          }, suffix, toTheRightWidget);
      add(recordImage1);
      recordImage1.setVisible(false);
      add(recordImage2);
      recordImage2.setVisible(false);
      getElement().setId("MyPlayAudioPanel");
      setHeight(HEIGHT_OF_RECORD_ROW + "px");
    }

    /**
     * @param optionalToTheRight
     * @see mitll.langtest.client.sound.PlayAudioPanel#PlayAudioPanel(mitll.langtest.client.sound.SoundManagerAPI, String, com.google.gwt.user.client.ui.Widget)
     */
    @Override
    protected void addButtons(Widget optionalToTheRight) {
      if (postAudioRecordButton == null) logger.warning("huh? postAudioRecordButton is null???");
      else add(postAudioRecordButton);
      super.addButtons(optionalToTheRight);
    }
  }

  protected class MyWaveformPostAudioRecordButton extends WaveformPostAudioRecordButton {
    // private long then,now;
    /**
     * @param audioType
     * @param recordButtonTitle
     * @see #makePostAudioRecordButton(String, String)
     */
    protected MyWaveformPostAudioRecordButton(String audioType, String recordButtonTitle) {
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
      afterPlayWidget.setVisible(false);
    }

    @Override
    public void stopRecording(long duration) {
      //  now = System.currentTimeMillis();
      // logger.info("stopRecording " + now + " diff " + (now-then) + " millis");
      super.stopRecording(duration);
      showStop();
    }

    @Override
    public void flip(boolean first) {
      flipRecordImages(first);
    }

    /**
     * From Paul Gatewood:
     * <p>
     * Reasonable upper limit outside of an acoustic isolation room is 70dB Dynamic Range
     * <p>
     * I would put
     * 40-70dB in the Green
     * From (29for iOS and 32 for lptp) to 40 in the Yellow
     * Below the threshold is in the red.
     *
     * @param result
     * @see PostAudioRecordButton#postAudioFile
     */
    @Override
    public void useResult(AudioAnswer result) {
      super.useResult(result);
      showDynamicRange(result);
    }

    @Override
    protected void useInvalidResult(AudioAnswer result) {
      super.useInvalidResult(result);
      showDynamicRange(result);
    }

    /**
     * Set the value on the progress bar to reflect the dynamic range we measure on the audio.
     *
     * @param result
     * @see #useResult(AudioAnswer)
     */
    private void showDynamicRange(AudioAnswer result) {
      double dynamicRange = result.getDynamicRange();
      double percent = dynamicRange / 70;
      progressBar.setPercent(100 * percent);
      progressBar.setText("" + roundToTenth(dynamicRange));
      progressBar.setColor(dynamicRange > MIN_GOOD_DYNAMIC_RANGE ?
          ProgressBarBase.Color.SUCCESS : dynamicRange > MIN_VALID_DYNAMIC_RANGE ?
          ProgressBarBase.Color.WARNING :
          ProgressBarBase.Color.DANGER);
      afterPlayWidget.setVisible(true);
    }

    private float roundToTenth(double totalHours) {
      return ((float) ((Math.round(totalHours * 10d)))) / 10f;
    }
  }
}
