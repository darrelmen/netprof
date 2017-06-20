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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.initial.PopupHelper;
import mitll.langtest.client.initial.WavCallback;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.services.LangTestDatabaseAsync;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Just for dialog support -- {@link mitll.langtest.client.contextPractice.DialogWindow}
 * This binds a record button with the act of posting recorded audio to the server.
 * <p/>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/18/12
 * Time: 6:51 PM
 * To change this template use File | Settings | File Templates.
 * <p>
 * TODO : make PostAudioRecordButton extend this class.
 */
public abstract class SimplePostAudioRecordButton extends RecordButton implements RecordButton.RecordingListener {
  private Logger logger = Logger.getLogger("SimplePostAudioRecordButton");

  private static final String RELEASE_TO_STOP = "Release";

  private static final int LOG_ROUNDTRIP_THRESHOLD = 3000;
  private int reqid = 0;
  private final ExerciseController controller;
  private final LangTestDatabaseAsync service;
  private final String textToAlign;
  private final String transliteration;
  private final String identifier;
  protected AudioAnswer lastResult;

  /**
   * Make a record button that returns the alignment and score.
   *
   * @param controller  needed for recording
   * @param service     to post the audio to
   * @param textToAlign to align the audio to
   */
  protected SimplePostAudioRecordButton(final ExerciseController controller, LangTestDatabaseAsync service,
                                     String textToAlign, String transliteration) {
    this(controller, service, textToAlign, transliteration, "item");
  }

  /**
   * Make a record button that returns the alignment and score.
   *
   * @param controller  needed for recording
   * @param service     to post the audio to
   * @param textToAlign to align the audio to
   * @param identifier  optional, but if you want to associate the audio with an item "e.g. Dialog Item #3".
   */
  private SimplePostAudioRecordButton(final ExerciseController controller, LangTestDatabaseAsync service,
                                      String textToAlign, String transliteration, String identifier) {
    this(controller, service, "Record", RELEASE_TO_STOP, textToAlign, transliteration, identifier);
  }

  /**
   * @param controller
   * @param service
   * @param recordButtonTitle
   * @param stopButtonTitle
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.ASRRecordAudioPanel.MyPostAudioRecordButton
   */
  private SimplePostAudioRecordButton(final ExerciseController controller, LangTestDatabaseAsync service,
                                      String recordButtonTitle, String stopButtonTitle, String textToAlign, String transliteration, String identifier) {
    super(controller.getRecordTimeout(), true, recordButtonTitle, stopButtonTitle, controller.getProps());
    setRecordingListener(this);
    this.controller = controller;
    this.service = service;
    this.textToAlign = textToAlign;
    this.transliteration = transliteration;
    this.identifier = identifier;
    getElement().setId("PostAudioRecordButton");
    getElement().getStyle().setMarginTop(1, Style.Unit.PX);
    getElement().getStyle().setMarginBottom(1, Style.Unit.PX);
  }

  /**
   * @param duration
   * @see RecordButton#stop(long)
   */
  public boolean stopRecording(long duration) {
    controller.stopRecording(new WavCallback() {
      @Override
      public void getBase64EncodedWavFile(String bytes) {
        postAudioFile(bytes);
      }
    });
    return true;
  }

  private void postAudioFile(String base64EncodedWavFile) {
    reqid++;

    controller.getScoringService().getAlignment(base64EncodedWavFile,
        textToAlign,
        transliteration,
        identifier,
        reqid,
        controller.getBrowserInfo(), getAlignmentCallback());
  }

