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

import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.dialog.IRehearseView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.ClientExercise;

import java.util.logging.Logger;

class ContinuousDialogRecordAudioPanel extends NoFeedbackRecordAudioPanel<ClientExercise> {
  private final Logger logger = Logger.getLogger("ContinuousDialogRecordAudioPanel");

  private IRehearseView rehearseView;
  private IRecordResponseListener recordDialogTurn;
  private static final boolean DEBUG = false;

  ContinuousDialogRecordAudioPanel(ClientExercise exercise,
                                   ExerciseController controller,
                                   SessionManager sessionManager,
                                   IRehearseView rehearseView,
                                   IRecordResponseListener recordDialogTurn) {
    super(exercise, controller, sessionManager);
    this.rehearseView = rehearseView;
    this.recordDialogTurn = recordDialogTurn;
  }

  @Override
  protected boolean useMicrophoneIcon() {
    return true;
  }

  /**
   * SO in an async world, this result may not be for this exercise panel!
   *
   * @param result
   * @see PostAudioRecordButton#onPostSuccess(AudioAnswer, long)
   * @see FeedbackPostAudioRecordButton#useResult
   */
  @Override
  public void useResult(AudioAnswer result) {
    super.useResult(result);
    rehearseView.useResult(result);

    if (DEBUG) {
      logger.info("useResult got for ex " + result.getExid() + " vs local " + exercise.getID() +
          " = " + result.getValidity() + " " + result.getPretestScore());
    }
    // logger.info("useResult got words " + result.getPretestScore().getWordScores());
  }

  @Override
  Widget getPopupTargetWidget() {
    Widget widget = recordDialogTurn.myGetPopupTargetWidget();
    logger.info("getPopupTargetWidget " + widget.getElement().getId());

    return widget;
  }

  /**
   * @param response
   * @see PostAudioRecordButton#usePartial
   */
  @Override
  public void usePartial(StreamResponse response) {
    recordDialogTurn.usePartial(response);
  }

  /**
   *
   */
  @Override
  public void onPostFailure() {
    logger.info("onPostFailure exid " + exercise.getID());
    stopRecording();
  }

  /**
   * TODO : do something smarter here on invalid state????
   *
   * @param exid
   * @param isValid
   * @see PostAudioRecordButton#useInvalidResult
   */
  @Override
  public void useInvalidResult(int exid, boolean isValid) {
    super.useInvalidResult(exid, isValid);
    rehearseView.useInvalidResult(exid);
    //getPostAudioRecordButton().setVisible(false);
    logger.info("useInvalidResult got valid = " + isValid);
  }

  /**
   * @see RecordButton.RecordingListener#stopRecording(long, boolean)
   */
  @Override
  public void stopRecording() {
    // logger.info("stopRecording for " + exercise.getID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage());
    super.stopRecording();
    rehearseView.stopRecording();
  }

  @Override
  public int getDialogSessionID() {
    return rehearseView.getDialogSessionID();
  }
}
