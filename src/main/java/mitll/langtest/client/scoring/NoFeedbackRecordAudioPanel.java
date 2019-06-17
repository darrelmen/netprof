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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.exercise.ScoredExercise;

import java.util.logging.Logger;

import static mitll.langtest.client.scoring.TwoColumnExercisePanel.CONTEXT_INDENT;

public abstract class NoFeedbackRecordAudioPanel<T extends HasID & ScoredExercise> extends DivWidget
    implements RecordingAudioListener {
  private final Logger logger = Logger.getLogger("NoFeedbackRecordAudioPanel");

  final ExerciseController controller;
  final T exercise;
  private PostAudioRecordButton postAudioRecordButton;
  private RecorderPlayAudioPanel playAudioPanel;
  DivWidget recordFeedback;
  DivWidget scoreFeedback;
  /**
   * @see #getDeviceValue
   */
  private final SessionManager sessionManager;

  private static final boolean DEBUG = false;

  /**
   * @param exercise
   * @param controller
   * @see RefAudioGetter#addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  NoFeedbackRecordAudioPanel(T exercise, ExerciseController controller, SessionManager sessionManager) {
    this.exercise = exercise;
    this.controller = controller;
    this.sessionManager = sessionManager;
    // getElement().setId("NoFeedbackRecordAudioPanel_" + exercise.getID());
  }

  /**
   * @see RecordDialogExercisePanel#addWidgets
   */
  public void addWidgets() {
    long then = System.currentTimeMillis();
    DivWidget col = new DivWidget();
    col.add(scoreFeedback = new DivWidget());
    col.getElement().setId("scoreFeedbackContainer");
    scoreFeedback.getElement().setId("scoreFeedback_" + getExID());

    // add record feedback
    {
      recordFeedback = makePlayAudioPanel().getRecordFeedback(null);
      Style style = recordFeedback.getElement().getStyle();
      style.setMarginTop(7, Style.Unit.PX);
      style.setProperty("minWidth", CONTEXT_INDENT + "px");

      scoreFeedback.add(recordFeedback);
    }

    add(col);

    long now = System.currentTimeMillis();

    if (DEBUG) {
      logger.info("addWidgets " + (now - then) + " millis, has " + getWidgetCount() + " widgets...");
    }
    //scoreFeedback.getElement().setId("scoreFeedbackRow");
  }

  public int getExID() {
    return exercise.getID();
  }

  /**
   * So here we're trying to make the record and play buttons know about each other
   * to the extent that when we're recording, we can't play audio, and when we're playing
   * audio, we can't record. We also mark the widget as busy so we can't move on to a different exercise.
   *
   * @return
   * @see #addWidgets
   * @see AudioPanel#getPlayButtons
   */
  RecorderPlayAudioPanel makePlayAudioPanel() {
    // long then = System.currentTimeMillis();
    NoFeedbackRecordAudioPanel outer = this;
    postAudioRecordButton = new FeedbackPostAudioRecordButton(getExID(), this, controller) {
      /**
       * @see PostAudioRecordButton#startRecording()
       * @return
       */
      @Override
      protected String getDevice() {
        // logger.info("no feedback device");
        return getDeviceValue();
      }

      @Override
      protected void onAttach() {
        int tabIndex = getTabIndex();
        super.onAttach();

        if (-1 == tabIndex) {
          setTabIndex(-1);
        }
      }

      @Override
      protected Widget getPopupTargetWidget() {
//        logger.info("getPopupTargetWidget target is " + outer.getElement().getId());
        return scoreFeedback;
      }

      public void showPopupLater(String toShow) {
        //  logger.info("showPopupLater target is " + outer.getElement().getId());
        super.showPopupDismissLater(toShow);
        showInvalidResultPopup(toShow);
      }

      @Override
      protected boolean shouldAddToAudioTable() {
        return outer.shouldAddToAudioTable();
      }

      @Override
      protected boolean shouldUseRecordingStopDelay() {
        return outer.shouldUseRecordingStopDelay();
      }

      @Override
      protected AudioType getAudioType() {
        //   AudioType audioType = outer.getAudioType();
        //   logger.info("getAudioType ex " + exercise.getID() + " = " + audioType);
        return outer.getAudioType();
      }
    };
    postAudioRecordButton.addStyleName("leftFiveMargin");
    postAudioRecordButton.setVisible(controller.getProjectStartupInfo().isHasModel());

    playAudioPanel = new RecorderPlayAudioPanel(postAudioRecordButton, controller, exercise, useMicrophoneIcon());

    playAudioPanel.hidePlayButton();
//
//    if (DEBUG) {
//      long now = System.currentTimeMillis();
//      logger.info("makePlayAudioPanel : took " + (now - then) + " for makeAudioPanel");
//    }

    return playAudioPanel;
  }

  public void showInvalidResultPopup(String message) {
  }

  protected boolean shouldUseRecordingStopDelay() {
    return true;
  }

  protected boolean shouldAddToAudioTable() {
    return false;
  }

  protected AudioType getAudioType() {
    return AudioType.LEARN;
  }

  protected boolean useMicrophoneIcon() {
    return false;
  }

  Widget getPopupTargetWidget() {
    //  logger.info("getPopupTargetWidget " + this.getId());
    return postAudioRecordButton;
  }

  private String getDeviceValue() {
    String session = sessionManager.getSession();
    return session == null ? controller.getBrowserInfo() : session;
  }

  /**
   * @return
   * @see TwoColumnExercisePanel#makeFirstRow
   */
  public PostAudioRecordButton getPostAudioRecordButton() {
    return postAudioRecordButton;
  }

  /**
   * @see SimpleRecordAudioPanel#startRecording
   */
  @Override
  public void startRecording() {
    if (DEBUG) logger.info("startRecording...");
    setVisible(true);
    playAudioPanel.setEnabled(false);
    playAudioPanel.showFirstRecord();

    clearScoreFeedback();
  }

  void clearScoreFeedback() {
    scoreFeedback.clear();
    scoreFeedback.add(recordFeedback);
  }

  /**
   * @see RecordButton.RecordingListener#stopRecording(long, boolean)
   */
  @Override
  public void stopRecording() {
    if (DEBUG) logger.info("stopRecording on " + getExID());
    playAudioPanel.setEnabled(true);
    playAudioPanel.hideRecord();
  }

  /**
   * @see RecordDialogExercisePanel#cancelRecording()
   */
  public void cancelRecording() {
    postAudioRecordButton.cancelRecording();
  }

  public void gotShortDurationRecording() {
    if (DEBUG) logger.info("gotShortDurationRecording");
    playAudioPanel.hideRecord();
    setVisible(true);
  }

  /**
   * @param result IGNORED HERE
   * @see FeedbackPostAudioRecordButton#useResult
   */
  @Override
  public void useResult(AudioAnswer result) {
    //logger.info("useScoredResult " + result);
    setVisible(true);
    playAudioPanel.showPlayButton();
  }

  @Override
  public void usePartial(StreamResponse validity) {
  }

  @Override
  public void gotAbort() {
  }

  /**
   * @see FeedbackPostAudioRecordButton#onPostFailure()
   */
  @Override
  public void onPostFailure() {
    logger.info("onPostFailure...");
  }

  @Override
  public void useInvalidResult(int exid, boolean isValid) {
    //  logger.info("useInvalidResult " + isValid);
    if (!isValid) playAudioPanel.hidePlayButton();
    else playAudioPanel.showPlayButton();
    playAudioPanel.setEnabled(isValid);
  }

  /**
   * @return
   * @see SimpleRecordAudioPanel#addWidgets()
   */
  DivWidget getScoreFeedback() {
    return scoreFeedback;
  }

  public RecorderPlayAudioPanel getPlayAudioPanel() {
    return playAudioPanel;
  }
}