  private AsyncCallback<AudioAnswer> getAlignmentCallback() {
    final long then = System.currentTimeMillis();

    return new AsyncCallback<AudioAnswer>() {
      public void onFailure(Throwable caught) {
        long now = System.currentTimeMillis();
        logger.info("PostAudioRecordButton : (failure) posting audio took " + (now - then) + " millis");

        logMessage("failed to post audio for " + controller.getUser());// + " exercise " + exercise.getOldID());
        showPopup(AudioAnswer.Validity.INVALID.getPrompt());
      }

      /**
       * Feedback for when audio isn't valid for some reason.
       * @param toShow
       */
      private void showPopup(String toShow) {
        new PopupHelper().showPopup(toShow, getOuter(), 3000);
      }

      public void onSuccess(AudioAnswer result) {
        long now = System.currentTimeMillis();
        long roundtrip = now - then;

        logger.info("PostAudioRecordButton : Got audio answer " + result);
        if (result.getReqid() != reqid) {
          logger.info("ignoring old response " + result);
          return;
        }
        if (result.getValidity() == AudioAnswer.Validity.OK) {
          //      validAudio = true;
          useResult(result);
        } else {
          //    validAudio = false;
          //  new Exception().printStackTrace();

          showPopup(result.getValidity().getPrompt());
          useInvalidResult(result);
        }
        if (controller.isLogClientMessages() || roundtrip > LOG_ROUNDTRIP_THRESHOLD) {
          logRoundtripTime(result, roundtrip);
        }
      }
    };
  }

  private Widget getOuter() {
    return this;
  }

  private void logRoundtripTime(AudioAnswer result, long roundtrip) {
    String message = "PostAudioRecordButton : (success) User #" + controller.getUser() +
        " post audio took " + roundtrip + " millis, audio dur " +
        result.getDurationInMillis() + " millis, " +
        " " + ((float) roundtrip / (float) result.getDurationInMillis()) + " roundtrip/audio duration ratio.";
    logMessage(message);
  }

  private void logMessage(String message) {
    service.logMessage(message, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  public void startRecording() {
    controller.startRecording();
  }

  protected void useInvalidResult(AudioAnswer result) {
    controller.logEvent(this, "recordButton", "N/A", "invalid recording " + result.getValidity());
  }

  public abstract void useResult(AudioAnswer result);

  public HorizontalPanel getSentColors(String sentToColor) {
    HorizontalPanel colorfulSent = new HorizontalPanel();
    if (null == lastResult.getPretestScore()) {
      HTML bad = new HTML("no result available");
      bad.getElement().getStyle().setBackgroundColor("#000000");
      bad.getElement().getStyle().setProperty("color", "#FFFFFF");
      bad.getElement().getStyle().setProperty("margin", "5px 10px");
      bad.getElement().getStyle().setProperty("fontSize", "130%");
      colorfulSent.add(bad);
      return colorfulSent;
    }
    List<TranscriptSegment> ts = lastResult.getPretestScore().getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
    String[] words = sentToColor.replaceAll("-", " ").split("\\s+");
    int wordIndex = 0;
    for (TranscriptSegment wordInfo : ts) {
      if (wordInfo.getEvent().contains("<"))
        continue;
      HTML word = new HTML(words[wordIndex] + " ");
      word.getElement().getStyle().setProperty("fontSize", "130%");
      word.getElement().getStyle().setProperty("marginLeft", "2px");
      word.getElement().getStyle().setBackgroundColor(SimpleColumnChart.getColor(wordInfo.getScore()));
      colorfulSent.add(word);
      wordIndex += 1;
    }
    colorfulSent.getElement().getStyle().setProperty("margin", "5px 10px");
    return colorfulSent;
  }

  public Map<String, Float> getPhoneScores() {
    if (null == lastResult.getPretestScore())
      return new HashMap<String, Float>();
    return lastResult.getPretestScore().getPhoneScores();
  }

  public DivWidget getScoreBar(float score) {
    int iscore = (int) (100f * score);
    final int HEIGHT = 18;
    DivWidget bar = new DivWidget();
    TooltipHelper tooltipHelper = new TooltipHelper();
    bar.setWidth(iscore + "px");
    bar.setHeight(HEIGHT + "px");
    bar.getElement().getStyle().setBackgroundColor(SimpleColumnChart.getColor(score));
    bar.getElement().getStyle().setMarginTop(2, Style.Unit.PX);

    tooltipHelper.createAddTooltip(bar, "Score " + score + "%", Placement.BOTTOM);
    return bar;
  }
}
